package com.gamor.mithrax.domain.tts;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TtsTextChunkerTest {

    @Test
    public void emptyTextYieldsNoChunks() {
        assertTrue(TtsTextChunker.chunk("   ").isEmpty());
    }

    @Test
    public void shortAnswerStaysOneChunk() {
        List<String> chunks = TtsTextChunker.chunk("The client requested that the API be ready by Friday.");
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).contains("Friday"));
    }

    @Test
    public void longAnswerIsSplitBelowLimit() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 80; i++) {
            builder.append("Sentence number ").append(i).append(" explains a local memory fact. ");
        }
        String text = builder.toString();
        assertTrue(text.length() > TtsTextChunker.MAX_CHUNK_CHARS);
        List<String> chunks = TtsTextChunker.chunk(text);
        assertTrue(chunks.size() >= 2);
        int total = 0;
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= TtsTextChunker.MAX_CHUNK_CHARS);
            total += chunk.length();
        }
        assertTrue(total >= TtsTextChunker.MAX_CHUNK_CHARS);
    }
}
