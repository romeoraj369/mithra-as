package com.gamor.mithrax.data.local.privacy;

import android.content.Context;

import androidx.annotation.NonNull;

import com.gamor.mithrax.data.local.conversation.ConversationEntity;
import com.gamor.mithrax.data.local.conversation.ConversationRepository;
import com.gamor.mithrax.data.local.memory.MemoryRepository;
import com.gamor.mithrax.data.local.recording.RecordingFileStore;
import com.gamor.mithrax.device.ai.OnDeviceLocalLanguageModel;
import com.gamor.mithrax.device.ai.QwenLocalLanguageModel;
import com.gamor.mithrax.device.ai.UnprovisionedLocalLanguageModel;
import com.gamor.mithrax.device.stt.VoskModelManager;
import com.gamor.mithrax.domain.ai.LocalLanguageModel;
import com.gamor.mithrax.domain.privacy.ModelStatus;
import com.gamor.mithrax.domain.privacy.StorageSnapshot;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Destructive local-only data operations. Nothing here contacts a network.
 */
public class LocalPrivacyStore {

    public static final String DATABASE_NAME = "mithrax.db";

    private final Context appContext;
    private final ConversationRepository conversations;
    private final MemoryRepository memories;
    private final VoskModelManager voskModelManager;
    private final LocalLanguageModel languageModel;

    public LocalPrivacyStore(@NonNull Context context,
                             @NonNull ConversationRepository conversations,
                             @NonNull MemoryRepository memories,
                             @NonNull VoskModelManager voskModelManager,
                             @NonNull LocalLanguageModel languageModel) {
        this.appContext = context.getApplicationContext();
        this.conversations = conversations;
        this.memories = memories;
        this.voskModelManager = voskModelManager;
        this.languageModel = languageModel;
    }

    public void deleteConversationSync(@NonNull String conversationId) {
        conversations.deleteSync(conversationId);
    }

    public void deleteAudioSync(@NonNull String conversationId) {
        conversations.deleteAudioSync(conversationId);
    }

    public void deleteTranscriptSync(@NonNull String conversationId) {
        conversations.deleteTranscriptSync(conversationId);
    }

    public void deleteMemoriesForConversationSync(@NonNull String conversationId) {
        memories.deleteByConversationSync(conversationId);
    }

    public void deleteAllConversationsSync() {
        conversations.deleteAllSync();
        RecordingFileStore.deleteAllInDirectory(appContext);
    }

    public void clearAllMemoriesSync() {
        memories.deleteAllSync();
    }

    @NonNull
    public StorageSnapshot snapshot() {
        List<ConversationEntity> rows = conversations.getAllSync();
        Set<String> audioPaths = new LinkedHashSet<>();
        long audioBytes = 0L;
        File recordingsDir = RecordingFileStore.directory(appContext);
        File[] stored = recordingsDir.listFiles();
        if (stored != null) {
            for (File file : stored) {
                if (file.isFile()) {
                    audioPaths.add(canonical(file));
                    audioBytes += file.length();
                }
            }
        }
        for (ConversationEntity conversation : rows) {
            if (!conversation.hasAudioFile()) {
                continue;
            }
            File file = new File(conversation.audioPath);
            String key = canonical(file);
            if (audioPaths.add(key) && file.isFile()) {
                audioBytes += file.length();
            }
        }
        return new StorageSnapshot(
                rows.size(),
                memories.countActiveSync(),
                audioPaths.size(),
                audioBytes,
                databaseBytes()
        );
    }

    @NonNull
    public ModelStatus modelStatus() {
        boolean speechBundled = voskModelManager.isModelAvailable(appContext);
        String speechDetail = speechBundled
                ? "Vosk speech recognition runs on this phone from a bundled model. Audio is not sent to a speech API."
                : "No Vosk model is bundled in this install, so on-device transcription is not available. MithraX still does not send audio to a cloud speech service.";
        boolean llmInstalled = languageModel.isAvailable();
        String languageDetail;
        if (languageModel instanceof QwenLocalLanguageModel) {
            QwenLocalLanguageModel qwen = (QwenLocalLanguageModel) languageModel;
            languageDetail = qwen.modelStatusText();
            llmInstalled = qwen.hasModelFile() && qwen.isRuntimeReady();
        } else if (llmInstalled) {
            languageDetail = languageDetailFor(languageModel);
        } else {
            languageDetail = UnprovisionedLocalLanguageModel.MESSAGE
                    + ". Ask MithraX still answers on this phone using stored conversations and memories. There is no cloud chatbot in this app.";
        }
        return new ModelStatus(
                "Speech recognition",
                speechDetail,
                "On-device language model",
                languageDetail,
                speechBundled,
                llmInstalled
        );
    }

    @NonNull
    private static String languageDetailFor(@NonNull LocalLanguageModel languageModel) {
        if (languageModel instanceof QwenLocalLanguageModel) {
            return QwenLocalLanguageModel.DETAIL;
        }
        if (languageModel instanceof OnDeviceLocalLanguageModel) {
            return OnDeviceLocalLanguageModel.DETAIL;
        }
        return "An on-device language model is installed on this phone.";
    }

    private long databaseBytes() {
        File db = appContext.getDatabasePath(DATABASE_NAME);
        long total = 0L;
        if (db.exists()) {
            total += db.length();
        }
        File wal = new File(db.getPath() + "-wal");
        File shm = new File(db.getPath() + "-shm");
        if (wal.exists()) {
            total += wal.length();
        }
        if (shm.exists()) {
            total += shm.length();
        }
        return total;
    }

    @NonNull
    private static String canonical(@NonNull File file) {
        try {
            return file.getCanonicalPath();
        } catch (Exception e) {
            return file.getAbsolutePath();
        }
    }
}
