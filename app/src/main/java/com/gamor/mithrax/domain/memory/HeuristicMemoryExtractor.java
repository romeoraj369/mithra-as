package com.gamor.mithrax.domain.memory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gamor.mithrax.domain.conversation.ActionItem;
import com.gamor.mithrax.domain.understanding.ConversationInsights;
import com.gamor.mithrax.domain.understanding.TranscriptSentences;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * On-device extractor that keeps information likely to matter after the meeting.
 * Tolerates unpunctuated, lowercase offline STT. Swap for a neural extractor via
 * {@link MemoryExtractor} without changing callers.
 */
public final class HeuristicMemoryExtractor implements MemoryExtractor {

    public static final String META_ORIGIN = "origin";
    public static final String ORIGIN_HEURISTIC = "heuristic";
    public static final String META_OWNER = "owner";
    public static final String META_TASK = "task";
    public static final String META_DEADLINE = "deadline";
    public static final String META_ACTOR = "actor";
    public static final String META_NAME = "name";

    private static final String WEEKDAY =
            "monday|tuesday|wednesday|thursday|friday|saturday|sunday|"
                    + "tomorrow|tonight|today|eod|eow|next week";
    private static final Pattern WILL_BY = Pattern.compile(
            "(?i)\\b([a-z]{2,}(?:\\s+[a-z]{2,})?)\\s+will\\s+(.+?)\\s+by\\s+("
                    + WEEKDAY + "|\\d{1,2}(?:st|nd|rd|th)?(?:\\s+[a-z]+)?)\\b");
    private static final Pattern WILL = Pattern.compile(
            "(?i)\\b([a-z]{2,}(?:\\s+[a-z]{2,})?)\\s+will\\s+(.+)$");
    private static final Pattern CLIENT_WANTS_BY = Pattern.compile(
            "(?i)\\b(?:the\\s+)?(client|customer)\\s+wants?\\s+(.+?)\\s+by\\s+("
                    + WEEKDAY + "|\\d{1,2}(?:st|nd|rd|th)?(?:\\s+[a-z]+)?)\\b");
    private static final Pattern CLIENT_WANTS = Pattern.compile(
            "(?i)\\b(?:the\\s+)?(client|customer)\\s+wants?\\s+(.+)$");
    private static final Pattern DECISION = Pattern.compile(
            "(?i)\\bwe\\s+(?:have\\s+)?(?:decided|agreed)(?:\\s+to)?\\s+(.+)$");
    private static final Pattern PREFERENCE = Pattern.compile(
            "(?i)\\b(?:i|we|they|he|she)\\s+prefer(?:s)?\\s+(.+)$");
    private static final Pattern BY_DEADLINE = Pattern.compile(
            "(?i)\\bby\\s+(" + WEEKDAY + "|\\d{1,2}(?:st|nd|rd|th)?(?:\\s+[A-Za-z]+)?)\\b");

    @NonNull
    @Override
    public List<ExtractedMemory> extract(@NonNull MemoryExtractionRequest request) {
        List<ExtractedMemory> memories = new ArrayList<>();
        String transcript = request.transcript == null ? "" : request.transcript.trim();
        if (!transcript.isEmpty()) {
            scanWholeTranscript(transcript, memories);
            for (String sentence : TranscriptSentences.split(transcript)) {
                extractFromSentence(sentence, memories);
            }
        }
        addFromInsights(request.insights, memories);
        return dedupe(memories);
    }

    private void scanWholeTranscript(@NonNull String transcript, @NonNull List<ExtractedMemory> out) {
        Matcher willBy = WILL_BY.matcher(transcript);
        while (willBy.find()) {
            if (isFillerOwner(willBy.group(1))) {
                continue;
            }
            addAction(out, properName(willBy.group(1)), trimTask(willBy.group(2)),
                    cleanDeadline(willBy.group(3)), willBy.group(), 0.88f);
        }
        Matcher clientBy = CLIENT_WANTS_BY.matcher(transcript);
        while (clientBy.find()) {
            addClientRequest(out, clientBy.group(1), clientBy.group(2), clientBy.group(3));
        }
    }

