package com.gamor.mithrax.device.llama;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Manages the on-device Qwen3 GGUF model. Does not download weights from the network.
 */
public final class QwenModelManager {

  public static final String MODEL_FILENAME = "Qwen3-4B-Q4_K_M.gguf";
  /** Reject partial copies from failed adb transfers. */
  private static final long MIN_MODEL_BYTES = 2_000_000_000L;
  private static final String TAG = "QwenModelManager";
  private static final String MODELS_DIR = "models";

  private final Context appContext;

  public QwenModelManager(@NonNull Context context) {
    this.appContext = context.getApplicationContext();
  }

  @NonNull
  public File getPreferredModelFile() {
    return new File(getModelsDirectory(), MODEL_FILENAME);
  }

  @NonNull
  public File getModelsDirectory() {
    return new File(appContext.getFilesDir(), MODELS_DIR);
  }

  public boolean isModelInstalled() {
    File model = resolveInstalledModel();
    return model != null;
  }

  @Nullable
  public File resolveInstalledModel() {
    File best = null;
    for (File candidate : candidatePaths()) {
      if (!isUsableModelFile(candidate)) {
        continue;
      }
      if (best == null || candidate.length() > best.length()) {
        best = candidate;
      }
    }
    if (best != null) {
      Log.i(TAG, "Using Qwen model at " + best.getAbsolutePath()
              + " (" + formatSize(best.length()) + ")");
    }
    return best;
  }

  public boolean isUsableModelFile(@Nullable File file) {
    return file != null
            && file.isFile()
            && file.canRead()
            && file.length() >= MIN_MODEL_BYTES;
  }

  @NonNull
  public List<File> candidatePaths() {
    List<File> paths = new ArrayList<>();
    paths.add(getPreferredModelFile());
    File external = appContext.getExternalFilesDir(MODELS_DIR);
    if (external != null) {
      paths.add(new File(external, MODEL_FILENAME));
    }
  File download = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS);
    if (download != null) {
      paths.add(new File(download, MODEL_FILENAME));
    }
    paths.add(new File("/sdcard/Download", MODEL_FILENAME));
    paths.add(new File("/data/local/tmp", MODEL_FILENAME));
    return Collections.unmodifiableList(paths);
  }

  public long getInstalledModelSizeBytes() {
    File model = resolveInstalledModel();
    return model == null ? 0L : model.length();
  }

  @NonNull
  public String getExpectedModelName() {
    return MODEL_FILENAME;
  }

  @Nullable
  public String getInstalledModelPath() {
    File model = resolveInstalledModel();
    return model == null ? null : model.getAbsolutePath();
  }

  public void ensureModelsDirectory() {
    File dir = getModelsDirectory();
    if (!dir.exists() && !dir.mkdirs()) {
      Log.w(TAG, "Could not create models directory at " + dir.getAbsolutePath());
    }
  }

  @NonNull
  public String installHint() {
    File preferred = getPreferredModelFile();
    File external = appContext.getExternalFilesDir(MODELS_DIR);
    String externalPath = external == null
            ? "/sdcard/Android/data/com.gamor.mithrax/files/models/" + MODEL_FILENAME
            : new File(external, MODEL_FILENAME).getAbsolutePath();
    return "Install the model with:\n"
            + "adb shell mkdir -p /sdcard/Android/data/com.gamor.mithrax/files/models\n"
            + "adb push /path/to/" + MODEL_FILENAME + " " + externalPath + "\n"
            + "Or copy to: " + preferred.getAbsolutePath();
  }

  @NonNull
  public String formatStatus() {
    File model = resolveInstalledModel();
    if (model == null) {
      return "Qwen3 model not found.\n" + installHint();
    }
    return "Qwen3 model ready (" + formatSize(model.length()) + ")\n" + model.getAbsolutePath();
  }

  @NonNull
  private static String formatSize(long bytes) {
    return String.format(Locale.US, "%.1f GB", bytes / 1_000_000_000.0);
  }
}
