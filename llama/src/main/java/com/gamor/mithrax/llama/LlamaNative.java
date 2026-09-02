package com.gamor.mithrax.llama;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Low-level JNI bridge to llama.cpp. Callers should use {@link com.gamor.mithrax.device.llama.LocalLlamaRuntime}.
 */
public final class LlamaNative {

  private static volatile boolean libraryLoaded;

  private LlamaNative() {
  }

  private static synchronized void ensureLoaded() {
    if (libraryLoaded) {
      return;
    }
    System.loadLibrary("mithrax-llama");
    libraryLoaded = true;
  }

  public static native void initBackend(@NonNull String nativeLibDir);

  @NonNull
  public static native String systemInfo();

  /**
   * @return 0 on success
   */
  public static native int loadModel(@NonNull String modelPath, int contextSize);

  /**
   * Runs system + user prompts and fills {@code outResponse[0]} with generated text.
   *
   * @return 0 on success, 4 if cancelled
   */
  public static native int complete(@NonNull String systemPrompt,
                                    @NonNull String userPrompt,
                                    int maxTokens,
                                    @NonNull String[] outResponse);

  public static native void requestCancel();

  public static native void unloadModel();

  public static native void shutdownBackend();

  public static void initBackendChecked(@NonNull String nativeLibDir) {
    ensureLoaded();
    initBackend(nativeLibDir);
  }

  @NonNull
  public static String systemInfoChecked() {
    ensureLoaded();
    return systemInfo();
  }

  public static int loadModelChecked(@NonNull String modelPath, int contextSize) {
    ensureLoaded();
    return loadModel(modelPath, contextSize);
  }

  public static int completeChecked(@NonNull String systemPrompt,
                                    @NonNull String userPrompt,
                                    int maxTokens,
                                    @NonNull String[] outResponse) {
    ensureLoaded();
    return complete(systemPrompt, userPrompt, maxTokens, outResponse);
  }

  public static void requestCancelChecked() {
    if (!libraryLoaded) {
      return;
    }
    requestCancel();
  }

  public static void unloadModelChecked() {
    if (!libraryLoaded) {
      return;
    }
    unloadModel();
  }

  public static void shutdownBackendChecked() {
    if (!libraryLoaded) {
      return;
    }
    shutdownBackend();
    libraryLoaded = false;
  }

  @NonNull
  public static String completeBlocking(@NonNull String systemPrompt,
                                        @NonNull String userPrompt,
                                        int maxTokens) throws LlamaException {
    String[] out = new String[1];
    int code = completeChecked(systemPrompt, userPrompt, maxTokens, out);
  if (code == 4) {
      throw new LlamaException(LlamaException.Kind.CANCELLED, "Generation cancelled");
    }
    if (code != 0) {
      throw new LlamaException(LlamaException.Kind.INFERENCE_FAILED,
              "Native inference failed with code " + code);
    }
    return out[0] == null ? "" : out[0];
  }

  public static final class LlamaException extends Exception {
    public enum Kind {
      CANCELLED,
      INFERENCE_FAILED
    }

    public final Kind kind;

    public LlamaException(@NonNull Kind kind, @NonNull String message) {
      super(message);
      this.kind = kind;
    }
  }
}
