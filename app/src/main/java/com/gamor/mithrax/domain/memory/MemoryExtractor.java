package com.gamor.mithrax.domain.memory;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Turns a conversation into durable local memories. Implementations must not
 * call a network or cloud API.
 */
public interface MemoryExtractor {

    @NonNull
    List<ExtractedMemory> extract(@NonNull MemoryExtractionRequest request);
}
