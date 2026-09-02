package com.gamor.mithrax.domain.ask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * One retrieved passage used to answer a question. Text must come from local storage.
 */
public final class ContextPassage {

    public enum Kind {
        MEMORY,
        CONVERSATION
    }

    @NonNull
    public final Kind kind;
    @NonNull
    public final String conversationId;
    @NonNull
    public final String conversationTitle;
    @NonNull
    public final String text;
    @Nullable
    public final String memoryType;
    public final int score;

    public ContextPassage(@NonNull Kind kind,
                          @NonNull String conversationId,
                          @NonNull String conversationTitle,
                          @NonNull String text,
                          @Nullable String memoryType,
                          int score) {
        this.kind = kind;
        this.conversationId = conversationId;
        this.conversationTitle = conversationTitle;
        this.text = text;
        this.memoryType = memoryType;
        this.score = score;
    }
}
