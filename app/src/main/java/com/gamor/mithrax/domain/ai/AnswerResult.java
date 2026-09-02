package com.gamor.mithrax.domain.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Output of on-device question answering. Implementations must not invent facts.
 */
public final class AnswerResult {

    public enum Kind {
        SUCCESS,
        NOT_FOUND,
        UNAVAILABLE,
        FAILED,
        CANCELLED
    }

    @NonNull
    public final Kind kind;
    @NonNull
    public final String text;
    @Nullable
    public final String message;

    private AnswerResult(@NonNull Kind kind, @NonNull String text, @Nullable String message) {
        this.kind = kind;
        this.text = text;
        this.message = message;
    }

    @NonNull
    public static AnswerResult success(@NonNull String text) {
        return new AnswerResult(Kind.SUCCESS, text, null);
    }

    @NonNull
    public static AnswerResult notFound(@NonNull String text) {
        return new AnswerResult(Kind.NOT_FOUND, text, null);
    }

    @NonNull
    public static AnswerResult unavailable(@NonNull String message) {
        return new AnswerResult(Kind.UNAVAILABLE, "", message);
    }

    @NonNull
    public static AnswerResult failed(@NonNull String message) {
        return new AnswerResult(Kind.FAILED, "", message);
    }

    @NonNull
    public static AnswerResult cancelled() {
        return new AnswerResult(Kind.CANCELLED, "", null);
    }
}
