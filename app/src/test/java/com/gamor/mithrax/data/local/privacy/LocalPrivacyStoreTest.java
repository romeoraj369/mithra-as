package com.gamor.mithrax.data.local.privacy;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.gamor.mithrax.data.local.AppDatabase;
import com.gamor.mithrax.data.local.conversation.ConversationEntity;
import com.gamor.mithrax.data.local.conversation.ConversationRepository;
import com.gamor.mithrax.data.local.memory.MemoryEntity;
import com.gamor.mithrax.data.local.memory.MemoryRepository;
import com.gamor.mithrax.data.local.recording.RecordingFileStore;
import com.gamor.mithrax.data.local.search.FtsSearchService;
import com.gamor.mithrax.data.local.search.LocalSearchIndex;
import com.gamor.mithrax.device.ai.OnDeviceLocalLanguageModel;
import com.gamor.mithrax.device.ai.UnprovisionedLocalLanguageModel;
import com.gamor.mithrax.device.stt.VoskModelManager;
import com.gamor.mithrax.domain.memory.MemoryType;
import com.gamor.mithrax.domain.privacy.StorageSnapshot;
import com.gamor.mithrax.domain.recording.ProcessingStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LocalPrivacyStoreTest {

    private AppDatabase database;
    private ConversationRepository conversations;
    private MemoryRepository memories;
    private FtsSearchService search;
    private LocalPrivacyStore privacy;
    private File audioFile;

    @Before
    public void setUp() throws Exception {
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
        privacy = new LocalPrivacyStore(
                ApplicationProvider.getApplicationContext(),
                conversations,
                memories,
                new VoskModelManager(),
                new UnprovisionedLocalLanguageModel()
        );
        audioFile = RecordingFileStore.fileForId(ApplicationProvider.getApplicationContext(), "c-keep");
        writeBytes(audioFile, "fake-audio");
        seed();
    }

    @After
    public void tearDown() {
        database.close();
        RecordingFileStore.deleteAllInDirectory(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void deleteAudioRemovesFileAndClearsPathButKeepsText() {
        privacy.deleteAudioSync("c-keep");

        assertFalse(audioFile.exists());
        ConversationEntity stored = conversations.getByIdSync("c-keep");
        assertEquals("", stored.audioPath);
        assertEquals("Unique xylophone transcript about the client.", stored.transcript);
        assertEquals(1, memories.getByConversationSync("c-keep").size());
        assertFalse(search.search("xylophone").conversations.isEmpty());
    }

    @Test
    public void deleteTranscriptRemovesTextAndSearchHit() {
        privacy.deleteTranscriptSync("c-keep");

        ConversationEntity stored = conversations.getByIdSync("c-keep");
        assertEquals("", stored.transcript);
        assertTrue(search.search("xylophone").conversations.isEmpty());
        assertFalse(search.search("budget").memories.isEmpty());
        assertTrue(audioFile.exists());
    }

    @Test
    public void deleteMemoriesRemovesRowsAndSearchHits() {
        privacy.deleteMemoriesForConversationSync("c-keep");

        assertTrue(memories.getByConversationSync("c-keep").isEmpty());
        assertNull(database.memoryDao().getById("m-keep"));
        assertTrue(search.search("budget").memories.isEmpty());
        assertNotNull(conversations.getByIdSync("c-keep"));
        assertTrue(audioFile.exists());
    }

    @Test
    public void deleteConversationRemovesRowAudioMemoriesAndIndex() {
        File otherAudio = RecordingFileStore.fileForId(
                ApplicationProvider.getApplicationContext(), "c-other");
        assertTrue(otherAudio.exists());

        privacy.deleteConversationSync("c-keep");

        assertNull(conversations.getByIdSync("c-keep"));
        assertNull(database.memoryDao().getById("m-keep"));
        assertFalse(audioFile.exists());
        assertTrue(search.search("xylophone").isEmpty());
        assertTrue(search.search("budget").isEmpty());
        assertNotNull(conversations.getByIdSync("c-other"));
    }

    @Test
    public void clearAllMemoriesLeavesConversationsAndAudio() {
        privacy.clearAllMemoriesSync();

        assertTrue(memories.getActiveSync().isEmpty());
        assertTrue(database.memoryDao().getAllRows().isEmpty());
        assertTrue(search.search("budget").memories.isEmpty());
        assertNotNull(conversations.getByIdSync("c-keep"));
        assertTrue(audioFile.exists());
    }

    @Test
    public void deleteAllConversationsWipesLocalStore() {
        privacy.deleteAllConversationsSync();

        assertTrue(conversations.getAllSync().isEmpty());
        assertTrue(database.memoryDao().getAllRows().isEmpty());
        assertTrue(search.search("xylophone").isEmpty());
        assertTrue(search.search("othernote").isEmpty());
        assertFalse(audioFile.exists());
        assertEquals(0, RecordingFileStore.fileCount(
                RecordingFileStore.directory(ApplicationProvider.getApplicationContext())));
    }

    @Test
    public void snapshotCountsStoredConversationsMemoriesAndAudio() {
        StorageSnapshot snapshot = privacy.snapshot();
        assertEquals(2, snapshot.conversationCount);
        assertEquals(2, snapshot.memoryCount);
        assertTrue(snapshot.audioFileCount >= 2);
        assertTrue(snapshot.audioBytes > 0L);
        assertFalse(privacy.modelStatus().onDeviceLanguageModelInstalled);
    }

    @Test
    public void modelStatusReportsInstalledOnDeviceLanguageModel() {
        LocalPrivacyStore installed = new LocalPrivacyStore(
                ApplicationProvider.getApplicationContext(),
                conversations,
                memories,
                new VoskModelManager(),
                new OnDeviceLocalLanguageModel()
        );
        assertTrue(installed.modelStatus().onDeviceLanguageModelInstalled);
        assertEquals(OnDeviceLocalLanguageModel.DETAIL, installed.modelStatus().languageDetail);
    }

    private void seed() throws Exception {
        ConversationEntity keep = ConversationEntity.createCapture(
                "c-keep",
                "Monday notes",
                1_000L,
                1_000L,
                audioFile.getAbsolutePath(),
                ProcessingStatus.COMPLETED.name()
        );
        keep.transcript = "Unique xylophone transcript about the client.";
        conversations.insertSync(keep);
        memories.replaceForConversationSync("c-keep", Collections.singletonList(
                MemoryEntity.create("m-keep", MemoryType.FACT.name(),
                        "Budget is five thousand.", "c-keep", 1_100L, 0.8f, new LinkedHashMap<>())));

        File other = RecordingFileStore.fileForId(ApplicationProvider.getApplicationContext(), "c-other");
        writeBytes(other, "other-audio");
        ConversationEntity second = ConversationEntity.createCapture(
                "c-other",
                "Other meeting",
                2_000L,
                500L,
                other.getAbsolutePath(),
                ProcessingStatus.COMPLETED.name()
        );
        second.transcript = "A separate othernote conversation.";
        conversations.insertSync(second);
        memories.replaceForConversationSync("c-other", Collections.singletonList(
                MemoryEntity.create("m-other", MemoryType.FACT.name(),
                        "Othernote fact.", "c-other", 2_100L, 0.7f, new LinkedHashMap<>())));
    }

    private static void writeBytes(@NonNull File file, @NonNull String content) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            assertTrue(parent.mkdirs());
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}
