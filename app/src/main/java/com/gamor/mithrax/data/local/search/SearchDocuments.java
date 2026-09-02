package com.gamor.mithrax.data.local.search;

import androidx.annotation.NonNull;

import com.gamor.mithrax.data.local.conversation.ConversationEntity;
import com.gamor.mithrax.data.local.memory.MemoryEntity;
import com.gamor.mithrax.domain.conversation.ActionItem;

import java.util.Map;

final class SearchDocuments {

    private SearchDocuments() {
    }

    @NonNull
    static ConversationFtsEntity conversation(@NonNull ConversationEntity entity) {
        return new ConversationFtsEntity(
                entity.id,
                nullToEmpty(entity.title),
                nullToEmpty(entity.transcript),
                nullToEmpty(entity.summary),
                insights(entity),
                metadataText(entity.metadata)
        );
    }

    @NonNull
    static MemoryFtsEntity memory(@NonNull MemoryEntity entity) {
        StringBuilder body = new StringBuilder();
        append(body, entity.type);
        append(body, entity.content);
        append(body, metadataText(entity.metadata));
        return new MemoryFtsEntity(entity.id, entity.sourceConversationId, body.toString().trim());
    }

    @NonNull
    private static String insights(@NonNull ConversationEntity entity) {
        StringBuilder text = new StringBuilder();
        if (entity.keyPoints != null) {
            for (String point : entity.keyPoints) {
                append(text, point);
            }
        }
        if (entity.actionItems != null) {
            for (ActionItem item : entity.actionItems) {
                if (item != null) {
                    append(text, item.text);
                }
            }
        }
        if (entity.importantFacts != null) {
            for (String fact : entity.importantFacts) {
                append(text, fact);
            }
        }
        return text.toString().trim();
    }

    @NonNull
    static String metadataText(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (String value : metadata.values()) {
            append(text, value);
        }
        for (String key : metadata.keySet()) {
            append(text, key);
        }
        return text.toString().trim();
    }

    private static void append(@NonNull StringBuilder target, String value) {
        if (value != null && !value.trim().isEmpty()) {
            if (target.length() > 0) {
                target.append(' ');
            }
            target.append(value.trim());
        }
    }

    @NonNull
    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
