package com.securityrecorder.app.ui.settings;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import com.securityrecorder.app.data.preferences.AppPreferences;

/**
 * ViewModel managing app settings mutations, backup export, and JSON restore.
 */
public class SettingsViewModel extends AndroidViewModel {

    private final AppPreferences preferences;

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        this.preferences = new AppPreferences(application);
    }

    public AppPreferences getPreferences() {
        return preferences;
    }

    public String backupSettings() {
        return preferences.exportSettingsToJson();
    }

    public boolean restoreSettings(String json) {
        return preferences.restoreSettingsFromJson(json);
    }
}
