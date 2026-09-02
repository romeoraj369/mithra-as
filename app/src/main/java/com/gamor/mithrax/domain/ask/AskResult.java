package com.gamor.mithrax.domain.ask;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AskResult {

    public enum Kind {
        ANSWERED,
        NOT_FOUND,
        FAILED
    }

    @NonNull
    public final Kind kind;
    @NonNull
    public final String question;
    @NonNull
    public final String answer;
    @NonNull
    public final List<AnswerSource> sources;
    public final boolean usedOnDeviceModel;

    public AskResult(@NonNull Kind kind,
                     @NonNull String question,
                     @NonNull String answer,
                     @NonNull List<AnswerSource> sources,
                     boolean usedOnDeviceModel) {
        this.kind = kind;
        this.question = question;
        this.answer = answer;
        this.sources = Collections.unmodifiableList(new ArrayList<>(sources));
        this.usedOnDeviceModel = usedOnDeviceModel;
    }

    @NonNull
    public static AskResult answered(@NonNull String question,
                                     @NonNull String answer,
                                     @NonNull List<AnswerSource> sources,
                                     boolean usedOnDeviceModel) {
        return new AskResult(Kind.ANSWERED, question, answer, sources, usedOnDeviceModel);
    }

    @NonNull
    public static AskResult notFound(@NonNull String question, @NonNull String message) {
        return new AskResult(Kind.NOT_FOUND, question, message, Collections.emptyList(), false);
    }

    @NonNull
    public static AskResult failed(@NonNull String question, @NonNull String message) {
        return new AskResult(Kind.FAILED, question, message, Collections.emptyList(), false);
    }
}
