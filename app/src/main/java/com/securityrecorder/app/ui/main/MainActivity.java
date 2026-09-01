package com.securityrecorder.app.ui.main;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;
import com.securityrecorder.app.R;
import com.securityrecorder.app.SecurityRecorderApp;
import com.securityrecorder.app.data.model.FilterType;
import com.securityrecorder.app.data.model.VideoItem;
import com.securityrecorder.app.data.preferences.AppPreferences;
import com.securityrecorder.app.databinding.ActivityMainBinding;
import com.securityrecorder.app.services.AudioRecordingService;
import com.securityrecorder.app.services.CameraRecordingService;
import com.securityrecorder.app.ui.auth.AuthActivity;
import com.securityrecorder.app.ui.common.AboutActivity;
import com.securityrecorder.app.ui.player.AudioPlayerBottomSheet;
import com.securityrecorder.app.ui.player.ImageViewerDialog;
import com.securityrecorder.app.ui.player.MetadataBottomSheetDialog;
import com.securityrecorder.app.ui.player.VideoPlayerActivity;
import com.securityrecorder.app.ui.recorder.CameraRecorderActivity;
import com.securityrecorder.app.utils.FileUtils;
import com.securityrecorder.app.utils.HapticUtils;
import com.securityrecorder.app.utils.LocationHelper;
import com.securityrecorder.app.utils.NotificationHelper;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Main Surveillance Activity:
 * - 4 Bottom Icon-only tabs: Home, Files, Vault, Settings
 * - Single Exclusive Active Recording Mode (Audio, Photo, Video)
 * - Geo-Tag & Camera device watermark stamping on image & metadata persistence
 * - Metadata Viewer from Item & Selection Bar
 * - Instant Favorite Star toggle and persistence
 * - 3-dots toolbar opening About Screen
 * Developed by: Amit Bharat · Rooys Soft Tech
 */
public class MainActivity extends AppCompatActivity implements VideoAdapter.OnVideoItemClickListener {

    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private VideoAdapter filesAdapter;
    private VideoAdapter vaultAdapter;
    private AppPreferences preferences;

    private boolean isAppUnlocked = false;
    private boolean isVaultUnlocked = false;
    private int currentTabId = R.id.nav_home;
    private ImageCapture imageCapture;
    private boolean isSmallPreviewVisible = false;
    private long lastBackPressTime = 0;
    private static final long BACK_PRESS_INTERVAL = 2000L;

