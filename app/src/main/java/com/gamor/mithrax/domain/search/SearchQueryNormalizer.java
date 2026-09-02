package com.gamor.mithrax.domain.search;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Turns a typed question into FTS tokens. Safe for SQLite MATCH (no cloud NLP).
 */
public final class SearchQueryNormalizer {

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "a", "an", "the", "and", "or", "of", "to", "in", "on", "for", "with", "about",
            "what", "was", "were", "is", "are", "be", "been", "being",
            "discussed", "discuss", "discussion", "tell", "me", "please",
            "how", "when", "where", "who", "why", "did", "does", "do",
            "can", "could", "would", "should", "any", "some", "this", "that",
            "from", "into", "over", "after", "before", "by",
            "say", "said", "says", "mention", "mentioned", "asked", "ask", "told"
    ));

    private static final Map<String, List<String>> SYNONYMS = new LinkedHashMap<>();

    static {
        SYNONYMS.put("deadline", Arrays.asList("due", "friday", "ready", "completion"));
        SYNONYMS.put("due", Arrays.asList("deadline", "friday"));
        SYNONYMS.put("client", Arrays.asList("customer"));
        SYNONYMS.put("customer", Arrays.asList("client"));
    }

    private SearchQueryNormalizer() {
    }

    @NonNull
    public static ParsedQuery parse(@NonNull String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return new ParsedQuery(trimmed, Collections.emptyList(), "");
        }
        String[] parts = trimmed.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", " ").trim()
                .split("\\s+");
        List<String> tokens = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String part : parts) {
            if (part.length() < 2 || STOPWORDS.contains(part) || !seen.add(part)) {
                continue;
            }
            tokens.add(part);
        }
        if (tokens.isEmpty()) {
            return new ParsedQuery(trimmed, tokens, "");
        }
        List<String> scoringTokens = new ArrayList<>(tokens);
        for (String token : tokens) {
            List<String> extras = SYNONYMS.get(token);
            if (extras == null) {
                continue;
            }
            for (String extra : extras) {
                if (seen.add(extra)) {
                    scoringTokens.add(extra);
                }
            }
        }
        return new ParsedQuery(trimmed, scoringTokens, buildMatch(tokens));
    }

    @NonNull
    private static String buildMatch(@NonNull List<String> tokens) {
        StringBuilder match = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) {
                match.append(' ');
            }
            String token = tokens.get(i);
            List<String> extras = SYNONYMS.get(token);
            if (extras == null || extras.isEmpty()) {
                match.append(token).append('*');
                continue;
            }
            match.append('(').append(token).append('*');
            for (String extra : extras) {
                match.append(" OR ").append(extra).append('*');
            }
            match.append(')');
        }
        return match.toString();
    }

    public static final class ParsedQuery {
        @NonNull
        public final String raw;
        @NonNull
        public final List<String> tokens;
        @NonNull
        public final String ftsMatch;

        ParsedQuery(@NonNull String raw, @NonNull List<String> tokens, @NonNull String ftsMatch) {
            this.raw = raw;
            this.tokens = Collections.unmodifiableList(new ArrayList<>(tokens));
            this.ftsMatch = ftsMatch;
        }

        public boolean isEmpty() {
            return tokens.isEmpty();
        }
    }
}
