package com.gamor.mithrax.device.llama;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gamor.mithrax.llama.LlamaNative;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Android-side wrapper around llama.cpp. Keeps native details out of the domain layer.
 */
public final class LocalLlamaRuntime {

  private static final String TAG = "LocalLlamaRuntime";

  public static final int DEFAULT_CONTEXT_SIZE = 4096;
  public static final int DEFAULT_MAX_TOKENS = 768;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final AtomicBoolean cancelled = new AtomicBoolean(false);
  private final Object lock = new Object();

  private volatile boolean backendReady;
  private volatile boolean modelLoaded;
  private volatile long modelLoadStartedAtMs;
  private volatile long modelLoadFinishedAtMs;
  private volatile long lastInferenceStartedAtMs;
  private volatile long lastInferenceFinishedAtMs;
  private volatile long lastFirstTokenAtMs;

  @Nullable
  private String loadedModelPath;

  public void init(@NonNull Context context) throws LlamaRuntimeException {
    synchronized (lock) {
      if (backendReady) {
        return;
      }
      try {
        String nativeLibDir = context.getApplicationInfo().nativeLibraryDir;
        LlamaNative.initBackendChecked(nativeLibDir);
        backendReady = true;
        Log.i(TAG, "llama.cpp backend ready");
        Log.d(TAG, "System info:\n" + LlamaNative.systemInfoChecked());
      } catch (UnsatisfiedLinkError e) {
        throw new LlamaRuntimeException("Native llama.cpp library is unavailable", e);
      }
    }
  }

  public boolean isModelLoaded() {
    return modelLoaded;
  }

  @Nullable
  public String getLoadedModelPath() {
    return loadedModelPath;
  }

  public void loadModel(@NonNull String modelPath, int contextSize) throws LlamaRuntimeException {
    synchronized (lock) {
      if (!backendReady) {
        throw new LlamaRuntimeException("llama.cpp backend is not initialized");
      }
      File file = new File(modelPath);
      if (!file.isFile() || !file.canRead()) {
        throw new LlamaRuntimeException("Model file is missing or unreadable");
      }
      unloadModelLocked();
      modelLoadStartedAtMs = System.currentTimeMillis();
      int code = LlamaNative.loadModelChecked(modelPath, contextSize);
      modelLoadFinishedAtMs = System.currentTimeMillis();
      if (code != 0) {
        throw new LlamaRuntimeException("Failed to load model (code " + code + ")");
      }
      modelLoaded = true;
      loadedModelPath = file.getAbsolutePath();
      Log.i(TAG, "Model loaded in " + (modelLoadFinishedAtMs - modelLoadStartedAtMs) + " ms");
    }
  }

  @NonNull
  public Future<String> generate(@NonNull String systemPrompt,
                                 @NonNull String userPrompt,
                                 int maxTokens) {
    return executor.submit(() -> generateBlocking(systemPrompt, userPrompt, maxTokens));
  }

  @NonNull
  public String generateBlocking(@NonNull String systemPrompt,
                                 @NonNull String userPrompt,
                                 int maxTokens) throws LlamaRuntimeException {
    synchronized (lock) {
      if (!modelLoaded) {
        throw new LlamaRuntimeException("No model is loaded");
      }
      cancelled.set(false);
      lastInferenceStartedAtMs = System.currentTimeMillis();
      lastFirstTokenAtMs = 0L;
      try {
        String[] out = new String[1];
        int code = LlamaNative.completeChecked(systemPrompt, userPrompt, maxTokens, out);
        lastInferenceFinishedAtMs = System.currentTimeMillis();
        if (code == 4 || cancelled.get()) {
          throw new LlamaRuntimeException("Generation cancelled", true);
        }
        if (code != 0) {
          throw new LlamaRuntimeException("Inference failed with code " + code);
        }
        logInferenceMetrics(out[0] == null ? 0 : out[0].length());
        return out[0] == null ? "" : out[0];
      } catch (LlamaRuntimeException e) {
        throw e;
      } catch (RuntimeException e) {
        throw new LlamaRuntimeException("Native inference crashed", e);
      }
    }
  }

  public void cancel() {
    cancelled.set(true);
    LlamaNative.requestCancelChecked();
  }

  public void unloadModel() {
    synchronized (lock) {
      unloadModelLocked();
    }
  }

  public void close() {
    synchronized (lock) {
      unloadModelLocked();
      if (backendReady) {
        LlamaNative.shutdownBackendChecked();
        backendReady = false;
      }
    }
    executor.shutdownNow();
  }

  public long getModelLoadTimeMs() {
    if (modelLoadFinishedAtMs <= modelLoadStartedAtMs) {
      return 0L;
    }
    return modelLoadFinishedAtMs - modelLoadStartedAtMs;
  }

  public long getLastInferenceDurationMs() {
    if (lastInferenceFinishedAtMs <= lastInferenceStartedAtMs) {
      return 0L;
    }
    return lastInferenceFinishedAtMs - lastInferenceStartedAtMs;
  }

  private void unloadModelLocked() {
    if (modelLoaded) {
      LlamaNative.unloadModelChecked();
      modelLoaded = false;
      loadedModelPath = null;
      Log.i(TAG, "Model unloaded");
    }
  }

  private void logInferenceMetrics(int responseChars) {
    long duration = getLastInferenceDurationMs();
    Log.i(TAG, "Inference finished in " + duration + " ms, responseChars=" + responseChars);
  }

  public static final class LlamaRuntimeException extends Exception {
    public final boolean cancelled;

    public LlamaRuntimeException(@NonNull String message) {
      this(message, false);
    }

    public LlamaRuntimeException(@NonNull String message, boolean cancelled) {
      super(message);
      this.cancelled = cancelled;
    }

    public LlamaRuntimeException(@NonNull String message, @NonNull Throwable cause) {
      super(message, cause);
      this.cancelled = false;
    }
  }
}
