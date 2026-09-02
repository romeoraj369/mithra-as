package com.gamor.mithrax.data.local.conversation;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.gamor.mithrax.domain.conversation.ActionItem;

import java.util.List;
import java.util.Map;

@Dao
public interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY createdAtMillis DESC")
    LiveData<List<ConversationEntity>> observeAll();

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    LiveData<ConversationEntity> observeById(String id);

    @Query("SELECT * FROM conversations ORDER BY createdAtMillis DESC")
    List<ConversationEntity> getAllSync();

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    ConversationEntity getById(String id);

    @Query("SELECT * FROM conversations WHERE processingStatus = :status ORDER BY createdAtMillis ASC")
    List<ConversationEntity> getByStatus(String status);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ConversationEntity conversation);

    @Query("UPDATE conversations SET durationMillis = :durationMillis, processingStatus = :status, "
            + "updatedAtMillis = :updatedAtMillis WHERE id = :id")
    void updateDurationAndStatus(String id, long durationMillis, String status, long updatedAtMillis);

    @Query("UPDATE conversations SET transcript = :transcript, processingStatus = :status, "
            + "updatedAtMillis = :updatedAtMillis WHERE id = :id")
    void updateTranscript(String id, String transcript, String status, long updatedAtMillis);

    @Query("UPDATE conversations SET title = :title, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    void updateTitle(String id, String title, long updatedAtMillis);

    @Query("UPDATE conversations SET audioPath = :audioPath, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    void updateAudioPath(String id, String audioPath, long updatedAtMillis);

    @Query("UPDATE conversations SET summary = :summary, keyPoints = :keyPoints, "
            + "actionItems = :actionItems, importantFacts = :importantFacts, "
            + "updatedAtMillis = :updatedAtMillis WHERE id = :id")
    void updateInsights(String id,
                        String summary,
                        List<String> keyPoints,
                        List<ActionItem> actionItems,
                        List<String> importantFacts,
                        long updatedAtMillis);

    @Query("UPDATE conversations SET metadata = :metadata, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    void updateMetadata(String id, Map<String, String> metadata, long updatedAtMillis);

    @Query("UPDATE conversations SET processingStatus = :status, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    void updateStatus(String id, String status, long updatedAtMillis);

    @Query("UPDATE conversations SET summary = :summary, keyPoints = :keyPoints, "
            + "actionItems = :actionItems, importantFacts = :importantFacts, "
            + "metadata = :metadata, processingStatus = :status, updatedAtMillis = :updatedAtMillis "
            + "WHERE id = :id")
    void completeInsights(String id,
                          String summary,
                          List<String> keyPoints,
                          List<ActionItem> actionItems,
                          List<String> importantFacts,
                          Map<String, String> metadata,
                          String status,
                          long updatedAtMillis);

    @Delete
    void delete(ConversationEntity conversation);

    @Query("DELETE FROM conversations")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM conversations")
    int count();
}
