package com.gamor.mithrax.domain.ai;

import androidx.annotation.NonNull;

import com.gamor.mithrax.domain.conversation.ActionItem;
import com.gamor.mithrax.domain.understanding.ConversationInsights;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FakeLocalLanguageModel implements LocalLanguageModel {

    public boolean available = true;
    public SummarizationResult.Kind kind = SummarizationResult.Kind.SUCCESS;
    public String message = "inference failed";
    public ConversationInsights insights;
    public final List<SummarizationRequest> requests = new ArrayList<>();
    public String answerText;
    public final List<AnswerRequest> answerRequests = new ArrayList<>();
    public final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    public boolean waitForCancel;

    @Override
    public boolean isAvailable() {
        return available;
    }

    @NonNull
    @Override
    public SummarizationResult summarize(@NonNull SummarizationRequest request) {
        requests.add(request);
        if (waitForCancel) {
            while (!cancelRequested.get()) {
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return SummarizationResult.cancelled();
                }
            }
            return SummarizationResult.cancelled();
        }
        if (kind == SummarizationResult.Kind.FAILED) {
            return SummarizationResult.failed(message);
        }
        if (kind == SummarizationResult.Kind.UNAVAILABLE) {
            return SummarizationResult.unavailable(message);
        }
        if (kind == SummarizationResult.Kind.CANCELLED) {
            return SummarizationResult.cancelled();
        }
        ConversationInsights output = insights != null
                ? insights
                : new ConversationInsights(
                "Summary: " + request.transcript,
                Collections.singletonList("Point from chunk " + request.chunkIndex),
                Collections.singletonList(new ActionItem("Action " + request.chunkIndex, false)),
                Collections.singletonList("Fact " + request.chunkIndex)
        );
        return SummarizationResult.success(output, 1);
    }

    @NonNull
    @Override
    public AnswerResult answer(@NonNull AnswerRequest request) {
        answerRequests.add(request);
        if (kind == SummarizationResult.Kind.FAILED) {
            return AnswerResult.failed(message);
        }
        if (kind == SummarizationResult.Kind.UNAVAILABLE) {
            return AnswerResult.unavailable(message);
        }
        if (answerText != null) {
            return AnswerResult.success(answerText);
        }
        if (!request.context.isEmpty()) {
            return AnswerResult.success(request.context.passages.get(0).text);
        }
        return AnswerResult.notFound("no context");
    }

    @Override
    public void cancel() {
        cancelRequested.set(true);
    }
}
