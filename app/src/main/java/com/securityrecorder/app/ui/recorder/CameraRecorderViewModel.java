package com.securityrecorder.app.ui.recorder;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.securityrecorder.app.data.model.StorageInfo;
import com.securityrecorder.app.data.preferences.AppPreferences;
import com.securityrecorder.app.data.repository.VideoRepository;
import com.securityrecorder.app.utils.DateTimeUtils;
import com.securityrecorder.app.utils.FileUtils;
import java.io.File;

/**
 * ViewModel managing recording session timers, battery levels, storage limits, and metadata storage.
 */
public class CameraRecorderViewModel extends AndroidViewModel {

    public enum RecordingState {
        IDLE,
        RECORDING,
        STOPPING
    }

    private final MutableLiveData<RecordingState> recordingState = new MutableLiveData<>(RecordingState.IDLE);
    private final MutableLiveData<Long> recordingDurationMs = new MutableLiveData<>(0L);
    private final MutableLiveData<String> formattedDuration = new MutableLiveData<>("00:00:00");
    private final MutableLiveData<Integer> batteryLevel = new MutableLiveData<>(100);
    private final MutableLiveData<Boolean> isStorageCritical = new MutableLiveData<>(false);
    private final MutableLiveData<File> activeOutputFile = new MutableLiveData<>(null);

    private final VideoRepository repository;
    private final AppPreferences preferences;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private long startTimeMs = 0;
    private Runnable timerRunnable;

    private BroadcastReceiver batteryReceiver;

    public CameraRecorderViewModel(@NonNull Application application) {
        super(application);
        this.repository = new VideoRepository(application);
        this.preferences = new AppPreferences(application);
        registerBatteryReceiver();
        checkStorage();
    }

    public LiveData<RecordingState> getRecordingState() {
        return recordingState;
    }

    public LiveData<Long> getRecordingDurationMs() {
        return recordingDurationMs;
    }

    public LiveData<String> getFormattedDuration() {
        return formattedDuration;
    }

    public LiveData<Integer> getBatteryLevel() {
        return batteryLevel;
    }

    public LiveData<Boolean> getIsStorageCritical() {
        return isStorageCritical;
    }

    public void startRecordingSession(File outputFile) {
        activeOutputFile.setValue(outputFile);
        startTimeMs = System.currentTimeMillis();
        recordingState.setValue(RecordingState.RECORDING);

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (recordingState.getValue() == RecordingState.RECORDING) {
                    long elapsed = System.currentTimeMillis() - startTimeMs;
                    recordingDurationMs.setValue(elapsed);
                    formattedDuration.setValue(DateTimeUtils.formatDuration(elapsed));

                    // Check storage limits
                    checkStorage();

                    // Check max duration limit
                    int maxMins = preferences.getMaxDurationMins();
                    if (maxMins > 0 && elapsed >= (maxMins * 60L * 1000L)) {
                        stopRecordingSession();
                        return;
                    }

                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.post(timerRunnable);
    }

    public void stopRecordingSession() {
        if (recordingState.getValue() == RecordingState.RECORDING) {
            recordingState.setValue(RecordingState.STOPPING);
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    public void onRecordingFinalized(File file, String location, String resolution) {
        long duration = recordingDurationMs.getValue() != null ? recordingDurationMs.getValue() : 0L;
        recordingState.setValue(RecordingState.IDLE);
        recordingDurationMs.setValue(0L);
        formattedDuration.setValue("00:00:00");

        if (file != null && file.exists() && file.length() > 0) {
            repository.insertRecordedVideo(file, resolution, location, "video/mp4", duration);
        }
    }

    public void checkStorage() {
        StorageInfo info = FileUtils.getStorageMetrics(getApplication());
        boolean critical = info.isCriticallyLow();
        isStorageCritical.setValue(critical);
        if (critical && recordingState.getValue() == RecordingState.RECORDING) {
            stopRecordingSession();
        }
    }

    private void registerBatteryReceiver() {
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0) {
                    int pct = (int) ((level / (float) scale) * 100);
                    batteryLevel.setValue(pct);
                }
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        getApplication().registerReceiver(batteryReceiver, filter);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        timerHandler.removeCallbacks(timerRunnable);
        if (batteryReceiver != null) {
            try {
                getApplication().unregisterReceiver(batteryReceiver);
            } catch (Exception ignored) {}
        }
    }
}
