package com.gamor.mithrax.ui.meetings;

import android.app.Dialog;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.gamor.mithrax.R;

import java.io.File;
import java.io.IOException;

public class RecordingPlaybackDialog extends DialogFragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_PATH = "path";
    private static final String ARG_TRANSCRIPT = "transcript";

    @Nullable
    private MediaPlayer mediaPlayer;

    @NonNull
    public static RecordingPlaybackDialog newInstance(@NonNull String title,
                                                      @NonNull String path,
                                                      @NonNull String transcript) {
        RecordingPlaybackDialog dialog = new RecordingPlaybackDialog();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_PATH, path);
        args.putString(ARG_TRANSCRIPT, transcript);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        String title = args.getString(ARG_TITLE, getString(R.string.recording_open_title));
        String path = args.getString(ARG_PATH, "");
        String transcript = args.getString(ARG_TRANSCRIPT, "");
        String body = transcript.trim().isEmpty()
                ? getString(R.string.recording_no_transcript)
                : transcript;
        File file = new File(path);
        if (!file.exists()) {
            return new AlertDialog.Builder(requireContext())
                    .setTitle(title)
                    .setMessage(R.string.recording_file_missing)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            releasePlayer();
            return new AlertDialog.Builder(requireContext())
                    .setTitle(title)
                    .setMessage(R.string.recording_playback_failed)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        }

        return new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(body)
                .setNegativeButton(R.string.action_stop_playback, (d, w) -> dismiss())
                .create();
    }

    @Override
    public void onDestroyView() {
        releasePlayer();
        super.onDestroyView();
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException ignored) {
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
