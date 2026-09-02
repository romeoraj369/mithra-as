package com.gamor.mithrax.data.local.search;

import androidx.room.Entity;
import androidx.room.FtsOptions;
import androidx.room.Fts4;
import androidx.room.Ignore;

/**
 * SQLite FTS4 index of memory text. sourceId links back to memories.id.
 */
@Fts4(
        tokenizer = FtsOptions.TOKENIZER_PORTER,
        notIndexed = {"sourceId", "sourceConversationId"}
)
@Entity(tableName = "memory_fts")
public class MemoryFtsEntity {

    public String sourceId;
    public String sourceConversationId;
    public String body;

    public MemoryFtsEntity() {
    }

    @Ignore
    public MemoryFtsEntity(String sourceId, String sourceConversationId, String body) {
        this.sourceId = sourceId;
        this.sourceConversationId = sourceConversationId;
        this.body = body;
    }
}