    private final android.content.BroadcastReceiver screenOffReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                Boolean isVideoRec = CameraRecordingService.isRecordingLiveData.getValue();
                if (Boolean.TRUE.equals(isVideoRec)) {
                    stopRecordingService();
                }
                Boolean isAudioRec = AudioRecordingService.isAudioRecordingLiveData.getValue();
                if (Boolean.TRUE.equals(isAudioRec)) {
                    stopAudioRecordingService();
                }
            }
        }
    };

    private final ActivityResultLauncher<Intent> appAuthLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    isAppUnlocked = true;
                } else {
                    finish();
                }
            });

    private final ActivityResultLauncher<Intent> vaultAuthLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    isVaultUnlocked = true;
                    updateVaultUi();
                    Toast.makeText(this, "Vault Unlocked", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> pinSetupLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    binding.switchAppLock.setChecked(true);
                    Toast.makeText(this, "Security PIN configured successfully", Toast.LENGTH_SHORT).show();
                } else {
                    binding.switchAppLock.setChecked(preferences.isAppLockEnabled());
                }
            });

    private final ActivityResultLauncher<String[]> videoPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean cameraGranted = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
                if (cameraGranted) {
                    startRecordingService();
                } else {
                    Toast.makeText(this, R.string.permission_camera_rationale, Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String[]> audioPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean audioGranted = Boolean.TRUE.equals(result.get(Manifest.permission.RECORD_AUDIO));
                if (audioGranted) {
                    startAudioRecordingService();
                } else {
                    Toast.makeText(this, R.string.permission_audio_rationale, Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String[]> photoPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean cameraGranted = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
                if (cameraGranted) {
                    captureStealthPhoto();
                } else {
                    Toast.makeText(this, R.string.permission_camera_rationale, Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean fine = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                boolean coarse = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (fine || coarse) {
                    preferences.setLocationEnabled(true);
                    updateGeoTagButtonUi();
                    Toast.makeText(this, "GPS Geo-Tagging Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    preferences.setLocationEnabled(false);
                    updateGeoTagButtonUi();
                    Toast.makeText(this, "Location permission required for Geo-Tagging", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferences = new AppPreferences(this);
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Keep screen awake while app is in active use
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        NotificationHelper.createNotificationChannels(this);

        // Register screen lock / screen off receiver to auto-save files when physical power button is pressed
        IntentFilter screenFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenOffReceiver, screenFilter);

        checkAppSecurityLock();
        setupBottomNavigation();
        setupTopToolbar();
        setupHomeTab();
        setupFilesTab();
        setupVaultTab();
        setupSettingsTab();
        setupSelectionBar();
        observeViewModel();
        observeRecordingServices();
        handleIncomingIntent(getIntent());

        switchToTab(R.id.nav_home);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (NotificationHelper.ACTION_STOP_RECORDING.equals(action)) {
                stopRecordingService();
            } else if (NotificationHelper.ACTION_STOP_AUDIO_RECORDING.equals(action)) {
                stopAudioRecordingService();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.syncStorage();
        updateSettingsUiValues();
        updateGeoTagButtonUi();
        updateCameraLensButtonUi();
    }

    private void checkAppSecurityLock() {
        if (preferences.isAppLockEnabled() && !isAppUnlocked) {
            Intent intent = new Intent(this, AuthActivity.class);
            appAuthLauncher.launch(intent);
        }
    }

    // --- Bottom Navigation & Tabs ---

    private void setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            HapticUtils.performClickFeedback(this);
            int id = item.getItemId();
            switchToTab(id);
            return true;
        });
    }

    private void switchToTab(int tabId) {
        currentTabId = tabId;
        exitSelectionMode();

        binding.tabHome.setVisibility(tabId == R.id.nav_home ? View.VISIBLE : View.GONE);
        binding.tabFiles.setVisibility(tabId == R.id.nav_files ? View.VISIBLE : View.GONE);
        binding.tabVault.setVisibility(tabId == R.id.nav_vault ? View.VISIBLE : View.GONE);
        binding.tabSettings.setVisibility(tabId == R.id.nav_settings ? View.VISIBLE : View.GONE);

        if (tabId == R.id.nav_home) {
            binding.layoutSearchBarContainer.setVisibility(View.GONE);
            binding.btnToggleSearch.setVisibility(View.GONE);
            binding.btnToggleLayout.setVisibility(View.GONE);
        } else if (tabId == R.id.nav_files) {
            binding.layoutSearchBarContainer.setVisibility(View.VISIBLE);
            binding.scrollFilterChips.setVisibility(View.VISIBLE);
            binding.btnToggleSearch.setVisibility(View.VISIBLE);
            binding.btnToggleLayout.setVisibility(View.VISIBLE);
            updateToggleLayoutIcon(preferences.isGridLayout());
        } else if (tabId == R.id.nav_vault) {
            binding.layoutSearchBarContainer.setVisibility(isVaultUnlocked ? View.VISIBLE : View.GONE);
            binding.scrollFilterChips.setVisibility(View.GONE);
            binding.btnToggleSearch.setVisibility(isVaultUnlocked ? View.VISIBLE : View.GONE);
            binding.btnToggleLayout.setVisibility(isVaultUnlocked ? View.VISIBLE : View.GONE);
            updateVaultUi();
        } else if (tabId == R.id.nav_settings) {
            binding.layoutSearchBarContainer.setVisibility(View.GONE);
            binding.btnToggleSearch.setVisibility(View.GONE);
            binding.btnToggleLayout.setVisibility(View.GONE);
            updateSettingsUiValues();
        }
    }

    // --- Top Toolbar ---

    private void setupTopToolbar() {
        updateToggleLayoutIcon(preferences.isGridLayout());

        binding.btnToggleSearch.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            int curVis = binding.layoutSearchBarContainer.getVisibility();
            binding.layoutSearchBarContainer.setVisibility(curVis == View.VISIBLE ? View.GONE : View.VISIBLE);
            if (binding.layoutSearchBarContainer.getVisibility() == View.VISIBLE) {
                binding.etSearch.requestFocus();
            }
        });

        binding.btnToggleLayout.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            boolean newGridState = !preferences.isGridLayout();
            preferences.setGridLayout(newGridState);
            updateToggleLayoutIcon(newGridState);

            applyLayoutManagers(newGridState);
            filesAdapter.setGridLayout(newGridState);
            vaultAdapter.setGridLayout(newGridState);
        });

        binding.btnMenuOverflow.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, binding.btnMenuOverflow);
            popup.getMenuInflater().inflate(R.menu.menu_toolbar_overflow, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_settings) {
                    startActivity(new Intent(this, com.securityrecorder.app.ui.settings.SettingsActivity.class));
                    return true;
                } else if (id == R.id.action_about) {
                    startActivity(new Intent(this, com.securityrecorder.app.ui.common.AboutActivity.class));
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void updateToggleLayoutIcon(boolean isGrid) {
        binding.btnToggleLayout.setImageResource(isGrid ? R.drawable.ic_list_view : R.drawable.ic_grid_view);
    }

    private void applyLayoutManagers(boolean isGrid) {
        if (isGrid) {
            binding.rvVideos.setLayoutManager(new GridLayoutManager(this, 2));
            binding.rvVaultVideos.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            binding.rvVideos.setLayoutManager(new LinearLayoutManager(this));
            binding.rvVaultVideos.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    // --- Tab 1: Home Dashboard (Mutually Exclusive Recording Actions) ---

    private void setupHomeTab() {
        updateGeoTagButtonUi();
        updateCameraLensButtonUi();

        // GeoTag Toggle Button
        binding.btnToggleGeoTag.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            boolean willEnable = !preferences.isLocationEnabled();
            if (willEnable) {
                if (LocationHelper.hasLocationPermission(this)) {
                    preferences.setLocationEnabled(true);
                    updateGeoTagButtonUi();
                    Toast.makeText(this, "GPS Geo-Tagging Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    locationPermissionLauncher.launch(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    });
                }
            } else {
                preferences.setLocationEnabled(false);
                updateGeoTagButtonUi();
                Toast.makeText(this, "GPS Geo-Tagging Disabled", Toast.LENGTH_SHORT).show();
            }
        });

        // Front / Rear Camera Lens Toggle Button
        binding.btnToggleCameraLens.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            boolean willBeFront = !preferences.isFrontCamera();
            preferences.setFrontCamera(willBeFront);
            updateCameraLensButtonUi();
            Toast.makeText(this, willBeFront ? "Switched to Front Camera" : "Switched to Rear Camera", Toast.LENGTH_SHORT).show();
        });

        // Video Preview Toggle Button (appears when video is recording)
        binding.btnToggleVideoPreview.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            isSmallPreviewVisible = !isSmallPreviewVisible;
            binding.cardSmallCameraPreview.setVisibility(isSmallPreviewVisible ? View.VISIBLE : View.GONE);
            binding.btnToggleVideoPreview.setText(isSmallPreviewVisible ? "Hide Preview" : "Show Preview");
        });

        // Audio Button
        binding.fabHomeAudio.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            Boolean isAudioRec = AudioRecordingService.isAudioRecordingLiveData.getValue();
            Boolean isVideoRec = CameraRecordingService.isRecordingLiveData.getValue();

            if (Boolean.TRUE.equals(isVideoRec)) {
                // If video is recording, stop it and start audio
                stopRecordingService();
                binding.getRoot().postDelayed(this::checkPermissionsAndStartAudioRecording, 300);
            } else if (Boolean.TRUE.equals(isAudioRec)) {
                // Stop audio recording
                stopAudioRecordingService();
            } else {
                // Start audio recording
                checkPermissionsAndStartAudioRecording();
            }
        });

        // Photo Button (Stealth capture)
        binding.fabHomePhoto.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            Boolean isAudioRec = AudioRecordingService.isAudioRecordingLiveData.getValue();
            Boolean isVideoRec = CameraRecordingService.isRecordingLiveData.getValue();

            if (Boolean.TRUE.equals(isVideoRec)) {
                stopRecordingService();
            }
            if (Boolean.TRUE.equals(isAudioRec)) {
                stopAudioRecordingService();
            }

            binding.getRoot().postDelayed(this::checkPermissionsAndCapturePhoto, 200);
        });

        // Video Button (Launches Full Live Camera Recorder)
        binding.fabHomeVideo.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            Boolean isVideoRec = CameraRecordingService.isRecordingLiveData.getValue();
            if (Boolean.TRUE.equals(isVideoRec)) {
                stopRecordingService();
            } else {
                startActivity(new Intent(this, CameraRecorderActivity.class));
            }
        });
    }

    private void updateGeoTagButtonUi() {
        boolean isGps = preferences.isLocationEnabled();
        binding.btnToggleGeoTag.setText(isGps ? "Geo-Tag: ON" : "Geo-Tag: OFF");
        binding.btnToggleGeoTag.setTextColor(isGps ? 0xFF38BDF8 : 0xFFA1A1AA);
    }

    private void updateCameraLensButtonUi() {
        boolean isFront = preferences.isFrontCamera();
        binding.btnToggleCameraLens.setText(isFront ? "Cam: Front" : "Cam: Rear");
        binding.btnToggleCameraLens.setTextColor(isFront ? 0xFFF59E0B : 0xFF38BDF8);
    }

    // --- Tab 2: Files Screen ---

    private void setupFilesTab() {
        boolean isGrid = preferences.isGridLayout();
        filesAdapter = new VideoAdapter(isGrid, this);
        binding.rvVideos.setAdapter(filesAdapter);
        applyLayoutManagers(isGrid);

        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            HapticUtils.performClickFeedback(this);
            viewModel.syncStorage();
        });

        setupSearchAndFilters();
    }

    private void setupSearchAndFilters() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s != null ? s.toString() : "";
                binding.btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                if (currentTabId == R.id.nav_vault) {
                    viewModel.setVaultSearchQuery(query);
                } else {
                    viewModel.setSearchQuery(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnClearSearch.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            binding.etSearch.setText("");
        });

        binding.chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                binding.chipAll.setChecked(true);
                return;
            }
            HapticUtils.performClickFeedback(this);
            int id = checkedIds.get(0);
            if (id == R.id.chipToday) {
                viewModel.setFilter(FilterType.TODAY);
            } else if (id == R.id.chipThisWeek) {
                viewModel.setFilter(FilterType.THIS_WEEK);
            } else if (id == R.id.chipThisMonth) {
                viewModel.setFilter(FilterType.THIS_MONTH);
            } else if (id == R.id.chipFavorites) {
                viewModel.setFilter(FilterType.FAVORITES);
            } else {
                viewModel.setFilter(FilterType.ALL);
            }
        });
    }

    // --- Tab 3: Vault Screen ---

    private void setupVaultTab() {
        boolean isGrid = preferences.isGridLayout();
        vaultAdapter = new VideoAdapter(isGrid, new VideoAdapter.OnVideoItemClickListener() {
            @Override
            public void onVideoClick(VideoItem video) {
                MainActivity.this.onVideoClick(video);
            }

            @Override
            public void onFavoriteToggle(VideoItem video) {
                MainActivity.this.onFavoriteToggle(video);
            }

            @Override
            public void onVideoLongClick(VideoItem video) {
                MainActivity.this.onVideoLongClick(video);
            }

            @Override
            public void onSelectionChanged(VideoItem video, boolean isSelected) {
                MainActivity.this.onSelectionChanged(video, isSelected);
            }

            @Override
            public void onInfoClick(VideoItem video) {
                MainActivity.this.onInfoClick(video);
            }
        });
        binding.rvVaultVideos.setAdapter(vaultAdapter);

        binding.btnUnlockVault.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            Intent intent = new Intent(this, AuthActivity.class);
            vaultAuthLauncher.launch(intent);
        });

        binding.btnLockVaultNow.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            isVaultUnlocked = false;
            exitSelectionMode();
            updateVaultUi();
            Toast.makeText(this, "Vault Locked", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateVaultUi() {
        if (isVaultUnlocked) {
            binding.layoutVaultLocked.setVisibility(View.GONE);
            binding.layoutVaultUnlocked.setVisibility(View.VISIBLE);
            binding.layoutSearchBarContainer.setVisibility(currentTabId == R.id.nav_vault ? View.VISIBLE : View.GONE);
            binding.btnToggleSearch.setVisibility(currentTabId == R.id.nav_vault ? View.VISIBLE : View.GONE);
            binding.btnToggleLayout.setVisibility(currentTabId == R.id.nav_vault ? View.VISIBLE : View.GONE);
        } else {
            binding.layoutVaultLocked.setVisibility(View.VISIBLE);
            binding.layoutVaultUnlocked.setVisibility(View.GONE);
            binding.layoutSearchBarContainer.setVisibility(View.GONE);
            binding.btnToggleSearch.setVisibility(View.GONE);
            binding.btnToggleLayout.setVisibility(View.GONE);
        }
    }

    // --- Tab 4: Settings Screen ---

    private void setupSettingsTab() {
        updateSettingsUiValues();

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
            if (checked) {
                if (LocationHelper.hasLocationPermission(this)) {
                    preferences.setLocationEnabled(true);
                    updateGeoTagButtonUi();
                } else {
                    locationPermissionLauncher.launch(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    });
                }
            } else {
                preferences.setLocationEnabled(false);
                updateGeoTagButtonUi();
            }
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

        // Restore Settings
        binding.btnRestoreSettings.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null) {
                CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
                if (text != null && preferences.restoreSettingsFromJson(text.toString())) {
                    updateSettingsUiValues();
                    Toast.makeText(this, "Settings restored from clipboard", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Invalid settings data on clipboard", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show();
            }
        });

        // About & Information
        binding.rowAboutApp.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            startActivity(new Intent(this, com.securityrecorder.app.ui.common.AboutActivity.class));
        });
    }

    private void updateSettingsUiValues() {
        binding.tvResolutionSummary.setText(preferences.getResolution().toUpperCase());
        binding.tvFpsSummary.setText(preferences.getFrameRate() + " FPS");

        int maxMins = preferences.getMaxDurationMins();
        binding.tvMaxDurationSummary.setText(maxMins > 0 ? maxMins + " Minutes" : "No Limit");

        boolean autoClean = preferences.isAutoCleanupEnabled();
        binding.tvCleanupSummary.setText(autoClean ? "Older than " + preferences.getCleanupDays() + " days" : "Disabled");

        binding.switchAudio.setChecked(preferences.isAudioEnabled());
        binding.switchLocation.setChecked(preferences.isLocationEnabled());
        binding.switchStabilization.setChecked(preferences.isStabilizationEnabled());
        binding.switchLockscreenCamera.setChecked(preferences.isLockscreenCameraEnabled());
        binding.switchVolumeShutter.setChecked(preferences.isVolumeShutterEnabled());
        binding.switchGridOverlay.setChecked(preferences.isGridOverlayEnabled());
        binding.switchAppLock.setChecked(preferences.isAppLockEnabled());
    }

    private void launchPinSetup() {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.putExtra(AuthActivity.EXTRA_MODE_SETUP, true);
        pinSetupLauncher.launch(intent);
    }

    // --- Floating Multi-Selection Bar with Metadata Info Action ---

    private void setupSelectionBar() {
        // 1. Status / Count toggle (Select all / Deselect all)
        binding.btnSelectionStatus.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            VideoAdapter currentAdapter = getCurrentActiveAdapter();
            int selectedCount = currentAdapter.getSelectedCount();
            int totalCount = currentAdapter.getItemCount();
            boolean selectAll = selectedCount < totalCount;
            currentAdapter.selectAll(selectAll);
            updateSelectionBar();
        });

        // 2. Info / Metadata
        binding.btnSelectionInfo.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            VideoAdapter currentAdapter = getCurrentActiveAdapter();
            List<VideoItem> items = currentAdapter.getSelectedItems();
            if (!items.isEmpty()) {
                onInfoClick(items.get(0));
            }
        });

        // 3. Share
        binding.btnSelectionShare.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            VideoAdapter currentAdapter = getCurrentActiveAdapter();
            List<VideoItem> items = currentAdapter.getSelectedItems();
            if (items.isEmpty()) return;

            List<String> paths = new ArrayList<>();
            for (VideoItem item : items) {
                paths.add(item.getFilePath());
            }
            FileUtils.shareMultipleFiles(this, paths);
        });

        // 4. Like / Heart
        binding.btnSelectionLike.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            VideoAdapter currentAdapter = getCurrentActiveAdapter();
            List<VideoItem> items = currentAdapter.getSelectedItems();
            if (items.isEmpty()) return;

            viewModel.toggleFavoriteMultiple(items, () -> {
                Toast.makeText(this, "Favorites updated", Toast.LENGTH_SHORT).show();
                exitSelectionMode();
            });
        });

        // 5. Vault (Move to Vault or Restore from Vault)
        binding.btnSelectionVault.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            VideoAdapter currentAdapter = getCurrentActiveAdapter();
            List<VideoItem> items = currentAdapter.getSelectedItems();
            if (items.isEmpty()) return;

            if (currentTabId == R.id.nav_vault) {
                viewModel.restoreFromVault(items, () -> {
                    Toast.makeText(this, items.size() + " item(s) unvaulted to Files", Toast.LENGTH_SHORT).show();
                    exitSelectionMode();
                });
            } else {
                viewModel.moveToVault(items, () -> {
                    Toast.makeText(this, items.size() + " item(s) moved to Secure Vault", Toast.LENGTH_SHORT).show();
                    exitSelectionMode();
                });
            }
        });

        // 6. Delete
        binding.btnSelectionDelete.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            promptDeleteSelectedVideos();
        });

        // 7. Close
        binding.btnSelectionClose.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            exitSelectionMode();
        });
    }

    private VideoAdapter getCurrentActiveAdapter() {
        return (currentTabId == R.id.nav_vault) ? vaultAdapter : filesAdapter;
    }

    private void enterSelectionMode() {
        VideoAdapter currentAdapter = getCurrentActiveAdapter();
        currentAdapter.setSelectionMode(true);
        binding.layoutSelectionBar.setVisibility(View.VISIBLE);
        updateSelectionBar();
    }

    private void exitSelectionMode() {
        if (filesAdapter != null) filesAdapter.setSelectionMode(false);
        if (vaultAdapter != null) vaultAdapter.setSelectionMode(false);
        binding.layoutSelectionBar.setVisibility(View.GONE);
    }

    private void updateSelectionBar() {
        VideoAdapter currentAdapter = getCurrentActiveAdapter();
        int selectedCount = currentAdapter.getSelectedCount();

        binding.tvSelectedCount.setText(String.valueOf(selectedCount));
        binding.btnSelectionVault.setImageResource(currentTabId == R.id.nav_vault ? R.drawable.ic_vault_unlocked : R.drawable.ic_vault);

        boolean hasSelection = selectedCount > 0;
        binding.btnSelectionInfo.setEnabled(hasSelection);
        binding.btnSelectionInfo.setAlpha(hasSelection ? 1.0f : 0.4f);
        binding.btnSelectionShare.setEnabled(hasSelection);
        binding.btnSelectionShare.setAlpha(hasSelection ? 1.0f : 0.4f);
        binding.btnSelectionLike.setEnabled(hasSelection);
        binding.btnSelectionLike.setAlpha(hasSelection ? 1.0f : 0.4f);
        binding.btnSelectionVault.setEnabled(hasSelection);
        binding.btnSelectionVault.setAlpha(hasSelection ? 1.0f : 0.4f);
        binding.btnSelectionDelete.setEnabled(hasSelection);
        binding.btnSelectionDelete.setAlpha(hasSelection ? 1.0f : 0.4f);
    }

    private void promptDeleteSelectedVideos() {
        VideoAdapter currentAdapter = getCurrentActiveAdapter();
        List<VideoItem> selectedItems = currentAdapter.getSelectedItems();
        if (selectedItems.isEmpty()) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Items")
                .setMessage("Are you sure you want to permanently delete " + selectedItems.size() + " selected item(s)?")
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    viewModel.deleteVideos(selectedItems, () -> {
                        Toast.makeText(MainActivity.this, selectedItems.size() + " item(s) deleted", Toast.LENGTH_SHORT).show();
                        exitSelectionMode();
                        viewModel.syncStorage();
                    });
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    // --- Media Item Clicks, Metadata & Interactions ---

    @Override
    public void onInfoClick(VideoItem video) {
        if (video != null) {
            HapticUtils.performClickFeedback(this);
            MetadataBottomSheetDialog.newInstance(video).show(getSupportFragmentManager(), "MetadataDialog");
        }
    }

    @Override
    public void onVideoClick(VideoItem item) {
        VideoAdapter currentAdapter = getCurrentActiveAdapter();
        if (currentAdapter.isSelectionMode()) {
            boolean newState = !item.isSelected();
            item.setSelected(newState);
            updateSelectionBar();
            currentAdapter.notifyItemRangeChanged(0, currentAdapter.getItemCount());
        } else {
            HapticUtils.performClickFeedback(this);
            if (item.isAudio()) {
                AudioPlayerBottomSheet.newInstance(item, new AudioPlayerBottomSheet.OnAudioActionListener() {
                    @Override
                    public void onVaultToggled(VideoItem audioItem) {
                        List<VideoItem> single = new ArrayList<>();
                        single.add(audioItem);
                        if (audioItem.isVault()) {
                            viewModel.restoreFromVault(single, () -> Toast.makeText(MainActivity.this, "Audio unvaulted", Toast.LENGTH_SHORT).show());
                        } else {
                            viewModel.moveToVault(single, () -> Toast.makeText(MainActivity.this, "Audio moved to Vault", Toast.LENGTH_SHORT).show());
                        }
                    }

                    @Override
                    public void onDeleted(VideoItem audioItem) {
                        List<VideoItem> single = new ArrayList<>();
                        single.add(audioItem);
                        viewModel.deleteVideos(single, () -> Toast.makeText(MainActivity.this, "Audio deleted", Toast.LENGTH_SHORT).show());
                    }
                }).show(getSupportFragmentManager(), "AudioPlayer");
            } else if (item.isImage()) {
                ImageViewerDialog.show(this, item, new ImageViewerDialog.OnImageActionListener() {
                    @Override
                    public void onFavoriteToggled(VideoItem photoItem) {
                        viewModel.toggleFavorite(photoItem);
                    }

                    @Override
                    public void onVaultToggled(VideoItem photoItem) {
                        List<VideoItem> single = new ArrayList<>();
                        single.add(photoItem);
                        if (photoItem.isVault()) {
                            viewModel.restoreFromVault(single, () -> Toast.makeText(MainActivity.this, "Photo unvaulted", Toast.LENGTH_SHORT).show());
                        } else {
                            viewModel.moveToVault(single, () -> Toast.makeText(MainActivity.this, "Photo moved to Vault", Toast.LENGTH_SHORT).show());
                        }
                    }

                    @Override
                    public void onDeleted(VideoItem photoItem) {
                        List<VideoItem> single = new ArrayList<>();
                        single.add(photoItem);
                        viewModel.deleteVideos(single, () -> Toast.makeText(MainActivity.this, "Photo deleted", Toast.LENGTH_SHORT).show());
                    }
                });
            } else {
                Intent intent = new Intent(this, VideoPlayerActivity.class);
                intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_ITEM, item);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onFavoriteToggle(VideoItem item) {
        HapticUtils.performClickFeedback(this);
        viewModel.toggleFavorite(item);
    }

    @Override
    public void onVideoLongClick(VideoItem item) {
        HapticUtils.performClickFeedback(this);
        VideoAdapter currentAdapter = getCurrentActiveAdapter();
        if (!currentAdapter.isSelectionMode()) {
            enterSelectionMode();
            item.setSelected(true);
            updateSelectionBar();
            currentAdapter.notifyItemRangeChanged(0, currentAdapter.getItemCount());
        }
    }

    @Override
    public void onSelectionChanged(VideoItem item, boolean isSelected) {
        updateSelectionBar();
    }

    // --- ViewModel & Recording Observers ---

    private void observeViewModel() {
        viewModel.getVideos().observe(this, videos -> {
            filesAdapter.submitList(videos);
            boolean isEmpty = videos == null || videos.isEmpty();
            binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.rvVideos.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });

        viewModel.getVaultVideos().observe(this, vaultItems -> {
            vaultAdapter.submitList(vaultItems);
            boolean isEmpty = vaultItems == null || vaultItems.isEmpty();
            binding.layoutVaultEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.rvVaultVideos.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            int count = vaultItems != null ? vaultItems.size() : 0;
            binding.tvVaultHeaderCount.setText("🔒 Vault Active (" + count + " item" + (count == 1 ? "" : "s") + ")");
        });

        viewModel.getIsRefreshing().observe(this, isRefreshing -> {
            binding.swipeRefreshLayout.setRefreshing(isRefreshing);
        });
    }

    private void observeRecordingServices() {
        CameraRecordingService.isRecordingLiveData.observe(this, isRecording -> {
            if (Boolean.TRUE.equals(isRecording)) {
                binding.fabHomeVideo.setImageResource(R.drawable.ic_stop_record);
                binding.fabHomeVideo.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.md_theme_dark_surface));
                binding.layoutRecordingDot.setVisibility(View.VISIBLE);
                binding.btnToggleVideoPreview.setVisibility(View.VISIBLE);
                binding.tvHomeStatusTitle.setText("Recording Video");
                binding.tvHomeStatusDesc.setText("Surveillance video capture in progress.");
                startHeaderPulsingAnimation();
            } else {
                binding.fabHomeVideo.setImageResource(R.drawable.ic_camera_record);
                binding.fabHomeVideo.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.recording_red));
                binding.btnToggleVideoPreview.setVisibility(View.GONE);
                binding.cardSmallCameraPreview.setVisibility(View.GONE);
                isSmallPreviewVisible = false;

                Boolean isAudioRec = AudioRecordingService.isAudioRecordingLiveData.getValue();
                if (!Boolean.TRUE.equals(isAudioRec)) {
                    binding.layoutRecordingDot.setVisibility(View.GONE);
                    binding.tvHomeStatusTitle.setText("Surveillance Ready");
                    binding.tvHomeStatusDesc.setText("Select an action below to record audio, take photo, or record video.");
                    stopHeaderPulsingAnimation();
                }
                viewModel.syncStorage();
            }
        });

        CameraRecordingService.durationLiveData.observe(this, duration -> {
            Boolean isVideoRec = CameraRecordingService.isRecordingLiveData.getValue();
            if (Boolean.TRUE.equals(isVideoRec)) {
                binding.tvCornerRecordingTime.setText(duration);
            }
        });

        AudioRecordingService.isAudioRecordingLiveData.observe(this, isAudioRecording -> {
            if (Boolean.TRUE.equals(isAudioRecording)) {
                binding.fabHomeAudio.setImageResource(R.drawable.ic_stop_record);
                binding.fabHomeAudio.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.md_theme_dark_surface));
                binding.layoutRecordingDot.setVisibility(View.VISIBLE);
                binding.tvHomeStatusTitle.setText("Recording Audio");
                binding.tvHomeStatusDesc.setText("Surveillance audio capture in progress.");
                startHeaderPulsingAnimation();
            } else {
                binding.fabHomeAudio.setImageResource(R.drawable.ic_mic);
                binding.fabHomeAudio.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.md_theme_dark_primaryContainer));

                Boolean isVideoRec = CameraRecordingService.isRecordingLiveData.getValue();
                if (!Boolean.TRUE.equals(isVideoRec)) {
                    binding.layoutRecordingDot.setVisibility(View.GONE);
                    binding.tvHomeStatusTitle.setText("Surveillance Ready");
                    binding.tvHomeStatusDesc.setText("Select an action below to record audio, take photo, or record video.");
                    stopHeaderPulsingAnimation();
                }
                viewModel.syncStorage();
            }
        });

        AudioRecordingService.audioDurationLiveData.observe(this, duration -> {
            Boolean isAudioRec = AudioRecordingService.isAudioRecordingLiveData.getValue();
            if (Boolean.TRUE.equals(isAudioRec)) {
                binding.tvCornerRecordingTime.setText(duration);
            }
        });
    }

    // --- Photo Capture Pipeline with Geotag Watermark & EXIF ---

    private void checkPermissionsAndCapturePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            captureStealthPhoto();
        } else {
            photoPermissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
        }
    }

    private void captureStealthPhoto() {
        HapticUtils.performRecordingStartFeedback(this);
        binding.layoutRecordingDot.setVisibility(View.VISIBLE);
        binding.tvCornerRecordingTime.setText("CAPTURE");

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                ImageCapture capture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = preferences.isFrontCamera()
                        ? CameraSelector.DEFAULT_FRONT_CAMERA
                        : CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.bindToLifecycle(this, cameraSelector, capture);

                File recDir = FileUtils.getRecordingDirectory(this);
                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
                File photoFile = new File(recDir, "PHOTO_" + timestamp + ".jpg");

                ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

                capture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        HapticUtils.performRecordingStopFeedback(MainActivity.this);

                        String locationStr = "Not available";
                        if (preferences.isLocationEnabled()) {
                            Location loc = LocationHelper.getLastKnownLocation(MainActivity.this);
                            locationStr = LocationHelper.stampGeoTagOnImage(MainActivity.this, photoFile, loc);
                        }

                        binding.layoutRecordingDot.postDelayed(() -> {
                            Boolean isVideoRec = CameraRecordingService.isRecordingLiveData.getValue();
                            Boolean isAudioRec = AudioRecordingService.isAudioRecordingLiveData.getValue();
                            if (!Boolean.TRUE.equals(isVideoRec) && !Boolean.TRUE.equals(isAudioRec)) {
                                binding.layoutRecordingDot.setVisibility(View.GONE);
                            }
                        }, 1000);

                        final String finalLocation = locationStr;
                        viewModel.insertPhoto(photoFile, finalLocation, () -> {
                            Toast.makeText(MainActivity.this, preferences.isLocationEnabled() ? "Geotagged photo captured & saved" : "Photo captured & saved", Toast.LENGTH_SHORT).show();
                            viewModel.syncStorage();
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        binding.layoutRecordingDot.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Photo capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                binding.layoutRecordingDot.setVisibility(View.GONE);
                Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // --- Service Starters & Permissions ---

    private void checkPermissionsAndStartAudioRecording() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.RECORD_AUDIO);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        boolean allGranted = true;
        for (String perm : perms) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            startAudioRecordingService();
        } else {
            audioPermissionLauncher.launch(perms.toArray(new String[0]));
        }
    }

    private void startAudioRecordingService() {
        HapticUtils.performRecordingStartFeedback(this);
        Intent serviceIntent = new Intent(this, AudioRecordingService.class);
        serviceIntent.setAction(AudioRecordingService.ACTION_START_AUDIO);
        ContextCompat.startForegroundService(this, serviceIntent);
        Toast.makeText(this, "Audio recording started", Toast.LENGTH_SHORT).show();
    }

    private void stopAudioRecordingService() {
        HapticUtils.performRecordingStopFeedback(this);
        Intent serviceIntent = new Intent(this, AudioRecordingService.class);
        serviceIntent.setAction(AudioRecordingService.ACTION_STOP_AUDIO);
        startService(serviceIntent);
        Toast.makeText(this, "Audio recording saved", Toast.LENGTH_SHORT).show();
    }

    private void checkPermissionsAndStartRecording() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.CAMERA);
        perms.add(Manifest.permission.RECORD_AUDIO);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        boolean allGranted = true;
        for (String perm : perms) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            startRecordingService();
        } else {
            videoPermissionLauncher.launch(perms.toArray(new String[0]));
        }
    }

    private void startRecordingService() {
        HapticUtils.performRecordingStartFeedback(this);
        CameraRecordingService.previewViewRef = isSmallPreviewVisible ? binding.smallPreviewView : null;
        Intent serviceIntent = new Intent(this, CameraRecordingService.class);
        serviceIntent.setAction(CameraRecordingService.ACTION_START);
        ContextCompat.startForegroundService(this, serviceIntent);
        Toast.makeText(this, "Video recording started", Toast.LENGTH_SHORT).show();
    }

    private void stopRecordingService() {
        HapticUtils.performRecordingStopFeedback(this);
        Intent serviceIntent = new Intent(this, CameraRecordingService.class);
        serviceIntent.setAction(CameraRecordingService.ACTION_STOP);
        startService(serviceIntent);
        Toast.makeText(this, "Video recording saved", Toast.LENGTH_SHORT).show();
    }

    private void startHeaderPulsingAnimation() {
        AlphaAnimation pulse = new AlphaAnimation(1.0f, 0.2f);
        pulse.setDuration(500);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setRepeatCount(Animation.INFINITE);
        binding.viewHeaderRedDot.startAnimation(pulse);
    }

    private void stopHeaderPulsingAnimation() {
        binding.viewHeaderRedDot.clearAnimation();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (preferences.isVolumeShutterEnabled()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                Boolean isRecording = CameraRecordingService.isRecordingLiveData.getValue();
                if (Boolean.TRUE.equals(isRecording)) {
                    stopRecordingService();
                } else {
                    checkPermissionsAndStartRecording();
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        VideoAdapter currentAdapter = getCurrentActiveAdapter();
        if (currentAdapter != null && currentAdapter.isSelectionMode()) {
            exitSelectionMode();
            return;
        }
        if (currentTabId != R.id.nav_home) {
            binding.bottomNavigationView.setSelectedItemId(R.id.nav_home);
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBackPressTime < BACK_PRESS_INTERVAL) {
            finishAffinity();
        } else {
            lastBackPressTime = currentTime;
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(screenOffReceiver);
        } catch (Exception ignored) {}
    }
}
