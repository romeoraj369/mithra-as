package com.gamor.mithrax.device.understanding;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.gamor.mithrax.data.local.AppDatabase;
import com.gamor.mithrax.data.local.conversation.ConversationEntity;
import com.gamor.mithrax.data.local.conversation.ConversationRepository;
import com.gamor.mithrax.device.ai.OnDeviceLocalLanguageModel;
import com.gamor.mithrax.device.ai.UnprovisionedLocalLanguageModel;
import com.gamor.mithrax.domain.ai.FakeLocalLanguageModel;
import com.gamor.mithrax.domain.ai.SummarizationResult;
import com.gamor.mithrax.domain.conversation.ActionItem;
import com.gamor.mithrax.domain.recording.ProcessingStatus;
import com.gamor.mithrax.domain.understanding.ConversationInsights;
import com.gamor.mithrax.domain.understanding.SummarizationMetadata;
import com.gamor.mithrax.domain.understanding.SummarizationPipeline;
import com.gamor.mithrax.domain.understanding.TranscriptChunker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SummarizationControllerTest {

    private AppDatabase database;
    private ConversationRepository repository;

    @Before
    public void setUp() {
        database = Room.inMemoryDatabaseBuilder(
                        ApplicationProvider.getApplicationContext(),
                        AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        repository = new ConversationRepository(database.conversationDao());
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void storesInsightsFromLocalModel() throws InterruptedException {
        FakeLocalLanguageModel model = new FakeLocalLanguageModel();
        model.insights = new ConversationInsights(
                "Team agreed to ship.",
                Collections.singletonList("Ship Friday"),
                Collections.singletonList(new ActionItem("Email QA", false)),
                Collections.singletonList("Budget unchanged")
        );
        SummarizationController controller = new SummarizationController(
                repository, new SummarizationPipeline(model, new TranscriptChunker()));
        insertConversation("c1", "We will ship Friday.");

        controller.enqueue("c1");
        ConversationEntity stored = awaitCompleted("c1");
        assertEquals("Team agreed to ship.", stored.summary);
        assertEquals("Ship Friday", stored.keyPoints.get(0));
        assertEquals("Email QA", stored.actionItems.get(0).text);
        assertEquals("Budget unchanged", stored.importantFacts.get(0));
        assertEquals(SummarizationMetadata.COMPLETED, stored.metadata.get(SummarizationMetadata.KEY_STATUS));
        assertEquals("We will ship Friday.", stored.transcript);
    }

    @Test
    public void emptyTranscriptSkipsModelAndCompletes() throws InterruptedException {
        FakeLocalLanguageModel model = new FakeLocalLanguageModel();
        SummarizationController controller = new SummarizationController(
                repository, new SummarizationPipeline(model, new TranscriptChunker()));
        insertConversation("c2", "   ");

        controller.enqueue("c2");
        ConversationEntity stored = awaitCompleted("c2");
        assertEquals("", stored.summary);
        assertEquals(SummarizationMetadata.SKIPPED_EMPTY,
                stored.metadata.get(SummarizationMetadata.KEY_STATUS));
        assertTrue(model.requests.isEmpty());
    }

    @Test
    public void onDeviceModelStoresExtractiveInsights() throws InterruptedException {
        SummarizationController controller = new SummarizationController(
                repository, new OnDeviceLocalLanguageModel());
        insertConversation("c-on-device",
                "The client wants the API ready by Friday. Raj will email QA tomorrow.");

        controller.enqueue("c-on-device");
        ConversationEntity stored = awaitCompleted("c-on-device");
        assertTrue(stored.summary.length() > 0);
        assertFalse(stored.keyPoints.isEmpty());
        assertEquals(SummarizationMetadata.COMPLETED, stored.metadata.get(SummarizationMetadata.KEY_STATUS));
        assertEquals(ProcessingStatus.COMPLETED.name(), stored.processingStatus);
    }

    @Test
    public void unprovisionedModelLeavesTranscriptAndRecordsUnavailable() throws InterruptedException {
        SummarizationController controller = new SummarizationController(
                repository, new UnprovisionedLocalLanguageModel());
        insertConversation("c3", "Keep this transcript.");

        controller.enqueue("c3");
        ConversationEntity stored = awaitCompleted("c3");
        assertEquals("Keep this transcript.", stored.transcript);
        assertEquals("", stored.summary);
        assertEquals(SummarizationMetadata.UNAVAILABLE,
                stored.metadata.get(SummarizationMetadata.KEY_STATUS));
        assertEquals(ProcessingStatus.COMPLETED.name(), stored.processingStatus);
    }

    @Test
    public void inferenceFailureDoesNotOverwriteTranscript() throws InterruptedException {
        FakeLocalLanguageModel model = new FakeLocalLanguageModel();
        model.kind = SummarizationResult.Kind.FAILED;
        model.message = "native crash";
        SummarizationController controller = new SummarizationController(
                repository, new SummarizationPipeline(model, new TranscriptChunker()));
        insertConversation("c4", "Original words.");

        controller.enqueue("c4");
        ConversationEntity stored = awaitCompleted("c4");
        assertEquals("Original words.", stored.transcript);
        assertEquals("", stored.summary);
        assertEquals(SummarizationMetadata.FAILED, stored.metadata.get(SummarizationMetadata.KEY_STATUS));
        assertEquals("native crash", stored.metadata.get(SummarizationMetadata.KEY_ERROR));
        assertEquals(ProcessingStatus.FAILED.name(), stored.processingStatus);
    }

    private void insertConversation(String id, String transcript) {
        ConversationEntity entity = ConversationEntity.createCapture(
                id, "Title", 1L, 1_000L, "", ProcessingStatus.SUMMARIZING.name());
        entity.transcript = transcript;
        repository.insertSync(entity);
    }

    private ConversationEntity awaitCompleted(String id) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        ConversationEntity entity = null;
        while (System.nanoTime() < deadline) {
            entity = repository.getByIdSync(id);
            if (entity != null && isTerminal(entity.processingStatus)) {
                return entity;
            }
            Thread.sleep(20L);
        }
        throw new AssertionError("Conversation " + id + " did not complete, last=" +
                (entity == null ? "null" : entity.processingStatus));
    }

    private static boolean isTerminal(@NonNull String status) {
        return ProcessingStatus.COMPLETED.name().equals(status)
                || ProcessingStatus.FAILED.name().equals(status);
    }
}
