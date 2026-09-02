package com.gamor.mithrax.domain.understanding;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits a transcript into clauses. Offline STT often omits punctuation, so this
 * also breaks before common fact-bearing phrases.
 */
public final class TranscriptSentences {

    private static final Pattern PUNCTUATION_SPLIT =
            Pattern.compile("(?<=[.!?])\\s+|\\n+");
    private static final Pattern CLAUSE_SPLIT = Pattern.compile(
            "(?i)\\s+(?=(?:the\\s+)?(?:client|customer)\\b"
                    + "|we\\s+(?:have\\s+)?(?:decided|agreed)"
                    + "|i\\s+prefer\\b"
                    + "|[a-z]{2,}\\s+will\\b"
                    + "|[a-z]{2,}\\s+(?:needs?|has|have)\\s+to\\b)");

    private TranscriptSentences() {
    }

    @NonNull
    public static List<String> split(@NonNull String transcript) {
        String trimmed = transcript.trim();
        List<String> sentences = new ArrayList<>();
        if (trimmed.isEmpty()) {
            return sentences;
        }
        for (String raw : PUNCTUATION_SPLIT.split(trimmed)) {
            String piece = raw.trim();
            if (piece.isEmpty()) {
                continue;
            }
            if (piece.length() > 80 && !piece.contains(".") && !piece.contains("?")
                    && !piece.contains("!")) {
                for (String clause : CLAUSE_SPLIT.split(piece)) {
                    addIfUseful(sentences, clause);
                }
            } else {
                addIfUseful(sentences, piece);
            }
        }
        return sentences;
    }

    private static void addIfUseful(@NonNull List<String> sentences, @NonNull String raw) {
        String sentence = raw.replaceAll("[.!?]+$", "").trim();
        if (sentence.length() >= 8) {
            sentences.add(sentence);
        }
    }
}
