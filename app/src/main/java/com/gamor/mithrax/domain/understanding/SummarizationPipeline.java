package com.gamor.mithrax.domain.understanding;

import androidx.annotation.NonNull;

import com.gamor.mithrax.domain.ai.LocalLanguageModel;
import com.gamor.mithrax.domain.ai.SummarizationRequest;
import com.gamor.mithrax.domain.ai.SummarizationResult;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Transcript → local model → structured insights. The model implementation is swappable.
 */
public final class SummarizationPipeline {

    private final LocalLanguageModel model;
    private final TranscriptChunker chunker;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public SummarizationPipeline(@NonNull LocalLanguageModel model, @NonNull TranscriptChunker chunker) {
        this.model = model;
        this.chunker = chunker;
    }

    public void resetCancel() {
        cancelled.set(false);
    }

    public void cancel() {
        cancelled.set(true);
        model.cancel();
    }

    @NonNull
    public SummarizationResult run(@NonNull String conversationId, @NonNull String transcript) {
        if (cancelled.get()) {
            return SummarizationResult.cancelled();
        }
        if (transcript.trim().isEmpty()) {
            return SummarizationResult.skippedEmpty();
        }
        if (!model.isAvailable()) {
            return SummarizationResult.unavailable("Local language model is not installed");
        }
        List<String> chunks = chunker.chunk(transcript);
        if (chunks.isEmpty()) {
            return SummarizationResult.skippedEmpty();
        }
        ConversationInsights combined = ConversationInsights.empty();
        for (int i = 0; i < chunks.size(); i++) {
            if (cancelled.get()) {
                return SummarizationResult.cancelled();
            }
            SummarizationRequest request = new SummarizationRequest(
                    conversationId, chunks.get(i), i, chunks.size());
            SummarizationResult piece = model.summarize(request);
            if (piece.kind == SummarizationResult.Kind.CANCELLED || cancelled.get()) {
                return SummarizationResult.cancelled();
            }
            if (piece.kind == SummarizationResult.Kind.UNAVAILABLE) {
                String message = piece.message == null
                        ? "Local language model is not installed" : piece.message;
                return SummarizationResult.unavailable(message);
            }
            if (piece.kind == SummarizationResult.Kind.FAILED) {
                String message = piece.message == null ? "Local inference failed" : piece.message;
                return SummarizationResult.failed(message);
            }
            if (piece.kind == SummarizationResult.Kind.SUCCESS) {
                combined = combined.merge(piece.insights);
            }
        }
        if (cancelled.get()) {
            return SummarizationResult.cancelled();
        }
        return SummarizationResult.success(combined, chunks.size());
    }
}
