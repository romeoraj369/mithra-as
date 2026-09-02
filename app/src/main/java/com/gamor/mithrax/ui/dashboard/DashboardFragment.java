package com.gamor.mithrax.ui.dashboard;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.gamor.mithrax.MithraXApplication;
import com.gamor.mithrax.R;
import com.gamor.mithrax.device.audio.MicrophoneLease;
import com.gamor.mithrax.device.audio.MicrophonePermission;
import com.gamor.mithrax.device.stt.SpeechListeningController;
import com.gamor.mithrax.device.stt.VoskModelManager;
import com.gamor.mithrax.domain.tts.SpeakResult;
import com.gamor.mithrax.domain.understanding.KeywordInterpreter;

import org.vosk.Model;

import java.io.IOException;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";

    private final KeywordInterpreter interpreter = new KeywordInterpreter();
    private final SpeechListeningController speechController = new SpeechListeningController();

    private final ActivityResultLauncher<String> microphonePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startListening();
                } else {
                    setStatus(getString(R.string.status_mic_permission_denied));
                }
            });

    private TextView statusText;
    private Button startButton;
    private boolean modelReady;
    @Nullable
    private String lastError;
    private final MicrophoneLease.Holder microphoneHolder = this::stopListening;

    public DashboardFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        statusText = view.findViewById(R.id.statusText1);
        startButton = view.findViewById(R.id.startButton1);
        startButton.setOnClickListener(v -> onMicClicked());
        updateButtonText();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refreshStatus();
        loadModel();
        app().getMicrophoneLease().register(microphoneHolder);
        app().getRecordingSession().observe().observe(getViewLifecycleOwner(), state -> {
            if (state.recording) {
                stopListening();
                app().getSpeechPlaybackController().stop();
            }
        });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            stopListening();
            app().getSpeechPlaybackController().stop();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopListening();
        app().getSpeechPlaybackController().stop();
    }

    @Override
    public void onDestroyView() {
        if (getContext() != null) {
            app().getMicrophoneLease().unregister(microphoneHolder);
        }
        stopListening();
        statusText = null;
        startButton = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        speechController.stop();
        super.onDestroy();
    }

    private void onMicClicked() {
        if (speechController.isListening()) {
            stopListening();
            refreshStatus();
            return;
        }
        if (app().getRecordingSession().isRecording()) {
            return;
        }
        if (!MicrophonePermission.isGranted(requireContext())) {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            return;
        }
        startListening();
    }

    private MithraXApplication app() {
        return (MithraXApplication) requireContext().getApplicationContext();
    }

    private void loadModel() {
        modelReady = false;
        lastError = null;
        refreshStatus();
        VoskModelManager manager = app().getVoskModelManager();
        manager.ensureLoaded(requireContext(), new VoskModelManager.Callback() {
            @Override
            public void onLoaded(@NonNull Model model) {
                modelReady = true;
                lastError = null;
                refreshStatus();
            }

            @Override
            public void onError(@NonNull String message) {
                modelReady = false;
                lastError = message;
                refreshStatus();
            }
        });
    }

    private void startListening() {
        if (!isAdded() || app().getRecordingSession().isRecording()) {
            return;
        }
        Model model = app().getVoskModelManager().getModelIfReady();
        if (model == null) {
            if (lastError == null) {
                lastError = getString(R.string.status_model_not_ready);
            }
            refreshStatus();
            Log.e(TAG, "Model is not ready");
            return;
        }
        try {
            speechController.start(model, new SpeechListeningController.Listener() {
                @Override
                public void onPartialText(@NonNull String text) {
                    setStatus(text);
                }

                @Override
                public void onUtterance(@NonNull String text) {
                    handleUtterance(text);
                }

                @Override
                public void onError(@NonNull String message) {
                    stopListening();
                    lastError = getString(R.string.status_stt_error, message);
                    refreshStatus();
                }

                @Override
                public void onTimeout() {
                    stopListening();
                    setStatus(getString(R.string.status_listening_timeout));
                }
            });
            lastError = null;
            setStatus(getString(R.string.status_listening));
            updateButtonText();
        } catch (IOException e) {
            Log.e(TAG, "Unable to start listening", e);
            lastError = getString(R.string.status_stt_error,
                    e.getMessage() == null ? "start failed" : e.getMessage());
            refreshStatus();
        }
    }

    private void handleUtterance(@NonNull String recognizedText) {
        setStatus(recognizedText.isEmpty()
                ? getString(R.string.status_empty_transcript)
                : recognizedText);
        String response = interpreter.interpret(recognizedText);
        stopListening();
        SpeakResult spoken = app().getSpeechPlaybackController().speakAutomatically(response);
        if (spoken.kind == SpeakResult.Kind.UNAVAILABLE || spoken.kind == SpeakResult.Kind.FAILED) {
            lastError = spoken.message;
        }
        setStatus(response);
        updateButtonText();
    }

    private void stopListening() {
        speechController.stop();
        updateButtonText();
    }

    private void refreshStatus() {
        if (speechController.isListening()) {
            setStatus(getString(R.string.status_listening));
            return;
        }
        if (lastError != null) {
            setStatus(lastError);
            return;
        }
        if (!modelReady) {
            setStatus(getString(R.string.status_loading_model));
            return;
        }
        setStatus(getString(R.string.status_ready));
        updateButtonText();
    }

    private void setStatus(@NonNull String text) {
        if (statusText != null) {
            statusText.setText(text);
        }
    }

    private void updateButtonText() {
        if (startButton == null) {
            return;
        }
        startButton.setText(speechController.isListening()
                ? R.string.action_stop_listening
                : R.string.action_start_listening);
    }
}
