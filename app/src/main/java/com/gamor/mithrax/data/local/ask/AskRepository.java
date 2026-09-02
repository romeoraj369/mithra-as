package com.gamor.mithrax.data.local.ask;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.gamor.mithrax.domain.ask.AskResult;
import com.gamor.mithrax.domain.ask.AskService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AskRepository {

    public interface Callback {
        void onResult(@NonNull AskResult result);
    }

    private final AskService askService;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile int generation;

    public AskRepository(@NonNull AskService askService) {
        this.askService = askService;
    }

    public void ask(@NonNull String question, @NonNull Callback callback) {
        int token = ++generation;
        io.execute(() -> {
            AskResult result = askService.ask(question);
            mainHandler.post(() -> {
                if (token == generation) {
                    callback.onResult(result);
                }
            });
        });
    }
}
