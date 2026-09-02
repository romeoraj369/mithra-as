package com.gamor.mithrax.domain.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One local match. Metadata is display-only (type, timestamps, source conversation).
 */
public final class SearchHit {

    public enum Kind {
        CONVERSATION,
        MEMORY
    }

    @NonNull
    public final Kind kind;
    @NonNull
    public final String id;
    @NonNull
    public final String title;
    @NonNull
    public final String snippet;
    @NonNull
    public final Map<String, String> metadata;
    @Nullable
    public final String sourceConversationId;

    public SearchHit(@NonNull Kind kind,
                     @NonNull String id,
                     @NonNull String title,
                     @NonNull String snippet,
                     @NonNull Map<String, String> metadata,
                     @Nullable String sourceConversationId) {
        this.kind = kind;
        this.id = id;
        this.title = title;
        this.snippet = snippet;
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
        this.sourceConversationId = sourceConversationId;
    }
}
