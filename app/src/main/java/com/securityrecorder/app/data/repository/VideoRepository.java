package com.securityrecorder.app.data.repository;

import android.app.Application;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.securityrecorder.app.data.local.AppDatabase;
import com.securityrecorder.app.data.local.VideoRecordDao;
import com.securityrecorder.app.data.local.VideoRecordEntity;
import com.securityrecorder.app.data.model.FilterType;
import com.securityrecorder.app.data.model.VideoItem;
import com.securityrecorder.app.data.preferences.AppPreferences;
import com.securityrecorder.app.utils.DateTimeUtils;
import com.securityrecorder.app.utils.FileUtils;
import com.securityrecorder.app.utils.LocationHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Repository mediating between filesystem recordings (audios, photos, videos, vault) and Room DB metadata.
 */
public class VideoRepository {

    private final Application application;
    private final VideoRecordDao videoRecordDao;

    public VideoRepository(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getInstance(application);
        this.videoRecordDao = db.videoRecordDao();
    }

    public void syncStorageWithDatabase(Runnable onComplete) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            File recDir = FileUtils.getRecordingDirectory(application);
            File vaultDir = FileUtils.getVaultDirectory(application);

            // Deduplicate & prune missing records from DB
            List<VideoRecordEntity> allDb = videoRecordDao.getAllVideosSync();
            java.util.Set<String> seenPaths = new java.util.HashSet<>();
            for (VideoRecordEntity entity : allDb) {
                File f = new File(entity.getFilePath());
                if (!f.exists() || f.length() < 10 || seenPaths.contains(entity.getFilePath())) {
                    videoRecordDao.delete(entity);
                } else {
                    seenPaths.add(entity.getFilePath());
                }
            }

            // Scan normal recordings
            scanDirectory(recDir, false);

            // Scan vault recordings
            scanDirectory(vaultDir, true);

