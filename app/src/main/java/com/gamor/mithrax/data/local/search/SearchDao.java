package com.gamor.mithrax.data.local.search;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SearchDao {

    @Insert
    void insertConversationFts(ConversationFtsEntity entity);

    @Query("DELETE FROM conversation_fts WHERE sourceId = :sourceId")
    void deleteConversationFts(String sourceId);

    @Query("SELECT sourceId FROM conversation_fts WHERE conversation_fts MATCH :matchQuery")
    List<String> matchConversationIds(String matchQuery);

    @Insert
    void insertMemoryFts(MemoryFtsEntity entity);

    @Query("DELETE FROM memory_fts WHERE sourceId = :sourceId")
    void deleteMemoryFts(String sourceId);

    @Query("DELETE FROM memory_fts WHERE sourceConversationId = :conversationId")
    void deleteMemoryFtsForConversation(String conversationId);

    @Query("SELECT sourceId FROM memory_fts WHERE memory_fts MATCH :matchQuery")
    List<String> matchMemoryIds(String matchQuery);

    @Query("DELETE FROM conversation_fts")
    void deleteAllConversationFts();

    @Query("DELETE FROM memory_fts")
    void deleteAllMemoryFts();
}
