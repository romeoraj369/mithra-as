package com.gamor.mithrax.device.ai;

import androidx.annotation.NonNull;

import com.gamor.mithrax.domain.ai.LocalLanguageModel;
import com.gamor.mithrax.domain.ai.AnswerRequest;
import com.gamor.mithrax.domain.ai.AnswerResult;
import com.gamor.mithrax.domain.ai.SummarizationRequest;
import com.gamor.mithrax.domain.ai.SummarizationResult;

/**
 * Isolated integration point for a future on-device LLM.
 * Replace this wiring in {@link com.gamor.mithrax.MithraXApplication} when a local
 * model is bundled. This class must not download weights or call a cloud API.
 */
public final class UnprovisionedLocalLanguageModel implements LocalLanguageModel {

    public static final String MESSAGE =
            "On-device language model is not installed yet";

    @Override
    public boolean isAvailable() {
        return false;
    }

    @NonNull
    @Override
    public SummarizationResult summarize(@NonNull SummarizationRequest request) {
        return SummarizationResult.unavailable(MESSAGE);
    }

    @NonNull
    @Override
    public AnswerResult answer(@NonNull AnswerRequest request) {
        return AnswerResult.unavailable(MESSAGE);
    }

    @Override
    public void cancel() {
        // Nothing to cancel until a local runtime is wired here.
    }
}
