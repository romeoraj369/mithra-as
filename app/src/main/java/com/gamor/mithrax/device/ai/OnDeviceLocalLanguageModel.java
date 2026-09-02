package com.gamor.mithrax.device.ai;

import androidx.annotation.NonNull;

import com.gamor.mithrax.domain.ai.AnswerRequest;
import com.gamor.mithrax.domain.ai.AnswerResult;
import com.gamor.mithrax.domain.ai.LocalLanguageModel;
import com.gamor.mithrax.domain.ai.SummarizationRequest;
import com.gamor.mithrax.domain.ai.SummarizationResult;
import com.gamor.mithrax.domain.ask.AskResult;
import com.gamor.mithrax.domain.ask.GroundedContextAnswerer;
import com.gamor.mithrax.domain.understanding.ConversationInsights;
import com.gamor.mithrax.domain.understanding.ExtractiveLanguageUnderstanding;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * On-device language model: understands transcripts and answers only from
 * retrieved local context. Does not download weights or call a cloud API.
 */
public final class OnDeviceLocalLanguageModel implements LocalLanguageModel {

    public static final String DETAIL =
            "A language model is installed on this phone. Transcripts are understood "
                    + "on-device and questions are answered from stored conversations and memories.";

    private final ExtractiveLanguageUnderstanding understanding;
    private final GroundedContextAnswerer answerer;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public OnDeviceLocalLanguageModel() {
        this(new ExtractiveLanguageUnderstanding(), new GroundedContextAnswerer());
    }

    OnDeviceLocalLanguageModel(@NonNull ExtractiveLanguageUnderstanding understanding,
                               @NonNull GroundedContextAnswerer answerer) {
        this.understanding = understanding;
        this.answerer = answerer;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @NonNull
    @Override
    public SummarizationResult summarize(@NonNull SummarizationRequest request) {
        cancelled.set(false);
        ConversationInsights insights = understanding.understand(request.transcript);
        if (cancelled.get()) {
            return SummarizationResult.cancelled();
        }
        return SummarizationResult.success(insights, 1);
    }

    @NonNull
    @Override
    public AnswerResult answer(@NonNull AnswerRequest request) {
        cancelled.set(false);
        AskResult result = answerer.answer(request.question, request.context);
        if (result.kind == AskResult.Kind.ANSWERED && !result.answer.trim().isEmpty()) {
            return AnswerResult.success(result.answer.trim());
        }
        if (result.kind == AskResult.Kind.NOT_FOUND) {
            return AnswerResult.notFound(result.answer);
        }
        return AnswerResult.failed(result.answer);
    }

    @Override
    public void cancel() {
        cancelled.set(true);
    }
}
