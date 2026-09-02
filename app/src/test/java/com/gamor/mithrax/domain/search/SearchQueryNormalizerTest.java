package com.gamor.mithrax.domain.search;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SearchQueryNormalizerTest {

    @Test
    public void stripsQuestionWordsFromNaturalQuery() {
        SearchQueryNormalizer.ParsedQuery parsed = SearchQueryNormalizer.parse(
                "What was discussed about the database migration?");
        assertEquals("database", parsed.tokens.get(0));
        assertEquals("migration", parsed.tokens.get(1));
        assertEquals("database* migration*", parsed.ftsMatch);
    }

    @Test
    public void keepsPaymentApiTokens() {
        SearchQueryNormalizer.ParsedQuery parsed = SearchQueryNormalizer.parse("payment API");
        assertEquals(2, parsed.tokens.size());
        assertTrue(parsed.tokens.contains("payment"));
        assertTrue(parsed.tokens.contains("api"));
    }

    @Test
    public void keepsFridayDeadlineAndRaj() {
        assertEquals("friday* (deadline* OR due* OR friday* OR ready* OR completion*)",
                SearchQueryNormalizer.parse("Friday deadline").ftsMatch);
        assertEquals("raj*", SearchQueryNormalizer.parse("Raj").ftsMatch);
    }

    @Test
    public void stripsSayFromClientApiDeadlineQuestion() {
        SearchQueryNormalizer.ParsedQuery parsed = SearchQueryNormalizer.parse(
                "What did the client say about the API deadline?");
        assertTrue(parsed.tokens.contains("client"));
        assertTrue(parsed.tokens.contains("api"));
        assertTrue(parsed.tokens.contains("deadline"));
        assertEquals("(client* OR customer*) api* (deadline* OR due* OR friday* OR ready* OR completion*)",
                parsed.ftsMatch);
    }

    @Test
    public void emptyAndStopwordsOnlyAreEmpty() {
        assertTrue(SearchQueryNormalizer.parse("   ").isEmpty());
        assertTrue(SearchQueryNormalizer.parse("what about the").isEmpty());
    }
}
