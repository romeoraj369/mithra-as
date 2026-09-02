package com.gamor.mithrax.domain.understanding;

import androidx.annotation.NonNull;

import com.gamor.mithrax.domain.conversation.ActionItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Structured local understanding of a conversation. Independent of any remote API.
 */
public final class ConversationInsights {

    @NonNull
    public final String summary;
    @NonNull
    public final List<String> keyPoints;
    @NonNull
    public final List<ActionItem> actionItems;
    @NonNull
    public final List<String> importantFacts;

    public ConversationInsights(@NonNull String summary,
                                @NonNull List<String> keyPoints,
                                @NonNull List<ActionItem> actionItems,
                                @NonNull List<String> importantFacts) {
        this.summary = summary;
        this.keyPoints = Collections.unmodifiableList(new ArrayList<>(keyPoints));
        this.actionItems = Collections.unmodifiableList(new ArrayList<>(actionItems));
        this.importantFacts = Collections.unmodifiableList(new ArrayList<>(importantFacts));
    }

    @NonNull
    public static ConversationInsights empty() {
        return new ConversationInsights(
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    @NonNull
    public ConversationInsights merge(@NonNull ConversationInsights other) {
        String mergedSummary = summary;
        if (other.summary != null && !other.summary.trim().isEmpty()) {
            mergedSummary = mergedSummary.isEmpty()
                    ? other.summary.trim()
                    : mergedSummary + "\n\n" + other.summary.trim();
        }
        return new ConversationInsights(
                mergedSummary,
                mergeUnique(keyPoints, other.keyPoints),
                mergeActionItems(actionItems, other.actionItems),
                mergeUnique(importantFacts, other.importantFacts)
        );
    }

    @NonNull
    private static List<String> mergeUnique(@NonNull List<String> first, @NonNull List<String> second) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String item : first) {
            if (item != null && !item.trim().isEmpty()) {
                values.add(item.trim());
            }
        }
        for (String item : second) {
            if (item != null && !item.trim().isEmpty()) {
                values.add(item.trim());
            }
        }
        return new ArrayList<>(values);
    }

    @NonNull
    private static List<ActionItem> mergeActionItems(@NonNull List<ActionItem> first,
                                                     @NonNull List<ActionItem> second) {
        List<ActionItem> merged = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (ActionItem item : first) {
            addAction(merged, seen, item);
        }
        for (ActionItem item : second) {
            addAction(merged, seen, item);
        }
        return merged;
    }

    private static void addAction(@NonNull List<ActionItem> merged,
                                  @NonNull LinkedHashSet<String> seen,
                                  ActionItem item) {
        if (item == null || item.text.trim().isEmpty()) {
            return;
        }
        String key = item.text.trim();
        if (seen.add(key)) {
            merged.add(new ActionItem(key, item.completed));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConversationInsights)) {
            return false;
        }
        ConversationInsights that = (ConversationInsights) o;
        return summary.equals(that.summary)
                && keyPoints.equals(that.keyPoints)
                && actionItems.equals(that.actionItems)
                && importantFacts.equals(that.importantFacts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(summary, keyPoints, actionItems, importantFacts);
    }
}
