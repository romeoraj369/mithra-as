package com.gamor.mithrax.domain.conversation;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * A follow-up captured from a conversation. Stored locally with the conversation.
 */
public final class ActionItem {

    @NonNull
    public final String text;
    public final boolean completed;

    public ActionItem(@NonNull String text, boolean completed) {
        this.text = text;
        this.completed = completed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ActionItem)) {
            return false;
        }
        ActionItem that = (ActionItem) o;
        return completed == that.completed && text.equals(that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, completed);
    }
}
