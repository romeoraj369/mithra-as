package com.gamor.mithrax.domain.tts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory TTS engine for tests. Does not play audio or use a network.
 */
public final class FakeTextToSpeechEngine implements TextToSpeechEngine {

    public boolean available = true;
    public boolean failSpeak;
    @Nullable
    public String unavailableMessage = "TTS unavailable";
    public final List<String> spoken = new ArrayList<>();
    public int stopCount;
    public int prepareCount;

    private boolean speaking;
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void addListener(@NonNull Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public void prepare() {
        prepareCount++;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public boolean isSpeaking() {
        return speaking;
    }

    @Nullable
    @Override
    public String unavailableReason() {
        return available ? null : unavailableMessage;
    }

    @NonNull
    @Override
    public SpeakResult speak(@NonNull String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return SpeakResult.skippedEmpty();
        }
        if (!available) {
            return SpeakResult.unavailable(unavailableMessage == null ? "unavailable" : unavailableMessage);
        }
        if (failSpeak) {
            notifyError("speak failed");
            return SpeakResult.failed("speak failed");
        }
        List<String> chunks = TtsTextChunker.chunk(trimmed);
        spoken.addAll(chunks);
        setSpeaking(true);
        return SpeakResult.started(chunks.size());
    }

    @Override
    public void stop() {
        stopCount++;
        setSpeaking(false);
    }

    @Override
    public void shutdown() {
        stop();
        available = false;
    }

    public void finishSpeaking() {
        setSpeaking(false);
    }

    private void setSpeaking(boolean value) {
        if (speaking == value) {
            return;
        }
        speaking = value;
        for (Listener listener : listeners) {
            listener.onSpeakingChanged(value);
        }
    }

    private void notifyError(@NonNull String message) {
        for (Listener listener : listeners) {
            listener.onError(message);
        }
    }
}
