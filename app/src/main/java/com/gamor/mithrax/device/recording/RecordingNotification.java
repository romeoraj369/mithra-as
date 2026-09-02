package com.gamor.mithrax.device.recording;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.gamor.mithrax.MainActivity;
import com.gamor.mithrax.R;

public final class RecordingNotification {

    public static final String CHANNEL_ID = "mithrax_recording";
    public static final int NOTIFICATION_ID = 1001;

    private RecordingNotification() {
    }

    public static void ensureChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.recording_notification_channel),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.recording_notification_channel_desc));
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    @NonNull
    public static Notification build(@NonNull Context context, @NonNull String elapsed) {
        Intent openApp = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(context, RecordingService.class)
                .setAction(RecordingService.ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(
                context,
                1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_recording_24)
                .setContentTitle(context.getString(R.string.recording_notification_title))
                .setContentText(context.getString(R.string.recording_notification_text, elapsed))
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setColor(ContextCompat.getColor(context, R.color.recording_red))
                .setContentIntent(contentIntent)
                .addAction(R.drawable.ic_stop_24, context.getString(R.string.action_stop_recording), stopPending)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build();
    }
}
