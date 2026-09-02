package com.gamor.mithrax.domain.ai;

import androidx.annotation.NonNull;

import com.gamor.mithrax.domain.ask.RetrievedContext;

/**
 * Question plus retrieved local passages. Must not leave the device.
 */
public final class AnswerRequest {

    @NonNull
    public final String question;
    @NonNull
    public final RetrievedContext context;

    public AnswerRequest(@NonNull String question, @NonNull RetrievedContext context) {
        this.question = question;
        this.context = context;
    }
}
