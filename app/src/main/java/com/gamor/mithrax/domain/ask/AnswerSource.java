package com.gamor.mithrax.domain.ask;

import androidx.annotation.NonNull;

import java.util.Objects;

public final class AnswerSource {

    @NonNull
    public final String conversationId;
    @NonNull
    public final String title;
    @NonNull
    public final String excerpt;

    public AnswerSource(@NonNull String conversationId, @NonNull String title, @NonNull String excerpt) {
        this.conversationId = conversationId;
        this.title = title;
        this.excerpt = excerpt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AnswerSource)) {
            return false;
        }
        AnswerSource that = (AnswerSource) o;
        return conversationId.equals(that.conversationId) && title.equals(that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversationId, title);
    }
}
