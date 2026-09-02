package com.gamor.mithrax.domain.tts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * On-device speech synthesis. Implementations must not send text to a cloud TTS service.
 * Swap the Application-wired engine when a bundled local voice is added.
 */
public interface TextToSpeechEngine {

    interface Listener {
        void onSpeakingChanged(boolean speaking);

        void onError(@NonNull String message);
    }

    void addListener(@NonNull Listener listener);

    void removeListener(@NonNull Listener listener);

    /**
     * Starts the local engine if needed. Safe to call more than once.
     */
    void prepare();

    boolean isAvailable();

    boolean isSpeaking();

    @Nullable
    String unavailableReason();

    @NonNull
    SpeakResult speak(@NonNull String text);

    void stop();

    void shutdown();
}
