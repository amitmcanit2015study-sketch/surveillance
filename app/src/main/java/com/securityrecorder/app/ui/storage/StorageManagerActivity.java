package com.securityrecorder.app.ui.storage;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.securityrecorder.app.R;
import com.securityrecorder.app.data.local.AppDatabase;
import com.securityrecorder.app.data.model.StorageInfo;
import com.securityrecorder.app.data.repository.VideoRepository;
import com.securityrecorder.app.databinding.ActivityStorageManagerBinding;
import com.securityrecorder.app.utils.FileUtils;
import com.securityrecorder.app.utils.HapticUtils;

/**
 * Storage management screen displaying capacity breakdown and manual maintenance actions.
 */
public class StorageManagerActivity extends AppCompatActivity {

    private ActivityStorageManagerBinding binding;
    private VideoRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStorageManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new VideoRepository(getApplication());

        binding.storageToolbar.setNavigationOnClickListener(v -> finish());
        loadStorageMetrics();

        binding.btnRunCleanup.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            AppDatabase.databaseWriteExecutor.execute(() -> {
                int cleaned = repository.cleanupOldRecordings(30);
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.storage_cleanup_done, cleaned), Toast.LENGTH_SHORT).show();
                    loadStorageMetrics();
                });
            });
        });
    }

    private void loadStorageMetrics() {
        StorageInfo info = FileUtils.getStorageMetrics(this);

        if (info.getTotalInternalBytes() > 0) {
            int usedPct = (int) ((info.getUsedInternalBytes() * 100) / info.getTotalInternalBytes());
            binding.pbInternalStorage.setProgress(usedPct);
            binding.tvInternalUsed.setText("Used: " + FileUtils.formatFileSize(info.getUsedInternalBytes()));
            binding.tvInternalFree.setText("Free: " + FileUtils.formatFileSize(info.getFreeInternalBytes()));
        }

        binding.tvRecordingsSize.setText(
                FileUtils.formatFileSize(info.getRecordingsTotalBytes()) + " (" + info.getRecordingsCount() + " clips)"
        );
    }
}
