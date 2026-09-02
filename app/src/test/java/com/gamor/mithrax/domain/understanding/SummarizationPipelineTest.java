package com.gamor.mithrax.domain.understanding;

import com.gamor.mithrax.domain.ai.FakeLocalLanguageModel;
import com.gamor.mithrax.domain.ai.SummarizationResult;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SummarizationPipelineTest {

    @Test
    public void emptyTranscriptDoesNotCallModel() {
        FakeLocalLanguageModel model = new FakeLocalLanguageModel();
        SummarizationPipeline pipeline = new SummarizationPipeline(model, new TranscriptChunker());
        SummarizationResult result = pipeline.run("c1", "  ");
        assertEquals(SummarizationResult.Kind.SKIPPED_EMPTY, result.kind);
        assertTrue(model.requests.isEmpty());
        assertTrue(result.insights.summary.isEmpty());
    }

    @Test
    public void unavailableModelDoesNotInfer() {
        FakeLocalLanguageModel model = new FakeLocalLanguageModel();
        model.available = false;
        SummarizationPipeline pipeline = new SummarizationPipeline(model, new TranscriptChunker());
        SummarizationResult result = pipeline.run("c1", "We should ship Friday.");
        assertEquals(SummarizationResult.Kind.UNAVAILABLE, result.kind);
        assertTrue(model.requests.isEmpty());
    }

    @Test
    public void inferenceFailureIsReported() {
        FakeLocalLanguageModel model = new FakeLocalLanguageModel();
        model.kind = SummarizationResult.Kind.FAILED;
        model.message = "oom";
        SummarizationPipeline pipeline = new SummarizationPipeline(model, new TranscriptChunker());
        SummarizationResult result = pipeline.run("c1", "A complete transcript.");
        assertEquals(SummarizationResult.Kind.FAILED, result.kind);
        assertEquals("oom", result.message);
        assertEquals(1, model.requests.size());
    }

    @Test
    public void longTranscriptIsChunkedThenMerged() {
        FakeLocalLanguageModel model = new FakeLocalLanguageModel();
        SummarizationPipeline pipeline =
                new SummarizationPipeline(model, new TranscriptChunker(24, 4));
        String transcript = "Alpha meeting notes. Bravo follow up. Charlie deadline Friday.";
        SummarizationResult result = pipeline.run("c1", transcript);
        assertEquals(SummarizationResult.Kind.SUCCESS, result.kind);
        assertTrue(model.requests.size() >= 2);
        assertEquals(model.requests.size(), result.chunkCount);
        assertTrue(result.insights.keyPoints.size() >= 2);
        assertTrue(result.insights.actionItems.size() >= 2);
    }

    @Test
    public void cancelStopsInference() throws InterruptedException {
        FakeLocalLanguageModel model = new FakeLocalLanguageModel();
        model.waitForCancel = true;
        SummarizationPipeline pipeline = new SummarizationPipeline(model, new TranscriptChunker());
        SummarizationResult[] holder = new SummarizationResult[1];
        CountDownLatch started = new CountDownLatch(1);
        Thread worker = new Thread(() -> {
            started.countDown();
            holder[0] = pipeline.run("c1", "Please summarize this transcript.");
        });
        worker.start();
        assertTrue(started.await(1, TimeUnit.SECONDS));
        Thread.sleep(40L);
        pipeline.cancel();
        worker.join(2_000L);
        assertEquals(SummarizationResult.Kind.CANCELLED, holder[0].kind);
        assertTrue(model.cancelRequested.get());
    }

    @Test
    public void successReturnsStructuredInsights() {
        FakeLocalLanguageModel model = new FakeLocalLanguageModel();
        SummarizationPipeline pipeline = new SummarizationPipeline(model, new TranscriptChunker());
        SummarizationResult result = pipeline.run("c1", "Ship the fix today.");
        assertEquals(SummarizationResult.Kind.SUCCESS, result.kind);
        assertEquals("Summary: Ship the fix today.", result.insights.summary);
        assertEquals(1, result.insights.keyPoints.size());
        assertEquals(1, result.insights.actionItems.size());
        assertEquals(1, result.insights.importantFacts.size());
    }
}
