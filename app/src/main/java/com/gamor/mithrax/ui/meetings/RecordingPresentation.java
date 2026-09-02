package com.gamor.mithrax.ui.meetings;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.gamor.mithrax.R;
import com.gamor.mithrax.data.local.conversation.ConversationEntity;
import com.gamor.mithrax.domain.recording.ProcessingStatus;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class RecordingPresentation {

    private RecordingPresentation() {
    }

    @NonNull
    public static String formatDuration(long durationMillis) {
        long totalSeconds = Math.max(0L, TimeUnit.MILLISECONDS.toSeconds(durationMillis));
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    @StringRes
    public static int statusLabelRes(@NonNull String processingStatus) {
        if (ProcessingStatus.RECORDING.name().equals(processingStatus)) {
            return R.string.recording_status_recording;
        }
        if (ProcessingStatus.TRANSCRIBING.name().equals(processingStatus)) {
            return R.string.recording_status_transcribing;
        }
        if (ProcessingStatus.SUMMARIZING.name().equals(processingStatus)) {
            return R.string.recording_status_summarizing;
        }
        if (ProcessingStatus.FAILED.name().equals(processingStatus)) {
            return R.string.recording_status_failed;
        }
        return R.string.recording_status_completed;
    }

    @ColorRes
    public static int statusColorRes(@NonNull String processingStatus) {
        if (ProcessingStatus.RECORDING.name().equals(processingStatus)
                || ProcessingStatus.FAILED.name().equals(processingStatus)) {
            return R.color.recording_red;
        }
        if (ProcessingStatus.TRANSCRIBING.name().equals(processingStatus)
                || ProcessingStatus.SUMMARIZING.name().equals(processingStatus)) {
            return R.color.recording_transcribing;
        }
        return R.color.teal_700;
    }

    @NonNull
    public static String conversationBody(@NonNull ConversationEntity conversation) {
        StringBuilder body = new StringBuilder();
        if (conversation.summary != null && !conversation.summary.trim().isEmpty()) {
            body.append(conversation.summary.trim());
        }
        if (conversation.importantFacts != null) {
            for (String fact : conversation.importantFacts) {
                if (fact != null && !fact.trim().isEmpty()) {
                    if (body.length() > 0) {
                        body.append("\n\n");
                    }
                    body.append(fact.trim());
                }
            }
        }
        if (conversation.transcript != null && !conversation.transcript.trim().isEmpty()) {
            if (body.length() > 0) {
                body.append("\n\n");
            }
            body.append(conversation.transcript.trim());
        }
        return body.toString();
    }
}
