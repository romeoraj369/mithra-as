package com.gamor.mithrax.ui.search;

import com.gamor.mithrax.domain.search.SearchHit;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class SearchResultsAdapterTest {

    @Test
    public void conversationMetaIncludesStatusAndDate() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("status", "COMPLETED");
        metadata.put("createdAt", "Aug 24, 2026");
        SearchHit hit = new SearchHit(
                SearchHit.Kind.CONVERSATION, "c1", "Title", "snippet", metadata, "c1");
        String line = SearchResultsAdapter.metaLine(hit);
        assertTrue(line.contains("COMPLETED"));
        assertTrue(line.contains("Aug 24, 2026"));
    }

    @Test
    public void memoryMetaIncludesTypeOwnerDeadline() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("type", "ACTION");
        metadata.put("owner", "Raj");
        metadata.put("deadline", "Friday");
        SearchHit hit = new SearchHit(
                SearchHit.Kind.MEMORY, "m1", "ACTION memory", "Raj will implement", metadata, "c1");
        String line = SearchResultsAdapter.metaLine(hit);
        assertTrue(line.contains("ACTION"));
        assertTrue(line.contains("Raj"));
        assertTrue(line.contains("Friday"));
    }
}
