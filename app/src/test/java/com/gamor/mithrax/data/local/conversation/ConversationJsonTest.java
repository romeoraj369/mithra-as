package com.gamor.mithrax.data.local.conversation;

import com.gamor.mithrax.domain.conversation.ActionItem;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConversationJsonTest {

    @Test
    public void stringListRoundTrip() {
        List<String> original = Arrays.asList("alpha", "quote \"inside\"", "line\nbreak");
        String json = ConversationJson.stringList(original);
        assertEquals(original, ConversationJson.parseStringList(json));
    }

    @Test
    public void emptyAndNullListsBecomeEmpty() {
        assertTrue(ConversationJson.parseStringList("[]").isEmpty());
        assertTrue(ConversationJson.parseStringList(null).isEmpty());
        assertEquals("[]", ConversationJson.stringList(null));
        assertEquals("[]", ConversationJson.stringList(Collections.emptyList()));
    }

    @Test
    public void actionItemsRoundTrip() {
        List<ActionItem> original = Arrays.asList(
                new ActionItem("Call back", false),
                new ActionItem("Send \"notes\"", true)
        );
        String json = ConversationJson.actionItems(original);
        assertEquals(original, ConversationJson.parseActionItems(json));
    }

    @Test
    public void metadataRoundTripPreservesOrder() {
        Map<String, String> original = new LinkedHashMap<>();
        original.put("source", "microphone");
        original.put("codec", "aac");
        Map<String, String> parsed = ConversationJson.parseStringMap(ConversationJson.stringMap(original));
        assertEquals(original, parsed);
        assertEquals(Arrays.asList("source", "codec"), Arrays.asList(parsed.keySet().toArray()));
    }
}
