package com.gamor.mithrax.domain.memory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gamor.mithrax.domain.understanding.ConversationInsights;

/**
 * Input for on-device memory extraction. Must not leave the device.
 */
public final class MemoryExtractionRequest {

    @NonNull
    public final String conversationId;
    @NonNull
    public final String transcript;
    @Nullable
    public final ConversationInsights insights;

    public MemoryExtractionRequest(@NonNull String conversationId,
                                   @NonNull String transcript,
                                   @Nullable ConversationInsights insights) {
        this.conversationId = conversationId;
        this.transcript = transcript;
        this.insights = insights;
    }
}
