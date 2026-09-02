package com.gamor.mithrax.domain.stt;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

/**
 * Local speech-to-text. Implementations must not use a network.
 */
public interface SpeechToTextEngine {

    boolean isModelAvailable();

    void transcribe(@NonNull File audioFile, @NonNull Callback callback);

    void cancel();

    interface Callback {
        void onProgress(@Nullable String partialTranscript);

        void onCompleted(@NonNull String transcript);

        void onError(@NonNull String message);

        void onCancelled();
    }
}
