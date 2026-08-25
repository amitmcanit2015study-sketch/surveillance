package com.securityrecorder.app.ui.common;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.securityrecorder.app.R;
import com.securityrecorder.app.databinding.ActivityAboutBinding;
import com.securityrecorder.app.utils.FileUtils;
import com.securityrecorder.app.utils.HapticUtils;

/**
 * About Screen displaying app details, credits, and options to share APK or send feedback.
 */
public class AboutActivity extends AppCompatActivity {

    private ActivityAboutBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.aboutToolbar.setNavigationOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            finish();
        });

        binding.btnAboutShareApp.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            FileUtils.shareApp(this);
        });

        binding.btnSendFeedback.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            try {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:" + getString(R.string.company_email)));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback for " + getString(R.string.app_name));
                startActivity(Intent.createChooser(emailIntent, "Send Feedback"));
            } catch (Exception e) {
                FileUtils.shareApp(this);
            }
        });
    }
}
