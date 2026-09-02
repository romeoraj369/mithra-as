package com.gamor.mithrax.data.local.recording;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

/**
 * App-private recording files. Nothing is placed on shared storage or uploaded.
 */
public final class RecordingFileStore {

    private static final String DIRECTORY = "recordings";
    private static final String EXTENSION = ".m4a";

    private RecordingFileStore() {
    }

    @NonNull
    public static File directory(@NonNull Context context) {
        File dir = new File(context.getFilesDir(), DIRECTORY);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    @NonNull
    public static File fileForId(@NonNull Context context, @NonNull String recordingId) {
        return new File(directory(context), recordingId + EXTENSION);
    }

    public static void deleteQuietly(@Nullable String path) {
        if (path == null || path.trim().isEmpty()) {
            return;
        }
        File file = new File(path);
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    public static void deleteAllInDirectory(@NonNull Context context) {
        File dir = directory(context);
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isFile()) {
                //noinspection ResultOfMethodCallIgnored
                child.delete();
            }
        }
    }

    public static long sizeOfDirectory(@NonNull File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            return 0L;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return 0L;
        }
        long total = 0L;
        for (File child : children) {
            if (child.isFile()) {
                total += child.length();
            } else if (child.isDirectory()) {
                total += sizeOfDirectory(child);
            }
        }
        return total;
    }

    public static int fileCount(@NonNull File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            return 0;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return 0;
        }
        int count = 0;
        for (File child : children) {
            if (child.isFile()) {
                count++;
            }
        }
        return count;
    }
}
