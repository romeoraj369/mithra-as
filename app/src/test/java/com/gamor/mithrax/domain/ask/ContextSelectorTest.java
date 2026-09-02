package com.gamor.mithrax.domain.ask;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ContextSelectorTest {

    @Test
    public void prefersFactMemoryMatchingClientApiDeadline() {
        ContextPassage fact = new ContextPassage(
                ContextPassage.Kind.MEMORY,
                "c-monday",
                "Monday client call",
                "Client requested API completion by Friday. deadline Friday",
                "FACT",
                0
        );
        ContextPassage transcript = new ContextPassage(
                ContextPassage.Kind.CONVERSATION,
                "c-monday",
                "Monday client call",
                "The client wants the API ready by Friday.",
                null,
                0
        );

        RetrievedContext selected = ContextSelector.select(
                "What did the client say about the API deadline?",
                Arrays.asList(transcript, fact));

        assertEquals(2, selected.passages.size());
        assertEquals(ContextPassage.Kind.MEMORY, selected.passages.get(0).kind);
        assertEquals("FACT", selected.passages.get(0).memoryType);
        assertTrue(selected.passages.get(0).score > selected.passages.get(1).score);
    }

    @Test
    public void dropsPassagesWithNoTokenOverlap() {
        RetrievedContext selected = ContextSelector.select(
                "What did the client say about the API deadline?",
                Collections.singletonList(new ContextPassage(
                        ContextPassage.Kind.CONVERSATION,
                        "c-other",
                        "Lunch",
                        "We ordered sandwiches.",
                        null,
                        0
                )));
        assertTrue(selected.isEmpty());
    }
}
