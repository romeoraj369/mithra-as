package com.gamor.mithrax.domain.understanding;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Splits long transcripts so a future local model can stay within a context window.
 */
public final class TranscriptChunker {

    public static final int DEFAULT_MAX_CHARS = 6_000;
    public static final int DEFAULT_OVERLAP_CHARS = 400;

    private final int maxChars;
    private final int overlapChars;

    public TranscriptChunker() {
        this(DEFAULT_MAX_CHARS, DEFAULT_OVERLAP_CHARS);
    }

    public TranscriptChunker(int maxChars, int overlapChars) {
        if (maxChars <= 0) {
            throw new IllegalArgumentException("maxChars must be positive");
        }
        if (overlapChars < 0 || overlapChars >= maxChars) {
            throw new IllegalArgumentException("overlap must be >= 0 and < maxChars");
        }
        this.maxChars = maxChars;
        this.overlapChars = overlapChars;
    }

    @NonNull
    public List<String> chunk(@NonNull String transcript) {
        String text = transcript.trim();
        if (text.isEmpty()) {
            return Collections.emptyList();
        }
        if (text.length() <= maxChars) {
            return Collections.singletonList(text);
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxChars, text.length());
            if (end < text.length()) {
                int breakAt = lastNaturalBreak(text, start, end);
                if (breakAt > start) {
                    end = breakAt;
                }
            }
            String piece = text.substring(start, end).trim();
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
            if (end >= text.length()) {
                break;
            }
            int nextStart = end - overlapChars;
            if (nextStart <= start) {
                nextStart = end;
            }
            start = nextStart;
        }
        return chunks;
    }

    private static int lastNaturalBreak(@NonNull String text, int start, int end) {
        int minEnd = start + Math.max(1, (end - start) / 4);
        for (int i = end - 1; i >= minEnd; i--) {
            char c = text.charAt(i);
            if (c == '\n' || c == '.' || c == '!' || c == '?') {
                return i + 1;
            }
        }
        for (int i = end - 1; i >= minEnd; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i + 1;
            }
        }
        return end;
    }
}
