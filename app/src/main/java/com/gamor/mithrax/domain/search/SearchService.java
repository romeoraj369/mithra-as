package com.gamor.mithrax.domain.search;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * Local retrieval of conversations and memories. Implementations must not use a network.
 * A later vector/semantic implementation can satisfy this same contract.
 */
public interface SearchService {

    @NonNull
    SearchResults search(@NonNull String query);

    @NonNull
    default SearchResults empty() {
        return new SearchResults(Collections.emptyList(), Collections.emptyList());
    }
}
