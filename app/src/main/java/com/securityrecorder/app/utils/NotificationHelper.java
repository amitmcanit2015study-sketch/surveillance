package com.securityrecorder.app.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.securityrecorder.app.R;
import com.securityrecorder.app.ui.main.MainActivity;

/**
 * System notification channel and builder helper for both Video and Audio Recording.
 */
public class NotificationHelper {

    public static final String CHANNEL_RECORDING = "channel_recording";
    public static final String CHANNEL_AUDIO_RECORDING = "channel_audio_recording";
    public static final String CHANNEL_ALERTS = "channel_alerts";

    public static final int NOTIFICATION_ID_RECORDING = 1001;
    public static final int NOTIFICATION_ID_STORAGE = 1002;
    public static final int NOTIFICATION_ID_AUDIO = 1003;

    public static final String ACTION_STOP_RECORDING = "com.securityrecorder.app.ACTION_STOP_RECORDING";
    public static final String ACTION_STOP_AUDIO_RECORDING = "com.securityrecorder.app.ACTION_STOP_AUDIO_RECORDING";

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            NotificationChannel recordingChannel = new NotificationChannel(
                    CHANNEL_RECORDING,
                    "Camera Recording",
                    NotificationManager.IMPORTANCE_LOW
            );
            recordingChannel.setDescription("Shows active video recording status and controls");

            NotificationChannel audioChannel = new NotificationChannel(
                    CHANNEL_AUDIO_RECORDING,
                    "Audio Recording",
                    NotificationManager.IMPORTANCE_LOW
            );
            audioChannel.setDescription("Shows active audio recording status and controls");

            NotificationChannel alertChannel = new NotificationChannel(
                    CHANNEL_ALERTS,
                    "Security Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertChannel.setDescription("Important storage and safety alerts");

            manager.createNotificationChannel(recordingChannel);
            manager.createNotificationChannel(audioChannel);
            manager.createNotificationChannel(alertChannel);
        }
    }

    public static Notification buildRecordingNotification(Context context, String durationText) {
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Intent stopIntent = new Intent(context, MainActivity.class);
        stopIntent.setAction(ACTION_STOP_RECORDING);
        stopIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent stopPendingIntent = PendingIntent.getActivity(
                context,
                1,
                stopIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(context, CHANNEL_RECORDING)
                .setContentTitle("Surveillance · Video Recording")
                .setContentText("Recording in progress: " + durationText)
                .setSmallIcon(R.drawable.ic_camera_record)
                .setContentIntent(openPendingIntent)
                .addAction(R.drawable.ic_stop_record, "Stop Recording", stopPendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    public static Notification buildAudioRecordingNotification(Context context, String durationText) {
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context,
                2,
                openIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Intent stopIntent = new Intent(context, MainActivity.class);
        stopIntent.setAction(ACTION_STOP_AUDIO_RECORDING);
        stopIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent stopPendingIntent = PendingIntent.getActivity(
                context,
                3,
                stopIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(context, CHANNEL_AUDIO_RECORDING)
                .setContentTitle("Surveillance · Audio Recording")
                .setContentText("Audio capturing: " + durationText)
                .setSmallIcon(R.drawable.ic_mic)
                .setContentIntent(openPendingIntent)
                .addAction(R.drawable.ic_stop_record, "Stop Audio", stopPendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    public static void showRecordingNotification(Context context, String durationText) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID_RECORDING, buildRecordingNotification(context, durationText));
        }
    }

    public static void cancelRecordingNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(NOTIFICATION_ID_RECORDING);
        }
    }

    public static void showAudioNotification(Context context, String durationText) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID_AUDIO, buildAudioRecordingNotification(context, durationText));
        }
    }

    public static void cancelAudioNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(NOTIFICATION_ID_AUDIO);
        }
    }

    public static void showStorageWarningNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ALERTS)
                .setContentTitle(context.getString(R.string.storage_warning_title))
                .setContentText(context.getString(R.string.storage_critical_message))
                .setSmallIcon(R.drawable.ic_storage)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        manager.notify(NOTIFICATION_ID_STORAGE, notification);
    }
}
