package com.gamor.mithrax.data.local.ask;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.gamor.mithrax.data.local.AppDatabase;
import com.gamor.mithrax.data.local.conversation.ConversationEntity;
import com.gamor.mithrax.data.local.conversation.ConversationRepository;
import com.gamor.mithrax.data.local.memory.MemoryEntity;
import com.gamor.mithrax.data.local.memory.MemoryRepository;
import com.gamor.mithrax.data.local.search.FtsSearchService;
import com.gamor.mithrax.data.local.search.LocalSearchIndex;
import com.gamor.mithrax.device.ai.OnDeviceLocalLanguageModel;
import com.gamor.mithrax.device.ai.UnprovisionedLocalLanguageModel;
import com.gamor.mithrax.domain.ai.FakeLocalLanguageModel;
import com.gamor.mithrax.domain.ask.AskResult;
import com.gamor.mithrax.domain.ask.GroundedContextAnswerer;
import com.gamor.mithrax.domain.memory.ExtractedMemory;
import com.gamor.mithrax.domain.memory.HeuristicMemoryExtractor;
import com.gamor.mithrax.domain.memory.MemoryExtractionRequest;
import com.gamor.mithrax.domain.recording.ProcessingStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LocalAskServiceTest {

    private static final String QUESTION =
            "What did the client say about the API deadline?";
    private static final String EXPECTED =
            "The client requested that the API be ready by Friday.";

    private AppDatabase database;
    private ConversationRepository conversations;
    private MemoryRepository memories;
    private FtsSearchService search;
    private RoomPassageLoader passages;

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
        passages = new RoomPassageLoader(conversations, memories);
        seedStoredConversation();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void answersClientApiDeadlineFromStoredConversationWithoutCloudModel() {
        LocalAskService ask = new LocalAskService(
                search, passages, new UnprovisionedLocalLanguageModel());

        AskResult result = ask.ask(QUESTION);

        assertEquals(AskResult.Kind.ANSWERED, result.kind);
        assertEquals(EXPECTED, result.answer);
        assertFalse(result.usedOnDeviceModel);
        assertFalse(result.sources.isEmpty());
        assertEquals("c-monday", result.sources.get(0).conversationId);
        assertEquals("Monday client call", result.sources.get(0).title);
    }

    @Test
    public void saysSoWhenNothingRelevantIsStored() {
        LocalAskService ask = new LocalAskService(
                search, passages, new UnprovisionedLocalLanguageModel());

        AskResult result = ask.ask("What is the quantum banana recipe?");

        assertEquals(AskResult.Kind.NOT_FOUND, result.kind);
        assertEquals(GroundedContextAnswerer.NOT_FOUND, result.answer);
        assertTrue(result.sources.isEmpty());
    }

    @Test
    public void onDeviceModelMarksAnswerAsUsingLocalModel() {
        LocalAskService ask = new LocalAskService(
                search, passages, new OnDeviceLocalLanguageModel());

        AskResult result = ask.ask(QUESTION);

        assertEquals(AskResult.Kind.ANSWERED, result.kind);
        assertEquals(EXPECTED, result.answer);
        assertTrue(result.usedOnDeviceModel);
        assertEquals("c-monday", result.sources.get(0).conversationId);
    }

    @Test
    public void usesLocalModelTextButKeepsRetrievedSources() {
        FakeLocalLanguageModel model = new FakeLocalLanguageModel();
        model.answerText = "The client requested that the API be ready by Friday.";
        LocalAskService ask = new LocalAskService(search, passages, model);

        AskResult result = ask.ask(QUESTION);

        assertEquals(AskResult.Kind.ANSWERED, result.kind);
        assertTrue(result.usedOnDeviceModel);
        assertEquals(model.answerText, result.answer);
        assertEquals("c-monday", result.sources.get(0).conversationId);
        assertEquals(1, model.answerRequests.size());
        assertEquals(QUESTION, model.answerRequests.get(0).question);
        assertFalse(model.answerRequests.get(0).context.isEmpty());
    }

    private void seedStoredConversation() {
        ConversationEntity monday = ConversationEntity.createCapture(
                "c-monday",
                "Monday client call",
                1_000L,
                90_000L,
                "",
                ProcessingStatus.COMPLETED.name()
        );
        monday.transcript = "The client wants the API ready by Friday.";
        conversations.insertSync(monday);

        List<ExtractedMemory> extracted = new HeuristicMemoryExtractor().extract(
                new MemoryExtractionRequest("c-monday", monday.transcript, null));
        List<MemoryEntity> stored = new ArrayList<>();
        int i = 0;
        for (ExtractedMemory memory : extracted) {
            stored.add(MemoryEntity.create(
                    "m-monday-" + i,
                    memory.type.name(),
                    memory.content,
                    "c-monday",
                    1_100L + i,
                    memory.confidence,
                    memory.metadata
            ));
            i++;
        }
        memories.replaceForConversationSync("c-monday", stored);
    }
}
