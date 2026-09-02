package com.gamor.mithrax.domain.ask;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds an answer only from retrieved local passages. Does not invent facts
 * and does not call a network. Used when a bundled on-device LLM is not available.
 */
public final class GroundedContextAnswerer {

    public static final String NOT_FOUND =
            "I don't have that in your stored conversations or memories on this device.";

    private static final Pattern CLIENT_API_COMPLETION = Pattern.compile(
            "(?i)client requested api completion by (.+?)\\.?$");
    private static final Pattern CLIENT_WANTS_API = Pattern.compile(
            "(?i)(?:the\\s+)?client wants (?:the\\s+)?api ready by (.+?)\\.?$");

    @NonNull
    public AskResult answer(@NonNull String question, @NonNull RetrievedContext context) {
        if (context.isEmpty()) {
            return AskResult.notFound(question, NOT_FOUND);
        }
        ContextPassage best = context.passages.get(0);
        if (best.score <= 0) {
            return AskResult.notFound(question, NOT_FOUND);
        }
        String text = phrase(best.text);
        List<AnswerSource> sources = sources(context);
        return AskResult.answered(question, text, sources, false);
    }

    @NonNull
    static String phrase(@NonNull String raw) {
        String trimmed = firstSentence(raw.replaceAll("\\s+", " ").trim());
        Matcher completion = CLIENT_API_COMPLETION.matcher(trimmed);
        if (completion.matches()) {
            return "The client requested that the API be ready by " + completion.group(1).trim() + ".";
        }
        Matcher wants = CLIENT_WANTS_API.matcher(trimmed);
        if (wants.matches()) {
            return "The client requested that the API be ready by " + wants.group(1).trim() + ".";
        }
        if (trimmed.isEmpty()) {
            return NOT_FOUND;
        }
        if (!trimmed.endsWith(".") && !trimmed.endsWith("?") && !trimmed.endsWith("!")) {
            trimmed = trimmed + ".";
        }
        String first = trimmed.substring(0, 1).toUpperCase(Locale.US);
        return first + trimmed.substring(1);
    }

    @NonNull
    private static String firstSentence(@NonNull String text) {
        int end = text.indexOf(". ");
        if (end > 0) {
            return text.substring(0, end + 1).trim();
        }
        return text;
    }

    @NonNull
    private static List<AnswerSource> sources(@NonNull RetrievedContext context) {
        LinkedHashSet<AnswerSource> unique = new LinkedHashSet<>();
        for (ContextPassage passage : context.passages) {
            unique.add(new AnswerSource(
                    passage.conversationId,
                    passage.conversationTitle,
                    excerpt(passage.text)
            ));
        }
        return new ArrayList<>(unique);
    }

    @NonNull
    private static String excerpt(@NonNull String text) {
        String trimmed = text.replaceAll("\\s+", " ").trim();
        if (trimmed.length() <= 160) {
            return trimmed;
        }
        return trimmed.substring(0, 160).trim() + "…";
    }
}
