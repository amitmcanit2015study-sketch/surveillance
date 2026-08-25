package com.securityrecorder.app.data.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared preferences manager for security recorder settings, backup, and restore.
 */
public class AppPreferences {

    private static final String PREF_NAME = "security_recorder_prefs";

    // Keys
    private static final String KEY_RESOLUTION = "pref_resolution";
    private static final String KEY_FRAME_RATE = "pref_frame_rate";
    private static final String KEY_AUDIO_ENABLED = "pref_audio_enabled";
    private static final String KEY_LOCATION_ENABLED = "pref_location_enabled";
    private static final String KEY_FRONT_CAMERA = "pref_front_camera";
    private static final String KEY_STABILIZATION = "pref_stabilization";
    private static final String KEY_APP_LOCK_ENABLED = "pref_app_lock_enabled";
    private static final String KEY_SECURITY_PIN = "pref_security_pin";
    private static final String KEY_LAYOUT_GRID = "pref_layout_grid";
    private static final String KEY_AUTO_CLEANUP = "pref_auto_cleanup";
    private static final String KEY_CLEANUP_DAYS = "pref_cleanup_days";
    private static final String KEY_MAX_DURATION_MINS = "pref_max_duration_mins";
    private static final String KEY_THEME_MODE = "pref_theme_mode";
    private static final String KEY_LOCKSCREEN_CAMERA = "pref_lockscreen_camera";
    private static final String KEY_VOLUME_SHUTTER = "pref_volume_shutter";
    private static final String KEY_MOTION_DETECTION = "pref_motion_detection";
    private static final String KEY_GRID_OVERLAY = "pref_grid_overlay";

    private final SharedPreferences prefs;

    public AppPreferences(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Resolution: "480p", "720p", "1080p", "4k"
    public String getResolution() {
        return prefs.getString(KEY_RESOLUTION, "1080p");
    }

    public void setResolution(String resolution) {
        prefs.edit().putString(KEY_RESOLUTION, resolution).apply();
    }

    // Frame Rate (FPS)
    public int getFrameRate() {
        return prefs.getInt(KEY_FRAME_RATE, 30);
    }

    public void setFrameRate(int fps) {
        prefs.edit().putInt(KEY_FRAME_RATE, fps).apply();
    }

    // Audio recording
    public boolean isAudioEnabled() {
        return prefs.getBoolean(KEY_AUDIO_ENABLED, true);
    }

    public void setAudioEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUDIO_ENABLED, enabled).apply();
    }

    // GPS location tagging
    public boolean isLocationEnabled() {
        return prefs.getBoolean(KEY_LOCATION_ENABLED, false);
    }

    public void setLocationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_LOCATION_ENABLED, enabled).apply();
    }

    // Camera facing (true for front, false for back)
    public boolean isFrontCamera() {
        return prefs.getBoolean(KEY_FRONT_CAMERA, false);
    }

    public void setFrontCamera(boolean front) {
        prefs.edit().putBoolean(KEY_FRONT_CAMERA, front).apply();
    }

    // Stabilization
    public boolean isStabilizationEnabled() {
        return prefs.getBoolean(KEY_STABILIZATION, true);
    }

    public void setStabilizationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_STABILIZATION, enabled).apply();
    }

    // App Lock
    public boolean isAppLockEnabled() {
        return prefs.getBoolean(KEY_APP_LOCK_ENABLED, false);
    }

    public void setAppLockEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply();
    }

    public String getSecurityPin() {
        return prefs.getString(KEY_SECURITY_PIN, "");
    }

    public void setSecurityPin(String pin) {
        prefs.edit().putString(KEY_SECURITY_PIN, pin).apply();
    }

    public boolean hasPinConfigured() {
        String pin = getSecurityPin();
        return pin != null && pin.length() == 4;
    }

    // Layout mode: true = Grid, false = List
    public boolean isGridLayout() {
        return prefs.getBoolean(KEY_LAYOUT_GRID, true);
    }

    public void setGridLayout(boolean isGrid) {
        prefs.edit().putBoolean(KEY_LAYOUT_GRID, isGrid).apply();
    }

    // Auto Cleanup
    public boolean isAutoCleanupEnabled() {
        return prefs.getBoolean(KEY_AUTO_CLEANUP, false);
    }

    public void setAutoCleanupEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_CLEANUP, enabled).apply();
    }

    public int getCleanupDays() {
        return prefs.getInt(KEY_CLEANUP_DAYS, 30);
    }

    public void setCleanupDays(int days) {
        prefs.edit().putInt(KEY_CLEANUP_DAYS, days).apply();
    }

    // Max Duration (minutes, 0 = unlimited)
    public int getMaxDurationMins() {
        return prefs.getInt(KEY_MAX_DURATION_MINS, 0);
    }

    public void setMaxDurationMins(int mins) {
        prefs.edit().putInt(KEY_MAX_DURATION_MINS, mins).apply();
    }

    // Theme Mode
    public int getThemeMode() {
        return prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public void setThemeMode(int mode) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
    }

    // Lockscreen Camera Access (Allow camera when device is locked)
    public boolean isLockscreenCameraEnabled() {
        return prefs.getBoolean(KEY_LOCKSCREEN_CAMERA, true);
    }

    public void setLockscreenCameraEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_LOCKSCREEN_CAMERA, enabled).apply();
    }

    // Hardware Volume Key Shutter
    public boolean isVolumeShutterEnabled() {
        return prefs.getBoolean(KEY_VOLUME_SHUTTER, true);
    }

    public void setVolumeShutterEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VOLUME_SHUTTER, enabled).apply();
    }

    // Motion Detection Analyzer
    public boolean isMotionDetectionEnabled() {
        return prefs.getBoolean(KEY_MOTION_DETECTION, false);
    }

    public void setMotionDetectionEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_MOTION_DETECTION, enabled).apply();
    }

    // Rule-of-Thirds Grid Overlay
    public boolean isGridOverlayEnabled() {
        return prefs.getBoolean(KEY_GRID_OVERLAY, false);
    }

    public void setGridOverlayEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_GRID_OVERLAY, enabled).apply();
    }

    // Backup & Restore settings as JSON
    public String exportSettingsToJson() {
        Map<String, ?> all = prefs.getAll();
        return new Gson().toJson(all);
    }

    public boolean restoreSettingsFromJson(String json) {
        try {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> map = new Gson().fromJson(json, type);
            if (map == null) return false;

            SharedPreferences.Editor editor = prefs.edit();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Object val = entry.getValue();
                if (val instanceof Boolean) {
                    editor.putBoolean(entry.getKey(), (Boolean) val);
                } else if (val instanceof Integer) {
                    editor.putInt(entry.getKey(), (Integer) val);
                } else if (val instanceof Double) {
                    editor.putInt(entry.getKey(), ((Double) val).intValue());
                } else if (val instanceof Long) {
                    editor.putLong(entry.getKey(), (Long) val);
                } else if (val instanceof String) {
                    editor.putString(entry.getKey(), (String) val);
                }
            }
            return editor.commit();
        } catch (Exception e) {
            return false;
        }
    }
}
