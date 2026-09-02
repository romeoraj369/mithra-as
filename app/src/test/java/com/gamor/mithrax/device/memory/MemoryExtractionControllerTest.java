package com.gamor.mithrax.device.memory;

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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MemoryExtractionControllerTest {

    private AppDatabase database;
    private ConversationRepository conversations;
    private MemoryRepository memories;
    private MemoryExtractionController controller;

    @Before
    public void setUp() {
        database = Room.inMemoryDatabaseBuilder(
                        ApplicationProvider.getApplicationContext(),
                        AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        conversations = new ConversationRepository(database.conversationDao());
        memories = new MemoryRepository(database.memoryDao());
        controller = new MemoryExtractionController(
                conversations, memories, new HeuristicMemoryExtractor());
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void storesLinkedMemoriesFromRealisticTranscript() {
        ConversationEntity conversation = ConversationEntity.createCapture(
                "meet-1",
                "Payment sync",
                1L,
                8_000L,
                "",
                ProcessingStatus.COMPLETED.name()
        );
        conversation.transcript = "Raj will implement the payment API by Friday. "
                + "The client wants the API ready by Friday.";
        conversations.insertSync(conversation);

        controller.extract("meet-1");

        List<MemoryEntity> stored = memories.getByConversationSync("meet-1");
        assertTrue(stored.size() >= 3);
        assertTrue(hasType(stored, MemoryType.ACTION));
        assertTrue(hasType(stored, MemoryType.PERSON));
        assertTrue(hasType(stored, MemoryType.FACT));
        assertTrue(hasType(stored, MemoryType.DEADLINE));
        for (MemoryEntity memory : stored) {
            assertEquals("meet-1", memory.sourceConversationId);
            assertTrue(memory.createdAtMillis > 0L);
            assertFalseDeleted(memory);
        }
        assertEquals("Raj will implement the payment API by Friday.",
                firstContent(stored, MemoryType.ACTION));
        assertEquals("Client requested API completion by Friday.",
                firstContent(stored, MemoryType.FACT));
    }

    @Test
    public void reExtractReplacesPreviousMemoriesForTheConversation() {
        ConversationEntity conversation = ConversationEntity.createCapture(
                "meet-2", "Notes", 1L, 1_000L, "", ProcessingStatus.COMPLETED.name());
        conversation.transcript = "Raj will implement the payment API by Friday.";
        conversations.insertSync(conversation);
        controller.extract("meet-2");
        int firstCount = memories.getByConversationSync("meet-2").size();

        conversation.transcript = "We decided to postpone the launch.";
        conversations.insertSync(conversation);
        controller.extract("meet-2");

        List<MemoryEntity> stored = memories.getByConversationSync("meet-2");
        assertTrue(stored.size() < firstCount || stored.size() == 1);
        assertEquals("We decided to postpone the launch.",
                firstContent(stored, MemoryType.DECISION));
        assertTrue(memories.getByTypeSync(MemoryType.ACTION).isEmpty());
    }

    private static boolean hasType(List<MemoryEntity> memories, MemoryType type) {
        for (MemoryEntity memory : memories) {
            if (type.name().equals(memory.type)) {
                return true;
            }
        }
        return false;
    }

    private static String firstContent(List<MemoryEntity> memories, MemoryType type) {
        for (MemoryEntity memory : memories) {
            if (type.name().equals(memory.type)) {
                return memory.content;
            }
        }
        throw new AssertionError("Missing " + type);
    }

    private static void assertFalseDeleted(MemoryEntity memory) {
        assertTrue(!memory.deleted);
    }
}
