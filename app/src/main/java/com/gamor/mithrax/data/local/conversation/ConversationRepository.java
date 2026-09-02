package com.gamor.mithrax.data.local.conversation;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.gamor.mithrax.data.local.recording.RecordingFileStore;
import com.gamor.mithrax.data.local.search.LocalSearchIndex;
import com.gamor.mithrax.domain.conversation.ActionItem;
import com.gamor.mithrax.domain.recording.ProcessingStatus;
import com.gamor.mithrax.domain.understanding.ConversationInsights;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConversationRepository {

    public interface Callback {
        void onComplete();
    }

    public interface LoadCallback {
        void onLoaded(@NonNull List<ConversationEntity> conversations);

        void onError(@NonNull String message);
    }

    private final ConversationDao dao;
    @Nullable
    private final LocalSearchIndex searchIndex;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ConversationRepository(@NonNull ConversationDao dao) {
        this(dao, null);
    }

    public ConversationRepository(@NonNull ConversationDao dao, @Nullable LocalSearchIndex searchIndex) {
        this.dao = dao;
        this.searchIndex = searchIndex;
    }

    @NonNull
    public LiveData<List<ConversationEntity>> observeAll() {
        return dao.observeAll();
    }

    @NonNull
    public LiveData<ConversationEntity> observeById(@NonNull String id) {
        return dao.observeById(id);
    }

    public void loadAll(@NonNull LoadCallback callback) {
        io.execute(() -> {
            try {
                List<ConversationEntity> conversations = dao.getAllSync();
                mainHandler.post(() -> callback.onLoaded(conversations));
            } catch (RuntimeException e) {
                String message = e.getMessage() == null ? "Unable to load conversations" : e.getMessage();
                mainHandler.post(() -> callback.onError(message));
            }
        });
    }

    @NonNull
    public List<ConversationEntity> getAllSync() {
        List<ConversationEntity> conversations = dao.getAllSync();
        return conversations == null ? Collections.emptyList() : conversations;
    }

    @Nullable
    public ConversationEntity getByIdSync(@NonNull String id) {
        return dao.getById(id);
    }

    @NonNull
    public List<ConversationEntity> getByStatusSync(@NonNull String status) {
        List<ConversationEntity> conversations = dao.getByStatus(status);
        return conversations == null ? Collections.emptyList() : conversations;
    }

    public void insert(@NonNull ConversationEntity conversation) {
        io.execute(() -> {
            dao.insert(conversation);
            reindex(conversation.id);
        });
    }

    public void insertSync(@NonNull ConversationEntity conversation) {
        dao.insert(conversation);
        reindex(conversation.id);
    }

    public void completeCapture(@NonNull String id,
                                long durationMillis,
                                @NonNull ConversationEntity fallback,
                                @NonNull Callback callback) {
        io.execute(() -> {
            ConversationEntity existing = dao.getById(id);
            long now = System.currentTimeMillis();
            if (existing == null) {
                fallback.updatedAtMillis = now;
                dao.insert(fallback);
            } else {
                dao.updateDurationAndStatus(id, durationMillis, ProcessingStatus.TRANSCRIBING.name(), now);
            }
            reindex(id);
            callback.onComplete();
        });
    }

    public void updateDurationAndStatusSync(@NonNull String id,
                                            long durationMillis,
                                            @NonNull String status) {
        dao.updateDurationAndStatus(id, durationMillis, status, System.currentTimeMillis());
        reindex(id);
    }

    public void updateTranscript(@NonNull String id, @NonNull String transcript, @NonNull String status) {
        dao.updateTranscript(id, transcript, status, System.currentTimeMillis());
        reindex(id);
    }

    public void updateTitle(@NonNull String id, @NonNull String title) {
        io.execute(() -> {
            dao.updateTitle(id, title, System.currentTimeMillis());
            reindex(id);
        });
    }

    public void updateAudioPath(@NonNull String id, @NonNull String audioPath) {
        io.execute(() -> dao.updateAudioPath(id, audioPath, System.currentTimeMillis()));
    }

    public void updateInsights(@NonNull String id,
                               @NonNull String summary,
                               @NonNull List<String> keyPoints,
                               @NonNull List<ActionItem> actionItems,
                               @NonNull List<String> importantFacts) {
        io.execute(() -> {
            dao.updateInsights(id, summary, keyPoints, actionItems, importantFacts, System.currentTimeMillis());
            reindex(id);
        });
    }

    public void updateInsightsSync(@NonNull String id,
                                   @NonNull String summary,
                                   @NonNull List<String> keyPoints,
                                   @NonNull List<ActionItem> actionItems,
                                   @NonNull List<String> importantFacts) {
        dao.updateInsights(id, summary, keyPoints, actionItems, importantFacts, System.currentTimeMillis());
        reindex(id);
    }

    public void updateMetadataSync(@NonNull String id, @NonNull Map<String, String> metadata) {
        dao.updateMetadata(id, metadata, System.currentTimeMillis());
        reindex(id);
    }

    public void updateStatusSync(@NonNull String id, @NonNull String status) {
        dao.updateStatus(id, status, System.currentTimeMillis());
    }

    public void completeInsightsSync(@NonNull String id,
                                     @NonNull ConversationInsights insights,
                                     @NonNull String status,
                                     @NonNull Map<String, String> metadata) {
        dao.completeInsights(
                id,
                insights.summary,
                insights.keyPoints,
                insights.actionItems,
                insights.importantFacts,
                metadata,
                status,
                System.currentTimeMillis()
        );
        reindex(id);
    }

    public void delete(@NonNull ConversationEntity conversation, @NonNull Callback callback) {
        delete(conversation.id, callback);
    }

    public void delete(@NonNull String id, @NonNull Callback callback) {
        io.execute(() -> {
            deleteSync(id);
            mainHandler.post(callback::onComplete);
        });
    }

    public void deleteSync(@NonNull String id) {
        ConversationEntity conversation = dao.getById(id);
        if (conversation == null) {
            return;
        }
        if (searchIndex != null) {
            searchIndex.removeConversation(conversation.id);
        }
        String audioPath = conversation.audioPath;
        dao.delete(conversation);
        if (audioPath != null && !audioPath.isEmpty()) {
            RecordingFileStore.deleteQuietly(audioPath);
        }
    }

    public void deleteAudioSync(@NonNull String id) {
        ConversationEntity conversation = dao.getById(id);
        if (conversation == null) {
            return;
        }
        if (conversation.hasAudioFile()) {
            RecordingFileStore.deleteQuietly(conversation.audioPath);
        }
        dao.updateAudioPath(id, "", System.currentTimeMillis());
    }

    public void deleteTranscriptSync(@NonNull String id) {
        ConversationEntity conversation = dao.getById(id);
        if (conversation == null) {
            return;
        }
        dao.updateTranscript(id, "", conversation.processingStatus, System.currentTimeMillis());
        reindex(id);
    }

    public void deleteAllSync() {
        List<ConversationEntity> conversations = getAllSync();
        for (ConversationEntity conversation : conversations) {
            if (searchIndex != null) {
                searchIndex.removeConversation(conversation.id);
            }
            if (conversation.hasAudioFile()) {
                RecordingFileStore.deleteQuietly(conversation.audioPath);
            }
        }
        dao.deleteAll();
        if (searchIndex != null) {
            searchIndex.clearAll();
        }
    }

    public int countSync() {
        return dao.count();
    }

    private void reindex(@NonNull String id) {
        if (searchIndex == null) {
            return;
        }
        searchIndex.upsertConversation(dao.getById(id));
    }
}
