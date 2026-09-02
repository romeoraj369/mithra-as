package com.gamor.mithrax.device.stt;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gamor.mithrax.data.local.conversation.ConversationEntity;
import com.gamor.mithrax.data.local.conversation.ConversationRepository;
import com.gamor.mithrax.device.understanding.SummarizationController;
import com.gamor.mithrax.domain.recording.ProcessingStatus;
import com.gamor.mithrax.domain.stt.SpeechToTextEngine;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Queues local transcription after a recording is saved.
 */
public class TranscriptionController {

    private static final String TAG = "Transcription";

    private final ConversationRepository repository;
    private final VoskSpeechToTextEngine engine;
    private final SummarizationController summarizationController;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    private volatile String activeRecordingId;

    public TranscriptionController(@NonNull ConversationRepository repository,
                                   @NonNull VoskSpeechToTextEngine engine,
                                   @NonNull SummarizationController summarizationController) {
        this.repository = repository;
        this.engine = engine;
        this.summarizationController = summarizationController;
    }

    public void enqueue(@NonNull String recordingId) {
        executor.execute(() -> transcribe(recordingId));
    }

    public void resumeInterrupted() {
        executor.execute(() -> {
            try {
                List<ConversationEntity> conversations =
                        repository.getByStatusSync(ProcessingStatus.TRANSCRIBING.name());
                for (ConversationEntity conversation : conversations) {
                    transcribe(conversation.id);
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to resume interrupted transcription", e);
            }
        });
    }

    public void cancel(@NonNull String recordingId) {
        String active = activeRecordingId;
        if (recordingId.equals(active)) {
            engine.cancel();
        }
    }

    private void transcribe(@NonNull String recordingId) {
        ConversationEntity conversation = repository.getByIdSync(recordingId);
        if (conversation == null || !conversation.hasAudioFile()) {
            return;
        }
        activeRecordingId = recordingId;
        engine.resetCancel();
        repository.updateTranscript(recordingId, conversation.transcript, ProcessingStatus.TRANSCRIBING.name());
        engine.transcribe(new File(conversation.audioPath), new SpeechToTextEngine.Callback() {
            @Override
            public void onProgress(@Nullable String partialTranscript) {
                if (partialTranscript != null) {
                    repository.updateTranscript(
                            recordingId, partialTranscript, ProcessingStatus.TRANSCRIBING.name());
                }
            }

            @Override
            public void onCompleted(@NonNull String transcript) {
                Log.d(TAG, "Transcription completed for " + recordingId);
                repository.updateTranscript(
                        recordingId, transcript, ProcessingStatus.SUMMARIZING.name());
                activeRecordingId = null;
                summarizationController.enqueue(recordingId);
            }

            @Override
            public void onError(@NonNull String message) {
                Log.e(TAG, "Transcription failed for " + recordingId);
                ConversationEntity latest = repository.getByIdSync(recordingId);
                String kept = latest == null || latest.transcript == null ? "" : latest.transcript;
                repository.updateTranscript(recordingId, kept, ProcessingStatus.FAILED.name());
                activeRecordingId = null;
            }

            @Override
            public void onCancelled() {
                Log.d(TAG, "Transcription cancelled for " + recordingId);
                activeRecordingId = null;
            }
        });
    }
}
