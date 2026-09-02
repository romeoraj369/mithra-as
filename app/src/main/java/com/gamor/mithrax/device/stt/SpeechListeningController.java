package com.gamor.mithrax.device.stt;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;

import java.io.IOException;

/**
 * Wraps the existing Vosk SpeechService so UI does not own microphone lifecycle.
 */
public class SpeechListeningController implements RecognitionListener {

    private static final String TAG = "SpeechListening";
    private static final float SAMPLE_RATE = 16000.0f;

    public interface Listener {
        void onPartialText(@NonNull String text);

        void onUtterance(@NonNull String text);

        void onError(@NonNull String message);

        void onTimeout();
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    private SpeechService speechService;
    @Nullable
    private Listener listener;
    private boolean listening;

    public boolean isListening() {
        return listening;
    }

    public void start(@NonNull Model model, @NonNull Listener listener) throws IOException {
        stop();
        this.listener = listener;
        Recognizer recognizer = new Recognizer(model, SAMPLE_RATE);
        speechService = new SpeechService(recognizer, SAMPLE_RATE);
        speechService.startListening(this);
        listening = true;
        Log.d(TAG, "Listening started");
    }

    public void stop() {
        listening = false;
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
            speechService = null;
            Log.d(TAG, "Listening stopped");
        }
        listener = null;
    }

    @Override
    public void onPartialResult(String hypothesis) {
        String text = VoskHypothesisParser.extractText(hypothesis);
        if (text.isEmpty()) {
            return;
        }
        mainHandler.post(() -> {
            Listener current = listener;
            if (current != null) {
                current.onPartialText(text);
            }
        });
    }

    @Override
    public void onResult(String hypothesis) {
        String text = VoskHypothesisParser.extractText(hypothesis);
        mainHandler.post(() -> {
            Listener current = listener;
            if (current != null) {
                current.onUtterance(text);
            }
        });
    }

    @Override
    public void onFinalResult(String hypothesis) {
        // Final result is unused; onResult already delivers completed utterances.
    }

    @Override
    public void onError(Exception e) {
        Log.e(TAG, "Recognition error", e);
        String message = e.getMessage() == null ? "Speech recognition error" : e.getMessage();
        mainHandler.post(() -> {
            Listener current = listener;
            if (current != null) {
                current.onError(message);
            }
        });
    }

    @Override
    public void onTimeout() {
        mainHandler.post(() -> {
            Listener current = listener;
            if (current != null) {
                current.onTimeout();
            }
        });
    }
}
