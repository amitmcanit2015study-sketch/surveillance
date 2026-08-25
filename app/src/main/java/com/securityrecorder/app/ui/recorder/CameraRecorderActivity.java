package com.securityrecorder.app.ui.recorder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoRecordEvent;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.securityrecorder.app.R;
import com.securityrecorder.app.data.preferences.AppPreferences;
import com.securityrecorder.app.databinding.ActivityCameraRecorderBinding;
import com.securityrecorder.app.ui.main.MainActivity;
import com.securityrecorder.app.utils.DateTimeUtils;
import com.securityrecorder.app.utils.FileUtils;
import com.securityrecorder.app.utils.HapticUtils;
import com.securityrecorder.app.utils.LocationHelper;
import java.io.File;
import java.util.Locale;

/**
 * CameraX Recording Activity with Discrete Text-Only Mode by default.
 * Shows text status ("RECORDING" / "NOT RECORDING") without exposing the live camera feed on-screen.
 * Directly appears on top of the lock screen when the device is locked.
 */
public class CameraRecorderActivity extends AppCompatActivity {

    private ActivityCameraRecorderBinding binding;
    private CameraRecorderViewModel viewModel;
    private CameraHelper cameraHelper;
    private AppPreferences preferences;
    private KeyguardManager keyguardManager;

    private Recording activeRecording;
    private File currentOutputFile;
    private boolean isFrontCamera = false;
    private String currentResolution = "1080p";
    private boolean isAudioEnabled = true;
    private boolean isPreviewVisible = false; // Default: False (Discrete Text Mode)

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private float currentZoomLevel = 0.0f;
    private final Handler zoomBadgeHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean cameraGranted = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
                if (cameraGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, R.string.permission_camera_rationale, Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        configureLockscreenAndDisplayFlags();

        binding = ActivityCameraRecorderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferences = new AppPreferences(this);
        keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        viewModel = new ViewModelProvider(this).get(CameraRecorderViewModel.class);
        cameraHelper = new CameraHelper();

        isFrontCamera = preferences.isFrontCamera();
        currentResolution = preferences.getResolution();
        isAudioEnabled = preferences.isAudioEnabled();

        initUi();
        setupGestureDetectors();
        observeViewModel();
        checkPermissionsAndStartCamera();
    }

