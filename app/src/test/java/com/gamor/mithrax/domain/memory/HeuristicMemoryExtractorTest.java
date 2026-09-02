package com.gamor.mithrax.domain.memory;

import com.gamor.mithrax.domain.conversation.ActionItem;
import com.gamor.mithrax.domain.understanding.ConversationInsights;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HeuristicMemoryExtractorTest {

    private final HeuristicMemoryExtractor extractor = new HeuristicMemoryExtractor();

    @Test
    public void extractsOwnerTaskAndDeadline() {
        List<ExtractedMemory> memories = extractor.extract(request(
                "c1",
                "Raj will implement the payment API by Friday."));

        ExtractedMemory action = firstOfType(memories, MemoryType.ACTION);
        assertEquals("Raj will implement the payment API by Friday.", action.content);
        assertEquals("Raj", action.metadata.get(HeuristicMemoryExtractor.META_OWNER));
        assertEquals("implement the payment API", action.metadata.get(HeuristicMemoryExtractor.META_TASK));
        assertEquals("Friday", action.metadata.get(HeuristicMemoryExtractor.META_DEADLINE));

        ExtractedMemory person = firstOfType(memories, MemoryType.PERSON);
        assertEquals("Raj", person.content);

        ExtractedMemory deadline = firstOfType(memories, MemoryType.DEADLINE);
        assertTrue(deadline.content.toLowerCase().contains("friday"));
        assertTrue(deadline.content.toLowerCase().contains("payment api"));
    }

    @Test
    public void extractsClientRequestAsDurableFact() {
        List<ExtractedMemory> memories = extractor.extract(request(
                "c2",
                "The client wants the API ready by Friday."));

        ExtractedMemory fact = firstOfType(memories, MemoryType.FACT);
        assertEquals("Client requested API completion by Friday.", fact.content);
        assertEquals("client", fact.metadata.get(HeuristicMemoryExtractor.META_ACTOR));
        assertEquals("Friday", fact.metadata.get(HeuristicMemoryExtractor.META_DEADLINE));
        assertTrue(hasType(memories, MemoryType.DEADLINE));
    }

    @Test
    public void extractsDecisionAndPreference() {
        List<ExtractedMemory> memories = extractor.extract(request(
                "c3",
                "We decided to use Room for local storage. I prefer dark mode."));

        assertEquals("We decided to use Room for local storage.",
                firstOfType(memories, MemoryType.DECISION).content);
        assertEquals("Preference: dark mode.",
                firstOfType(memories, MemoryType.PREFERENCE).content);
    }

    @Test
    public void emptyTranscriptYieldsNoMemories() {
        assertTrue(extractor.extract(request("c4", "   ")).isEmpty());
    }

    @Test
    public void liftsActionItemsFromInsights() {
        ConversationInsights insights = new ConversationInsights(
                "Standup notes",
                Collections.emptyList(),
                Collections.singletonList(new ActionItem("Email the design doc", false)),
                Collections.singletonList("Budget is unchanged")
        );
        List<ExtractedMemory> memories = extractor.extract(
                new MemoryExtractionRequest("c5", "", insights));
        assertEquals("Email the design doc.", firstOfType(memories, MemoryType.ACTION).content);
        assertEquals("Budget is unchanged.", firstOfType(memories, MemoryType.FACT).content);
    }

    private static MemoryExtractionRequest request(String id, String transcript) {
        return new MemoryExtractionRequest(id, transcript, null);
    }

    private static ExtractedMemory firstOfType(List<ExtractedMemory> memories, MemoryType type) {
        for (ExtractedMemory memory : memories) {
            if (memory.type == type) {
                return memory;
            }
        }
        throw new AssertionError("Missing memory type " + type + " in " + memories);
    }

    private static boolean hasType(List<ExtractedMemory> memories, MemoryType type) {
        for (ExtractedMemory memory : memories) {
            if (memory.type == type) {
                return true;
            }
        }
        return false;
    }
}
