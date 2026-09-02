package com.gamor.mithrax.data.local.conversation;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.gamor.mithrax.data.local.AppDatabase;
import com.gamor.mithrax.domain.conversation.ActionItem;
import com.gamor.mithrax.domain.recording.ProcessingStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ConversationDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase database;
    private ConversationDao dao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = database.conversationDao();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void insertAndReadIndependentOfAudio() throws InterruptedException {
        ConversationEntity conversation = ConversationEntity.createCapture(
                "c1",
                "Standup",
                1_000L,
                12_000L,
                "",
                ProcessingStatus.COMPLETED.name()
        );
        conversation.transcript = "We shipped the fix.";
        dao.insert(conversation);

        ConversationEntity loaded = dao.getById("c1");
        assertNotNull(loaded);
        assertEquals("Standup", loaded.title);
        assertEquals("", loaded.audioPath);
        assertFalse(loaded.hasAudioFile());
        assertEquals("We shipped the fix.", loaded.transcript);

        List<ConversationEntity> observed = awaitValue(dao.observeAll());
        assertEquals(1, observed.size());
        assertEquals("c1", observed.get(0).id);
    }

    @Test
    public void listsNewestFirst() {
        dao.insert(ConversationEntity.createCapture(
                "old", "Old", 1L, 0L, "", ProcessingStatus.COMPLETED.name()));
        dao.insert(ConversationEntity.createCapture(
                "new", "New", 2L, 0L, "", ProcessingStatus.COMPLETED.name()));

        List<ConversationEntity> all = dao.getAllSync();
        assertEquals("new", all.get(0).id);
        assertEquals("old", all.get(1).id);
    }

    @Test
    public void updatesTranscriptStatusAndInsights() {
        dao.insert(ConversationEntity.createCapture(
                "c1", "Call", 10L, 0L, "/tmp/a.m4a", ProcessingStatus.TRANSCRIBING.name()));

        dao.updateTranscript("c1", "hello world", ProcessingStatus.COMPLETED.name(), 50L);
        dao.updateInsights(
                "c1",
                "A short summary",
                Collections.singletonList("Ship today"),
                Collections.singletonList(new ActionItem("Email Alex", false)),
                Arrays.asList("Budget is 5k"),
                60L
        );
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", "local");
        dao.updateMetadata("c1", metadata, 70L);

        ConversationEntity loaded = dao.getById("c1");
        assertEquals("hello world", loaded.transcript);
        assertEquals(ProcessingStatus.COMPLETED.name(), loaded.processingStatus);
        assertEquals("A short summary", loaded.summary);
        assertEquals(Collections.singletonList("Ship today"), loaded.keyPoints);
        assertEquals(Collections.singletonList(new ActionItem("Email Alex", false)), loaded.actionItems);
        assertEquals(Collections.singletonList("Budget is 5k"), loaded.importantFacts);
        assertEquals("local", loaded.metadata.get("source"));
        assertEquals(70L, loaded.updatedAtMillis);
        assertEquals("/tmp/a.m4a", loaded.audioPath);
    }

    @Test
    public void getByStatusAndDelete() {
        dao.insert(ConversationEntity.createCapture(
                "a", "A", 1L, 0L, "", ProcessingStatus.TRANSCRIBING.name()));
        dao.insert(ConversationEntity.createCapture(
                "b", "B", 2L, 0L, "", ProcessingStatus.COMPLETED.name()));

        List<ConversationEntity> transcribing = dao.getByStatus(ProcessingStatus.TRANSCRIBING.name());
        assertEquals(1, transcribing.size());
        assertEquals("a", transcribing.get(0).id);

        dao.delete(transcribing.get(0));
        assertTrue(dao.getByStatus(ProcessingStatus.TRANSCRIBING.name()).isEmpty());
        assertEquals(1, dao.getAllSync().size());
    }

    private static <T> T awaitValue(LiveData<T> liveData) throws InterruptedException {
        AtomicReference<T> value = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Observer<T> observer = data -> {
            value.set(data);
            latch.countDown();
        };
        liveData.observeForever(observer);
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        liveData.removeObserver(observer);
        return value.get();
    }
}
