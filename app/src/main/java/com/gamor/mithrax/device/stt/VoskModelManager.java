package com.gamor.mithrax.device.stt;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.vosk.Model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Loads the on-device Vosk model once per process. Does not download models.
 */
public class VoskModelManager {

    private static final String TAG = "VoskModelManager";
    private static final String ASSET_MODEL_FOLDER = "model";

    public interface Callback {
        void onLoaded(@NonNull Model model);

        void onError(@NonNull String message);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<Callback> pendingCallbacks = new ArrayList<>();

    @Nullable
    private volatile Model model;
    @Nullable
    private volatile String lastError;
    private boolean loading;

    public void ensureLoaded(@NonNull Context appContext, @NonNull Callback callback) {
        Model loaded = model;
        if (loaded != null) {
            mainHandler.post(() -> callback.onLoaded(loaded));
            return;
        }
        mainHandler.post(() -> {
            if (model != null) {
                callback.onLoaded(model);
                return;
            }
            pendingCallbacks.add(callback);
            if (loading) {
                return;
            }
            loading = true;
            lastError = null;
            Context context = appContext.getApplicationContext();
            executor.execute(() -> loadOnBackground(context));
        });
    }

    public boolean isModelAvailable(@NonNull Context context) {
        try {
            return hasUsableModelAssets(context);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Loads the model on the calling thread. Safe to call from a worker.
     */
    @NonNull
    public Model loadBlocking(@NonNull Context appContext) throws IOException {
        Model loaded = model;
        if (loaded != null) {
            return loaded;
        }
        synchronized (this) {
            if (model != null) {
                return model;
            }
            Context context = appContext.getApplicationContext();
            File modelDir = new File(context.getFilesDir(), ASSET_MODEL_FOLDER);
            if (!hasUsableModelAssets(context)) {
                throw new IOException(
                        "On-device STT model is not installed. Place a complete Vosk model under assets/model (including am/ and conf/).");
            }
            if (!isUsableModelDirectory(modelDir)) {
                if (modelDir.exists()) {
                    deleteRecursively(modelDir);
                }
                copyAssets(ASSET_MODEL_FOLDER, modelDir, context);
            }
            model = new Model(modelDir.getAbsolutePath());
            lastError = null;
            return model;
        }
    }

    @Nullable
    public Model getModelIfReady() {
        return model;
    }

    private void loadOnBackground(@NonNull Context context) {
        try {
            Model loadedModel = loadBlocking(context);
            dispatchSuccess(loadedModel);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load Vosk model", e);
            String message = e.getMessage() == null ? "Failed to load model" : e.getMessage();
            dispatchError(message);
        }
    }

    boolean hasUsableModelAssets(@NonNull Context context) throws IOException {
        String[] acoustic = context.getAssets().list(ASSET_MODEL_FOLDER + "/am");
        String[] conf = context.getAssets().list(ASSET_MODEL_FOLDER + "/conf");
        return acoustic != null && acoustic.length > 0 && conf != null && conf.length > 0;
    }

    private boolean isUsableModelDirectory(@NonNull File modelDir) {
        if (!modelDir.exists() || !modelDir.isDirectory()) {
            return false;
        }
        File acoustic = new File(modelDir, "am");
        File conf = new File(modelDir, "conf");
        if (!acoustic.isDirectory() || !conf.isDirectory()) {
            return false;
        }
        String[] acousticFiles = acoustic.list();
        String[] confFiles = conf.list();
        return acousticFiles != null && acousticFiles.length > 0
                && confFiles != null && confFiles.length > 0;
    }

    private void deleteRecursively(@NonNull File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    private void copyAssets(@NonNull String assetFolder, @NonNull File destFolder, @NonNull Context context)
            throws IOException {
        String[] assets = context.getAssets().list(assetFolder);
        if (assets == null || assets.length == 0) {
            return;
        }
        if (!destFolder.exists() && !destFolder.mkdirs()) {
            throw new IOException("Could not create destination directory: " + destFolder.getAbsolutePath());
        }
        for (String asset : assets) {
            String assetPath = assetFolder + "/" + asset;
            File destFile = new File(destFolder, asset);
            String[] subAssets = context.getAssets().list(assetPath);
            if (subAssets != null && subAssets.length > 0) {
                copyAssets(assetPath, destFile, context);
            } else {
                try (InputStream in = context.getAssets().open(assetPath);
                     FileOutputStream out = new FileOutputStream(destFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
            }
        }
    }

    private void dispatchSuccess(@NonNull Model loadedModel) {
        mainHandler.post(() -> {
            model = loadedModel;
            loading = false;
            lastError = null;
            List<Callback> callbacks = new ArrayList<>(pendingCallbacks);
            pendingCallbacks.clear();
            for (Callback callback : callbacks) {
                callback.onLoaded(loadedModel);
            }
        });
    }

    private void dispatchError(@NonNull String message) {
        mainHandler.post(() -> {
            lastError = message;
            loading = false;
            List<Callback> callbacks = new ArrayList<>(pendingCallbacks);
            pendingCallbacks.clear();
            for (Callback callback : callbacks) {
                callback.onError(message);
            }
        });
    }
}