    /**
     * Configure Android Window & Keyguard flags so camera activity displays directly when the device is locked.
     */
    private void configureLockscreenAndDisplayFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );
    }

    private void initUi() {
        binding.btnQualityToggle.setText(currentResolution.toUpperCase());
        updateAudioButtonUi();

        // Viewfinder preview is visible by default
        updatePreviewVisibility(true);

        binding.btnBack.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            handleExitAction();
        });

        binding.btnTogglePreview.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            updatePreviewVisibility(!isPreviewVisible);
        });

        binding.btnTorch.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            boolean newState = !cameraHelper.isTorchOn();
            cameraHelper.toggleTorch(newState);
            binding.btnTorch.setImageResource(newState ? R.drawable.ic_flash_on : R.drawable.ic_flash_off);
        });

        binding.btnSwitchCamera.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            if (activeRecording != null) {
                Toast.makeText(this, "Cannot flip camera during recording", Toast.LENGTH_SHORT).show();
                return;
            }
            isFrontCamera = !isFrontCamera;
            preferences.setFrontCamera(isFrontCamera);
            startCamera();
        });

        binding.btnQualityToggle.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            if (activeRecording != null) {
                Toast.makeText(this, "Cannot change quality during recording", Toast.LENGTH_SHORT).show();
                return;
            }
            cycleResolution();
        });

        binding.btnToggleAudio.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            isAudioEnabled = !isAudioEnabled;
            preferences.setAudioEnabled(isAudioEnabled);
            updateAudioButtonUi();
        });

        binding.btnToggleGrid.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            boolean newGridState = binding.gridOverlay.getVisibility() != View.VISIBLE;
            binding.gridOverlay.setVisibility(newGridState ? View.VISIBLE : View.GONE);
            preferences.setGridOverlayEnabled(newGridState);
        });

        binding.fabShutter.setOnClickListener(v -> {
            if (activeRecording != null) {
                HapticUtils.performRecordingStopFeedback(this);
                stopRecording();
            } else {
                HapticUtils.performRecordingStartFeedback(this);
                startRecording();
            }
        });

        binding.btnGalleryQuickView.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            handleGalleryNavigation();
        });
    }

    private void updatePreviewVisibility(boolean showPreview) {
        isPreviewVisible = showPreview;
        binding.layoutDiscreteCover.setVisibility(showPreview ? View.GONE : View.VISIBLE);
        binding.btnTogglePreview.setImageResource(showPreview ? R.drawable.ic_visibility : R.drawable.ic_visibility_off);
        binding.gridOverlay.setVisibility((showPreview && preferences.isGridOverlayEnabled()) ? View.VISIBLE : View.GONE);
    }

    private void handleGalleryNavigation() {
        if (activeRecording != null) {
            stopRecording();
        }

        if (keyguardManager != null && keyguardManager.isKeyguardLocked()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                keyguardManager.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
                    @Override
                    public void onDismissSucceeded() {
                        super.onDismissSucceeded();
                        navigateToGallery();
                    }

                    @Override
                    public void onDismissCancelled() {
                        super.onDismissCancelled();
                        Toast.makeText(CameraRecorderActivity.this, "Authentication required to view recordings", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                navigateToGallery();
            }
        } else {
            navigateToGallery();
        }
    }

    private void navigateToGallery() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void handleExitAction() {
        if (activeRecording != null) {
            stopRecording();
        }
        finish();
    }

    private void setupGestureDetectors() {
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scale = detector.getScaleFactor();
                if (scale > 1.0f) {
                    currentZoomLevel = Math.min(1.0f, currentZoomLevel + (scale - 1.0f) * 0.5f);
                } else {
                    currentZoomLevel = Math.max(0.0f, currentZoomLevel - (1.0f - scale) * 0.5f);
                }
                cameraHelper.setLinearZoom(currentZoomLevel);
                updateZoomBadge();
                return true;
            }
        });

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (isPreviewVisible) {
                    showFocusRing(e.getX(), e.getY());
                    cameraHelper.triggerFocusAndMetering(
                            e.getX(),
                            e.getY(),
                            binding.previewView.getWidth(),
                            binding.previewView.getHeight()
                    );
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                currentZoomLevel = (currentZoomLevel > 0.4f) ? 0.0f : 0.5f;
                cameraHelper.setLinearZoom(currentZoomLevel);
                updateZoomBadge();
                return true;
            }
        });

        binding.previewView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);
            return true;
        });

        binding.layoutDiscreteCover.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });
    }

    private void updateZoomBadge() {
        float ratio = 1.0f + (currentZoomLevel * 4.0f);
        binding.tvZoomBadge.setText(String.format(Locale.US, "%.1fx", ratio));
        binding.tvZoomBadge.setVisibility(View.VISIBLE);

        zoomBadgeHandler.removeCallbacksAndMessages(null);
        zoomBadgeHandler.postDelayed(() -> binding.tvZoomBadge.setVisibility(View.GONE), 1800);
    }

    private void showFocusRing(float x, float y) {
        int halfSize = binding.ivFocusRing.getWidth() / 2;
        if (halfSize == 0) halfSize = 64;

        binding.ivFocusRing.setX(x - halfSize);
        binding.ivFocusRing.setY(y - halfSize);
        binding.ivFocusRing.setVisibility(View.VISIBLE);

        ScaleAnimation scale = new ScaleAnimation(1.4f, 1.0f, 1.4f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(250);
        binding.ivFocusRing.startAnimation(scale);

        binding.ivFocusRing.postDelayed(() -> binding.ivFocusRing.setVisibility(View.GONE), 1500);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (preferences.isVolumeShutterEnabled()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                if (activeRecording != null) {
                    HapticUtils.performRecordingStopFeedback(this);
                    stopRecording();
                } else {
                    HapticUtils.performRecordingStartFeedback(this);
                    startRecording();
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void cycleResolution() {
        if ("1080p".equalsIgnoreCase(currentResolution)) {
            currentResolution = "4k";
        } else if ("4k".equalsIgnoreCase(currentResolution)) {
            currentResolution = "720p";
        } else if ("720p".equalsIgnoreCase(currentResolution)) {
            currentResolution = "480p";
        } else {
            currentResolution = "1080p";
        }
        preferences.setResolution(currentResolution);
        binding.btnQualityToggle.setText(currentResolution.toUpperCase());
        startCamera();
    }

    private void updateAudioButtonUi() {
        binding.btnToggleAudio.setImageResource(isAudioEnabled ? R.drawable.ic_mic : R.drawable.ic_mic_off);
        binding.tvMicStatus.setText(isAudioEnabled ? "AUDIO ON" : "AUDIO OFF");
    }

    private void observeViewModel() {
        viewModel.getFormattedDuration().observe(this, duration -> {
            binding.tvRecordingDuration.setText(duration);
            binding.tvDiscreteTimer.setText(duration);
        });

        viewModel.getBatteryLevel().observe(this, batteryPct -> {
            binding.tvBatteryStatus.setText(batteryPct + "%");
        });

        viewModel.getIsStorageCritical().observe(this, isCritical -> {
            binding.tvStorageWarning.setVisibility(isCritical ? View.VISIBLE : View.GONE);
        });

        viewModel.getRecordingState().observe(this, state -> {
            if (state == CameraRecorderViewModel.RecordingState.RECORDING) {
                binding.fabShutter.setImageResource(R.drawable.ic_stop_record);
                binding.fabShutter.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.md_theme_dark_surface));
                binding.layoutRecordingIndicator.setVisibility(isPreviewVisible ? View.VISIBLE : View.INVISIBLE);

                // Update discrete cover status
                binding.tvDiscreteStatus.setText("🔴 RECORDING IN PROGRESS");
                binding.tvDiscreteStatus.setTextColor(0xFFFF3B30);
                binding.tvDiscreteTimer.setVisibility(View.VISIBLE);
                binding.viewDiscreteRedDot.setVisibility(View.VISIBLE);
                binding.tvDiscreteHint.setText("Recording security video · Tap shutter or press Volume key to stop");

                startPulsingAnimation();
            } else {
                binding.fabShutter.setImageResource(R.drawable.ic_camera_record);
                binding.fabShutter.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.recording_red));
                binding.layoutRecordingIndicator.setVisibility(View.INVISIBLE);

                // Update discrete cover status
                binding.tvDiscreteStatus.setText("NOT RECORDING");
                binding.tvDiscreteStatus.setTextColor(0xFF94A3B8);
                binding.tvDiscreteTimer.setVisibility(View.GONE);
                binding.viewDiscreteRedDot.setVisibility(View.GONE);
                binding.tvDiscreteHint.setText("Standby · Tap shutter below or press Volume key to record");

                stopPulsingAnimation();
            }
        });
    }

    private void startPulsingAnimation() {
        AlphaAnimation pulse = new AlphaAnimation(1.0f, 0.2f);
        pulse.setDuration(600);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setRepeatCount(Animation.INFINITE);
        binding.viewRedDot.startAnimation(pulse);
        binding.viewDiscreteRedDot.startAnimation(pulse);
    }

    private void stopPulsingAnimation() {
        binding.viewRedDot.clearAnimation();
        binding.viewDiscreteRedDot.clearAnimation();
    }

    private void checkPermissionsAndStartCamera() {
        String[] requiredPermissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        boolean allGranted = true;
        for (String perm : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            startCamera();
        } else {
            permissionLauncher.launch(requiredPermissions);
        }
    }

    private void startCamera() {
        cameraHelper.initializeCamera(
                this,
                this,
                binding.previewView,
                isFrontCamera,
                currentResolution,
                new CameraHelper.CameraInitCallback() {
                    @Override
                    public void onInitialized(androidx.camera.video.VideoCapture<androidx.camera.video.Recorder> videoCapture) {
                        // Camera pipeline active
                    }

                    @Override
                    public void onError(Exception exception) {
                        Toast.makeText(CameraRecorderActivity.this, "Camera init failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @SuppressLint("MissingPermission")
    private void startRecording() {
        if (activeRecording != null) return;

        androidx.camera.video.VideoCapture<androidx.camera.video.Recorder> vc = cameraHelper.getVideoCapture();
        if (vc == null) {
            Toast.makeText(this, "Camera is initializing, please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        File recDir = FileUtils.getRecordingDirectory(this);
        String filename = DateTimeUtils.generateVideoFilename();
        currentOutputFile = new File(recDir, filename);

        FileOutputOptions outputOptions = new FileOutputOptions.Builder(currentOutputFile).build();

        try {
            PendingRecording pendingRecording = vc.getOutput().prepareRecording(CameraRecorderActivity.this, outputOptions);

            if (isAudioEnabled && ContextCompat.checkSelfPermission(CameraRecorderActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                pendingRecording.withAudioEnabled();
            }

            activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(CameraRecorderActivity.this), event -> {
                if (event instanceof VideoRecordEvent.Start) {
                    viewModel.startRecordingSession(currentOutputFile);
                } else if (event instanceof VideoRecordEvent.Finalize) {
                    VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) event;
                    String locationStr = "Unknown";
                    if (preferences.isLocationEnabled()) {
                        Location loc = LocationHelper.getLastKnownLocation(CameraRecorderActivity.this);
                        locationStr = LocationHelper.formatCoordinates(loc);
                    }

                    viewModel.onRecordingFinalized(currentOutputFile, locationStr, currentResolution);
                    activeRecording = null;

                    if (finalizeEvent.hasError()) {
                        Toast.makeText(CameraRecorderActivity.this, "Recording finalized with code: " + finalizeEvent.getError(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(CameraRecorderActivity.this, R.string.recording_stopped, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Failed to start: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (activeRecording != null) {
            activeRecording.stop();
            activeRecording = null;
            viewModel.stopRecordingSession();
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (activeRecording != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                android.util.Rational aspectRatio = new android.util.Rational(9, 16);
                android.app.PictureInPictureParams params = new android.app.PictureInPictureParams.Builder()
                        .setAspectRatio(aspectRatio)
                        .build();
                enterPictureInPictureMode(params);
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        zoomBadgeHandler.removeCallbacksAndMessages(null);
        if (activeRecording != null) {
            activeRecording.stop();
            activeRecording = null;
        }
        cameraHelper.release();
    }
}
