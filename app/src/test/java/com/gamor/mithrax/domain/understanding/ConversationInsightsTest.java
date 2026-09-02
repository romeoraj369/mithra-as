package com.gamor.mithrax.domain.understanding;

import com.gamor.mithrax.domain.conversation.ActionItem;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class ConversationInsightsTest {

    @Test
    public void mergeDeduplicatesAndJoinsSummaries() {
        ConversationInsights first = new ConversationInsights(
                "First",
                Collections.singletonList("A"),
                Collections.singletonList(new ActionItem("Call", false)),
                Collections.singletonList("Budget 5k")
        );
        ConversationInsights second = new ConversationInsights(
                "Second",
                Arrays.asList("A", "B"),
                Collections.singletonList(new ActionItem("Call", true)),
                Collections.singletonList("Deadline Friday")
        );
        ConversationInsights merged = first.merge(second);
        assertEquals("First\n\nSecond", merged.summary);
        assertEquals(Arrays.asList("A", "B"), merged.keyPoints);
        assertEquals(Collections.singletonList(new ActionItem("Call", false)), merged.actionItems);
        assertEquals(Arrays.asList("Budget 5k", "Deadline Friday"), merged.importantFacts);
    }
}
