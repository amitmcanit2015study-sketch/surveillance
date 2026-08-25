package com.securityrecorder.app.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.securityrecorder.app.SecurityRecorderApp;
import com.securityrecorder.app.data.preferences.AppPreferences;
import com.securityrecorder.app.data.repository.VideoRepository;

/**
 * Background WorkManager worker executing periodic automatic cleanup of old security recordings.
 */
public class CleanupWorker extends Worker {

    public CleanupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppPreferences prefs = new AppPreferences(getApplicationContext());
        if (!prefs.isAutoCleanupEnabled()) {
            return Result.success();
        }

        int days = prefs.getCleanupDays();
        VideoRepository repository = new VideoRepository((SecurityRecorderApp) getApplicationContext());
        repository.cleanupOldRecordings(days);

        return Result.success();
    }
}