    private void extractFromSentence(@NonNull String sentence, @NonNull List<ExtractedMemory> out) {
        Matcher willBy = WILL_BY.matcher(sentence);
        if (willBy.find() && !isFillerOwner(willBy.group(1))) {
            addAction(out, properName(willBy.group(1)), trimTask(willBy.group(2)),
                    cleanDeadline(willBy.group(3)), sentence, 0.88f);
            return;
        }
        Matcher clientBy = CLIENT_WANTS_BY.matcher(sentence);
        if (clientBy.find()) {
            addClientRequest(out, clientBy.group(1), clientBy.group(2), clientBy.group(3));
            return;
        }
        Matcher decision = DECISION.matcher(sentence);
        if (decision.find()) {
            String decided = trimTask(decision.group(1));
            Map<String, String> metadata = baseMetadata();
            metadata.put("decision", decided);
            out.add(new ExtractedMemory(
                    MemoryType.DECISION,
                    "We decided to " + decided + ".",
                    0.82f,
                    metadata));
            return;
        }
        Matcher preference = PREFERENCE.matcher(sentence);
        if (preference.find()) {
            String preferred = trimTask(preference.group(1));
            Map<String, String> metadata = baseMetadata();
            metadata.put("preference", preferred);
            out.add(new ExtractedMemory(
                    MemoryType.PREFERENCE,
                    "Preference: " + preferred + ".",
                    0.76f,
                    metadata));
            return;
        }
        Matcher will = WILL.matcher(sentence);
        if (will.find() && !isFillerOwner(will.group(1))) {
            addAction(out, properName(will.group(1)), trimTask(will.group(2)), null, sentence, 0.8f);
            return;
        }
        Matcher client = CLIENT_WANTS.matcher(sentence);
        if (client.find()) {
            String actor = capitalize(client.group(1));
            String want = trimTask(client.group(2));
            Map<String, String> metadata = baseMetadata();
            metadata.put(META_ACTOR, actor.toLowerCase(Locale.US));
            metadata.put(META_TASK, want);
            out.add(new ExtractedMemory(
                    MemoryType.FACT,
                    actor + " requested " + want + ".",
                    0.78f,
                    metadata));
            return;
        }
        Matcher by = BY_DEADLINE.matcher(sentence);
        if (by.find()) {
            String deadline = cleanDeadline(by.group(1));
            addDeadline(out, sentence, deadline, 0.7f);
            Map<String, String> metadata = baseMetadata();
            metadata.put(META_DEADLINE, deadline);
            out.add(new ExtractedMemory(MemoryType.CONTEXT, phrase(sentence), 0.62f, metadata));
        }
    }

    private void addClientRequest(@NonNull List<ExtractedMemory> out,
                                  @NonNull String actorRaw,
                                  @NonNull String wantRaw,
                                  @NonNull String deadlineRaw) {
        String actor = capitalize(actorRaw);
        String want = wantRaw.trim();
        String deadline = cleanDeadline(deadlineRaw);
        String content = clientRequestContent(want, deadline);
        Map<String, String> metadata = baseMetadata();
        metadata.put(META_ACTOR, actor.toLowerCase(Locale.US));
        metadata.put(META_TASK, want);
        metadata.put(META_DEADLINE, deadline);
        out.add(new ExtractedMemory(MemoryType.FACT, content, 0.84f, metadata));
        addDeadline(out, want, deadline, 0.8f);
    }

    private void addAction(@NonNull List<ExtractedMemory> out,
                           @NonNull String owner,
                           @NonNull String task,
                           @Nullable String deadline,
                           @NonNull String sentence,
                           float confidence) {
        Map<String, String> metadata = baseMetadata();
        metadata.put(META_OWNER, owner);
        metadata.put(META_TASK, task);
        if (deadline != null) {
            metadata.put(META_DEADLINE, deadline);
        }
        String content = deadline == null
                ? owner + " will " + task + "."
                : owner + " will " + task + " by " + deadline + ".";
        out.add(new ExtractedMemory(MemoryType.ACTION, content, confidence, metadata));
        Map<String, String> personMeta = baseMetadata();
        personMeta.put(META_NAME, owner);
        personMeta.put("role", "owner");
        out.add(new ExtractedMemory(MemoryType.PERSON, owner, Math.max(0.7f, confidence - 0.08f), personMeta));
        if (deadline != null) {
            addDeadline(out, task, deadline, confidence - 0.06f);
        }
    }

