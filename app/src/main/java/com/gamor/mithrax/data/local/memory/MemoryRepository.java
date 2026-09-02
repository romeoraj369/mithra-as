package com.gamor.mithrax.data.local.memory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.gamor.mithrax.data.local.search.LocalSearchIndex;
import com.gamor.mithrax.domain.memory.MemoryType;

import java.util.Collections;
import java.util.List;

/**
 * Local access to durable memories. No remote store.
 */
public class MemoryRepository {

    private final MemoryDao dao;
    @Nullable
    private final LocalSearchIndex searchIndex;

    public MemoryRepository(@NonNull MemoryDao dao) {
        this(dao, null);
    }

    public MemoryRepository(@NonNull MemoryDao dao, @Nullable LocalSearchIndex searchIndex) {
        this.dao = dao;
        this.searchIndex = searchIndex;
    }

    @NonNull
    public LiveData<List<MemoryEntity>> observeActive() {
        return dao.observeActive();
    }

    @NonNull
    public List<MemoryEntity> getActiveSync() {
        List<MemoryEntity> memories = dao.getActiveSync();
        return memories == null ? Collections.emptyList() : memories;
    }

    @Nullable
    public MemoryEntity getByIdSync(@NonNull String id) {
        return dao.getById(id);
    }

    @NonNull
    public List<MemoryEntity> getByConversationSync(@NonNull String conversationId) {
        List<MemoryEntity> memories = dao.getActiveByConversation(conversationId);
        return memories == null ? Collections.emptyList() : memories;
    }

    @NonNull
    public List<MemoryEntity> getByTypeSync(@NonNull MemoryType type) {
        List<MemoryEntity> memories = dao.getActiveByType(type.name());
        return memories == null ? Collections.emptyList() : memories;
    }

    @NonNull
    public List<MemoryEntity> searchSync(@NonNull String query) {
        if (query.trim().isEmpty()) {
            return getActiveSync();
        }
        List<MemoryEntity> memories = dao.searchActive(query.trim());
        return memories == null ? Collections.emptyList() : memories;
    }

    public void replaceForConversationSync(@NonNull String conversationId,
                                           @NonNull List<MemoryEntity> memories) {
        dao.replaceForConversation(conversationId, memories);
        if (searchIndex != null) {
            searchIndex.replaceMemories(conversationId, memories);
        }
    }

    public void deleteByConversationSync(@NonNull String conversationId) {
        dao.deleteByConversation(conversationId);
        if (searchIndex != null) {
            searchIndex.replaceMemories(conversationId, Collections.emptyList());
        }
    }

    public void deleteAllSync() {
        dao.deleteAll();
        if (searchIndex != null) {
            searchIndex.clearMemories();
        }
    }

    public int countActiveSync() {
        return dao.countActive();
    }

    public void softDeleteSync(@NonNull String id) {
        dao.softDelete(id);
        if (searchIndex != null) {
            searchIndex.removeMemory(id);
        }
    }

    public void restoreSync(@NonNull String id) {
        dao.restore(id);
        if (searchIndex != null) {
            searchIndex.upsertMemory(dao.getById(id));
        }
    }
}
