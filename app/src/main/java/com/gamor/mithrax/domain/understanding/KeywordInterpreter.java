package com.gamor.mithrax.domain.understanding;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Existing keyword replies. Replace later with a local LLM without changing callers.
 */
public class KeywordInterpreter {

    @NonNull
    public String interpret(@Nullable String inputText) {
        if (inputText == null || inputText.trim().isEmpty()) {
            return "I didn't catch that.";
        }

        String normalizedText = inputText.toLowerCase(Locale.US).trim();
        String response = "Sorry, I didn't understand that.";

        if (isMatch(normalizedText, new String[]{"hello", "hi", "hey", "greetings"})) {
            response = "Hi there! How can I help you today?";
        } else if (isMatch(normalizedText, new String[]{"how are you", "how's it going"})) {
            response = "I'm doing well, thank you for asking!";
        } else if (isMatch(normalizedText, new String[]{"good", "great", "fine", "okay"})) {
            if (normalizedText.contains("good morning")) {
                response = "Good morning to you too!";
            } else if (normalizedText.contains("good afternoon")) {
                response = "Good afternoon!";
            } else {
                response = "That's glad to hear!";
            }
        } else if (isMatch(normalizedText, new String[]{"bye", "goodbye", "see ya", "later"})) {
            response = "Goodbye! Have a great day!";
        } else if (normalizedText.contains("what time is it")) {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            response = "The current time is " + sdf.format(new Date());
        }

        return response;
    }

    private boolean isMatch(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
