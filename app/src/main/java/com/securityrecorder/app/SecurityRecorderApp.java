package com.securityrecorder.app;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.securityrecorder.app.data.preferences.AppPreferences;
import com.securityrecorder.app.utils.NotificationHelper;
import com.securityrecorder.app.workers.CleanupWorker;
import java.util.concurrent.TimeUnit;

/**
 * Gallery Security Recorder Application Class.
 * Developed by: Amit Bharat
 */
public class SecurityRecorderApp extends Application {

    private static SecurityRecorderApp instance;
    private AppPreferences appPreferences;

    public static SecurityRecorderApp getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        appPreferences = new AppPreferences(this);

        // Apply dark/light theme preference
        applyAppTheme();

        // Create system notification channels
        NotificationHelper.createNotificationChannels(this);

        // Schedule periodic cleanup worker if enabled
        scheduleCleanupWorker();
    }

    public void applyAppTheme() {
        int themeMode = appPreferences.getThemeMode();
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    public void scheduleCleanupWorker() {
        if (appPreferences.isAutoCleanupEnabled()) {
            PeriodicWorkRequest cleanupRequest =
                    new PeriodicWorkRequest.Builder(CleanupWorker.class, 24, TimeUnit.HOURS)
                            .build();

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "SecurityRecorderCleanupWork",
                    ExistingPeriodicWorkPolicy.KEEP,
                    cleanupRequest
            );
        } else {
            WorkManager.getInstance(this).cancelUniqueWork("SecurityRecorderCleanupWork");
        }
    }

    public AppPreferences getAppPreferences() {
        return appPreferences;
    }
}
