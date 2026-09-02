package com.gamor.mithrax.data.local.conversation;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.gamor.mithrax.data.local.AppDatabase;
import com.gamor.mithrax.domain.conversation.ActionItem;
import com.gamor.mithrax.domain.recording.ProcessingStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ConversationRepositoryTest {

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
    public void completeCaptureUpdatesExistingRow() throws InterruptedException {
        ConversationEntity inProgress = ConversationEntity.createCapture(
                "id-1",
                "Title",
                10L,
                0L,
                "/audio/id-1.m4a",
                ProcessingStatus.RECORDING.name()
        );
        repository.insertSync(inProgress);

        ConversationEntity fallback = ConversationEntity.createCapture(
                "id-1",
                "Title",
                10L,
                4_000L,
                "/audio/id-1.m4a",
                ProcessingStatus.TRANSCRIBING.name()
        );
        CountDownLatch latch = new CountDownLatch(1);
        repository.completeCapture("id-1", 4_000L, fallback, latch::countDown);
        assertTrue(latch.await(2, TimeUnit.SECONDS));

        ConversationEntity stored = repository.getByIdSync("id-1");
        assertEquals(4_000L, stored.durationMillis);
        assertEquals(ProcessingStatus.TRANSCRIBING.name(), stored.processingStatus);
        assertTrue(stored.updatedAtMillis >= 10L);
    }

    @Test
    public void insightUpdateIsReadableThroughRepository() {
        repository.insertSync(ConversationEntity.createCapture(
                "id-2", "Notes", 1L, 0L, "", ProcessingStatus.COMPLETED.name()));
        repository.updateInsightsSync(
                "id-2",
                "Summary text",
                Collections.singletonList("Point"),
                Collections.singletonList(new ActionItem("Do thing", false)),
                Collections.singletonList("Fact")
        );

        ConversationEntity stored = repository.getByIdSync("id-2");
        assertEquals("Summary text", stored.summary);
        assertEquals("Point", stored.keyPoints.get(0));
        assertEquals("Do thing", stored.actionItems.get(0).text);
        assertEquals("Fact", stored.importantFacts.get(0));

        List<ConversationEntity> byStatus =
                repository.getByStatusSync(ProcessingStatus.COMPLETED.name());
        assertEquals(1, byStatus.size());
    }

    @Test
    public void deleteByIdRemovesRow() {
        repository.insertSync(ConversationEntity.createCapture(
                "gone", "Temp", 1L, 0L, "", ProcessingStatus.RECORDING.name()));
        repository.deleteSync("gone");
        assertEquals(null, repository.getByIdSync("gone"));
        assertEquals(0, repository.countSync());
    }
}
