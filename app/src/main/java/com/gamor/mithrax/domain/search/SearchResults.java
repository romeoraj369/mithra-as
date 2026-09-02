package com.gamor.mithrax.domain.search;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SearchResults {

    @NonNull
    public final List<SearchHit> conversations;
    @NonNull
    public final List<SearchHit> memories;

    public SearchResults(@NonNull List<SearchHit> conversations, @NonNull List<SearchHit> memories) {
        this.conversations = Collections.unmodifiableList(new ArrayList<>(conversations));
        this.memories = Collections.unmodifiableList(new ArrayList<>(memories));
    }

    public boolean isEmpty() {
        return conversations.isEmpty() && memories.isEmpty();
    }

    public int size() {
        return conversations.size() + memories.size();
    }
}
