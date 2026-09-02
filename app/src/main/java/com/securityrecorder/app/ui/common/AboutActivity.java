package com.securityrecorder.app.ui.common;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.securityrecorder.app.databinding.ActivityAboutBinding;
import com.securityrecorder.app.utils.FileUtils;
import java.io.File;
import java.util.concurrent.Executors;

public class AboutActivity extends AppCompatActivity {

    private ActivityAboutBinding binding;

    private static final String APP_ABOUT_TEXT = "Surveillance - Modern Surveillance & Security Recording Manager\n\n"
            + "Surveillance is a free, ad-free surveillance and security recording manager designed for a fast, simple, and seamless experience. Easily capture, browse, and manage your recordings with a clean interface, smooth performance, and privacy at its core.\n\n"
            + "• Developed by: Amit Bharat\n"
            + "• Company: Rooys Soft Tech\n"
            + "• Contact: rooyssofttech2020@gmail.com\n"
            + "• Version: 1.0.1\n\n"
            + "Install the attached APK to get started!";

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.securityrecorder.app.utils.LocaleHelper.wrapContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupLanguageToggle();
        setupActions();
    }

    private void setupLanguageToggle() {
        boolean isHindi = com.securityrecorder.app.utils.LocaleHelper.isHindi(this);
        binding.toggleLanguageGroup.check(isHindi ? com.securityrecorder.app.R.id.btnLangHindi : com.securityrecorder.app.R.id.btnLangEnglish);

        binding.toggleLanguageGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                String targetLang = (checkedId == com.securityrecorder.app.R.id.btnLangHindi) ? "hi" : "en";
                if (!targetLang.equals(com.securityrecorder.app.utils.LocaleHelper.getLanguage(this))) {
                    com.securityrecorder.app.utils.LocaleHelper.setLocale(this, targetLang);
                }
            }
        });
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupActions() {
        binding.btnShareApp.setOnClickListener(v -> shareAppApk());
        binding.btnDownloadApk.setOnClickListener(v -> downloadAppApk());
        binding.btnFeedback.setOnClickListener(v -> sendFeedbackEmail());
        binding.tvEmail.setOnClickListener(v -> sendFeedbackEmail());
    }

    private void downloadAppApk() {
        Toast.makeText(this, "Downloading APK to Downloads folder...", Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                ApplicationInfo appInfo = getApplicationInfo();
                File originalApk = new File(appInfo.sourceDir);

                if (!originalApk.exists()) {
                    runOnUiThread(() -> Toast.makeText(this, "Could not find app APK file.", Toast.LENGTH_LONG).show());
                    return;
                }

                String fileName = "surveillance-amit-bharat.apk";
                boolean success = false;
                Uri downloadedUri = null;

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName);
                    values.put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive");
                    values.put(android.provider.MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS);

                    downloadedUri = getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (downloadedUri != null) {
                        try (java.io.InputStream in = new java.io.FileInputStream(originalApk);
                             java.io.OutputStream out = getContentResolver().openOutputStream(downloadedUri)) {
                            if (out != null) {
                                byte[] buffer = new byte[8192];
                                int bytesRead;
                                while ((bytesRead = in.read(buffer)) != -1) {
                                    out.write(buffer, 0, bytesRead);
                                }
                                out.flush();
                                success = true;
                            }
                        }
                    }
                } else {
                    File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs();
                    }
                    File destApk = new File(downloadsDir, fileName);
                    FileUtils.copyFile(originalApk, destApk);
                    android.media.MediaScannerConnection.scanFile(
                            this,
                            new String[]{destApk.getAbsolutePath()},
                            new String[]{"application/vnd.android.package-archive"},
                            null
                    );
                    downloadedUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", destApk);
                    success = true;
                }

                final boolean isSaved = success;
                final Uri finalUri = downloadedUri;

                runOnUiThread(() -> {
                    if (isSaved) {
                        com.google.android.material.snackbar.Snackbar.make(
                                binding.getRoot(),
                                "Saved to Downloads: " + fileName,
                                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                        ).setAction("Open Downloads", v -> {
                            try {
                                Intent intent = new Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            } catch (Exception e) {
                                if (finalUri != null) {
                                    try {
                                        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                                        viewIntent.setDataAndType(finalUri, "application/vnd.android.package-archive");
                                        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(viewIntent);
                                    } catch (Exception ignored) {}
                                }
                            }
                        }).show();
                    } else {
                        Toast.makeText(this, "Failed to save APK to Downloads.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error saving APK: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void shareAppApk() {
        Toast.makeText(this, "Preparing Surveillance APK to share...", Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                ApplicationInfo appInfo = getApplicationInfo();
                File originalApk = new File(appInfo.sourceDir);

                if (!originalApk.exists()) {
                    runOnUiThread(() -> shareAppDescriptionFallback());
                    return;
                }

                // Copy to cache dir with a clean, branded APK file name
                File shareDir = new File(getCacheDir(), "shared_apk");
                if (!shareDir.exists()) {
                    shareDir.mkdirs();
                }
                File targetApk = new File(shareDir, "surveillance-amit-bharat.apk");
                FileUtils.copyFile(originalApk, targetApk);

                Uri apkUri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        targetApk
                );

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/vnd.android.package-archive");
                shareIntent.putExtra(Intent.EXTRA_STREAM, apkUri);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Surveillance APK - by Amit Bharat (Rooys Soft Tech)");
                shareIntent.putExtra(Intent.EXTRA_TEXT, APP_ABOUT_TEXT);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                runOnUiThread(() -> {
                    startActivity(Intent.createChooser(shareIntent, "Share Surveillance APK & Details"));
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Sharing description...", Toast.LENGTH_SHORT).show();
                    shareAppDescriptionFallback();
                });
            }
        });
    }

    private void shareAppDescriptionFallback() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Surveillance - Modern Surveillance & Security Recording Manager");
        intent.putExtra(Intent.EXTRA_TEXT, APP_ABOUT_TEXT);
        startActivity(Intent.createChooser(intent, "Share Surveillance App"));
    }

    private void sendFeedbackEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:rooyssofttech2020@gmail.com"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"rooyssofttech2020@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Surveillance App - Feedback & Support");
        String body = "Hello Rooys Soft Tech Team,\n\n"
                + "Feedback / Feature Request / Bug Report:\n\n\n"
                + "------------------------------\n"
                + "Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + "\n"
                + "Android: " + android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT + ")\n"
                + "App Version: 1.0.1\n";
        intent.putExtra(Intent.EXTRA_TEXT, body);
        try {
            startActivity(Intent.createChooser(intent, "Send Email"));
        } catch (Exception ignored) {}
    }
}
