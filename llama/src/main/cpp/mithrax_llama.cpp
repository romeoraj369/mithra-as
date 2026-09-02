#include <android/log.h>
#include <atomic>
#include <jni.h>
#include <sstream>
#include <string>
#include <unistd.h>

#include "chat.h"
#include "common.h"
#include "llama.h"
#include "logging.h"
#include "sampling.h"

namespace {

constexpr int N_THREADS_MIN = 2;
constexpr int N_THREADS_MAX = 4;
constexpr int N_THREADS_HEADROOM = 2;
constexpr int DEFAULT_CONTEXT_SIZE = 4096;
constexpr int OVERFLOW_HEADROOM = 4;
constexpr int BATCH_SIZE = 512;
constexpr float DEFAULT_SAMPLER_TEMP = 0.2f;

constexpr const char *ROLE_SYSTEM = "system";
constexpr const char *ROLE_USER = "user";
constexpr const char *ROLE_ASSISTANT = "assistant";

llama_model *g_model = nullptr;
llama_context *g_context = nullptr;
llama_batch g_batch;
common_chat_templates_ptr g_chat_templates;
common_sampler *g_sampler = nullptr;

int g_context_size = DEFAULT_CONTEXT_SIZE;
std::atomic<bool> g_cancel_requested{false};

std::vector<common_chat_msg> chat_msgs;
llama_pos system_prompt_position = 0;
llama_pos current_position = 0;
llama_pos stop_generation_position = 0;
std::string cached_token_chars;
std::ostringstream assistant_ss;

void reset_chat_state(bool clear_kv_cache = true) {
    chat_msgs.clear();
    system_prompt_position = 0;
    current_position = 0;
    stop_generation_position = 0;
    cached_token_chars.clear();
    assistant_ss.str("");
    assistant_ss.clear();
    if (clear_kv_cache && g_context != nullptr) {
        llama_memory_clear(llama_get_memory(g_context), false);
    }
}

void shift_context() {
    const int n_discard = (current_position - system_prompt_position) / 2;
    LOGi("Discarding %d tokens from context", n_discard);
    llama_memory_seq_rm(llama_get_memory(g_context), 0, system_prompt_position,
                        system_prompt_position + n_discard);
    llama_memory_seq_add(llama_get_memory(g_context), 0,
                         system_prompt_position + n_discard, current_position, -n_discard);
    current_position -= n_discard;
}

std::string chat_add_and_format(const std::string &role, const std::string &content) {
    common_chat_msg new_msg;
    new_msg.role = role;
    new_msg.content = content;
    const bool add_generation_prompt = role == ROLE_USER;
    auto formatted = common_chat_format_single(
            g_chat_templates.get(), chat_msgs, new_msg, add_generation_prompt, false);
    chat_msgs.push_back(new_msg);
    return formatted;
}

int decode_tokens_in_batches(const llama_tokens &tokens,
                             const llama_pos start_pos,
                             const bool compute_last_logit = false) {
    for (int i = 0; i < static_cast<int>(tokens.size()); i += BATCH_SIZE) {
        if (g_cancel_requested.load()) {
            return 2;
        }
        const int cur_batch_size = std::min(static_cast<int>(tokens.size()) - i, BATCH_SIZE);
        common_batch_clear(g_batch);
        if (start_pos + i + cur_batch_size >= g_context_size - OVERFLOW_HEADROOM) {
            LOGw("Context full; shifting before decode");
            shift_context();
        }
        for (int j = 0; j < cur_batch_size; j++) {
            const llama_token token_id = tokens[i + j];
            const llama_pos position = start_pos + i + j;
            const bool want_logit = compute_last_logit && (i + j == static_cast<int>(tokens.size()) - 1);
            common_batch_add(g_batch, token_id, position, {0}, want_logit);
        }
        const int decode_result = llama_decode(g_context, g_batch);
        if (decode_result != 0) {
            LOGe("llama_decode failed with code %d", decode_result);
            return 1;
        }
    }
    return 0;
}

bool is_valid_utf8(const char *string) {
    if (!string) {
        return true;
    }
    const auto *bytes = reinterpret_cast<const unsigned char *>(string);
    int num;
    while (*bytes != 0x00) {
        if ((*bytes & 0x80) == 0x00) {
            num = 1;
        } else if ((*bytes & 0xE0) == 0xC0) {
            num = 2;
        } else if ((*bytes & 0xF0) == 0xE0) {
            num = 3;
        } else if ((*bytes & 0xF8) == 0xF0) {
            num = 4;
        } else {
            return false;
        }
        bytes += 1;
        for (int i = 1; i < num; ++i) {
            if ((*bytes & 0xC0) != 0x80) {
                return false;
            }
            bytes += 1;
        }
    }
    return true;
}

llama_context *create_context(llama_model *model, int n_ctx) {
    if (!model) {
        return nullptr;
    }
    const int n_threads = std::max(
            N_THREADS_MIN,
            std::min(N_THREADS_MAX, static_cast<int>(sysconf(_SC_NPROCESSORS_ONLN)) - N_THREADS_HEADROOM));
    LOGi("Using %d inference threads", n_threads);

    llama_context_params ctx_params = llama_context_default_params();
    const int trained_context_size = llama_model_n_ctx_train(model);
    if (n_ctx > trained_context_size) {
        LOGw("Requested context %d exceeds training context %d; clamping",
             n_ctx, trained_context_size);
        n_ctx = trained_context_size;
    }
    ctx_params.n_ctx = n_ctx;
    ctx_params.n_batch = BATCH_SIZE;
    ctx_params.n_ubatch = BATCH_SIZE;
    ctx_params.n_threads = n_threads;
    ctx_params.n_threads_batch = n_threads;
    return llama_init_from_model(model, ctx_params);
}

common_sampler *create_sampler() {
    common_params_sampling sparams;
    sparams.temp = DEFAULT_SAMPLER_TEMP;
    return common_sampler_init(g_model, sparams);
}

int process_system_prompt(const std::string &system_prompt) {
    reset_chat_state();
    std::string formatted_system_prompt = system_prompt;
    const bool has_chat_template = common_chat_templates_was_explicit(g_chat_templates.get());
    if (has_chat_template) {
        formatted_system_prompt = chat_add_and_format(ROLE_SYSTEM, system_prompt);
    }
    const auto system_tokens = common_tokenize(
            g_context, formatted_system_prompt, has_chat_template, has_chat_template);
    const int max_batch_size = g_context_size - OVERFLOW_HEADROOM;
    if (static_cast<int>(system_tokens.size()) > max_batch_size) {
        LOGe("System prompt too long: %d tokens", static_cast<int>(system_tokens.size()));
        return 1;
    }
    const int decode_result = decode_tokens_in_batches(system_tokens, current_position);
    if (decode_result != 0) {
        return decode_result;
    }
    system_prompt_position = current_position = static_cast<int>(system_tokens.size());
    return 0;
}

int process_user_prompt(const std::string &user_prompt, int n_predict) {
    stop_generation_position = 0;
    cached_token_chars.clear();
    assistant_ss.str("");
    assistant_ss.clear();

    std::string formatted_user_prompt = user_prompt;
    const bool has_chat_template = common_chat_templates_was_explicit(g_chat_templates.get());
    if (has_chat_template) {
        formatted_user_prompt = chat_add_and_format(ROLE_USER, user_prompt);
    }
    auto user_tokens = common_tokenize(
            g_context, formatted_user_prompt, has_chat_template, has_chat_template);
    const int max_batch_size = g_context_size - OVERFLOW_HEADROOM;
    if (static_cast<int>(user_tokens.size()) > max_batch_size) {
        const int skipped = static_cast<int>(user_tokens.size()) - max_batch_size;
        user_tokens.resize(max_batch_size);
        LOGw("User prompt truncated by %d tokens", skipped);
    }
    const int decode_result = decode_tokens_in_batches(user_tokens, current_position, true);
    if (decode_result != 0) {
        return decode_result;
    }
    current_position += static_cast<int>(user_tokens.size());
    stop_generation_position = current_position + n_predict;
    return 0;
}

int generate_tokens(std::string &out) {
    out.clear();
    while (!g_cancel_requested.load()) {
        if (current_position >= g_context_size - OVERFLOW_HEADROOM) {
            shift_context();
        }
        if (current_position >= stop_generation_position) {
            break;
        }
        const auto new_token_id = common_sampler_sample(g_sampler, g_context, -1);
        common_sampler_accept(g_sampler, new_token_id, true);
        common_batch_clear(g_batch);
        common_batch_add(g_batch, new_token_id, current_position, {0}, true);
        if (llama_decode(g_context, g_batch) != 0) {
            LOGe("llama_decode failed during generation");
            return 1;
        }
        current_position++;
        if (llama_vocab_is_eog(llama_model_get_vocab(g_model), new_token_id)) {
            break;
        }
        const auto new_token_chars = common_token_to_piece(g_context, new_token_id);
        cached_token_chars += new_token_chars;
        if (is_valid_utf8(cached_token_chars.c_str())) {
            assistant_ss << cached_token_chars;
            cached_token_chars.clear();
        }
    }
    out = assistant_ss.str();
    if (g_cancel_requested.load()) {
        return 2;
    }
    return 0;
}

void free_runtime_resources() {
    if (g_sampler != nullptr) {
        common_sampler_free(g_sampler);
        g_sampler = nullptr;
    }
    g_chat_templates.reset();
    if (g_batch.n_tokens > 0 || g_batch.token != nullptr) {
        llama_batch_free(g_batch);
        g_batch = llama_batch_init(BATCH_SIZE, 0, 1);
    }
    if (g_context != nullptr) {
        llama_free(g_context);
        g_context = nullptr;
    }
    if (g_model != nullptr) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
    reset_chat_state(false);
}

}  // namespace

