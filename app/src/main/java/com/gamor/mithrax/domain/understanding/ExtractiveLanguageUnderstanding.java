package com.gamor.mithrax.domain.understanding;

import androidx.annotation.NonNull;

import com.gamor.mithrax.domain.conversation.ActionItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * On-device understanding of a transcript: summary, key points, actions, and
 * facts. Does not call a network. Used as the local language-model implementation
 * until a bundled neural runtime is swapped in.
 */
public final class ExtractiveLanguageUnderstanding {

    private static final Pattern ACTION_CUE = Pattern.compile(
            "(?i)\\b(will|needs? to|has to|have to|going to|gonna|should|must)\\b");
    private static final Pattern FACT_CUE = Pattern.compile(
            "(?i)\\b(client|customer|deadline|due|friday|monday|tuesday|wednesday|"
                    + "thursday|saturday|sunday|tomorrow|decided|agreed|prefer|"
                    + "wants?|requested|remember)\\b");
    private static final Pattern DECISION_CUE = Pattern.compile(
            "(?i)\\b(decided|agreed)\\b");

    @NonNull
    public ConversationInsights understand(@NonNull String transcript) {
        List<String> sentences = TranscriptSentences.split(transcript);
        if (sentences.isEmpty()) {
            return ConversationInsights.empty();
        }
        List<ScoredSentence> scored = new ArrayList<>();
        for (String sentence : sentences) {
            scored.add(new ScoredSentence(sentence, score(sentence)));
        }
        Collections.sort(scored, new Comparator<ScoredSentence>() {
            @Override
            public int compare(ScoredSentence left, ScoredSentence right) {
                if (right.score != left.score) {
                    return right.score - left.score;
                }
                return 0;
            }
        });
        List<String> keyPoints = new ArrayList<>();
        List<ActionItem> actions = new ArrayList<>();
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        LinkedHashSet<String> seenPoints = new LinkedHashSet<>();
        for (ScoredSentence item : scored) {
            String phrase = phrase(item.text);
            if (item.score <= 0) {
                continue;
            }
            if (seenPoints.add(phrase.toLowerCase(Locale.US)) && keyPoints.size() < 6) {
                keyPoints.add(phrase);
            }
            if (ACTION_CUE.matcher(item.text).find() && actions.size() < 8) {
                actions.add(new ActionItem(phrase, false));
            } else if (item.score >= 3 && facts.size() < 8) {
                facts.add(phrase);
            } else if (DECISION_CUE.matcher(item.text).find() || FACT_CUE.matcher(item.text).find()) {
                facts.add(phrase);
            }
        }
        if (keyPoints.isEmpty()) {
            keyPoints.add(phrase(sentences.get(0)));
        }
        int summaryCount = Math.min(3, keyPoints.size());
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < summaryCount; i++) {
            if (i > 0) {
                summary.append(' ');
            }
            summary.append(keyPoints.get(i));
        }
        if (facts.isEmpty() && !keyPoints.isEmpty()) {
            facts.add(keyPoints.get(0));
        }
        return new ConversationInsights(
                summary.toString(),
                keyPoints,
                actions,
                new ArrayList<>(facts)
        );
    }

    static int score(@NonNull String sentence) {
        int score = 0;
        if (ACTION_CUE.matcher(sentence).find()) {
            score += 4;
        }
        if (DECISION_CUE.matcher(sentence).find()) {
            score += 4;
        }
        if (FACT_CUE.matcher(sentence).find()) {
            score += 3;
        }
        if (sentence.length() >= 12 && sentence.length() <= 220) {
            score += 1;
        }
        return score;
    }

    @NonNull
    static String phrase(@NonNull String raw) {
        String trimmed = raw.replaceAll("\\s+", " ").trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        String first = trimmed.substring(0, 1).toUpperCase(Locale.US);
        String body = first + trimmed.substring(1);
        if (!body.endsWith(".") && !body.endsWith("?") && !body.endsWith("!")) {
            body = body + ".";
        }
        return body;
    }

    private static final class ScoredSentence {
        final String text;
        final int score;

        ScoredSentence(String text, int score) {
            this.text = text;
            this.score = score;
        }
    }
}
