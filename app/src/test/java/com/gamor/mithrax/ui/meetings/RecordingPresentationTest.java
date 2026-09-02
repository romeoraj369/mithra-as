package com.gamor.mithrax.ui.meetings;

import com.gamor.mithrax.R;
import com.gamor.mithrax.data.local.conversation.ConversationEntity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RecordingPresentationTest {

    @Test
    public void formatsMinutesAndSeconds() {
        assertEquals("1:05", RecordingPresentation.formatDuration(65_000L));
    }

    @Test
    public void formatsHours() {
        assertEquals("1:02:03", RecordingPresentation.formatDuration(3_723_000L));
    }

    @Test
    public void clampsNegativeToZero() {
        assertEquals("0:00", RecordingPresentation.formatDuration(-10L));
    }

    @Test
    public void summarizingUsesDedicatedLabel() {
        assertEquals(
                R.string.recording_status_summarizing,
                RecordingPresentation.statusLabelRes("SUMMARIZING")
        );
    }

    @Test
    public void conversationBodyIncludesSummaryFactsAndTranscript() {
        ConversationEntity conversation = ConversationEntity.createCapture(
                "c1", "Title", 1L, 0L, "", "COMPLETED");
        conversation.summary = "Client deadline Friday.";
        conversation.importantFacts = java.util.Collections.singletonList("API must ship.");
        conversation.transcript = "The client wants the API ready by Friday.";
        String body = RecordingPresentation.conversationBody(conversation);
        assertTrue(body.contains("Client deadline Friday."));
        assertTrue(body.contains("API must ship."));
        assertTrue(body.contains("The client wants the API ready by Friday."));
    }
}
