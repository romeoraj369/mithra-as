package com.gamor.mithrax.data.local.search;

import androidx.room.Entity;
import androidx.room.FtsOptions;
import androidx.room.Fts4;
import androidx.room.Ignore;

/**
 * SQLite FTS4 index of conversation text. sourceId links back to conversations.id.
 */
@Fts4(
        tokenizer = FtsOptions.TOKENIZER_PORTER,
        notIndexed = {"sourceId"}
)
@Entity(tableName = "conversation_fts")
public class ConversationFtsEntity {

    public String sourceId;
    public String title;
    public String transcript;
    public String summary;
    public String insights;
    public String metadataText;

    public ConversationFtsEntity() {
    }

    @Ignore
    public ConversationFtsEntity(String sourceId,
                                 String title,
                                 String transcript,
                                 String summary,
                                 String insights,
                                 String metadataText) {
        this.sourceId = sourceId;
        this.title = title;
        this.transcript = transcript;
        this.summary = summary;
        this.insights = insights;
        this.metadataText = metadataText;
    }
}
