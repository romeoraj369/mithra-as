package com.gamor.mithrax.domain.tts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Outcome of a local speak request. Engines must not call a cloud TTS API.
 */
public final class SpeakResult {

    public enum Kind {
        STARTED,
        SKIPPED_EMPTY,
        SKIPPED_DISABLED,
        UNAVAILABLE,
        FAILED
    }

    @NonNull
    public final Kind kind;
    @Nullable
    public final String message;
    public final int chunkCount;

    private SpeakResult(@NonNull Kind kind, @Nullable String message, int chunkCount) {
        this.kind = kind;
        this.message = message;
        this.chunkCount = chunkCount;
    }

    @NonNull
    public static SpeakResult started(int chunkCount) {
        return new SpeakResult(Kind.STARTED, null, chunkCount);
    }

    @NonNull
    public static SpeakResult skippedEmpty() {
        return new SpeakResult(Kind.SKIPPED_EMPTY, null, 0);
    }

    @NonNull
    public static SpeakResult skippedDisabled() {
        return new SpeakResult(Kind.SKIPPED_DISABLED, null, 0);
    }

    @NonNull
    public static SpeakResult unavailable(@NonNull String message) {
        return new SpeakResult(Kind.UNAVAILABLE, message, 0);
    }

    @NonNull
    public static SpeakResult failed(@NonNull String message) {
        return new SpeakResult(Kind.FAILED, message, 0);
    }
}
