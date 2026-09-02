package com.gamor.mithrax.device.ai;

import com.gamor.mithrax.domain.ai.AnswerRequest;
import com.gamor.mithrax.domain.ai.AnswerResult;
import com.gamor.mithrax.domain.ai.SummarizationRequest;
import com.gamor.mithrax.domain.ai.SummarizationResult;
import com.gamor.mithrax.domain.ask.RetrievedContext;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class UnprovisionedLocalLanguageModelTest {

    @Test
    public void reportsUnavailableWithoutPretendingToInfer() {
        UnprovisionedLocalLanguageModel model = new UnprovisionedLocalLanguageModel();
        assertFalse(model.isAvailable());
        SummarizationResult result = model.summarize(
                new SummarizationRequest("c1", "Any transcript", 0, 1));
        assertEquals(SummarizationResult.Kind.UNAVAILABLE, result.kind);
        assertEquals(UnprovisionedLocalLanguageModel.MESSAGE, result.message);
        assertEquals("", result.insights.summary);
    }

    @Test
    public void answerReportsUnavailableWithoutPretendingToInfer() {
        UnprovisionedLocalLanguageModel model = new UnprovisionedLocalLanguageModel();
        AnswerResult result = model.answer(
                new AnswerRequest("What did the client say about the API deadline?",
                        RetrievedContext.empty()));
        assertEquals(AnswerResult.Kind.UNAVAILABLE, result.kind);
        assertEquals(UnprovisionedLocalLanguageModel.MESSAGE, result.message);
        assertEquals("", result.text);
    }
}
