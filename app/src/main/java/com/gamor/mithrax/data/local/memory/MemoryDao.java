package com.gamor.mithrax.data.local.memory;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public abstract class MemoryDao {

    @Query("SELECT * FROM memories WHERE deleted = 0 ORDER BY createdAtMillis DESC")
    public abstract LiveData<List<MemoryEntity>> observeActive();

    @Query("SELECT * FROM memories WHERE deleted = 0 ORDER BY createdAtMillis DESC")
    public abstract List<MemoryEntity> getActiveSync();

    @Query("SELECT * FROM memories WHERE id = :id LIMIT 1")
    public abstract MemoryEntity getById(String id);

    @Query("SELECT * FROM memories WHERE sourceConversationId = :conversationId AND deleted = 0 "
            + "ORDER BY createdAtMillis DESC")
    public abstract List<MemoryEntity> getActiveByConversation(String conversationId);

    @Query("SELECT * FROM memories WHERE type = :type AND deleted = 0 ORDER BY createdAtMillis DESC")
    public abstract List<MemoryEntity> getActiveByType(String type);

    @Query("SELECT * FROM memories WHERE deleted = 0 AND content LIKE '%' || :query || '%' "
            + "ORDER BY createdAtMillis DESC")
    public abstract List<MemoryEntity> searchActive(String query);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertAll(List<MemoryEntity> memories);

    @Query("UPDATE memories SET deleted = 1 WHERE id = :id")
    public abstract void softDelete(String id);

    @Query("UPDATE memories SET deleted = 0 WHERE id = :id")
    public abstract void restore(String id);

    @Query("SELECT * FROM memories")
    public abstract List<MemoryEntity> getAllRows();

    @Query("SELECT COUNT(*) FROM memories WHERE deleted = 0")
    public abstract int countActive();

    @Query("DELETE FROM memories WHERE sourceConversationId = :conversationId")
    public abstract void deleteByConversation(String conversationId);

    @Query("DELETE FROM memories")
    public abstract void deleteAll();

    @Transaction
    public void replaceForConversation(String conversationId, List<MemoryEntity> memories) {
        deleteByConversation(conversationId);
        if (memories != null && !memories.isEmpty()) {
            insertAll(memories);
        }
    }
}
