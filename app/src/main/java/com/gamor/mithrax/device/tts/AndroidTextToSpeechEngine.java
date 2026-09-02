package com.gamor.mithrax.device.tts;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gamor.mithrax.domain.tts.SpeakResult;
import com.gamor.mithrax.domain.tts.TextToSpeechEngine;
import com.gamor.mithrax.domain.tts.TtsTextChunker;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Android {@link TextToSpeech} backend. Prefers an offline voice. Does not call a cloud TTS API.
 */
public class AndroidTextToSpeechEngine implements TextToSpeechEngine {

    public static final String LANGUAGE_UNAVAILABLE =
            "This phone does not have an offline text-to-speech language installed.";
    public static final String ENGINE_UNAVAILABLE =
            "This phone’s text-to-speech engine is not available.";
    public static final String NETWORK_VOICE_ONLY =
            "The installed TTS engine only offers network voices. MithraX will not send answers to a cloud voice service. Install an offline language in system Text-to-speech settings.";

    private static final String TAG = "AndroidTts";
    private static final String UTTERANCE_PREFIX = "mithrax-tts-";

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    @Nullable
    private TextToSpeech textToSpeech;
    private boolean initializing;
    private boolean available;
    @Nullable
    private String unavailableReason;
    @Nullable
    private String pendingText;
    private boolean speaking;
    private int activeChunks;

    public AndroidTextToSpeechEngine(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

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
        mainHandler.post(this::prepareOnMain);
    }

    private void prepareOnMain() {
        if (available || initializing || textToSpeech != null) {
            return;
        }
        initializing = true;
        unavailableReason = null;
        textToSpeech = new TextToSpeech(appContext, this::onEngineInit);
    }

    private void onEngineInit(int status) {
        mainHandler.post(() -> {
            initializing = false;
            if (status != TextToSpeech.SUCCESS || textToSpeech == null) {
                available = false;
                unavailableReason = ENGINE_UNAVAILABLE;
                textToSpeech = null;
                pendingText = null;
                notifyError(ENGINE_UNAVAILABLE);
                return;
            }
            String languageError = configureOfflineLanguage(textToSpeech);
            if (languageError != null) {
                available = false;
                unavailableReason = languageError;
                textToSpeech.shutdown();
                textToSpeech = null;
                pendingText = null;
                notifyError(languageError);
                return;
            }
            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    mainHandler.post(() -> setSpeaking(true));
                }

                @Override
                public void onDone(String utteranceId) {
                    mainHandler.post(() -> onChunkFinished());
                }

                @Override
                public void onError(String utteranceId) {
                    mainHandler.post(() -> {
                        onChunkFinished();
                        notifyError("Text-to-speech failed while speaking.");
                    });
                }
            });
            available = true;
            unavailableReason = null;
            String pending = pendingText;
            pendingText = null;
            if (pending != null) {
                speak(pending);
            }
        });
    }

    @Nullable
    private String configureOfflineLanguage(@NonNull TextToSpeech tts) {
        Locale[] candidates = new Locale[]{Locale.getDefault(), Locale.US, Locale.ENGLISH};
        Locale chosen = null;
        for (Locale locale : candidates) {
            int result = tts.setLanguage(locale);
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                chosen = locale;
                break;
            }
        }
        if (chosen == null) {
            return LANGUAGE_UNAVAILABLE;
        }
        Voice offline = findOfflineVoice(tts, chosen);
        if (offline != null) {
            tts.setVoice(offline);
            return null;
        }
        Voice current = tts.getVoice();
        if (current != null && current.isNetworkConnectionRequired()) {
            return NETWORK_VOICE_ONLY;
        }
        return null;
    }

    @Nullable
    private static Voice findOfflineVoice(@NonNull TextToSpeech tts, @NonNull Locale locale) {
        Set<Voice> voices;
        try {
            voices = tts.getVoices();
        } catch (Exception e) {
            return null;
        }
        if (voices == null) {
            return null;
        }
        Voice languageMatch = null;
        for (Voice voice : voices) {
            if (voice == null || voice.isNetworkConnectionRequired()) {
                continue;
            }
            Set<String> features = voice.getFeatures();
            if (features != null && features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) {
                continue;
            }
            Locale voiceLocale = voice.getLocale();
            if (voiceLocale == null || !voiceLocale.getLanguage().equalsIgnoreCase(locale.getLanguage())) {
                continue;
            }
            if (locale.getCountry().isEmpty()
                    || voiceLocale.getCountry().equalsIgnoreCase(locale.getCountry())) {
                return voice;
            }
            languageMatch = voice;
        }
        return languageMatch;
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
        return unavailableReason;
    }

    @NonNull
    @Override
    public SpeakResult speak(@NonNull String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return SpeakResult.skippedEmpty();
        }
        if (unavailableReason != null && !available) {
            return SpeakResult.unavailable(unavailableReason);
        }
        List<String> chunks = TtsTextChunker.chunk(trimmed);
        if (chunks.isEmpty()) {
            return SpeakResult.skippedEmpty();
        }
        if (!available) {
            pendingText = trimmed;
            prepare();
            return SpeakResult.started(chunks.size());
        }
        TextToSpeech tts = textToSpeech;
        if (tts == null) {
            return SpeakResult.unavailable(ENGINE_UNAVAILABLE);
        }
        tts.stop();
        activeChunks = chunks.size();
        Bundle params = new Bundle();
        int queued = 0;
        for (int i = 0; i < chunks.size(); i++) {
            int queueMode = i == 0 ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD;
            int status = tts.speak(chunks.get(i), queueMode, params, UTTERANCE_PREFIX + i);
            if (status == TextToSpeech.SUCCESS) {
                queued++;
            }
        }
        if (queued == 0) {
            activeChunks = 0;
            setSpeaking(false);
            return SpeakResult.failed("The text-to-speech engine rejected the answer.");
        }
        setSpeaking(true);
        return SpeakResult.started(queued);
    }

    @Override
    public void stop() {
        pendingText = null;
        activeChunks = 0;
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        setSpeaking(false);
    }

    @Override
    public void shutdown() {
        stop();
        available = false;
        initializing = false;
        if (textToSpeech != null) {
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }

    private void onChunkFinished() {
        if (activeChunks > 0) {
            activeChunks--;
        }
        if (activeChunks <= 0) {
            setSpeaking(false);
        }
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
        Log.w(TAG, message);
        for (Listener listener : listeners) {
            listener.onError(message);
        }
    }
}
