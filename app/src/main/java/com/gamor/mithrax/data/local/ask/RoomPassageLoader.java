package com.gamor.mithrax.data.local.ask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gamor.mithrax.data.local.conversation.ConversationEntity;
import com.gamor.mithrax.data.local.conversation.ConversationRepository;
import com.gamor.mithrax.data.local.memory.MemoryEntity;
import com.gamor.mithrax.data.local.memory.MemoryRepository;
import com.gamor.mithrax.domain.ask.ContextPassage;
import com.gamor.mithrax.domain.search.SearchHit;
import com.gamor.mithrax.domain.search.SearchResults;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Turns search hits into full local passages from Room. No remote retrieval.
 */
public final class RoomPassageLoader {

    private static final int MAX_TRANSCRIPT_CHARS = 1_200;

    private final ConversationRepository conversations;
    private final MemoryRepository memories;

    public RoomPassageLoader(@NonNull ConversationRepository conversations,
                             @NonNull MemoryRepository memories) {
        this.conversations = conversations;
        this.memories = memories;
    }

    @NonNull
    public List<ContextPassage> load(@NonNull SearchResults results) {
        List<ContextPassage> passages = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (SearchHit hit : results.memories) {
            MemoryEntity memory = memories.getByIdSync(hit.id);
            if (memory == null || memory.deleted) {
                continue;
            }
            ConversationEntity conversation = conversations.getByIdSync(memory.sourceConversationId);
            String title = conversation == null ? "Conversation" : conversation.title;
            String key = "m:" + memory.id;
            if (!seen.add(key)) {
                continue;
            }
            passages.add(new ContextPassage(
                    ContextPassage.Kind.MEMORY,
                    memory.sourceConversationId,
                    title,
                    memoryText(memory),
                    memory.type,
                    0
            ));
        }
        for (SearchHit hit : results.conversations) {
            ConversationEntity conversation = conversations.getByIdSync(hit.id);
            if (conversation == null) {
                continue;
            }
            String key = "c:" + conversation.id;
            if (!seen.add(key)) {
                continue;
            }
            passages.add(new ContextPassage(
                    ContextPassage.Kind.CONVERSATION,
                    conversation.id,
                    conversation.title,
                    conversationText(conversation),
                    null,
                    0
            ));
        }
        return passages;
    }

    @NonNull
    private static String memoryText(@NonNull MemoryEntity memory) {
        StringBuilder text = new StringBuilder(nullToEmpty(memory.content));
        Map<String, String> metadata = memory.metadata;
        if (metadata != null) {
            String deadline = metadata.get("deadline");
            if (deadline != null && !deadline.trim().isEmpty()) {
                appendMeta(text, "deadline");
                appendMeta(text, deadline);
            }
            appendMeta(text, metadata.get("owner"));
            appendMeta(text, metadata.get("actor"));
            appendMeta(text, metadata.get("task"));
        }
        return text.toString().trim();
    }

    @NonNull
    private static String conversationText(@NonNull ConversationEntity conversation) {
        StringBuilder text = new StringBuilder();
        appendBlock(text, conversation.summary);
        appendBlock(text, conversation.transcript);
        if (conversation.importantFacts != null) {
            for (String fact : conversation.importantFacts) {
                appendBlock(text, fact);
            }
        }
        String packed = text.toString().trim();
        if (packed.length() > MAX_TRANSCRIPT_CHARS) {
            return packed.substring(0, MAX_TRANSCRIPT_CHARS).trim();
        }
        return packed;
    }

    private static void appendMeta(@NonNull StringBuilder target, @Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (target.length() > 0) {
            target.append(' ');
        }
        target.append(value.trim());
    }

    private static void appendBlock(@NonNull StringBuilder target, @Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (target.length() > 0) {
            target.append('\n');
        }
        target.append(value.trim());
    }

    @NonNull
    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
