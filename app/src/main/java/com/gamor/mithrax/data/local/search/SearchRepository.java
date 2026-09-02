package com.gamor.mithrax.data.local.search;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.gamor.mithrax.domain.search.SearchResults;
import com.gamor.mithrax.domain.search.SearchService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Async wrapper around {@link SearchService} for the UI. Swap the service to add semantic search.
 */
public class SearchRepository {

    public interface Callback {
        void onResults(@NonNull SearchResults results);

        void onError(@NonNull String message);
    }

    private final SearchService searchService;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile int generation;

    public SearchRepository(@NonNull SearchService searchService) {
        this.searchService = searchService;
    }

    public void search(@NonNull String query, @NonNull Callback callback) {
        int token = ++generation;
        io.execute(() -> {
            try {
                SearchResults results = searchService.search(query);
                mainHandler.post(() -> {
                    if (token == generation) {
                        callback.onResults(results);
                    }
                });
            } catch (RuntimeException e) {
                String message = e.getMessage() == null ? "Search failed" : e.getMessage();
                mainHandler.post(() -> {
                    if (token == generation) {
                        callback.onError(message);
                    }
                });
            }
        });
    }
}
