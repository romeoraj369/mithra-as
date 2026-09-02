package com.gamor.mithrax.device.understanding;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gamor.mithrax.data.local.conversation.ConversationEntity;
import com.gamor.mithrax.data.local.conversation.ConversationRepository;
import com.gamor.mithrax.domain.ai.LocalLanguageModel;
import com.gamor.mithrax.domain.ai.SummarizationResult;
import com.gamor.mithrax.domain.recording.ProcessingStatus;
import com.gamor.mithrax.domain.understanding.SummarizationMetadata;
import com.gamor.mithrax.device.memory.MemoryExtractionController;
import com.gamor.mithrax.domain.understanding.SummarizationPipeline;
import com.gamor.mithrax.domain.understanding.TranscriptChunker;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runs local summarization after a transcript exists and stores insights on-device.
 */
public class SummarizationController {

    private static final String TAG = "Summarization";

    private final ConversationRepository repository;
    private final SummarizationPipeline pipeline;
    @Nullable
    private final MemoryExtractionController memoryExtractionController;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    private volatile String activeConversationId;

    public SummarizationController(@NonNull ConversationRepository repository,
                                   @NonNull LocalLanguageModel model) {
        this(repository, new SummarizationPipeline(model, new TranscriptChunker()), null);
    }

    public SummarizationController(@NonNull ConversationRepository repository,
                                   @NonNull LocalLanguageModel model,
                                   @NonNull MemoryExtractionController memoryExtractionController) {
        this(repository, new SummarizationPipeline(model, new TranscriptChunker()),
                memoryExtractionController);
    }

    SummarizationController(@NonNull ConversationRepository repository,
                            @NonNull SummarizationPipeline pipeline) {
        this(repository, pipeline, null);
    }

    SummarizationController(@NonNull ConversationRepository repository,
                            @NonNull SummarizationPipeline pipeline,
                            @Nullable MemoryExtractionController memoryExtractionController) {
        this.repository = repository;
        this.pipeline = pipeline;
        this.memoryExtractionController = memoryExtractionController;
    }

    public void enqueue(@NonNull String conversationId) {
        executor.execute(() -> summarize(conversationId));
    }

    public void resumeInterrupted() {
        executor.execute(() -> {
            try {
                List<ConversationEntity> conversations =
                        repository.getByStatusSync(ProcessingStatus.SUMMARIZING.name());
                for (ConversationEntity conversation : conversations) {
                    summarize(conversation.id);
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to resume interrupted summarization", e);
            }
        });
    }

    public void cancel(@NonNull String conversationId) {
        String active = activeConversationId;
        if (conversationId.equals(active)) {
            pipeline.cancel();
        }
    }

    private void summarize(@NonNull String conversationId) {
        ConversationEntity conversation = repository.getByIdSync(conversationId);
        if (conversation == null) {
            return;
        }
        activeConversationId = conversationId;
        pipeline.resetCancel();
        repository.updateStatusSync(conversationId, ProcessingStatus.SUMMARIZING.name());
        SummarizationResult result = pipeline.run(conversationId, conversation.transcript);
        persist(conversationId, result);
        activeConversationId = null;
    }

    private void persist(@NonNull String conversationId, @NonNull SummarizationResult result) {
        ConversationEntity existing = repository.getByIdSync(conversationId);
        if (existing == null) {
            return;
        }
        Map<String, String> metadata = SummarizationMetadata.apply(existing.metadata, result);
        String status = result.kind == SummarizationResult.Kind.FAILED
                ? ProcessingStatus.FAILED.name()
                : ProcessingStatus.COMPLETED.name();
        repository.completeInsightsSync(
                conversationId,
                result.insights,
                status,
                metadata
        );
        if (memoryExtractionController != null) {
            memoryExtractionController.enqueue(conversationId);
        }
        if (result.kind == SummarizationResult.Kind.FAILED) {
            Log.e(TAG, "Summarization failed for " + conversationId + ": " + result.message);
        } else if (result.kind == SummarizationResult.Kind.UNAVAILABLE) {
            Log.w(TAG, "Summarization skipped; local model unavailable for " + conversationId);
        } else {
            Log.d(TAG, "Summarization finished for " + conversationId + " (" + result.kind + ")");
        }
    }
}
