package com.gamor.mithrax.device.stt;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Vosk emits JSON hypotheses such as {@code {"text":"hello"}} or {@code {"partial":"hel"}}.
 */
public final class VoskHypothesisParser {

    private VoskHypothesisParser() {
    }

    @NonNull
    public static String extractText(@Nullable String hypothesis) {
        if (hypothesis == null) {
            return "";
        }
        String trimmed = hypothesis.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.startsWith("{")) {
            try {
                JSONObject json = new JSONObject(trimmed);
                String text = json.optString("text", "").trim();
                if (!text.isEmpty()) {
                    return text;
                }
                return json.optString("partial", "").trim();
            } catch (JSONException ignored) {
                return trimmed;
            }
        }
        return trimmed;
    }
}
