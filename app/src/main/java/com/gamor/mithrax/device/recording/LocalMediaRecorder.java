package com.gamor.mithrax.device.recording;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * File-based local capture. Does not stream audio anywhere.
 */
public class LocalMediaRecorder {

    private static final String TAG = "LocalMediaRecorder";

    public interface ErrorListener {
        void onRecorderError(@NonNull String message);
    }

    @Nullable
    private MediaRecorder recorder;

    public void start(@NonNull Context context, @NonNull File outputFile, @NonNull ErrorListener errors)
            throws IOException {
        stopAndRelease();
        if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            outputFile.getParentFile().mkdirs();
        }

        MediaRecorder mediaRecorder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mediaRecorder = new MediaRecorder(context);
        } else {
            mediaRecorder = new MediaRecorder();
        }
        mediaRecorder.setOnErrorListener((mr, what, extra) -> {
            Log.e(TAG, "MediaRecorder error what=" + what + " extra=" + extra);
            errors.onRecorderError("Recording was interrupted");
        });
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioEncodingBitRate(128000);
        mediaRecorder.setAudioSamplingRate(44100);
        mediaRecorder.setOutputFile(outputFile.getAbsolutePath());
        mediaRecorder.prepare();
        mediaRecorder.start();
        recorder = mediaRecorder;
    }

    /**
     * @return true if stop completed with a usable file
     */
    public boolean stopAndRelease() {
        MediaRecorder current = recorder;
        recorder = null;
        if (current == null) {
            return false;
        }
        boolean stopped = false;
        try {
            current.stop();
            stopped = true;
        } catch (RuntimeException e) {
            Log.e(TAG, "MediaRecorder.stop failed (no valid audio)", e);
        } finally {
            try {
                current.reset();
            } catch (RuntimeException ignored) {
            }
            current.release();
        }
        return stopped;
    }

    public boolean isRunning() {
        return recorder != null;
    }
}
