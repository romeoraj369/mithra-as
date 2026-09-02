package com.gamor.mithrax.device.tts;

import androidx.annotation.NonNull;

import com.gamor.mithrax.domain.tts.SpeakResult;
import com.gamor.mithrax.domain.tts.TextToSpeechEngine;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

/**
 * Optional spoken replies. Auto-play honors the user setting; an explicit Speak action
 * bypasses that setting. Never starts speech for blank text.
 */
public class SpeechPlaybackController {

    public static final String UNAVAILABLE_FALLBACK =
            "Spoken replies are unavailable. This phone has no offline text-to-speech engine or language.";

    private final TextToSpeechEngine engine;
    private final BooleanSupplier spokenRepliesEnabled;
    private final CopyOnWriteArrayList<TextToSpeechEngine.Listener> listeners =
            new CopyOnWriteArrayList<>();

    public SpeechPlaybackController(@NonNull TextToSpeechEngine engine,
                                    @NonNull BooleanSupplier spokenRepliesEnabled) {
        this.engine = engine;
        this.spokenRepliesEnabled = spokenRepliesEnabled;
        this.engine.addListener(new TextToSpeechEngine.Listener() {
            @Override
            public void onSpeakingChanged(boolean speaking) {
                for (TextToSpeechEngine.Listener listener : listeners) {
                    listener.onSpeakingChanged(speaking);
                }
            }

            @Override
            public void onError(@NonNull String message) {
                for (TextToSpeechEngine.Listener listener : listeners) {
                    listener.onError(message);
                }
            }
        });
    }

    public void prepare() {
        engine.prepare();
    }

    public void addListener(@NonNull TextToSpeechEngine.Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(@NonNull TextToSpeechEngine.Listener listener) {
        listeners.remove(listener);
    }

    public boolean isSpeaking() {
        return engine.isSpeaking();
    }

    public boolean autoSpeakEnabled() {
        return spokenRepliesEnabled.getAsBoolean();
    }

    /**
     * Speaks after an answer is shown, only if the user enabled spoken replies.
     */
    @NonNull
    public SpeakResult speakAutomatically(@NonNull String text) {
        if (!spokenRepliesEnabled.getAsBoolean()) {
            return SpeakResult.skippedDisabled();
        }
        return speakNow(text);
    }

    /**
     * Speaks because the user tapped Speak. Works even when auto spoken replies are off.
     */
    @NonNull
    public SpeakResult speakNow(@NonNull String text) {
        engine.prepare();
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            engine.stop();
            return SpeakResult.skippedEmpty();
        }
        SpeakResult result = engine.speak(trimmed);
        if (result.kind == SpeakResult.Kind.UNAVAILABLE && result.message == null) {
            return SpeakResult.unavailable(UNAVAILABLE_FALLBACK);
        }
        return result;
    }

    public void stop() {
        engine.stop();
    }

    public void shutdown() {
        engine.shutdown();
    }
}
