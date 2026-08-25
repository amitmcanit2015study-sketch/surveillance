package com.securityrecorder.app.ui.settings;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.securityrecorder.app.R;
import com.securityrecorder.app.SecurityRecorderApp;
import com.securityrecorder.app.data.preferences.AppPreferences;
import com.securityrecorder.app.databinding.ActivitySettingsBinding;
import com.securityrecorder.app.ui.auth.AuthActivity;
import com.securityrecorder.app.utils.FileUtils;
import com.securityrecorder.app.utils.HapticUtils;

/**
 * Settings Activity for configuring recording quality, limits, security, and maintenance.
 */
public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SettingsViewModel viewModel;
    private AppPreferences preferences;

    private final ActivityResultLauncher<Intent> pinSetupLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    binding.switchAppLock.setChecked(true);
                    Toast.makeText(this, "Security PIN configured successfully", Toast.LENGTH_SHORT).show();
                } else {
                    binding.switchAppLock.setChecked(preferences.isAppLockEnabled());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        preferences = viewModel.getPreferences();

        initUi();
        setupListeners();
    }

    private void initUi() {
        binding.settingsToolbar.setNavigationOnClickListener(v -> finish());

        // Summaries
        binding.tvResolutionSummary.setText(preferences.getResolution().toUpperCase());
        binding.tvFpsSummary.setText(preferences.getFrameRate() + " FPS");

        int maxMins = preferences.getMaxDurationMins();
        binding.tvMaxDurationSummary.setText(maxMins > 0 ? maxMins + " Minutes" : "No Limit");

        boolean autoClean = preferences.isAutoCleanupEnabled();
        binding.tvCleanupSummary.setText(autoClean ? "Older than " + preferences.getCleanupDays() + " days" : "Disabled");

        // Switches
        binding.switchAudio.setChecked(preferences.isAudioEnabled());
        binding.switchLocation.setChecked(preferences.isLocationEnabled());
        binding.switchStabilization.setChecked(preferences.isStabilizationEnabled());
        binding.switchLockscreenCamera.setChecked(preferences.isLockscreenCameraEnabled());
        binding.switchVolumeShutter.setChecked(preferences.isVolumeShutterEnabled());
        binding.switchGridOverlay.setChecked(preferences.isGridOverlayEnabled());
        binding.switchAppLock.setChecked(preferences.isAppLockEnabled());
    }

    private void setupListeners() {
        // Resolution Picker
        binding.rowResolution.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            String[] resolutions = {"480p", "720p", "1080p", "4k"};
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.pref_resolution)
                    .setItems(resolutions, (dialog, which) -> {
                        String selected = resolutions[which];
                        preferences.setResolution(selected);
                        binding.tvResolutionSummary.setText(selected.toUpperCase());
                    }).show();
        });

        // Frame Rate Picker
        binding.rowFrameRate.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            String[] fpsOptions = {"30 FPS", "60 FPS"};
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.pref_framerate)
                    .setItems(fpsOptions, (dialog, which) -> {
                        int fps = which == 1 ? 60 : 30;
                        preferences.setFrameRate(fps);
                        binding.tvFpsSummary.setText(fps + " FPS");
                    }).show();
        });

        // Audio Switch
        binding.switchAudio.setOnCheckedChangeListener((btn, checked) -> {
            HapticUtils.performClickFeedback(this);
            preferences.setAudioEnabled(checked);
        });

        // Location Switch
        binding.switchLocation.setOnCheckedChangeListener((btn, checked) -> {
            HapticUtils.performClickFeedback(this);
            preferences.setLocationEnabled(checked);
        });

        // Stabilization Switch
        binding.switchStabilization.setOnCheckedChangeListener((btn, checked) -> {
            HapticUtils.performClickFeedback(this);
            preferences.setStabilizationEnabled(checked);
        });

        // Lockscreen Camera Switch
        binding.switchLockscreenCamera.setOnCheckedChangeListener((btn, checked) -> {
            HapticUtils.performClickFeedback(this);
            preferences.setLockscreenCameraEnabled(checked);
        });

        // Volume Shutter Switch
        binding.switchVolumeShutter.setOnCheckedChangeListener((btn, checked) -> {
            HapticUtils.performClickFeedback(this);
            preferences.setVolumeShutterEnabled(checked);
        });

        // Grid Overlay Switch
        binding.switchGridOverlay.setOnCheckedChangeListener((btn, checked) -> {
            HapticUtils.performClickFeedback(this);
            preferences.setGridOverlayEnabled(checked);
        });

        // App Lock Switch
        binding.switchAppLock.setOnCheckedChangeListener((btn, checked) -> {
            HapticUtils.performClickFeedback(this);
            if (checked) {
                if (!preferences.hasPinConfigured()) {
                    launchPinSetup();
                } else {
                    preferences.setAppLockEnabled(true);
                }
            } else {
                preferences.setAppLockEnabled(false);
            }
        });

        // Change PIN
        binding.rowChangePin.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            launchPinSetup();
        });

        // Max Duration Picker
        binding.rowMaxDuration.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            String[] durations = {"No Limit", "5 Minutes", "10 Minutes", "30 Minutes", "60 Minutes"};
            int[] values = {0, 5, 10, 30, 60};
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.pref_max_duration)
                    .setItems(durations, (dialog, which) -> {
                        int selectedVal = values[which];
                        preferences.setMaxDurationMins(selectedVal);
                        binding.tvMaxDurationSummary.setText(durations[which]);
                    }).show();
        });

        // Auto Cleanup Picker
        binding.rowAutoCleanup.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            String[] cleanupOptions = {"Disabled", "7 Days", "14 Days", "30 Days", "60 Days"};
            int[] daysValues = {0, 7, 14, 30, 60};
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.pref_auto_cleanup)
                    .setItems(cleanupOptions, (dialog, which) -> {
                        int val = daysValues[which];
                        if (val == 0) {
                            preferences.setAutoCleanupEnabled(false);
                            binding.tvCleanupSummary.setText("Disabled");
                        } else {
                            preferences.setAutoCleanupEnabled(true);
                            preferences.setCleanupDays(val);
                            binding.tvCleanupSummary.setText("Older than " + val + " days");
                        }
                        ((SecurityRecorderApp) getApplication()).scheduleCleanupWorker();
                    }).show();
        });

        // Backup Settings
        binding.btnBackupSettings.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            String json = viewModel.backupSettings();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Security Recorder Settings", json);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Settings configuration copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        // Restore Settings
        binding.btnRestoreSettings.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null) {
                CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
                if (text != null && viewModel.restoreSettings(text.toString())) {
                    initUi();
                    Toast.makeText(this, "Settings restored from clipboard", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Invalid settings data on clipboard", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show();
            }
        });

        // Share App (Description & APK)
        binding.btnShareApp.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            FileUtils.shareApp(this);
        });
    }

    private void launchPinSetup() {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.putExtra(AuthActivity.EXTRA_MODE_SETUP, true);
        pinSetupLauncher.launch(intent);
    }
}
