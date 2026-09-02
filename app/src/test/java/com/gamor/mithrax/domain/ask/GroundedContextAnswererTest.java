package com.gamor.mithrax.domain.ask;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GroundedContextAnswererTest {

    private final GroundedContextAnswerer answerer = new GroundedContextAnswerer();

    @Test
    public void phrasesClientApiCompletionMemory() {
        RetrievedContext context = new RetrievedContext(Collections.singletonList(
                new ContextPassage(
                        ContextPassage.Kind.MEMORY,
                        "c-monday",
                        "Monday client call",
                        "Client requested API completion by Friday. deadline Friday",
                        "FACT",
                        30
                )));

        AskResult result = answerer.answer(
                "What did the client say about the API deadline?", context);

        assertEquals(AskResult.Kind.ANSWERED, result.kind);
        assertEquals("The client requested that the API be ready by Friday.", result.answer);
        assertEquals("c-monday", result.sources.get(0).conversationId);
        assertEquals("Monday client call", result.sources.get(0).title);
    }

    @Test
    public void phrasesClientWantsApiTranscript() {
        String phrased = GroundedContextAnswerer.phrase("The client wants the API ready by Friday.");
        assertEquals("The client requested that the API be ready by Friday.", phrased);
    }

    @Test
    public void notFoundWhenContextEmptyOrUnrelated() {
        assertEquals(AskResult.Kind.NOT_FOUND,
                answerer.answer("anything", RetrievedContext.empty()).kind);
        AskResult missed = answerer.answer("anything", new RetrievedContext(Collections.singletonList(
                new ContextPassage(ContextPassage.Kind.CONVERSATION, "c1", "Other",
                        "We talked about lunch.", null, 0))));
        assertEquals(AskResult.Kind.NOT_FOUND, missed.kind);
        assertEquals(GroundedContextAnswerer.NOT_FOUND, missed.answer);
    }

    @Test
    public void doesNotInventWhenPassageIsUnrelatedWording() {
        AskResult result = answerer.answer("What did the client say about the API deadline?",
                new RetrievedContext(Arrays.asList(
                        new ContextPassage(ContextPassage.Kind.MEMORY, "c1", "Standup",
                                "Raj will implement the payment API by Friday.",
                                "ACTION",
                                20))));
        assertEquals(AskResult.Kind.ANSWERED, result.kind);
        assertTrue(result.answer.toLowerCase().contains("payment api"));
        assertTrue(result.answer.toLowerCase().contains("friday"));
    }
}
