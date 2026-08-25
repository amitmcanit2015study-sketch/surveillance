package com.securityrecorder.app.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.securityrecorder.app.data.repository.VideoRepository;
import com.securityrecorder.app.utils.DateTimeUtils;
import com.securityrecorder.app.utils.FileUtils;
import com.securityrecorder.app.utils.NotificationHelper;
import java.io.File;

/**
 * Foreground service managing uninterrupted high-fidelity audio capture (.m4a/AAC).
 * Runs seamlessly with WakeLock even when the phone is locked or screen is off.
 */
public class AudioRecordingService extends Service {

    public static final String ACTION_START_AUDIO = "com.securityrecorder.app.action.START_AUDIO";
    public static final String ACTION_STOP_AUDIO = "com.securityrecorder.app.action.STOP_AUDIO";

    public static final MutableLiveData<Boolean> isAudioRecordingLiveData = new MutableLiveData<>(false);
    public static final MutableLiveData<String> audioDurationLiveData = new MutableLiveData<>("00:00:00");

    private PowerManager.WakeLock wakeLock;
    private MediaRecorder mediaRecorder;
    private File currentOutputFile;
    private long recordingStartTime = 0;
    private boolean isRecording = false;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        acquireWakeLock();
    }

    private void acquireWakeLock() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "Surveillance::AudioRecordingWakeLock"
                );
                wakeLock.setReferenceCounted(false);
                wakeLock.acquire(12 * 60 * 60 * 1000L);
            }
        } catch (Exception ignored) {}
    }

    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception ignored) {}
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START_AUDIO.equals(action)) {
                startForeground(
                        NotificationHelper.NOTIFICATION_ID_AUDIO,
                        NotificationHelper.buildAudioRecordingNotification(this, "00:00:00")
                );
                startAudioCapture();
            } else if (ACTION_STOP_AUDIO.equals(action) || NotificationHelper.ACTION_STOP_AUDIO_RECORDING.equals(action)) {
                stopAudioCaptureAndShutdown();
            }
        }
        return START_NOT_STICKY;
    }

    private void startAudioCapture() {
        if (isRecording) return;

        try {
            File recDir = FileUtils.getRecordingDirectory(this);
            String filename = DateTimeUtils.generateVideoFilename().replace(".mp4", ".m4a");
            currentOutputFile = new File(recDir, filename);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mediaRecorder = new MediaRecorder(this);
            } else {
                mediaRecorder = new MediaRecorder();
            }

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(currentOutputFile.getAbsolutePath());

            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            recordingStartTime = System.currentTimeMillis();
            isAudioRecordingLiveData.postValue(true);

            timerRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isRecording) {
                        long elapsed = System.currentTimeMillis() - recordingStartTime;
                        String durationStr = DateTimeUtils.formatDuration(elapsed);
                        audioDurationLiveData.postValue(durationStr);
                        NotificationHelper.showAudioNotification(AudioRecordingService.this, durationStr);
                        timerHandler.postDelayed(this, 1000);
                    }
                }
            };
            timerHandler.post(timerRunnable);

        } catch (Exception e) {
            stopAudioCaptureAndShutdown();
        }
    }

    private void stopAudioCaptureAndShutdown() {
        timerHandler.removeCallbacks(timerRunnable);
        isRecording = false;
        isAudioRecordingLiveData.postValue(false);
        audioDurationLiveData.postValue("00:00:00");

        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();
            } catch (Exception ignored) {}
            mediaRecorder = null;
        }

        if (currentOutputFile != null && currentOutputFile.exists() && currentOutputFile.length() > 512) {
            long duration = System.currentTimeMillis() - recordingStartTime;
            new VideoRepository(getApplication()).insertRecordedVideo(
                    currentOutputFile,
                    "Audio (M4A)",
                    "Not available",
                    "audio/mp4a-latm",
                    duration
            );
        } else if (currentOutputFile != null && currentOutputFile.exists()) {
            FileUtils.deleteFile(currentOutputFile.getAbsolutePath());
        }

        stopForeground(true);
        NotificationHelper.cancelAudioNotification(this);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacksAndMessages(null);
        if (isRecording) {
            stopAudioCaptureAndShutdown();
        }
        releaseWakeLock();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
