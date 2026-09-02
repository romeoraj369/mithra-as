package com.gamor.mithrax.data.local.search;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.gamor.mithrax.data.local.AppDatabase;
import com.gamor.mithrax.data.local.conversation.ConversationEntity;
import com.gamor.mithrax.data.local.conversation.ConversationRepository;
import com.gamor.mithrax.data.local.memory.MemoryEntity;
import com.gamor.mithrax.data.local.memory.MemoryRepository;
import com.gamor.mithrax.domain.memory.HeuristicMemoryExtractor;
import com.gamor.mithrax.domain.memory.MemoryType;
import com.gamor.mithrax.domain.recording.ProcessingStatus;
import com.gamor.mithrax.domain.search.SearchHit;
import com.gamor.mithrax.domain.search.SearchResults;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class FtsSearchServiceTest {

    private AppDatabase database;
    private ConversationRepository conversations;
    private MemoryRepository memories;
    private FtsSearchService search;

    @Before
    public void setUp() {
        database = Room.inMemoryDatabaseBuilder(
                        ApplicationProvider.getApplicationContext(),
                        AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        LocalSearchIndex index = new LocalSearchIndex(database.searchDao());
        conversations = new ConversationRepository(database.conversationDao(), index);
        memories = new MemoryRepository(database.memoryDao(), index);
        search = new FtsSearchService(
                database.searchDao(),
                database.conversationDao(),
                database.memoryDao()
        );
        seed();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void findsConversationAboutDatabaseMigration() {
        SearchResults results = search.search("What was discussed about the database migration?");
        assertFalse(results.conversations.isEmpty());
        assertEquals("c-db", results.conversations.get(0).id);
        assertTrue(results.conversations.get(0).snippet.toLowerCase().contains("migration"));
        assertEquals("conversation", results.conversations.get(0).metadata.get("kind"));
    }

    @Test
    public void findsPaymentApiInConversationAndMemory() {
        SearchResults results = search.search("payment API");
        assertTrue(hasId(results, SearchHit.Kind.CONVERSATION, "c-pay"));
        assertTrue(hasSnippet(results.memories, "payment API"));
    }

    @Test
    public void findsFridayDeadlineMemory() {
        SearchResults results = search.search("Friday deadline");
        assertFalse(results.memories.isEmpty());
        assertTrue(hasType(results, "DEADLINE") || hasSnippet(results.memories, "Friday"));
    }

    @Test
    public void findsRajPersonMemory() {
        SearchResults results = search.search("Raj");
        assertTrue(hasSnippet(results.memories, "Raj") || hasId(results, SearchHit.Kind.MEMORY, "m-raj"));
        SearchHit person = firstMemory(results);
        assertEquals("c-pay", person.sourceConversationId);
        assertEquals("PERSON", person.metadata.get("type"));
    }

    @Test
    public void emptyQueryReturnsNoHits() {
        assertTrue(search.search("  ").isEmpty());
    }

    private void seed() {
        ConversationEntity migration = ConversationEntity.createCapture(
                "c-db",
                "Architecture review",
                1_000L,
                60_000L,
                "",
                ProcessingStatus.COMPLETED.name()
        );
        migration.transcript = "We talked about the database migration and agreed to keep Room local.";
        migration.summary = "Discussion of the database migration plan.";
        conversations.insertSync(migration);

        ConversationEntity payment = ConversationEntity.createCapture(
                "c-pay",
                "Payments standup",
                2_000L,
                45_000L,
                "",
                ProcessingStatus.COMPLETED.name()
        );
        payment.transcript = "Raj will implement the payment API by Friday.";
        conversations.insertSync(payment);

        Map<String, String> actionMeta = new LinkedHashMap<>();
        actionMeta.put(HeuristicMemoryExtractor.META_OWNER, "Raj");
        actionMeta.put(HeuristicMemoryExtractor.META_TASK, "implement the payment API");
        actionMeta.put(HeuristicMemoryExtractor.META_DEADLINE, "Friday");
        memories.replaceForConversationSync("c-pay", java.util.Arrays.asList(
                MemoryEntity.create("m-action", MemoryType.ACTION.name(),
                        "Raj will implement the payment API by Friday.",
                        "c-pay", 2_100L, 0.88f, actionMeta),
                MemoryEntity.create("m-raj", MemoryType.PERSON.name(),
                        "Raj", "c-pay", 2_101L, 0.8f, personMeta()),
                MemoryEntity.create("m-deadline", MemoryType.DEADLINE.name(),
                        "Implement the payment API by Friday.",
                        "c-pay", 2_102L, 0.82f, actionMeta)
        ));
    }

    private static Map<String, String> personMeta() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put(HeuristicMemoryExtractor.META_NAME, "Raj");
        return metadata;
    }

    private static boolean hasId(SearchResults results, SearchHit.Kind kind, String id) {
        for (SearchHit hit : kind == SearchHit.Kind.CONVERSATION ? results.conversations : results.memories) {
            if (id.equals(hit.id)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSnippet(java.util.List<SearchHit> hits, String needle) {
        String lower = needle.toLowerCase();
        for (SearchHit hit : hits) {
            if (hit.snippet.toLowerCase().contains(lower) || hit.title.toLowerCase().contains(lower)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasType(SearchResults results, String type) {
        for (SearchHit hit : results.memories) {
            if (type.equals(hit.metadata.get("type"))) {
                return true;
            }
        }
        return false;
    }

    private static SearchHit firstMemory(SearchResults results) {
        assertFalse(results.memories.isEmpty());
        for (SearchHit hit : results.memories) {
            if ("PERSON".equals(hit.metadata.get("type"))) {
                return hit;
            }
        }
        return results.memories.get(0);
    }
}
