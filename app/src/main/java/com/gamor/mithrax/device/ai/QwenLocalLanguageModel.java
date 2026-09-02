package com.gamor.mithrax.device.ai;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gamor.mithrax.domain.ai.AnswerRequest;
import com.gamor.mithrax.domain.ai.AnswerResult;
import com.gamor.mithrax.domain.ai.LocalLanguageModel;
import com.gamor.mithrax.domain.ai.SummarizationRequest;
import com.gamor.mithrax.domain.ai.SummarizationResult;
import com.gamor.mithrax.domain.ask.GroundedContextAnswerer;
import com.gamor.mithrax.device.llama.LocalLlamaRuntime;
import com.gamor.mithrax.device.llama.QwenModelManager;
import com.gamor.mithrax.device.llama.QwenPrompts;
import com.gamor.mithrax.device.llama.QwenResponseParser;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * On-device Qwen3 implementation backed by llama.cpp.
 */
public final class QwenLocalLanguageModel implements LocalLanguageModel {

  public static final String DETAIL =
          "Qwen3 runs locally on this phone through llama.cpp. "
                  + "Transcripts are understood on-device and questions are answered from stored "
                  + "conversations and memories.";

  private static final String TAG = "QwenLocalLanguageModel";

  private final QwenModelManager modelManager;
  private final LocalLlamaRuntime runtime;
  private final Context appContext;
  private final AtomicBoolean cancelled = new AtomicBoolean(false);
  private final int contextSize;
  private final int maxTokens;

  private volatile boolean runtimeReady;

  public QwenLocalLanguageModel(@NonNull Context context) {
    this(context, new QwenModelManager(context), new LocalLlamaRuntime(),
            LocalLlamaRuntime.DEFAULT_CONTEXT_SIZE, LocalLlamaRuntime.DEFAULT_MAX_TOKENS);
  }

  QwenLocalLanguageModel(@NonNull Context context,
                         @NonNull QwenModelManager modelManager,
                         @NonNull LocalLlamaRuntime runtime,
                         int contextSize,
                         int maxTokens) {
    this.modelManager = modelManager;
    this.runtime = runtime;
    this.appContext = context.getApplicationContext();
    this.contextSize = contextSize;
    this.maxTokens = maxTokens;
    modelManager.ensureModelsDirectory();
  }

  private void ensureBackend(@NonNull Context context) {
    if (runtimeReady) {
      return;
    }
    try {
      runtime.init(context);
      runtimeReady = true;
    } catch (LocalLlamaRuntime.LlamaRuntimeException e) {
      Log.e(TAG, "Failed to initialize llama.cpp backend", e);
      runtimeReady = false;
    }
  }

  @Override
  public boolean isAvailable() {
    ensureBackend(appContext);
    if (!runtimeReady || !modelManager.isModelInstalled()) {
      return false;
    }
    return true;
  }

  @NonNull
  @Override
  public SummarizationResult summarize(@NonNull SummarizationRequest request) {
    cancelled.set(false);
    if (request.transcript.trim().isEmpty()) {
      return SummarizationResult.skippedEmpty();
    }
    if (!isAvailable()) {
      return SummarizationResult.unavailable(unavailableMessage());
    }
    if (!ensureModelLoaded()) {
      return SummarizationResult.failed("On-device Qwen3 model could not be loaded");
    }
    try {
      String raw = runtime.generateBlocking(
              QwenPrompts.summarizationSystemPrompt(),
              QwenPrompts.summarizationUserPrompt(request),
              maxTokens);
      if (cancelled.get()) {
        return SummarizationResult.cancelled();
      }
      return SummarizationResult.success(QwenResponseParser.parseInsights(raw), 1);
    } catch (QwenResponseParser.ParseException e) {
      Log.w(TAG, "Failed to parse summarization output: " + e.getMessage());
      return SummarizationResult.failed("Could not parse local model output");
    } catch (LocalLlamaRuntime.LlamaRuntimeException e) {
      if (e.cancelled || cancelled.get()) {
        return SummarizationResult.cancelled();
      }
      Log.w(TAG, "Summarization inference failed", e);
      return SummarizationResult.failed(safeMessage(e));
    } catch (RuntimeException e) {
      Log.e(TAG, "Unexpected summarization failure", e);
      return SummarizationResult.failed("Local inference failed");
    }
  }

