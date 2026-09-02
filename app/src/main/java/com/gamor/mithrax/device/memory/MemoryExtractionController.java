package com.gamor.mithrax.device.memory;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gamor.mithrax.data.local.conversation.ConversationEntity;
import com.gamor.mithrax.data.local.conversation.ConversationRepository;
import com.gamor.mithrax.data.local.memory.MemoryEntity;
import com.gamor.mithrax.data.local.memory.MemoryRepository;
import com.gamor.mithrax.domain.memory.ExtractedMemory;
import com.gamor.mithrax.domain.memory.MemoryExtractionRequest;
import com.gamor.mithrax.domain.memory.MemoryExtractor;
import com.gamor.mithrax.domain.understanding.ConversationInsights;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runs local memory extraction after a transcript exists and stores results on-device.
 */
public class MemoryExtractionController {

    private static final String TAG = "MemoryExtraction";

    private final ConversationRepository conversationRepository;
    private final MemoryRepository memoryRepository;
    private final MemoryExtractor extractor;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public MemoryExtractionController(@NonNull ConversationRepository conversationRepository,
                                      @NonNull MemoryRepository memoryRepository,
                                      @NonNull MemoryExtractor extractor) {
        this.conversationRepository = conversationRepository;
        this.memoryRepository = memoryRepository;
        this.extractor = extractor;
    }

    public void enqueue(@NonNull String conversationId) {
        executor.execute(() -> extract(conversationId));
    }

    void extract(@NonNull String conversationId) {
        ConversationEntity conversation = conversationRepository.getByIdSync(conversationId);
        if (conversation == null) {
            return;
        }
        ConversationInsights insights = new ConversationInsights(
                conversation.summary,
                conversation.keyPoints,
                conversation.actionItems,
                conversation.importantFacts
        );
        List<ExtractedMemory> extracted = extractor.extract(
                new MemoryExtractionRequest(conversationId, conversation.transcript, insights));
        long now = System.currentTimeMillis();
        List<MemoryEntity> entities = new ArrayList<>();
        for (ExtractedMemory memory : extracted) {
            entities.add(MemoryEntity.create(
                    UUID.randomUUID().toString(),
                    memory.type.name(),
                    memory.content,
                    conversationId,
                    now,
                    memory.confidence,
                    memory.metadata
            ));
        }
        memoryRepository.replaceForConversationSync(conversationId, entities);
        Log.d(TAG, "Stored " + entities.size() + " memories for " + conversationId);
    }
}
