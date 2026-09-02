package com.gamor.mithrax.domain.memory;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A durable item inferred from a conversation, before it is persisted.
 */
public final class ExtractedMemory {

    @NonNull
    public final MemoryType type;
    @NonNull
    public final String content;
    public final float confidence;
    @NonNull
    public final Map<String, String> metadata;

    public ExtractedMemory(@NonNull MemoryType type,
                           @NonNull String content,
                           float confidence,
                           @NonNull Map<String, String> metadata) {
        this.type = type;
        this.content = content.trim();
        this.confidence = confidence;
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExtractedMemory)) {
            return false;
        }
        ExtractedMemory that = (ExtractedMemory) o;
        return type == that.type && content.equalsIgnoreCase(that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, content.toLowerCase());
    }
}
