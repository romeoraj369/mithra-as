package com.gamor.mithrax.device.tts;

import androidx.annotation.NonNull;

import com.gamor.mithrax.domain.tts.FakeTextToSpeechEngine;
import com.gamor.mithrax.domain.tts.SpeakResult;
import com.gamor.mithrax.domain.tts.TextToSpeechEngine;
import com.gamor.mithrax.domain.tts.TtsTextChunker;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpeechPlaybackControllerTest {

    private FakeTextToSpeechEngine engine;
    private AtomicBoolean enabled;
    private SpeechPlaybackController controller;

    @Before
    public void setUp() {
        engine = new FakeTextToSpeechEngine();
        enabled = new AtomicBoolean(false);
        controller = new SpeechPlaybackController(engine, enabled::get);
    }

    @Test
    public void explicitSpeakPlaysNormalAnswerWhenAutoIsOff() {
        SpeakResult result = controller.speakNow("The client requested that the API be ready by Friday.");
        assertEquals(SpeakResult.Kind.STARTED, result.kind);
        assertEquals(1, result.chunkCount);
        assertEquals(1, engine.spoken.size());
        assertTrue(engine.isSpeaking());
    }

    @Test
    public void emptyAnswerDoesNotStartSpeech() {
        SpeakResult result = controller.speakNow("   ");
        assertEquals(SpeakResult.Kind.SKIPPED_EMPTY, result.kind);
        assertTrue(engine.spoken.isEmpty());
        assertEquals(1, engine.stopCount);
        assertFalse(engine.isSpeaking());
    }

    @Test
    public void longAnswerIsQueuedInChunks() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 80; i++) {
            builder.append("This local answer sentence stays on the phone. ");
        }
        SpeakResult result = controller.speakNow(builder.toString());
        assertEquals(SpeakResult.Kind.STARTED, result.kind);
        assertTrue(result.chunkCount >= 2);
        assertEquals(result.chunkCount, engine.spoken.size());
        assertTrue(engine.spoken.get(0).length() <= TtsTextChunker.MAX_CHUNK_CHARS);
    }

    @Test
    public void stopEndsOngoingSpeech() {
        controller.speakNow("A normal local answer.");
        assertTrue(controller.isSpeaking());
        controller.stop();
        assertFalse(controller.isSpeaking());
        assertEquals(1, engine.stopCount);
    }

    @Test
    public void unavailableEngineIsReportedWithoutSpeaking() {
        engine.available = false;
        engine.unavailableMessage = "No offline TTS language";
        SpeakResult result = controller.speakNow("The client requested that the API be ready by Friday.");
        assertEquals(SpeakResult.Kind.UNAVAILABLE, result.kind);
        assertEquals("No offline TTS language", result.message);
        assertTrue(engine.spoken.isEmpty());
    }

    @Test
    public void autoSpeakDoesNothingUntilEnabled() {
        SpeakResult skipped = controller.speakAutomatically("The client requested that the API be ready by Friday.");
        assertEquals(SpeakResult.Kind.SKIPPED_DISABLED, skipped.kind);
        assertTrue(engine.spoken.isEmpty());

        enabled.set(true);
        SpeakResult spoken = controller.speakAutomatically("The client requested that the API be ready by Friday.");
        assertEquals(SpeakResult.Kind.STARTED, spoken.kind);
        assertEquals(1, engine.spoken.size());
    }

    @Test
    public void listenersSeeStop() {
        AtomicInteger speakingEvents = new AtomicInteger();
        controller.addListener(new TextToSpeechEngine.Listener() {
            @Override
            public void onSpeakingChanged(boolean speaking) {
                speakingEvents.incrementAndGet();
            }

            @Override
            public void onError(@NonNull String message) {
            }
        });
        controller.speakNow("Hello from local memory.");
        controller.stop();
        assertTrue(speakingEvents.get() >= 2);
    }
}
