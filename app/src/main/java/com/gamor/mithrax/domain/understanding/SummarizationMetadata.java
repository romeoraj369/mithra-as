package com.gamor.mithrax.domain.understanding;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gamor.mithrax.domain.ai.SummarizationResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Local conversation metadata keys for summarization. Not sent anywhere.
 */
public final class SummarizationMetadata {

    public static final String KEY_STATUS = "summarization";
    public static final String KEY_ERROR = "summarizationError";
    public static final String KEY_CHUNKS = "summarizationChunks";

    public static final String COMPLETED = "completed";
    public static final String SKIPPED_EMPTY = "skipped_empty";
    public static final String UNAVAILABLE = "unavailable";
    public static final String FAILED = "failed";
    public static final String CANCELLED = "cancelled";

    private SummarizationMetadata() {
    }

    @NonNull
    public static Map<String, String> apply(@Nullable Map<String, String> existing,
                                            @NonNull SummarizationResult result) {
        Map<String, String> metadata = existing == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(existing);
        metadata.put(KEY_STATUS, statusValue(result.kind));
        if (result.message == null || result.message.isEmpty()) {
            metadata.remove(KEY_ERROR);
        } else {
            metadata.put(KEY_ERROR, result.message);
        }
        if (result.chunkCount > 0) {
            metadata.put(KEY_CHUNKS, String.valueOf(result.chunkCount));
        } else {
            metadata.remove(KEY_CHUNKS);
        }
        return metadata;
    }

    @NonNull
    private static String statusValue(@NonNull SummarizationResult.Kind kind) {
        switch (kind) {
            case SUCCESS:
                return COMPLETED;
            case SKIPPED_EMPTY:
                return SKIPPED_EMPTY;
            case UNAVAILABLE:
                return UNAVAILABLE;
            case FAILED:
                return FAILED;
            case CANCELLED:
                return CANCELLED;
            default:
                return FAILED;
        }
    }
}
