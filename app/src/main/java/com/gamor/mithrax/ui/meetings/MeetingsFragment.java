package com.gamor.mithrax.ui.meetings;

import android.Manifest;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gamor.mithrax.MithraXApplication;
import com.gamor.mithrax.R;
import com.gamor.mithrax.data.local.conversation.ConversationEntity;
import com.gamor.mithrax.data.local.conversation.ConversationRepository;
import com.gamor.mithrax.device.audio.RecordingPermissions;
import com.gamor.mithrax.device.recording.RecordingService;
import com.gamor.mithrax.device.recording.RecordingSession;

import java.io.File;
import java.util.List;
import java.util.Map;

public class MeetingsFragment extends Fragment {

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    this::onPermissionResult);

    private RecordingListAdapter adapter;
    private View loadingState;
    private View emptyState;
    private View errorState;
    private TextView errorText;
    private RecyclerView list;
    private Button recordButton;
    private boolean initialLoadDone;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_meetings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadingState = view.findViewById(R.id.recordingsLoading);
        emptyState = view.findViewById(R.id.recordingsEmpty);
        errorState = view.findViewById(R.id.recordingsError);
        errorText = view.findViewById(R.id.recordingsErrorText);
        list = view.findViewById(R.id.recordingsList);
        recordButton = view.findViewById(R.id.recordButton);
        Button retry = view.findViewById(R.id.recordingsRetry);

        adapter = new RecordingListAdapter(new RecordingListAdapter.Listener() {
            @Override
            public void onOpen(@NonNull ConversationEntity recording) {
                openRecording(recording);
            }

            @Override
            public void onDelete(@NonNull ConversationEntity recording) {
                showManageData(recording);
            }
        });
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        retry.setOnClickListener(v -> loadHistory());
        recordButton.setOnClickListener(v -> onRecordClicked());

        showLoading();
        observeHistory();
        loadHistory();
        observeRecordingSession();
    }

    private MithraXApplication app() {
        return (MithraXApplication) requireContext().getApplicationContext();
    }

    private void observeHistory() {
        app().getConversationRepository().observeAll().observe(getViewLifecycleOwner(), recordings -> {
            initialLoadDone = true;
            renderList(recordings);
        });
    }

    private void loadHistory() {
        showLoading();
        app().getConversationRepository().loadAll(new ConversationRepository.LoadCallback() {
            @Override
            public void onLoaded(@NonNull List<ConversationEntity> recordings) {
                if (!isAdded()) {
                    return;
                }
                initialLoadDone = true;
                renderList(recordings);
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) {
                    return;
                }
                showError(message);
            }
        });
    }

    private void renderList(@Nullable List<ConversationEntity> recordings) {
        if (!initialLoadDone) {
            showLoading();
            return;
        }
        errorState.setVisibility(View.GONE);
        loadingState.setVisibility(View.GONE);
        if (recordings == null || recordings.isEmpty()) {
            adapter.submitList(null);
            list.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);
            adapter.submitList(recordings);
        }
    }

    private void showLoading() {
        loadingState.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
    }

    private void showError(@NonNull String message) {
        loadingState.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
        errorState.setVisibility(View.VISIBLE);
        errorText.setText(getString(R.string.recordings_error_message, message));
    }

    private void observeRecordingSession() {
        app().getRecordingSession().observe().observe(getViewLifecycleOwner(), this::bindRecordButton);
    }

    private void bindRecordButton(@NonNull RecordingSession.State state) {
        if (state.recording) {
            recordButton.setText(R.string.action_stop_recording);
            recordButton.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.recording_red)));
        } else {
            recordButton.setText(R.string.action_start_recording);
            recordButton.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.teal_700)));
        }
    }

    private void onRecordClicked() {
        if (app().getRecordingSession().isRecording()) {
            RecordingService.stop(requireContext());
            return;
        }
        if (!RecordingPermissions.hasAll(requireContext())) {
            permissionLauncher.launch(RecordingPermissions.required());
            return;
        }
        RecordingService.start(requireContext());
    }

    private void onPermissionResult(@NonNull Map<String, Boolean> result) {
        Boolean mic = result.get(Manifest.permission.RECORD_AUDIO);
        if (!Boolean.TRUE.equals(mic)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.recording_permission_title)
                    .setMessage(R.string.recording_permission_denied)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        if (!RecordingPermissions.hasNotifications(requireContext())) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.recording_permission_title)
                    .setMessage(R.string.recording_notification_denied)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        RecordingService.start(requireContext());
    }

    private void openRecording(@NonNull ConversationEntity recording) {
        String body = RecordingPresentation.conversationBody(recording);
        File file = new File(recording.audioPath);
        if (!file.exists()) {
            String message = body.isEmpty()
                    ? getString(R.string.recording_file_missing)
                    : body;
            new AlertDialog.Builder(requireContext())
                    .setTitle(recording.title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        RecordingPlaybackDialog.newInstance(
                        recording.title,
                        recording.audioPath,
                        body)
                .show(getParentFragmentManager(), "playback");
    }

    private void showManageData(@NonNull ConversationEntity recording) {
        if (isActiveRecording(recording.id)) {
            new AlertDialog.Builder(requireContext())
                    .setMessage(R.string.privacy_cannot_edit_while_recording)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        CharSequence[] items = new CharSequence[]{
                getString(R.string.privacy_action_delete_audio),
                getString(R.string.privacy_action_delete_transcript),
                getString(R.string.privacy_action_delete_memories),
                getString(R.string.privacy_action_delete_conversation)
        };
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.privacy_manage_title, recording.title))
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        confirmAction(R.string.privacy_delete_audio_title,
                                getString(R.string.privacy_delete_audio_message, recording.title),
                                () -> app().getPrivacyRepository().deleteAudio(recording.id, () -> {
                                }));
                    } else if (which == 1) {
                        confirmAction(R.string.privacy_delete_transcript_title,
                                getString(R.string.privacy_delete_transcript_message, recording.title),
                                () -> app().getPrivacyRepository().deleteTranscript(recording.id, () -> {
                                }));
                    } else if (which == 2) {
                        confirmAction(R.string.privacy_delete_memories_title,
                                getString(R.string.privacy_delete_memories_message, recording.title),
                                () -> app().getPrivacyRepository().deleteMemoriesForConversation(
                                        recording.id, () -> {
                                        }));
                    } else {
                        confirmAction(R.string.recording_delete_title,
                                getString(R.string.recording_delete_message, recording.title),
                                () -> {
                                    app().getTranscriptionController().cancel(recording.id);
                                    app().getSummarizationController().cancel(recording.id);
                                    app().getPrivacyRepository().deleteConversation(recording.id, () -> {
                                    });
                                });
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private boolean isActiveRecording(@NonNull String conversationId) {
        RecordingSession.State state = app().getRecordingSession().current();
        return state.recording && conversationId.equals(state.recordingId);
    }

    private void confirmAction(int titleRes, @NonNull String message, @NonNull Runnable onConfirm) {
        new AlertDialog.Builder(requireContext())
                .setTitle(titleRes)
                .setMessage(message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_delete, (d, w) -> onConfirm.run())
                .show();
    }
}
