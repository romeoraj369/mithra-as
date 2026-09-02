package com.gamor.mithrax.device.ai;

import com.gamor.mithrax.domain.ai.AnswerRequest;
import com.gamor.mithrax.domain.ai.AnswerResult;
import com.gamor.mithrax.domain.ai.SummarizationRequest;
import com.gamor.mithrax.domain.ai.SummarizationResult;
import com.gamor.mithrax.domain.ask.ContextPassage;
import com.gamor.mithrax.domain.ask.GroundedContextAnswerer;
import com.gamor.mithrax.domain.ask.RetrievedContext;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OnDeviceLocalLanguageModelTest {

    @Test
    public void isAvailableAndSummarizesTranscriptOnDevice() {
        OnDeviceLocalLanguageModel model = new OnDeviceLocalLanguageModel();
        assertTrue(model.isAvailable());

        SummarizationResult result = model.summarize(new SummarizationRequest(
                "c1",
                "The client wants the API ready by Friday. Raj will email QA tomorrow.",
                0,
                1));

        assertEquals(SummarizationResult.Kind.SUCCESS, result.kind);
        assertFalse(result.insights.summary.isEmpty());
        assertFalse(result.insights.keyPoints.isEmpty());
        assertFalse(result.insights.actionItems.isEmpty());
    }

    @Test
    public void answersOnlyFromRetrievedLocalContext() {
        OnDeviceLocalLanguageModel model = new OnDeviceLocalLanguageModel();
        RetrievedContext context = new RetrievedContext(Collections.singletonList(
                new ContextPassage(
                        ContextPassage.Kind.CONVERSATION,
                        "c-monday",
                        "Monday client call",
                        "The client wants the API ready by Friday.",
                        null,
                        20
                )));

        AnswerResult result = model.answer(new AnswerRequest(
                "What did the client say about the API deadline?", context));

        assertEquals(AnswerResult.Kind.SUCCESS, result.kind);
        assertEquals("The client requested that the API be ready by Friday.", result.text);
    }

    @Test
    public void notFoundWhenContextEmpty() {
        OnDeviceLocalLanguageModel model = new OnDeviceLocalLanguageModel();
        AnswerResult result = model.answer(new AnswerRequest("anything", RetrievedContext.empty()));
        assertEquals(AnswerResult.Kind.NOT_FOUND, result.kind);
        assertEquals(GroundedContextAnswerer.NOT_FOUND, result.text);
    }
}
