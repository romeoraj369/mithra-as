package com.gamor.mithrax.ui.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.gamor.mithrax.MithraXApplication;
import com.gamor.mithrax.R;
import com.gamor.mithrax.device.recording.RecordingSession;
import com.gamor.mithrax.domain.privacy.ModelStatus;
import com.gamor.mithrax.domain.privacy.StorageSnapshot;
import com.gamor.mithrax.ui.meetings.RecordingPresentation;
import com.gamor.mithrax.ui.privacy.PrivacyPresentation;

public class SettingsFragment extends PreferenceFragmentCompat {

    private Preference storagePreference;
    private Preference modelsPreference;
    private Preference recordingPreference;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_settings, rootKey);
        storagePreference = findPreference("privacy_storage");
        modelsPreference = findPreference("privacy_models");
        recordingPreference = findPreference("privacy_recording");

        Preference deleteAll = findPreference("delete_all_conversations");
        if (deleteAll != null) {
            deleteAll.setOnPreferenceClickListener(p -> {
                confirmDeleteAllConversations();
                return true;
            });
        }
        Preference clearMemories = findPreference("clear_all_memories");
        if (clearMemories != null) {
            clearMemories.setOnPreferenceClickListener(p -> {
                confirmClearAllMemories();
                return true;
            });
        }
        if (storagePreference != null) {
            storagePreference.setOnPreferenceClickListener(p -> {
                refreshOverview();
                return true;
            });
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        app().getRecordingSession().observe().observe(getViewLifecycleOwner(), this::bindRecordingStatus);
        refreshOverview();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshOverview();
    }

    private MithraXApplication app() {
        return (MithraXApplication) requireContext().getApplicationContext();
    }

    private void refreshOverview() {
        app().getPrivacyRepository().loadOverview((snapshot, modelStatus) -> {
            if (!isAdded()) {
                return;
            }
            bindStorage(snapshot);
            bindModels(modelStatus);
        });
    }

    private void bindStorage(@NonNull StorageSnapshot snapshot) {
        if (storagePreference != null) {
            storagePreference.setSummary(PrivacyPresentation.formatSnapshot(snapshot));
        }
    }

    private void bindModels(@NonNull ModelStatus status) {
        if (modelsPreference != null) {
            String summary = status.speechDetail + "\n\n" + status.languageDetail;
            modelsPreference.setSummary(summary);
            modelsPreference.setTitle(status.onDeviceLanguageModelInstalled
                    ? getString(R.string.settings_models_title_ready)
                    : getString(R.string.settings_models_title));
        }
    }

    private void bindRecordingStatus(@NonNull RecordingSession.State state) {
        if (recordingPreference == null || !isAdded()) {
            return;
        }
        String base = getString(R.string.settings_recording_summary);
        if (state.recording) {
            long elapsed = Math.max(0L, System.currentTimeMillis() - state.startedAtMillis);
            recordingPreference.setSummary(base + "\n\n"
                    + getString(R.string.settings_recording_active,
                    RecordingPresentation.formatDuration(elapsed)));
        } else {
            recordingPreference.setSummary(base + "\n\n" + getString(R.string.settings_recording_idle));
        }
    }

    private void confirmDeleteAllConversations() {
        if (app().getRecordingSession().isRecording()) {
            Toast.makeText(requireContext(), R.string.settings_delete_while_recording, Toast.LENGTH_LONG)
                    .show();
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_delete_all_conversations_title)
                .setMessage(R.string.settings_delete_all_conversations_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_delete, (d, w) -> {
                    app().getPrivacyRepository().deleteAllConversations(() -> {
                        if (!isAdded()) {
                            return;
                        }
                        Toast.makeText(requireContext(),
                                R.string.settings_delete_all_conversations_done, Toast.LENGTH_SHORT).show();
                        refreshOverview();
                    });
                })
                .show();
    }

    private void confirmClearAllMemories() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_clear_all_memories_title)
                .setMessage(R.string.settings_clear_all_memories_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_delete, (d, w) -> {
                    app().getPrivacyRepository().clearAllMemories(() -> {
                        if (!isAdded()) {
                            return;
                        }
                        Toast.makeText(requireContext(),
                                R.string.settings_clear_all_memories_done, Toast.LENGTH_SHORT).show();
                        refreshOverview();
                    });
                })
                .show();
    }
}
