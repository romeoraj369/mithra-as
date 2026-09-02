package com.gamor.mithrax.domain.ai;

import androidx.annotation.NonNull;

/**
 * On-device language model. Implementations must not call a network or cloud LLM API.
 * {@link com.gamor.mithrax.device.ai.QwenLocalLanguageModel} is the production
 * implementation. Keep {@link com.gamor.mithrax.device.ai.UnprovisionedLocalLanguageModel}
 * for tests and as a fallback if a future neural runtime is not bundled.
 */
public interface LocalLanguageModel {

    boolean isAvailable();

    @NonNull
    SummarizationResult summarize(@NonNull SummarizationRequest request);

    /**
     * Answer a question using only {@link AnswerRequest#context}. Do not use a network.
     */
    @NonNull
    AnswerResult answer(@NonNull AnswerRequest request);

    void cancel();
}
