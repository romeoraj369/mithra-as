package com.gamor.mithrax.ui.ask;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.gamor.mithrax.MithraXApplication;
import com.gamor.mithrax.R;
import com.gamor.mithrax.data.local.conversation.ConversationEntity;
import com.gamor.mithrax.device.tts.SpeechPlaybackController;
import com.gamor.mithrax.domain.ask.AnswerSource;
import com.gamor.mithrax.domain.ask.AskResult;
import com.gamor.mithrax.domain.tts.SpeakResult;
import com.gamor.mithrax.domain.tts.TextToSpeechEngine;
import com.gamor.mithrax.ui.meetings.RecordingPlaybackDialog;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AskFragment extends Fragment {

    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private TextInputEditText input;
    private View idleState;
    private View loadingState;
    private View emptyState;
    private View errorState;
    private View answerScroll;
    private TextView questionEcho;
    private TextView answerText;
    private TextView sourcesHeading;
    private LinearLayout sourcesList;
    private TextView emptyText;
    private TextView errorText;
    private Button speakButton;
    private Button stopSpeakButton;
    private TextView ttsStatus;
    private String lastQuestion = "";
    private String speakableText = "";
    private final TextToSpeechEngine.Listener ttsListener = new TextToSpeechEngine.Listener() {
        @Override
        public void onSpeakingChanged(boolean speaking) {
            if (!isAdded()) {
                return;
            }
            bindSpeakButtons(speaking);
            if (speaking) {
                showTtsStatus(getString(R.string.ask_tts_speaking));
            } else if (ttsStatus != null && ttsStatus.getText().toString().equals(getString(R.string.ask_tts_speaking))) {
                ttsStatus.setVisibility(View.GONE);
            }
        }

        @Override
        public void onError(@NonNull String message) {
            if (!isAdded()) {
                return;
            }
            showTtsStatus(getString(R.string.ask_tts_failed, message));
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ask, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        input = view.findViewById(R.id.askInput);
        idleState = view.findViewById(R.id.askIdle);
        loadingState = view.findViewById(R.id.askLoading);
        emptyState = view.findViewById(R.id.askEmpty);
        errorState = view.findViewById(R.id.askError);
        answerScroll = view.findViewById(R.id.askAnswerScroll);
        questionEcho = view.findViewById(R.id.askQuestionEcho);
        answerText = view.findViewById(R.id.askAnswerText);
        sourcesHeading = view.findViewById(R.id.askSourcesHeading);
        sourcesList = view.findViewById(R.id.askSourcesList);
        emptyText = view.findViewById(R.id.askEmptyText);
        errorText = view.findViewById(R.id.askErrorText);
        speakButton = view.findViewById(R.id.askSpeakButton);
        stopSpeakButton = view.findViewById(R.id.askStopSpeakButton);
        ttsStatus = view.findViewById(R.id.askTtsStatus);
        Button askButton = view.findViewById(R.id.askButton);
        Button retry = view.findViewById(R.id.askRetry);

        askButton.setOnClickListener(v -> submit());
        retry.setOnClickListener(v -> submit());
        speakButton.setOnClickListener(v -> speakAnswer());
        stopSpeakButton.setOnClickListener(v -> speech().stop());
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                submit();
                return true;
            }
            return false;
        });
        showIdle();
        speech().addListener(ttsListener);
        bindSpeakButtons(speech().isSpeaking());
    }

    @Override
    public void onPause() {
        speech().stop();
        super.onPause();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            speech().stop();
        }
    }

    @Override
    public void onDestroyView() {
        speech().removeListener(ttsListener);
        speech().stop();
        input = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    private MithraXApplication app() {
        return (MithraXApplication) requireContext().getApplicationContext();
    }

    private void submit() {
        if (input == null) {
            return;
        }
        String question = input.getText() == null ? "" : input.getText().toString().trim();
        lastQuestion = question;
        speech().stop();
        if (question.isEmpty()) {
            showEmpty(getString(R.string.ask_empty_question));
            return;
        }
        showLoading();
        app().getAskRepository().ask(question, result -> {
            if (!isAdded() || !question.equals(lastQuestion)) {
                return;
            }
            render(result);
        });
    }

    private SpeechPlaybackController speech() {
        return app().getSpeechPlaybackController();
    }

    private void render(@NonNull AskResult result) {
        if (result.kind == AskResult.Kind.FAILED) {
            speakableText = "";
            showError(result.answer);
            return;
        }
        if (result.kind == AskResult.Kind.NOT_FOUND) {
            speakableText = "";
            showEmpty(result.answer);
            return;
        }
        speakableText = result.answer == null ? "" : result.answer;
        questionEcho.setText(getString(R.string.ask_question_prefix, result.question));
        answerText.setText(result.answer);
        sourcesList.removeAllViews();
        if (result.sources.isEmpty()) {
            sourcesHeading.setVisibility(View.GONE);
        } else {
            sourcesHeading.setVisibility(View.VISIBLE);
            LayoutInflater inflater = getLayoutInflater();
            for (AnswerSource source : result.sources) {
                View row = inflater.inflate(R.layout.item_ask_source, sourcesList, false);
                TextView title = row.findViewById(R.id.askSourceTitle);
                TextView excerpt = row.findViewById(R.id.askSourceExcerpt);
                title.setText(getString(R.string.ask_source_title, source.title));
                excerpt.setText(source.excerpt);
                row.setOnClickListener(v -> openConversation(source.conversationId));
                sourcesList.addView(row);
            }
        }
        showAnswer();
        bindSpeakButtons(speech().isSpeaking());
        if (!speakableText.trim().isEmpty()) {
            applySpeakResult(speech().speakAutomatically(speakableText), false);
        }
    }

    private void speakAnswer() {
        applySpeakResult(speech().speakNow(speakableText), true);
    }

    private void applySpeakResult(@NonNull SpeakResult result, boolean userTriggered) {
        if (result.kind == SpeakResult.Kind.SKIPPED_EMPTY) {
            if (userTriggered) {
                showTtsStatus(getString(R.string.ask_tts_empty));
            }
            return;
        }
        if (result.kind == SpeakResult.Kind.UNAVAILABLE) {
            showTtsStatus(getString(R.string.ask_tts_unavailable,
                    result.message == null ? SpeechPlaybackController.UNAVAILABLE_FALLBACK : result.message));
            return;
        }
        if (result.kind == SpeakResult.Kind.FAILED) {
            showTtsStatus(getString(R.string.ask_tts_failed,
                    result.message == null ? "unknown error" : result.message));
            return;
        }
        if (result.kind == SpeakResult.Kind.STARTED) {
            showTtsStatus(getString(R.string.ask_tts_speaking));
        }
        bindSpeakButtons(speech().isSpeaking() || result.kind == SpeakResult.Kind.STARTED);
    }

    private void bindSpeakButtons(boolean speaking) {
        if (speakButton == null || stopSpeakButton == null) {
            return;
        }
        boolean hasText = speakableText != null && !speakableText.trim().isEmpty();
        speakButton.setEnabled(hasText && !speaking);
        stopSpeakButton.setEnabled(speaking);
    }

    private void showTtsStatus(@NonNull String message) {
        if (ttsStatus == null) {
            return;
        }
        ttsStatus.setVisibility(View.VISIBLE);
        ttsStatus.setText(message);
    }

    private void openConversation(@NonNull String conversationId) {
        io.execute(() -> {
            ConversationEntity conversation = app().getConversationRepository().getByIdSync(conversationId);
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) {
                    return;
                }
                if (conversation == null) {
                    new AlertDialog.Builder(requireContext())
                            .setMessage(R.string.search_source_missing)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                    return;
                }
                File file = new File(conversation.audioPath);
                if (!file.exists()) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle(conversation.title)
                            .setMessage(conversation.transcript == null || conversation.transcript.isEmpty()
                                    ? getString(R.string.recording_file_missing)
                                    : conversation.transcript)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                    return;
                }
                RecordingPlaybackDialog.newInstance(
                                conversation.title,
                                conversation.audioPath,
                                conversation.transcript == null ? "" : conversation.transcript)
                        .show(getParentFragmentManager(), "playback");
            });
        });
    }

    private void showIdle() {
        setVisible(idleState, true);
        setVisible(loadingState, false);
        setVisible(emptyState, false);
        setVisible(errorState, false);
        setVisible(answerScroll, false);
        speakableText = "";
        bindSpeakButtons(false);
    }

    private void showLoading() {
        setVisible(idleState, false);
        setVisible(loadingState, true);
        setVisible(emptyState, false);
        setVisible(errorState, false);
        setVisible(answerScroll, false);
    }

    private void showAnswer() {
        setVisible(idleState, false);
        setVisible(loadingState, false);
        setVisible(emptyState, false);
        setVisible(errorState, false);
        setVisible(answerScroll, true);
    }

    private void showEmpty(@NonNull String message) {
        emptyText.setText(message);
        setVisible(idleState, false);
        setVisible(loadingState, false);
        setVisible(emptyState, true);
        setVisible(errorState, false);
        setVisible(answerScroll, false);
    }

    private void showError(@NonNull String message) {
        errorText.setText(getString(R.string.ask_error_message, message));
        setVisible(idleState, false);
        setVisible(loadingState, false);
        setVisible(emptyState, false);
        setVisible(errorState, true);
        setVisible(answerScroll, false);
    }

    private static void setVisible(@Nullable View view, boolean visible) {
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
