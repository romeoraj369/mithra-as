package com.gamor.mithrax.device.recording;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.gamor.mithrax.MithraXApplication;
import com.gamor.mithrax.R;
import com.gamor.mithrax.data.local.conversation.ConversationEntity;
import com.gamor.mithrax.data.local.recording.RecordingFileStore;
import com.gamor.mithrax.domain.recording.ProcessingStatus;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Explicit, user-started capture. Runs as a microphone foreground service so
 * recording can continue when the UI is backgrounded. Audio never leaves the device.
 */
public class RecordingService extends Service {

    public static final String ACTION_START = "com.gamor.mithrax.action.START_RECORDING";
    public static final String ACTION_STOP = "com.gamor.mithrax.action.STOP_RECORDING";

    private static final String TAG = "RecordingService";

    private final LocalMediaRecorder mediaRecorder = new LocalMediaRecorder();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    private String recordingId;
    private long startedAtElapsed;
    private long createdAtWall;
    @Nullable
    private File outputFile;
    private boolean stopping;

    private final AudioManager.OnAudioFocusChangeListener audioFocusListener = focusChange -> {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS
                || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            Log.w(TAG, "Audio focus lost; stopping recording");
            stopAndPersist(true);
        }
    };

    private final Runnable notificationTicker = new Runnable() {
        @Override
        public void run() {
            if (!mediaRecorder.isRunning()) {
                return;
            }
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.notify(
                        RecordingNotification.NOTIFICATION_ID,
                        RecordingNotification.build(RecordingService.this, formatElapsed())
                );
            }
            mainHandler.postDelayed(this, 1000L);
        }
    };

    public static void start(@NonNull Context context) {
        Intent intent = new Intent(context, RecordingService.class).setAction(ACTION_START);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void stop(@NonNull Context context) {
        Intent intent = new Intent(context, RecordingService.class).setAction(ACTION_STOP);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        RecordingNotification.ensureChannel(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_STOP : intent.getAction();
        if (ACTION_START.equals(action)) {
            if (!mediaRecorder.isRunning()) {
                beginRecording();
            }
            return START_STICKY;
        }
        stopAndPersist(false);
        return START_NOT_STICKY;
    }

    private void beginRecording() {
        MithraXApplication app = (MithraXApplication) getApplication();
        app.getMicrophoneLease().releaseAll();

        recordingId = UUID.randomUUID().toString();
        outputFile = RecordingFileStore.fileForId(this, recordingId);
        createdAtWall = System.currentTimeMillis();
        startedAtElapsed = SystemClock.elapsedRealtime();

        try {
            enterForeground();
            mediaRecorder.start(this, outputFile, message -> mainHandler.post(() -> {
                session().setError(message);
                stopAndPersist(true);
            }));
            requestAudioFocus();
            session().setRecording(recordingId, createdAtWall);
            mainHandler.post(notificationTicker);
            String title = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
                    .format(new Date(createdAtWall));
            ConversationEntity inProgress = ConversationEntity.createCapture(
                    recordingId,
                    getString(R.string.recording_title_prefix, title),
                    createdAtWall,
                    0L,
                    outputFile.getAbsolutePath(),
                    ProcessingStatus.RECORDING.name()
            );
            app.getConversationRepository().insert(inProgress);
        } catch (IOException | RuntimeException e) {
            Log.e(TAG, "Unable to start recording", e);
            mediaRecorder.stopAndRelease();
            if (outputFile != null) {
                RecordingFileStore.deleteQuietly(outputFile.getAbsolutePath());
            }
            if (recordingId != null) {
                app.getConversationRepository().delete(recordingId, () -> {
                });
            }
            session().setError(getString(R.string.recording_start_failed));
            stopForeground(true);
            stopSelf();
        }
    }

    private void enterForeground() {
        android.app.Notification notification = RecordingNotification.build(this, formatElapsed());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                    RecordingNotification.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            );
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    RecordingNotification.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            );
        } else {
            startForeground(RecordingNotification.NOTIFICATION_ID, notification);
        }
    }

    private void stopAndPersist(boolean interrupted) {
        if (stopping) {
            return;
        }
        stopping = true;
        mainHandler.removeCallbacks(notificationTicker);
        abandonAudioFocus();

        boolean saved = mediaRecorder.stopAndRelease();
        File file = outputFile;
        String id = recordingId;
        long duration = Math.max(0L, SystemClock.elapsedRealtime() - startedAtElapsed);

        recordingId = null;
        outputFile = null;

        if (id != null && file != null && saved && file.exists() && file.length() > 0) {
            String title = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
                    .format(new Date(createdAtWall));
            ConversationEntity entity = ConversationEntity.createCapture(
                    id,
                    getString(R.string.recording_title_prefix, title),
                    createdAtWall,
                    duration,
                    file.getAbsolutePath(),
                    ProcessingStatus.TRANSCRIBING.name()
            );
            MithraXApplication app = (MithraXApplication) getApplication();
            app.getConversationRepository().completeCapture(id, duration, entity, () ->
                    app.getTranscriptionController().enqueue(id));
            session().setIdle();
        } else {
            if (file != null) {
                RecordingFileStore.deleteQuietly(file.getAbsolutePath());
            }
            if (id != null) {
                ((MithraXApplication) getApplication()).getConversationRepository().delete(id, () -> {
                });
            }
            if (interrupted) {
                session().setError(getString(R.string.recording_interrupted));
            } else if (id != null) {
                session().setError(getString(R.string.recording_empty));
            } else {
                session().setIdle();
            }
        }

        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        mainHandler.removeCallbacks(notificationTicker);
        if (mediaRecorder.isRunning()) {
            stopping = false;
            stopAndPersist(true);
        }
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // Keep recording after the task is backgrounded; only persist if the recorder is gone.
        super.onTaskRemoved(rootIntent);
    }

    private void requestAudioFocus() {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.requestAudioFocus(
                    audioFocusListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            );
        }
    }

    private void abandonAudioFocus() {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.abandonAudioFocus(audioFocusListener);
        }
    }

    @NonNull
    private String formatElapsed() {
        long elapsed = Math.max(0L, SystemClock.elapsedRealtime() - startedAtElapsed);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsed);
        return String.format(Locale.getDefault(), "%02d:%02d", seconds / 60L, seconds % 60L);
    }

    @NonNull
    private RecordingSession session() {
        return ((MithraXApplication) getApplication()).getRecordingSession();
    }
}
