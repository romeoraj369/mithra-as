package com.gamor.mithrax.domain.tts;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Splits answers into engine-safe utterances. Android TTS often rejects strings over ~4k chars.
 */
public final class TtsTextChunker {

    public static final int MAX_CHUNK_CHARS = 3_500;

    private TtsTextChunker() {
    }

    @NonNull
    public static List<String> chunk(@NonNull String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return Collections.emptyList();
        }
        if (trimmed.length() <= MAX_CHUNK_CHARS) {
            return Collections.singletonList(trimmed);
        }
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String[] sentences = trimmed.split("(?<=[.!?])\\s+");
        for (String sentence : sentences) {
            if (sentence.length() > MAX_CHUNK_CHARS) {
                flush(current, chunks);
                splitLong(sentence, chunks);
                continue;
            }
            if (current.length() > 0 && current.length() + 1 + sentence.length() > MAX_CHUNK_CHARS) {
                flush(current, chunks);
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(sentence);
        }
        flush(current, chunks);
        return chunks;
    }

    private static void splitLong(@NonNull String text, @NonNull List<String> chunks) {
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + MAX_CHUNK_CHARS);
            if (end < text.length()) {
                int space = text.lastIndexOf(' ', end);
                if (space > start) {
                    end = space;
                }
            }
            chunks.add(text.substring(start, end).trim());
            start = end;
            while (start < text.length() && text.charAt(start) == ' ') {
                start++;
            }
        }
    }

    private static void flush(@NonNull StringBuilder current, @NonNull List<String> chunks) {
        if (current.length() == 0) {
            return;
        }
        chunks.add(current.toString().trim());
        current.setLength(0);
    }
}
