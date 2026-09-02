package com.gamor.mithrax.domain.ask;

import androidx.annotation.NonNull;

import com.gamor.mithrax.domain.search.SearchQueryNormalizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Ranks retrieved passages by overlap with the question. No embeddings.
 */
public final class ContextSelector {

    public static final int MAX_PASSAGES = 6;

    private ContextSelector() {
    }

    @NonNull
    public static RetrievedContext select(@NonNull String question, @NonNull List<ContextPassage> candidates) {
        SearchQueryNormalizer.ParsedQuery parsed = SearchQueryNormalizer.parse(question);
        List<ContextPassage> scored = new ArrayList<>();
        for (ContextPassage candidate : candidates) {
            int score = score(parsed.tokens, candidate);
            if (score <= 0) {
                continue;
            }
            scored.add(new ContextPassage(
                    candidate.kind,
                    candidate.conversationId,
                    candidate.conversationTitle,
                    candidate.text,
                    candidate.memoryType,
                    score
            ));
        }
        Collections.sort(scored, new Comparator<ContextPassage>() {
            @Override
            public int compare(ContextPassage left, ContextPassage right) {
                if (right.score != left.score) {
                    return right.score - left.score;
                }
                int kind = kindRank(left.kind) - kindRank(right.kind);
                if (kind != 0) {
                    return kind;
                }
                return left.conversationTitle.compareToIgnoreCase(right.conversationTitle);
            }
        });
        if (scored.size() > MAX_PASSAGES) {
            scored = new ArrayList<>(scored.subList(0, MAX_PASSAGES));
        }
        return new RetrievedContext(scored);
    }

    static int score(@NonNull List<String> tokens, @NonNull ContextPassage passage) {
        if (tokens.isEmpty() || passage.text.trim().isEmpty()) {
            return 0;
        }
        String haystack = (passage.text + " " + nullToEmpty(passage.memoryType)
                + " " + passage.conversationTitle).toLowerCase(Locale.US);
        int hits = 0;
        for (String token : tokens) {
            if (haystack.contains(token.toLowerCase(Locale.US))) {
                hits++;
            }
        }
        if (hits == 0) {
            return 0;
        }
        int score = hits * 10;
        if (passage.kind == ContextPassage.Kind.MEMORY) {
            score += 4;
            if ("FACT".equals(passage.memoryType)
                    || "DEADLINE".equals(passage.memoryType)
                    || "ACTION".equals(passage.memoryType)
                    || "DECISION".equals(passage.memoryType)) {
                score += 3;
            }
        }
        return score;
    }

    private static int kindRank(@NonNull ContextPassage.Kind kind) {
        return kind == ContextPassage.Kind.MEMORY ? 0 : 1;
    }

    @NonNull
    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
