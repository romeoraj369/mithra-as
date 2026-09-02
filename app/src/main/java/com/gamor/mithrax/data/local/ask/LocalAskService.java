package com.gamor.mithrax.data.local.ask;

import androidx.annotation.NonNull;

import com.gamor.mithrax.domain.ai.AnswerRequest;
import com.gamor.mithrax.domain.ai.AnswerResult;
import com.gamor.mithrax.domain.ai.LocalLanguageModel;
import com.gamor.mithrax.domain.ask.AskResult;
import com.gamor.mithrax.domain.ask.AskService;
import com.gamor.mithrax.domain.ask.ContextSelector;
import com.gamor.mithrax.domain.ask.GroundedContextAnswerer;
import com.gamor.mithrax.domain.ask.RetrievedContext;
import com.gamor.mithrax.domain.search.SearchResults;
import com.gamor.mithrax.domain.search.SearchService;

/**
 * Question → local FTS retrieval → context selection → on-device answer.
 */
public class LocalAskService implements AskService {

    private final SearchService searchService;
    private final RoomPassageLoader passageLoader;
    private final LocalLanguageModel languageModel;
    private final GroundedContextAnswerer groundedAnswerer;

    public LocalAskService(@NonNull SearchService searchService,
                           @NonNull RoomPassageLoader passageLoader,
                           @NonNull LocalLanguageModel languageModel) {
        this(searchService, passageLoader, languageModel, new GroundedContextAnswerer());
    }

    public LocalAskService(@NonNull SearchService searchService,
                           @NonNull RoomPassageLoader passageLoader,
                           @NonNull LocalLanguageModel languageModel,
                           @NonNull GroundedContextAnswerer groundedAnswerer) {
        this.searchService = searchService;
        this.passageLoader = passageLoader;
        this.languageModel = languageModel;
        this.groundedAnswerer = groundedAnswerer;
    }

    @NonNull
    @Override
    public AskResult ask(@NonNull String question) {
        String trimmed = question.trim();
        if (trimmed.isEmpty()) {
            return AskResult.notFound(trimmed, GroundedContextAnswerer.NOT_FOUND);
        }
        try {
            SearchResults hits = searchService.search(trimmed);
            RetrievedContext context = ContextSelector.select(trimmed, passageLoader.load(hits));
            if (context.isEmpty()) {
                return AskResult.notFound(trimmed, GroundedContextAnswerer.NOT_FOUND);
            }
            if (languageModel.isAvailable()) {
                AnswerResult generated = languageModel.answer(new AnswerRequest(trimmed, context));
                if (generated.kind == AnswerResult.Kind.SUCCESS && !generated.text.trim().isEmpty()) {
                    AskResult grounded = groundedAnswerer.answer(trimmed, context);
                    return AskResult.answered(trimmed, generated.text.trim(), grounded.sources, true);
                }
                if (generated.kind == AnswerResult.Kind.FAILED) {
                    return AskResult.failed(trimmed,
                            generated.message == null ? "On-device answering failed" : generated.message);
                }
            }
            return groundedAnswerer.answer(trimmed, context);
        } catch (RuntimeException e) {
            String message = e.getMessage() == null ? "Ask failed" : e.getMessage();
            return AskResult.failed(trimmed, message);
        }
    }
}