  @NonNull
  @Override
  public AnswerResult answer(@NonNull AnswerRequest request) {
    cancelled.set(false);
    if (request.context.isEmpty()) {
      return AnswerResult.notFound(GroundedContextAnswerer.NOT_FOUND);
    }
    if (!isAvailable()) {
      return AnswerResult.unavailable(unavailableMessage());
    }
    if (!ensureModelLoaded()) {
      return AnswerResult.failed("On-device Qwen3 model could not be loaded");
    }
    try {
      String raw = runtime.generateBlocking(
              QwenPrompts.answerSystemPrompt(),
              QwenPrompts.answerUserPrompt(request),
              Math.min(maxTokens, 256));
      if (cancelled.get()) {
        return AnswerResult.cancelled();
      }
      String answer = QwenResponseParser.parseAnswer(raw);
      if (QwenResponseParser.isNotFoundAnswer(answer)) {
        return AnswerResult.notFound(
                answer.isEmpty() ? GroundedContextAnswerer.NOT_FOUND : answer);
      }
      return AnswerResult.success(answer);
    } catch (QwenResponseParser.ParseException e) {
      Log.w(TAG, "Failed to parse answer output: " + e.getMessage());
      return AnswerResult.failed("Could not parse local model answer");
    } catch (LocalLlamaRuntime.LlamaRuntimeException e) {
      if (e.cancelled || cancelled.get()) {
        return AnswerResult.cancelled();
      }
      Log.w(TAG, "Answering inference failed", e);
      return AnswerResult.failed(safeMessage(e));
    } catch (RuntimeException e) {
      Log.e(TAG, "Unexpected answering failure", e);
      return AnswerResult.failed("Local inference failed");
    }
  }

  @Override
  public void cancel() {
    cancelled.set(true);
    runtime.cancel();
  }

  @NonNull
  public String generateRawForMemory(@NonNull String systemPrompt, @NonNull String userPrompt)
          throws LocalLlamaRuntime.LlamaRuntimeException {
    if (!isAvailable()) {
      throw new LocalLlamaRuntime.LlamaRuntimeException(unavailableMessage());
    }
    if (!ensureModelLoaded()) {
      throw new LocalLlamaRuntime.LlamaRuntimeException("On-device Qwen3 model could not be loaded");
    }
    return runtime.generateBlocking(systemPrompt, userPrompt, maxTokens);
  }

  public boolean isRuntimeReady() {
    ensureBackend(appContext);
    return runtimeReady;
  }

  public boolean hasModelFile() {
    return modelManager.isModelInstalled();
  }

  @NonNull
  public String modelStatusText() {
    if (!hasModelFile()) {
      return modelManager.formatStatus();
    }
    if (!runtimeReady) {
      return modelManager.formatStatus() + "\nNative llama.cpp runtime is not ready on this device.";
    }
    if (runtime.isModelLoaded()) {
      return modelManager.formatStatus() + "\nModel is loaded and ready for inference.";
    }
    return modelManager.formatStatus() + "\nModel file found. It loads automatically on first use.";
  }

  public void shutdown() {
    runtime.close();
  }

  @NonNull
  public QwenModelManager getModelManager() {
    return modelManager;
  }

  @NonNull
  public LocalLlamaRuntime getRuntime() {
    return runtime;
  }

  private boolean ensureModelLoaded() {
    if (runtime.isModelLoaded()) {
      return true;
    }
    File model = modelManager.resolveInstalledModel();
    if (model == null) {
      return false;
    }
    try {
      runtime.loadModel(model.getAbsolutePath(), contextSize);
      return true;
    } catch (LocalLlamaRuntime.LlamaRuntimeException e) {
      Log.e(TAG, "Failed to load local model", e);
      return false;
    }
  }

  @NonNull
  private String unavailableMessage() {
    if (!runtimeReady) {
      return UnprovisionedLocalLanguageModel.MESSAGE;
    }
    if (!modelManager.isModelInstalled()) {
      return "On-device Qwen3 model is not installed. " + modelManager.installHint();
    }
    return "On-device Qwen3 model could not be loaded";
  }

  @NonNull
  private static String safeMessage(@NonNull LocalLlamaRuntime.LlamaRuntimeException e) {
    String message = e.getMessage();
    return message == null || message.isEmpty() ? "Local inference failed" : message;
  }
}
