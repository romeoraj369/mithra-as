package com.gamor.mithrax.device.stt;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.gamor.mithrax.device.audio.EncodedAudioDecoder;
import com.gamor.mithrax.device.audio.PcmResampler;
import com.gamor.mithrax.domain.stt.SpeechToTextEngine;

import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * File transcription with the existing on-device Vosk engine.
 */
public class VoskSpeechToTextEngine implements SpeechToTextEngine {

    private static final String TAG = "VoskSpeechToText";
    private static final float SAMPLE_RATE = PcmResampler.TARGET_RATE;

    private final Context appContext;
    private final VoskModelManager modelManager;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public VoskSpeechToTextEngine(@NonNull Context context, @NonNull VoskModelManager modelManager) {
        this.appContext = context.getApplicationContext();
        this.modelManager = modelManager;
    }

    @Override
    public boolean isModelAvailable() {
        return modelManager.isModelAvailable(appContext);
    }

    @Override
    public void cancel() {
        cancelled.set(true);
    }

    public void resetCancel() {
        cancelled.set(false);
    }

    @Override
    public void transcribe(@NonNull File audioFile, @NonNull Callback callback) {
        cancelled.set(false);
        if (!audioFile.exists()) {
            callback.onError("Audio file is missing");
            return;
        }
        if (!isModelAvailable()) {
            callback.onError("On-device STT model is not installed");
            return;
        }
        Model model;
        try {
            model = modelManager.loadBlocking(appContext);
        } catch (IOException e) {
            callback.onError(e.getMessage() == null ? "Failed to load STT model" : e.getMessage());
            return;
        }
        if (cancelled.get()) {
            callback.onCancelled();
            return;
        }
        try (Recognizer recognizer = new Recognizer(model, SAMPLE_RATE)) {
            StringBuilder completedUtterances = new StringBuilder();
            EncodedAudioDecoder.decodeToMono16k(audioFile, cancelled, samples -> {
                if (cancelled.get()) {
                    return false;
                }
                boolean utteranceEnded = recognizer.acceptWaveForm(samples, samples.length);
                if (utteranceEnded) {
                    String text = VoskHypothesisParser.extractText(recognizer.getResult());
                    if (!text.isEmpty()) {
                        if (completedUtterances.length() > 0) {
                            completedUtterances.append(' ');
                        }
                        completedUtterances.append(text);
                        callback.onProgress(completedUtterances.toString());
                    }
                } else {
                    String partial = VoskHypothesisParser.extractText(recognizer.getPartialResult());
                    if (!partial.isEmpty()) {
                        String prefix = completedUtterances.toString();
                        callback.onProgress(prefix.isEmpty() ? partial : prefix + " " + partial);
                    }
                }
                return true;
            });
            if (cancelled.get()) {
                callback.onCancelled();
                return;
            }
            String tail = VoskHypothesisParser.extractText(recognizer.getFinalResult());
            if (!tail.isEmpty()) {
                if (completedUtterances.length() > 0) {
                    completedUtterances.append(' ');
                }
                completedUtterances.append(tail);
            }
            callback.onCompleted(completedUtterances.toString().trim());
        } catch (IOException | RuntimeException e) {
            Log.e(TAG, "Local transcription failed", e);
            if (cancelled.get()) {
                callback.onCancelled();
            } else {
                String message = e.getMessage() == null ? "Transcription failed" : e.getMessage();
                callback.onError(message);
            }
        }
    }
}
