package com.gamor.mithrax.domain.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gamor.mithrax.domain.understanding.ConversationInsights;

/**
 * Output contract for on-device summarization.
 */
public final class SummarizationResult {

    public enum Kind {
        SUCCESS,
        SKIPPED_EMPTY,
        UNAVAILABLE,
        FAILED,
        CANCELLED
    }

    @NonNull
    public final Kind kind;
    @NonNull
    public final ConversationInsights insights;
    @Nullable
    public final String message;
    public final int chunkCount;

    private SummarizationResult(@NonNull Kind kind,
                                @NonNull ConversationInsights insights,
                                @Nullable String message,
                                int chunkCount) {
        this.kind = kind;
        this.insights = insights;
        this.message = message;
        this.chunkCount = chunkCount;
    }

    @NonNull
    public static SummarizationResult success(@NonNull ConversationInsights insights, int chunkCount) {
        return new SummarizationResult(Kind.SUCCESS, insights, null, chunkCount);
    }

    @NonNull
    public static SummarizationResult skippedEmpty() {
        return new SummarizationResult(Kind.SKIPPED_EMPTY, ConversationInsights.empty(), null, 0);
    }

    @NonNull
    public static SummarizationResult unavailable(@NonNull String message) {
        return new SummarizationResult(Kind.UNAVAILABLE, ConversationInsights.empty(), message, 0);
    }

    @NonNull
    public static SummarizationResult failed(@NonNull String message) {
        return new SummarizationResult(Kind.FAILED, ConversationInsights.empty(), message, 0);
    }

    @NonNull
    public static SummarizationResult cancelled() {
        return new SummarizationResult(Kind.CANCELLED, ConversationInsights.empty(), null, 0);
    }
}
