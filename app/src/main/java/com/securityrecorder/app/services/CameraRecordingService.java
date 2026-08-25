package com.securityrecorder.app.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.MutableLiveData;
import com.google.common.util.concurrent.ListenableFuture;
import com.securityrecorder.app.data.preferences.AppPreferences;
import com.securityrecorder.app.data.repository.VideoRepository;
import com.securityrecorder.app.utils.DateTimeUtils;
import com.securityrecorder.app.utils.FileUtils;
import com.securityrecorder.app.utils.LocationHelper;
import com.securityrecorder.app.utils.NotificationHelper;
import java.io.File;

/**
 * Android LifecycleService running as a Foreground Service with WakeLock.
 * Continues camera video capture uninterrupted even when the screen turns off or the device is locked.
 */
public class CameraRecordingService extends LifecycleService {

    public static final String ACTION_START = "com.securityrecorder.app.action.START_RECORDING";
    public static final String ACTION_STOP = "com.securityrecorder.app.action.STOP_RECORDING";

    // Observable LiveData for UI synchronization
    public static final MutableLiveData<Boolean> isRecordingLiveData = new MutableLiveData<>(false);
    public static final MutableLiveData<String> durationLiveData = new MutableLiveData<>("00:00:00");

    // Optional surface provider for live small camera preview
    public static androidx.camera.view.PreviewView previewViewRef = null;

    private PowerManager.WakeLock wakeLock;
    private AppPreferences preferences;
    private VideoRepository repository;

    private ProcessCameraProvider cameraProvider;
    private VideoCapture<Recorder> videoCapture;
    private Recording activeRecording;
    private File currentOutputFile;
    private long recordingStartTime = 0;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = new AppPreferences(this);
        repository = new VideoRepository(getApplication());
        acquireWakeLock();
    }

    private void acquireWakeLock() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "GallerySecurityRecorder::RecordingWakeLock"
                );
                wakeLock.setReferenceCounted(false);
                wakeLock.acquire(12 * 60 * 60 * 1000L); // 12-hour safe cap
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
        super.onStartCommand(intent, flags, startId);

        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                startForeground(
                        NotificationHelper.NOTIFICATION_ID_RECORDING,
                        NotificationHelper.buildRecordingNotification(this, "00:00:00")
                );
                initCameraAndStartRecording();
            } else if (ACTION_STOP.equals(action) || NotificationHelper.ACTION_STOP_RECORDING.equals(action)) {
                stopRecordingAndShutdown();
            }
        }
        return START_NOT_STICKY;
    }

    private void initCameraAndStartRecording() {
        if (activeRecording != null) return;

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                String resolutionSetting = preferences.getResolution();
                Quality targetQuality;
                if ("4k".equalsIgnoreCase(resolutionSetting)) {
                    targetQuality = Quality.UHD;
                } else if ("720p".equalsIgnoreCase(resolutionSetting)) {
                    targetQuality = Quality.HD;
                } else if ("480p".equalsIgnoreCase(resolutionSetting)) {
                    targetQuality = Quality.SD;
                } else {
                    targetQuality = Quality.FHD;
                }

                QualitySelector qualitySelector = QualitySelector.from(
                        targetQuality,
                        FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                );

                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(qualitySelector)
                        .build();

                videoCapture = VideoCapture.withOutput(recorder);

                CameraSelector cameraSelector = preferences.isFrontCamera()
                        ? CameraSelector.DEFAULT_FRONT_CAMERA
                        : CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();

                if (previewViewRef != null) {
                    androidx.camera.core.Preview preview = new androidx.camera.core.Preview.Builder().build();
                    preview.setSurfaceProvider(previewViewRef.getSurfaceProvider());
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture);
                } else {
                    cameraProvider.bindToLifecycle(this, cameraSelector, videoCapture);
                }

                startActualVideoCapture();

            } catch (Exception e) {
                stopRecordingAndShutdown();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("MissingPermission")
    private void startActualVideoCapture() {
        if (videoCapture == null) return;

        File recDir = FileUtils.getRecordingDirectory(this);
        String filename = DateTimeUtils.generateVideoFilename();
        currentOutputFile = new File(recDir, filename);

        FileOutputOptions outputOptions = new FileOutputOptions.Builder(currentOutputFile).build();

        try {
            PendingRecording pendingRecording = videoCapture.getOutput().prepareRecording(this, outputOptions);

            if (preferences.isAudioEnabled() && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                pendingRecording.withAudioEnabled();
            }

            activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(this), event -> {
                if (event instanceof VideoRecordEvent.Start) {
                    onRecordingSessionStarted();
                } else if (event instanceof VideoRecordEvent.Finalize) {
                    onRecordingSessionFinalized((VideoRecordEvent.Finalize) event);
                }
            });

        } catch (Exception e) {
            stopRecordingAndShutdown();
        }
    }

    private void onRecordingSessionStarted() {
        recordingStartTime = System.currentTimeMillis();
        isRecordingLiveData.postValue(true);

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (activeRecording != null) {
                    long elapsed = System.currentTimeMillis() - recordingStartTime;
                    String durationStr = DateTimeUtils.formatDuration(elapsed);
                    durationLiveData.postValue(durationStr);
                    NotificationHelper.showRecordingNotification(CameraRecordingService.this, durationStr);
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void onRecordingSessionFinalized(VideoRecordEvent.Finalize event) {
        timerHandler.removeCallbacks(timerRunnable);
        isRecordingLiveData.postValue(false);
        durationLiveData.postValue("00:00:00");

        if (!event.hasError() && currentOutputFile != null && currentOutputFile.exists() && currentOutputFile.length() > 1024) {
            long duration = System.currentTimeMillis() - recordingStartTime;
            String locationStr = "Unknown";
            if (preferences.isLocationEnabled()) {
                Location loc = LocationHelper.getLastKnownLocation(this);
                locationStr = LocationHelper.formatCoordinates(loc);
            }
            repository.insertRecordedVideo(currentOutputFile, preferences.getResolution(), locationStr, "video/mp4", duration);
        } else {
            // Delete corrupt or zero-byte recordings
            if (currentOutputFile != null && currentOutputFile.exists()) {
                FileUtils.deleteFile(currentOutputFile.getAbsolutePath());
            }
        }

        stopForeground(true);
        NotificationHelper.cancelRecordingNotification(this);
        stopSelf();
    }

    private void stopRecordingAndShutdown() {
        if (activeRecording != null) {
            activeRecording.stop();
            activeRecording = null;
        } else {
            stopForeground(true);
            NotificationHelper.cancelRecordingNotification(this);
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacksAndMessages(null);
        if (activeRecording != null) {
            activeRecording.stop();
            activeRecording = null;
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        isRecordingLiveData.postValue(false);
        durationLiveData.postValue("00:00:00");
        NotificationHelper.cancelRecordingNotification(this);
        releaseWakeLock();
    }

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        super.onBind(intent);
        return null;
    }
}
