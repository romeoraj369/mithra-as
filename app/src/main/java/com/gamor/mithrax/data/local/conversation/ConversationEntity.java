package com.gamor.mithrax.data.local.conversation;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.gamor.mithrax.domain.conversation.ActionItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Local conversation record. The audio file is only a reference and may be empty.
 */
@Entity(
        tableName = "conversations",
        indices = {
                @Index("createdAtMillis"),
                @Index("processingStatus")
        }
)
public class ConversationEntity {

    @PrimaryKey
    @NonNull
    public String id = "";

    @NonNull
    public String title = "";

    public long createdAtMillis;

    public long updatedAtMillis;

    public long durationMillis;

    /** Absolute local path, or empty when the conversation has no audio file. */
    @NonNull
    public String audioPath = "";

    @NonNull
    public String transcript = "";

    @NonNull
    public String processingStatus = "";

    @NonNull
    public String summary = "";

    @NonNull
    public List<String> keyPoints = new ArrayList<>();

    @NonNull
    public List<ActionItem> actionItems = new ArrayList<>();

    @NonNull
    public List<String> importantFacts = new ArrayList<>();

    @NonNull
    public Map<String, String> metadata = new LinkedHashMap<>();

    @NonNull
    public static ConversationEntity createCapture(@NonNull String id,
                                                   @NonNull String title,
                                                   long createdAtMillis,
                                                   long durationMillis,
                                                   @NonNull String audioPath,
                                                   @NonNull String processingStatus) {
        ConversationEntity entity = new ConversationEntity();
        entity.id = id;
        entity.title = title;
        entity.createdAtMillis = createdAtMillis;
        entity.updatedAtMillis = createdAtMillis;
        entity.durationMillis = durationMillis;
        entity.audioPath = audioPath;
        entity.processingStatus = processingStatus;
        entity.transcript = "";
        entity.summary = "";
        entity.keyPoints = new ArrayList<>();
        entity.actionItems = new ArrayList<>();
        entity.importantFacts = new ArrayList<>();
        entity.metadata = new LinkedHashMap<>();
        return entity;
    }

    public boolean hasAudioFile() {
        return !audioPath.isEmpty();
    }
}
