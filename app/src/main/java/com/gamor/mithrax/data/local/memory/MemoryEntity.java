package com.gamor.mithrax.data.local.memory;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.gamor.mithrax.data.local.conversation.ConversationEntity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Durable local memory linked to a source conversation.
 * Vector embeddings, if added later, should live in a separate table keyed by {@link #id}.
 */
@Entity(
        tableName = "memories",
        foreignKeys = @ForeignKey(
                entity = ConversationEntity.class,
                parentColumns = "id",
                childColumns = "sourceConversationId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index("sourceConversationId"),
                @Index("type"),
                @Index("deleted"),
                @Index("createdAtMillis")
        }
)
public class MemoryEntity {

    @PrimaryKey
    @NonNull
    public String id = "";

    @NonNull
    public String type = "";

    @NonNull
    public String content = "";

    @NonNull
    public String sourceConversationId = "";

    public long createdAtMillis;

    public float confidence;

    @NonNull
    public Map<String, String> metadata = new LinkedHashMap<>();

    public boolean deleted;

    @NonNull
    public static MemoryEntity create(@NonNull String id,
                                      @NonNull String type,
                                      @NonNull String content,
                                      @NonNull String sourceConversationId,
                                      long createdAtMillis,
                                      float confidence,
                                      @NonNull Map<String, String> metadata) {
        MemoryEntity entity = new MemoryEntity();
        entity.id = id;
        entity.type = type;
        entity.content = content;
        entity.sourceConversationId = sourceConversationId;
        entity.createdAtMillis = createdAtMillis;
        entity.confidence = confidence;
        entity.metadata = new LinkedHashMap<>(metadata);
        entity.deleted = false;
        return entity;
    }
}
