package com.gamor.mithrax.domain.ai;

import androidx.annotation.NonNull;

/**
 * Input contract for on-device summarization. Implementations must not send this off-device.
 */
public final class SummarizationRequest {

    @NonNull
    public final String conversationId;
    @NonNull
    public final String transcript;
    public final int chunkIndex;
    public final int chunkCount;

    public SummarizationRequest(@NonNull String conversationId,
                                @NonNull String transcript,
                                int chunkIndex,
                                int chunkCount) {
        this.conversationId = conversationId;
        this.transcript = transcript;
        this.chunkIndex = chunkIndex;
        this.chunkCount = chunkCount;
    }
}
