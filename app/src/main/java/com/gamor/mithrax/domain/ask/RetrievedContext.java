package com.gamor.mithrax.domain.ask;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RetrievedContext {

    @NonNull
    public final List<ContextPassage> passages;

    public RetrievedContext(@NonNull List<ContextPassage> passages) {
        this.passages = Collections.unmodifiableList(new ArrayList<>(passages));
    }

    @NonNull
    public static RetrievedContext empty() {
        return new RetrievedContext(Collections.emptyList());
    }

    public boolean isEmpty() {
        return passages.isEmpty();
    }

    @NonNull
    public String packedForModel() {
        StringBuilder pack = new StringBuilder();
        for (ContextPassage passage : passages) {
            pack.append('[').append(passage.kind);
            if (passage.memoryType != null) {
                pack.append(' ').append(passage.memoryType);
            }
            pack.append(" | ").append(passage.conversationTitle).append("]\n");
            pack.append(passage.text).append("\n\n");
        }
        return pack.toString().trim();
    }
}
