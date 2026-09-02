package com.gamor.mithrax.data.local.privacy;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.gamor.mithrax.domain.privacy.ModelStatus;
import com.gamor.mithrax.domain.privacy.StorageSnapshot;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrivacyRepository {

    public interface Callback {
        void onComplete();
    }

    public interface SnapshotCallback {
        void onSnapshot(@NonNull StorageSnapshot snapshot, @NonNull ModelStatus modelStatus);
    }

    private final LocalPrivacyStore store;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public PrivacyRepository(@NonNull LocalPrivacyStore store) {
        this.store = store;
    }

    @NonNull
    public LocalPrivacyStore store() {
        return store;
    }

    public void loadOverview(@NonNull SnapshotCallback callback) {
        io.execute(() -> {
            StorageSnapshot snapshot = store.snapshot();
            ModelStatus modelStatus = store.modelStatus();
            mainHandler.post(() -> callback.onSnapshot(snapshot, modelStatus));
        });
    }

    public void deleteConversation(@NonNull String conversationId, @NonNull Callback callback) {
        io.execute(() -> {
            store.deleteConversationSync(conversationId);
            mainHandler.post(callback::onComplete);
        });
    }

    public void deleteAudio(@NonNull String conversationId, @NonNull Callback callback) {
        io.execute(() -> {
            store.deleteAudioSync(conversationId);
            mainHandler.post(callback::onComplete);
        });
    }

    public void deleteTranscript(@NonNull String conversationId, @NonNull Callback callback) {
        io.execute(() -> {
            store.deleteTranscriptSync(conversationId);
            mainHandler.post(callback::onComplete);
        });
    }

    public void deleteMemoriesForConversation(@NonNull String conversationId, @NonNull Callback callback) {
        io.execute(() -> {
            store.deleteMemoriesForConversationSync(conversationId);
            mainHandler.post(callback::onComplete);
        });
    }

    public void deleteAllConversations(@NonNull Callback callback) {
        io.execute(() -> {
            store.deleteAllConversationsSync();
            mainHandler.post(callback::onComplete);
        });
    }

    public void clearAllMemories(@NonNull Callback callback) {
        io.execute(() -> {
            store.clearAllMemoriesSync();
            mainHandler.post(callback::onComplete);
        });
    }
}
