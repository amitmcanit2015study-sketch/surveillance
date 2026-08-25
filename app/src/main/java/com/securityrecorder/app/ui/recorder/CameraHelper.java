package com.securityrecorder.app.ui.recorder;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.VideoCapture;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.TimeUnit;

/**
 * CameraX helper for configuring video capture use case, resolution qualities, torch, zoom, focus, and lens switching.
 */
public class CameraHelper {

    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private VideoCapture<Recorder> videoCapture;
    private Preview preview;
    private boolean isTorchOn = false;

    public interface CameraInitCallback {
        void onInitialized(VideoCapture<Recorder> videoCapture);
        void onError(Exception exception);
    }

    public void initializeCamera(
            Context context,
            LifecycleOwner lifecycleOwner,
            PreviewView previewView,
            boolean useFrontCamera,
            String resolutionSetting,
            CameraInitCallback callback
    ) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                // Select quality based on preference
                Quality targetQuality;
                if ("4k".equalsIgnoreCase(resolutionSetting)) {
                    targetQuality = Quality.UHD;
                } else if ("720p".equalsIgnoreCase(resolutionSetting)) {
                    targetQuality = Quality.HD;
                } else if ("480p".equalsIgnoreCase(resolutionSetting)) {
                    targetQuality = Quality.SD;
                } else {
                    targetQuality = Quality.FHD; // Default 1080p
                }

                QualitySelector qualitySelector = QualitySelector.from(
                        targetQuality,
                        FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                );

                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(qualitySelector)
                        .build();

                videoCapture = VideoCapture.withOutput(recorder);

                preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = useFrontCamera
                        ? CameraSelector.DEFAULT_FRONT_CAMERA
                        : CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();

                camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        videoCapture
                );

                if (callback != null) {
                    callback.onInitialized(videoCapture);
                }

            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        }, ContextCompat.getMainExecutor(context));
    }

    public void triggerFocusAndMetering(float x, float y, float width, float height) {
        if (camera != null) {
            try {
                MeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(width, height);
                MeteringPoint point = factory.createPoint(x, y);
                FocusMeteringAction action = new FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE)
                        .setAutoCancelDuration(3, TimeUnit.SECONDS)
                        .build();
                camera.getCameraControl().startFocusAndMetering(action);
            } catch (Exception ignored) {}
        }
    }

    public void setZoomRatio(float ratio) {
        if (camera != null) {
            camera.getCameraControl().setZoomRatio(ratio);
        }
    }

    public void setLinearZoom(float linearZoom) {
        if (camera != null) {
            float clamped = Math.max(0.0f, Math.min(1.0f, linearZoom));
            camera.getCameraControl().setLinearZoom(clamped);
        }
    }

    public CameraInfo getCameraInfo() {
        return camera != null ? camera.getCameraInfo() : null;
    }

    public CameraControl getCameraControl() {
        return camera != null ? camera.getCameraControl() : null;
    }

    public VideoCapture<Recorder> getVideoCapture() {
        return videoCapture;
    }

    public void toggleTorch(boolean enable) {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            camera.getCameraControl().enableTorch(enable);
            isTorchOn = enable;
        }
    }

    public boolean isTorchOn() {
        return isTorchOn;
    }

    public boolean hasFlashUnit() {
        return camera != null && camera.getCameraInfo().hasFlashUnit();
    }

    public void release() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}
