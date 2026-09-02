package com.gamor.mithrax.domain.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;

public final class SearchSnippet {

    private SearchSnippet() {
    }

    @NonNull
    public static String extract(@Nullable String text, @NonNull List<String> tokens, int maxChars) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return "";
        }
        int window = Math.max(40, maxChars);
        String lower = normalized.toLowerCase(Locale.US);
        int found = -1;
        for (String token : tokens) {
            int at = lower.indexOf(token.toLowerCase(Locale.US));
            if (at >= 0 && (found < 0 || at < found)) {
                found = at;
            }
        }
        if (found < 0) {
            return ellipsize(normalized, window);
        }
        int start = Math.max(0, found - window / 4);
        int end = Math.min(normalized.length(), start + window);
        start = Math.max(0, end - window);
        String slice = normalized.substring(start, end).trim();
        if (start > 0) {
            slice = "…" + slice;
        }
        if (end < normalized.length()) {
            slice = slice + "…";
        }
        return slice;
    }

    @NonNull
    private static String ellipsize(@NonNull String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars).trim() + "…";
    }
}