extern "C" JNIEXPORT void JNICALL
Java_com_gamor_mithrax_llama_LlamaNative_initBackend(JNIEnv *env, jclass, jstring nativeLibDir) {
    llama_log_set(mithrax_android_log_callback, nullptr);
    const auto *path_to_backend = env->GetStringUTFChars(nativeLibDir, nullptr);
    LOGi("Loading GGML backends from %s", path_to_backend);
    ggml_backend_load_all_from_path(path_to_backend);
    env->ReleaseStringUTFChars(nativeLibDir, path_to_backend);
    llama_backend_init();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_gamor_mithrax_llama_LlamaNative_systemInfo(JNIEnv *env, jclass) {
    return env->NewStringUTF(llama_print_system_info());
}

extern "C" JNIEXPORT jint JNICALL
Java_com_gamor_mithrax_llama_LlamaNative_loadModel(JNIEnv *env, jclass, jstring jmodel_path, jint n_ctx) {
    g_cancel_requested.store(false);
    free_runtime_resources();

    const auto *model_path = env->GetStringUTFChars(jmodel_path, nullptr);
    LOGi("Loading model from %s", model_path);
    llama_model_params model_params = llama_model_default_params();
    g_model = llama_model_load_from_file(model_path, model_params);
    env->ReleaseStringUTFChars(jmodel_path, model_path);
    if (!g_model) {
        LOGe("Failed to load model");
        return 1;
    }

    g_context_size = n_ctx > 0 ? n_ctx : DEFAULT_CONTEXT_SIZE;
    g_context = create_context(g_model, g_context_size);
    if (!g_context) {
        LOGe("Failed to create llama context");
        free_runtime_resources();
        return 2;
    }

    g_batch = llama_batch_init(BATCH_SIZE, 0, 1);
    g_chat_templates = common_chat_templates_init(g_model, "");
    g_sampler = create_sampler();
    reset_chat_state();
    return 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_gamor_mithrax_llama_LlamaNative_complete(JNIEnv *env,
                                                  jclass,
                                                  jstring jsystem_prompt,
                                                  jstring juser_prompt,
                                                  jint max_tokens,
                                                  jobjectArray out_response) {
    if (!g_model || !g_context || !g_sampler) {
        LOGe("complete() called without a loaded model");
        return 3;
    }
    g_cancel_requested.store(false);

    const auto *system_prompt = env->GetStringUTFChars(jsystem_prompt, nullptr);
    const auto *user_prompt = env->GetStringUTFChars(juser_prompt, nullptr);
    std::string system_text(system_prompt);
    std::string user_text(user_prompt);
    env->ReleaseStringUTFChars(jsystem_prompt, system_prompt);
    env->ReleaseStringUTFChars(juser_prompt, user_prompt);

    int result = process_system_prompt(system_text);
    if (result != 0) {
        return result;
    }
    result = process_user_prompt(user_text, max_tokens > 0 ? max_tokens : 512);
    if (result != 0) {
        return result;
    }

    std::string response;
    result = generate_tokens(response);
    if (result == 2) {
        return 4;
    }
    if (result != 0) {
        return result;
    }

    jstring jresponse = env->NewStringUTF(response.c_str());
    env->SetObjectArrayElement(out_response, 0, jresponse);
    env->DeleteLocalRef(jresponse);
    return 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_gamor_mithrax_llama_LlamaNative_requestCancel(JNIEnv *, jclass) {
    g_cancel_requested.store(true);
}

extern "C" JNIEXPORT void JNICALL
Java_com_gamor_mithrax_llama_LlamaNative_unloadModel(JNIEnv *, jclass) {
    g_cancel_requested.store(true);
    free_runtime_resources();
}

extern "C" JNIEXPORT void JNICALL
Java_com_gamor_mithrax_llama_LlamaNative_shutdownBackend(JNIEnv *, jclass) {
    g_cancel_requested.store(true);
    free_runtime_resources();
    llama_backend_free();
}