    private void addDeadline(@NonNull List<ExtractedMemory> out,
                             @NonNull String taskOrSentence,
                             @NonNull String deadline,
                             float confidence) {
        String task = trimTask(taskOrSentence);
        Map<String, String> metadata = baseMetadata();
        metadata.put(META_DEADLINE, deadline);
        metadata.put(META_TASK, task);
        String content = capitalize(task) + " by " + deadline + ".";
        out.add(new ExtractedMemory(MemoryType.DEADLINE, content, confidence, metadata));
    }

    private void addFromInsights(@Nullable ConversationInsights insights,
                                 @NonNull List<ExtractedMemory> out) {
        if (insights == null) {
            return;
        }
        for (ActionItem item : insights.actionItems) {
            if (item == null || item.text.trim().isEmpty()) {
                continue;
            }
            Map<String, String> metadata = baseMetadata();
            metadata.put(META_TASK, item.text.trim());
            metadata.put("from", "insights");
            out.add(new ExtractedMemory(
                    MemoryType.ACTION,
                    item.text.trim().endsWith(".") ? item.text.trim() : item.text.trim() + ".",
                    0.7f,
                    metadata));
        }
        for (String fact : insights.importantFacts) {
            if (fact == null || fact.trim().isEmpty()) {
                continue;
            }
            Map<String, String> metadata = baseMetadata();
            metadata.put("from", "insights");
            out.add(new ExtractedMemory(
                    MemoryType.FACT,
                    fact.trim().endsWith(".") ? fact.trim() : fact.trim() + ".",
                    0.68f,
                    metadata));
        }
    }

    @NonNull
    private static String clientRequestContent(@NonNull String want, @NonNull String deadline) {
        if (want.toLowerCase(Locale.US).matches("(?:the\\s+)?api\\s+ready")) {
            return "Client requested API completion by " + deadline + ".";
        }
        return "Client requested " + trimTask(want) + " by " + deadline + ".";
    }

    @NonNull
    private static Map<String, String> baseMetadata() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put(META_ORIGIN, ORIGIN_HEURISTIC);
        return metadata;
    }

    @NonNull
    private static List<ExtractedMemory> dedupe(@NonNull List<ExtractedMemory> input) {
        List<ExtractedMemory> unique = new ArrayList<>();
        for (ExtractedMemory memory : input) {
            if (!memory.content.isEmpty() && !unique.contains(memory)) {
                unique.add(memory);
            }
        }
        return unique;
    }

    @NonNull
    private static String stripTrailingPunctuation(@NonNull String text) {
        return text.replaceAll("[.!?]+$", "").trim();
    }

    @NonNull
    private static String trimTask(@NonNull String text) {
        return stripTrailingPunctuation(text).replaceAll("(?i)^(to\\s+)", "").trim();
    }

    @NonNull
    private static String cleanDeadline(@NonNull String text) {
        String cleaned = stripTrailingPunctuation(text).trim();
        if (cleaned.isEmpty()) {
            return cleaned;
        }
        return cleaned.substring(0, 1).toUpperCase(Locale.US) + cleaned.substring(1).toLowerCase(Locale.US);
    }

    @NonNull
    private static String capitalize(@NonNull String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        return trimmed.substring(0, 1).toUpperCase(Locale.US)
                + trimmed.substring(1).toLowerCase(Locale.US);
    }

    @NonNull
    private static String properName(@NonNull String text) {
        String[] parts = text.trim().split("\\s+");
        StringBuilder name = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (name.length() > 0) {
                name.append(' ');
            }
            name.append(capitalize(part));
        }
        return name.toString();
    }

    @NonNull
    private static String phrase(@NonNull String sentence) {
        String trimmed = sentence.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        if (!trimmed.endsWith(".")) {
            trimmed = trimmed + ".";
        }
        return capitalize(trimmed.substring(0, 1)) + trimmed.substring(1);
    }

    private static boolean isFillerOwner(@Nullable String owner) {
        if (owner == null) {
            return true;
        }
        String value = owner.trim().toLowerCase(Locale.US);
        return value.equals("the") || value.equals("we") || value.equals("they")
                || value.equals("this") || value.equals("that") || value.equals("it")
                || value.equals("i") || value.equals("you") || value.equals("he")
                || value.equals("she") || value.equals("someone") || value.equals("who");
    }
}
