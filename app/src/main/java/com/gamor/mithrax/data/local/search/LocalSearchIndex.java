package com.gamor.mithrax.data.local.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gamor.mithrax.data.local.conversation.ConversationEntity;
import com.gamor.mithrax.data.local.memory.MemoryEntity;

import java.util.List;

/**
 * Keeps FTS tables aligned with stored conversations and memories.
 */
public class LocalSearchIndex {

    private final SearchDao dao;

    public LocalSearchIndex(@NonNull SearchDao dao) {
        this.dao = dao;
    }

    public void upsertConversation(@Nullable ConversationEntity conversation) {
        if (conversation == null || conversation.id == null || conversation.id.isEmpty()) {
            return;
        }
        dao.deleteConversationFts(conversation.id);
        dao.insertConversationFts(SearchDocuments.conversation(conversation));
    }

    public void removeConversation(@NonNull String conversationId) {
        dao.deleteConversationFts(conversationId);
        dao.deleteMemoryFtsForConversation(conversationId);
    }

    public void replaceMemories(@NonNull String conversationId, @Nullable List<MemoryEntity> memories) {
        dao.deleteMemoryFtsForConversation(conversationId);
        if (memories == null) {
            return;
        }
        for (MemoryEntity memory : memories) {
            if (memory != null && !memory.deleted) {
                dao.insertMemoryFts(SearchDocuments.memory(memory));
            }
        }
    }

    public void upsertMemory(@Nullable MemoryEntity memory) {
        if (memory == null || memory.id == null || memory.id.isEmpty()) {
            return;
        }
        dao.deleteMemoryFts(memory.id);
        if (!memory.deleted) {
            dao.insertMemoryFts(SearchDocuments.memory(memory));
        }
    }

    public void removeMemory(@NonNull String memoryId) {
        dao.deleteMemoryFts(memoryId);
    }

    public void clearMemories() {
        dao.deleteAllMemoryFts();
    }

    public void clearAll() {
        dao.deleteAllConversationFts();
        dao.deleteAllMemoryFts();
    }
}
