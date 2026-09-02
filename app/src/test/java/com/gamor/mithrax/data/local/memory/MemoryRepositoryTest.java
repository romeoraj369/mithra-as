package com.gamor.mithrax.data.local.memory;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.gamor.mithrax.data.local.AppDatabase;
import com.gamor.mithrax.data.local.conversation.ConversationEntity;
import com.gamor.mithrax.data.local.conversation.ConversationRepository;
import com.gamor.mithrax.domain.memory.MemoryType;
import com.gamor.mithrax.domain.recording.ProcessingStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MemoryRepositoryTest {

    private AppDatabase database;
    private ConversationRepository conversations;
    private MemoryRepository memories;

    @Before
    public void setUp() {
        database = Room.inMemoryDatabaseBuilder(
                        ApplicationProvider.getApplicationContext(),
                        AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        conversations = new ConversationRepository(database.conversationDao());
        memories = new MemoryRepository(database.memoryDao());
        conversations.insertSync(ConversationEntity.createCapture(
                "c1", "Standup", 1L, 1_000L, "", ProcessingStatus.COMPLETED.name()));
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void persistsAndRetrievesByConversationAndType() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("owner", "Raj");
        MemoryEntity stored = MemoryEntity.create(
                "m1",
                MemoryType.ACTION.name(),
                "Raj will implement the payment API by Friday.",
                "c1",
                10L,
                0.88f,
                metadata
        );
        memories.replaceForConversationSync("c1", Collections.singletonList(stored));

        List<MemoryEntity> byConversation = memories.getByConversationSync("c1");
        assertEquals(1, byConversation.size());
        assertEquals("c1", byConversation.get(0).sourceConversationId);
        assertEquals("Raj", byConversation.get(0).metadata.get("owner"));

        List<MemoryEntity> actions = memories.getByTypeSync(MemoryType.ACTION);
        assertEquals(1, actions.size());
        assertEquals("m1", actions.get(0).id);

        List<MemoryEntity> search = memories.searchSync("payment API");
        assertEquals(1, search.size());
    }

    @Test
    public void softDeleteHidesFromActiveQueriesAndRestoreBringsItBack() {
        memories.replaceForConversationSync("c1", Collections.singletonList(
                MemoryEntity.create("m2", MemoryType.FACT.name(), "Budget is 5k.", "c1",
                        1L, 0.7f, new LinkedHashMap<>())));
        memories.softDeleteSync("m2");
        assertTrue(memories.getActiveSync().isEmpty());
        assertTrue(memories.getByConversationSync("c1").isEmpty());
        assertTrue(memories.getByIdSync("m2").deleted);

        memories.restoreSync("m2");
        assertEquals(1, memories.getActiveSync().size());
    }

    @Test
    public void hardDeleteRemovesMemoryRowsNotJustHidesThem() {
        memories.replaceForConversationSync("c1", Collections.singletonList(
                MemoryEntity.create("m4", MemoryType.FACT.name(), "Secret fact.", "c1",
                        1L, 0.7f, new LinkedHashMap<>())));
        memories.deleteByConversationSync("c1");
        assertTrue(memories.getActiveSync().isEmpty());
        assertTrue(database.memoryDao().getById("m4") == null);
    }

    @Test
    public void deletingConversationRemovesLinkedMemories() {
        memories.replaceForConversationSync("c1", Collections.singletonList(
                MemoryEntity.create("m3", MemoryType.PERSON.name(), "Raj", "c1",
                        1L, 0.8f, new LinkedHashMap<>())));
        database.conversationDao().delete(conversations.getByIdSync("c1"));
        assertTrue(memories.getActiveSync().isEmpty());
        assertTrue(database.memoryDao().getById("m3") == null);
    }
}
