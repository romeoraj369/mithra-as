package com.gamor.mithrax.domain.understanding;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TranscriptChunkerTest {

    @Test
    public void emptyTranscriptYieldsNoChunks() {
        assertTrue(new TranscriptChunker(20, 4).chunk("   ").isEmpty());
    }

    @Test
    public void shortTranscriptIsSingleChunk() {
        List<String> chunks = new TranscriptChunker(100, 10).chunk("hello there");
        assertEquals(1, chunks.size());
        assertEquals("hello there", chunks.get(0));
    }

    @Test
    public void longTranscriptIsSplit() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            text.append("Sentence number ").append(i).append(". ");
        }
        List<String> chunks = new TranscriptChunker(40, 8).chunk(text.toString());
        assertTrue(chunks.size() >= 2);
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 40);
        }
    }
}
