package com.securityrecorder.app.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import androidx.core.content.FileProvider;
import com.securityrecorder.app.data.model.StorageInfo;
import com.securityrecorder.app.data.model.VideoItem;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * File I/O, storage calculation, MediaMetadata parsing, Vault management, and FileProvider helper.
 */
public class FileUtils {

    public static final String DIRECTORY_NAME = "SecurityRecorder";
    public static final String VAULT_DIR_NAME = ".vault";

    public static File getRecordingDirectory(Context context) {
        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), DIRECTORY_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getVaultDirectory(Context context) {
        File vaultDir = new File(getRecordingDirectory(context), VAULT_DIR_NAME);
        if (!vaultDir.exists()) {
            vaultDir.mkdirs();
        }
        return vaultDir;
    }

    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        digitGroups = Math.min(digitGroups, units.length - 1);
        return new DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static StorageInfo getStorageMetrics(Context context) {
        long totalInternal = 0;
        long freeInternal = 0;
        long totalExternal = 0;
        long freeExternal = 0;

        try {
            File internalPath = Environment.getDataDirectory();
            StatFs statInternal = new StatFs(internalPath.getPath());
            totalInternal = statInternal.getTotalBytes();
            freeInternal = statInternal.getAvailableBytes();
        } catch (Exception ignored) {}

        try {
            File[] externalDirs = context.getExternalFilesDirs(null);
            if (externalDirs != null && externalDirs.length > 1 && externalDirs[1] != null) {
                StatFs statExternal = new StatFs(externalDirs[1].getPath());
                totalExternal = statExternal.getTotalBytes();
                freeExternal = statExternal.getAvailableBytes();
            }
        } catch (Exception ignored) {}

        // Calculate app recordings directory size
        File recDir = getRecordingDirectory(context);
        long recBytes = 0;
        int recCount = 0;
        File[] files = recDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && isSupportedMediaFile(f.getName())) {
                    recBytes += f.length();
                    recCount++;
                }
            }
        }

        return new StorageInfo(totalInternal, freeInternal, totalExternal, freeExternal, recBytes, recCount);
    }

    public static boolean isSupportedMediaFile(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".mp3")
                || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp");
    }

    public static VideoItem extractMetadata(Context context, File file) {
        VideoItem item = new VideoItem();
        String name = file.getName();
        item.setTitle(name);
        item.setFilePath(file.getAbsolutePath());
        item.setSizeBytes(file.length());
        item.setTimestamp(file.lastModified());

        boolean isVault = file.getParentFile() != null && VAULT_DIR_NAME.equals(file.getParentFile().getName());
        item.setVault(isVault);

        String mediaType = VideoItem.inferMediaType(name);
        item.setMediaType(mediaType);

        Uri uri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                file
        );
        item.setUriString(uri.toString());

        if ("image".equalsIgnoreCase(mediaType)) {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                item.setResolution(options.outWidth + "x" + options.outHeight);
                item.setCodec("image/jpeg");
                item.setDurationMs(0);
                item.setLocation("Not available");
            } catch (Exception e) {
                item.setResolution("Photo");
                item.setCodec("image/jpeg");
                item.setDurationMs(0);
                item.setLocation("Not available");
            }
            return item;
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(file.getAbsolutePath());
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null) {
                item.setDurationMs(Long.parseLong(durationStr));
            }

            String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            boolean isAudio = "audio".equalsIgnoreCase(mediaType) || (width == null && height == null);

            if (isAudio) {
                item.setResolution("Audio (M4A)");
            } else {
                item.setResolution(width + "x" + height);
            }

            String location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION);
            if (location != null && !location.trim().isEmpty()) {
                item.setLocation(location);
            } else {
                item.setLocation("Not available");
            }

            String mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            item.setCodec(mime != null ? mime : (isAudio ? "audio/mp4a-latm" : "video/mp4"));

        } catch (Exception e) {
            item.setDurationMs(0);
            boolean isAudio = "audio".equalsIgnoreCase(mediaType);
            item.setResolution(isAudio ? "Audio (M4A)" : "1080p");
            item.setLocation("Not available");
            item.setCodec(isAudio ? "audio/mp4a-latm" : "video/mp4");
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {}
        }

        return item;
    }

    public static boolean deleteFile(String filePath) {
        if (filePath == null) return false;
        File file = new File(filePath);
        return file.exists() && file.delete();
    }

    public static boolean renameFile(String oldPath, String newName) {
        if (oldPath == null || newName == null) return false;
        File oldFile = new File(oldPath);
        if (!oldFile.exists()) return false;

        String ext = "";
        int dotIdx = oldFile.getName().lastIndexOf('.');
        if (dotIdx > 0) {
            ext = oldFile.getName().substring(dotIdx);
        }

        String finalName = newName;
        if (!finalName.contains(".") && !ext.isEmpty()) {
            finalName = finalName + ext;
        }

        File newFile = new File(oldFile.getParentFile(), finalName);
        return oldFile.renameTo(newFile);
    }

    public static File moveToVault(Context context, String filePath) {
        if (filePath == null) return null;
        File source = new File(filePath);
        if (!source.exists()) return null;

        File vaultDir = getVaultDirectory(context);
        File dest = new File(vaultDir, source.getName());
        if (source.renameTo(dest)) {
            return dest;
        }
        // Fallback copy & delete
        if (copyFile(source, dest)) {
            source.delete();
            return dest;
        }
        return null;
    }

    public static File restoreFromVault(Context context, String filePath) {
        if (filePath == null) return null;
        File source = new File(filePath);
        if (!source.exists()) return null;

        File recDir = getRecordingDirectory(context);
        File dest = new File(recDir, source.getName());
        if (source.renameTo(dest)) {
            return dest;
        }
        if (copyFile(source, dest)) {
            source.delete();
            return dest;
        }
        return null;
    }

    public static boolean copyFile(File src, File dst) {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void shareVideo(Context context, String filePath) {
        shareFile(context, filePath);
    }

    public static void shareFile(Context context, String filePath) {
        if (filePath == null) return;
        File file = new File(filePath);
        if (!file.exists()) return;

        Uri uri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                file
        );

        String mimeType = "video/mp4";
        String lower = file.getName().toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")) {
            mimeType = "image/*";
        } else if (lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".mp3")) {
            mimeType = "audio/*";
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, "Share Media"));
    }

    public static void shareMultipleFiles(Context context, List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) return;

        ArrayList<Uri> uris = new ArrayList<>();
        for (String path : filePaths) {
            File f = new File(path);
            if (f.exists()) {
                Uri uri = FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".fileprovider",
                        f
                );
                uris.add(uri);
            }
        }

        if (uris.isEmpty()) return;

        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("*/*");
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, "Share " + uris.size() + " Item(s)"));
    }

    public static void shareApp(Context context) {
        try {
            String appName = context.getString(com.securityrecorder.app.R.string.app_name);
            String description = context.getString(com.securityrecorder.app.R.string.about_description);
            String developer = context.getString(com.securityrecorder.app.R.string.developer_name);
            String company = context.getString(com.securityrecorder.app.R.string.company_name);

            String shareText = appName + " - by Amit Bharat\n"
                    + company + "\n\n"
                    + description;

            String apkPath = context.getApplicationInfo().sourceDir;
            File apkFile = new File(apkPath);

            Intent intent = new Intent(Intent.ACTION_SEND);
            if (apkFile.exists()) {
                Uri apkUri = FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".fileprovider",
                        apkFile
                );
                intent.setType("application/vnd.android.package-archive");
                intent.putExtra(Intent.EXTRA_STREAM, apkUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                intent.setType("text/plain");
            }

            intent.putExtra(Intent.EXTRA_SUBJECT, appName + " - " + developer);
            intent.putExtra(Intent.EXTRA_TEXT, shareText);

            context.startActivity(Intent.createChooser(intent, "Share " + appName + " (APK)"));
        } catch (Exception e) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, context.getString(com.securityrecorder.app.R.string.about_description));
            context.startActivity(Intent.createChooser(intent, "Share App"));
        }
    }
}