            if (onComplete != null) {
                new Handler(Looper.getMainLooper()).post(onComplete);
            }
        });
    }

    private void scanDirectory(File dir, boolean isVault) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isFile() && FileUtils.isSupportedMediaFile(file.getName()) && file.length() > 10) {
                VideoRecordEntity existing = videoRecordDao.findByFilePath(file.getAbsolutePath());
                if (existing == null) {
                    VideoItem parsed = FileUtils.extractMetadata(application, file);
                    String location = parsed.getLocation();
                    if ((location == null || location.equals("Not available")) && new AppPreferences(application).isLocationEnabled()) {
                        android.location.Location loc = LocationHelper.getLastKnownLocation(application);
                        location = LocationHelper.getFullLocationString(application, loc);
                    }
                    VideoRecordEntity entity = new VideoRecordEntity(
                            file.getName(),
                            file.getAbsolutePath(),
                            parsed.getUriString(),
                            file.lastModified(),
                            parsed.getDurationMs(),
                            file.length(),
                            false,
                            parsed.getResolution(),
                            location,
                            parsed.getCodec(),
                            isVault,
                            parsed.getMediaType()
                    );
                    videoRecordDao.insert(entity);
                } else if ((existing.getLocation() == null || existing.getLocation().equals("Not available")) && new AppPreferences(application).isLocationEnabled()) {
                    android.location.Location loc = LocationHelper.getLastKnownLocation(application);
                    String locStr = LocationHelper.getFullLocationString(application, loc);
                    if (!locStr.equals("Not available")) {
                        existing.setLocation(locStr);
                        videoRecordDao.update(existing);
                    }
                }
            }
        }
    }

    public LiveData<List<VideoItem>> getFilteredVideos(FilterType filterType, String searchQuery) {
        LiveData<List<VideoRecordEntity>> source;

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            source = videoRecordDao.searchVideosLive(searchQuery.trim());
        } else {
            switch (filterType) {
                case TODAY:
                    source = videoRecordDao.getVideosSinceLive(DateTimeUtils.getStartOfToday());
                    break;
                case THIS_WEEK:
                    source = videoRecordDao.getVideosSinceLive(DateTimeUtils.getStartOfThisWeek());
                    break;
                case THIS_MONTH:
                    source = videoRecordDao.getVideosSinceLive(DateTimeUtils.getStartOfThisMonth());
                    break;
                case FAVORITES:
                    source = videoRecordDao.getFavoriteVideosLive();
                    break;
                case ALL:
                default:
                    source = videoRecordDao.getAllVideosLive();
                    break;
            }
        }

        return Transformations.map(source, this::mapEntitiesToItems);
    }

    public LiveData<List<VideoItem>> getVaultVideos(String searchQuery) {
        LiveData<List<VideoRecordEntity>> source;
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            source = videoRecordDao.searchVaultVideosLive(searchQuery.trim());
        } else {
            source = videoRecordDao.getVaultVideosLive();
        }
        return Transformations.map(source, this::mapEntitiesToItems);
    }

    private List<VideoItem> mapEntitiesToItems(List<VideoRecordEntity> entities) {
        if (entities == null) return Collections.emptyList();
        List<VideoItem> items = new ArrayList<>();
        for (VideoRecordEntity e : entities) {
            VideoItem item = new VideoItem(
                    e.getId(),
                    e.getFilename(),
                    e.getFilePath(),
                    e.getUriString(),
                    e.getTimestamp(),
                    e.getDurationMs(),
                    e.getSizeBytes(),
                    e.isFavorite(),
                    e.getResolution(),
                    e.getLocation(),
                    e.getCodec(),
                    e.isVault(),
                    e.getMediaType()
            );
            items.add(item);
        }
        return items;
    }

    public void insertRecordedVideo(File file, String resolution, String location, String codec, long durationMs) {
        insertRecordedMedia(file, resolution, location, codec, durationMs, VideoItem.inferMediaType(file.getName()));
    }

    public void insertRecordedMedia(File file, String resolution, String location, String codec, long durationMs, String mediaType) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            VideoRecordEntity existing = videoRecordDao.findByFilePath(file.getAbsolutePath());
            if (existing != null) {
                if (durationMs > 0) {
                    existing.setDurationMs(durationMs);
                }
                if (location != null && !location.equals("Not available")) {
                    existing.setLocation(location);
                }
                if (resolution != null && !resolution.equals("1080p")) {
                    existing.setResolution(resolution);
                }
                existing.setSizeBytes(file.length());
                videoRecordDao.update(existing);
                return;
            }

            VideoItem parsed = FileUtils.extractMetadata(application, file);
            long finalDuration = durationMs > 0 ? durationMs : parsed.getDurationMs();
            String finalRes = resolution != null ? resolution : parsed.getResolution();
            String finalLoc = location != null ? location : parsed.getLocation();
            String finalType = mediaType != null ? mediaType : parsed.getMediaType();

            VideoRecordEntity entity = new VideoRecordEntity(
                    file.getName(),
                    file.getAbsolutePath(),
                    parsed.getUriString(),
                    file.lastModified(),
                    finalDuration,
                    file.length(),
                    false,
                    finalRes,
                    finalLoc,
                    codec != null ? codec : (parsed.getCodec()),
                    false,
                    finalType
            );
            videoRecordDao.insert(entity);
        });
    }

    public void toggleFavorite(VideoItem item) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            boolean newStatus = !item.isFavorite();
            item.setFavorite(newStatus);
            videoRecordDao.updateFavoriteByPathOrId(item.getId(), item.getFilePath(), newStatus);
        });
    }

    public void toggleFavoriteMultiple(List<VideoItem> items, Runnable onComplete) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (items != null) {
                for (VideoItem item : items) {
                    boolean newStatus = !item.isFavorite();
                    item.setFavorite(newStatus);
                    videoRecordDao.updateFavoriteByPathOrId(item.getId(), item.getFilePath(), newStatus);
                }
            }
            if (onComplete != null) {
                new Handler(Looper.getMainLooper()).post(onComplete);
            }
        });
    }

    public void moveToVault(List<VideoItem> items, Runnable onComplete) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (items != null) {
                for (VideoItem item : items) {
                    File dest = FileUtils.moveToVault(application, item.getFilePath());
                    if (dest != null) {
                        Uri newUri = FileProvider.getUriForFile(
                                application,
                                application.getPackageName() + ".fileprovider",
                                dest
                        );
                        item.setFilePath(dest.getAbsolutePath());
                        item.setUriString(newUri.toString());
                        item.setVault(true);
                        videoRecordDao.updateVaultStatus(item.getId(), true, dest.getAbsolutePath(), newUri.toString());
                    }
                }
            }
            if (onComplete != null) {
                new Handler(Looper.getMainLooper()).post(onComplete);
            }
        });
    }

    public void restoreFromVault(List<VideoItem> items, Runnable onComplete) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (items != null) {
                for (VideoItem item : items) {
                    File dest = FileUtils.restoreFromVault(application, item.getFilePath());
                    if (dest != null) {
                        Uri newUri = FileProvider.getUriForFile(
                                application,
                                application.getPackageName() + ".fileprovider",
                                dest
                        );
                        item.setFilePath(dest.getAbsolutePath());
                        item.setUriString(newUri.toString());
                        item.setVault(false);
                        videoRecordDao.updateVaultStatus(item.getId(), false, dest.getAbsolutePath(), newUri.toString());
                    }
                }
            }
            if (onComplete != null) {
                new Handler(Looper.getMainLooper()).post(onComplete);
            }
        });
    }

    public void deleteVideo(VideoItem item, Runnable onComplete) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (item != null) {
                FileUtils.deleteFile(item.getFilePath());
                videoRecordDao.deleteByFilePath(item.getFilePath());
            }
            if (onComplete != null) {
                new Handler(Looper.getMainLooper()).post(onComplete);
            }
        });
    }

    public void deleteVideos(List<VideoItem> items, Runnable onComplete) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (items != null) {
                for (VideoItem item : items) {
                    FileUtils.deleteFile(item.getFilePath());
                    videoRecordDao.deleteByFilePath(item.getFilePath());
                }
            }
            if (onComplete != null) {
                new Handler(Looper.getMainLooper()).post(onComplete);
            }
        });
    }

    public void renameVideo(VideoItem item, String newName, Runnable onComplete) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            String oldPath = item.getFilePath();
            if (FileUtils.renameFile(oldPath, newName)) {
                File oldFile = new File(oldPath);
                File newFile = new File(oldFile.getParentFile(), newName);
                item.setTitle(newFile.getName());
                item.setFilePath(newFile.getAbsolutePath());
                videoRecordDao.updateFilenameAndPath(item.getId(), newFile.getName(), newFile.getAbsolutePath());
            }

            if (onComplete != null) {
                new Handler(Looper.getMainLooper()).post(onComplete);
            }
        });
    }

    public int cleanupOldRecordings(int daysThreshold) {
        long thresholdTime = System.currentTimeMillis() - (daysThreshold * 24L * 60L * 60L * 1000L);
        List<VideoRecordEntity> oldVideos = videoRecordDao.getOldNonFavoriteVideos(thresholdTime);
        int deletedCount = 0;
        for (VideoRecordEntity v : oldVideos) {
            if (FileUtils.deleteFile(v.getFilePath())) {
                videoRecordDao.delete(v);
                deletedCount++;
            }
        }
        return deletedCount;
    }
}
