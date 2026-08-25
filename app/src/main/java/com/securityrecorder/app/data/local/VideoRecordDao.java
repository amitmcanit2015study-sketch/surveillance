package com.securityrecorder.app.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

/**
 * Data Access Object for recorded security videos, audios, images, and vaulted items.
 */
@Dao
public interface VideoRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(VideoRecordEntity video);

    @Update
    void update(VideoRecordEntity video);

    @Delete
    void delete(VideoRecordEntity video);

    @Query("DELETE FROM video_records WHERE filePath = :filePath")
    void deleteByFilePath(String filePath);

    // Regular / Non-vaulted files
    @Query("SELECT * FROM video_records WHERE isVault = 0 ORDER BY timestamp DESC")
    LiveData<List<VideoRecordEntity>> getAllVideosLive();

    @Query("SELECT * FROM video_records ORDER BY timestamp DESC")
    List<VideoRecordEntity> getAllVideosSync();

    @Query("SELECT * FROM video_records WHERE isVault = 0 AND isFavorite = 1 ORDER BY timestamp DESC")
    LiveData<List<VideoRecordEntity>> getFavoriteVideosLive();

    @Query("SELECT * FROM video_records WHERE isVault = 0 AND timestamp >= :startTime ORDER BY timestamp DESC")
    LiveData<List<VideoRecordEntity>> getVideosSinceLive(long startTime);

    @Query("SELECT * FROM video_records WHERE isVault = 0 AND timestamp >= :startTime AND timestamp < :endTime ORDER BY timestamp DESC")
    LiveData<List<VideoRecordEntity>> getVideosBetweenLive(long startTime, long endTime);

    @Query("SELECT * FROM video_records WHERE isVault = 0 AND filename LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    LiveData<List<VideoRecordEntity>> searchVideosLive(String query);

    // Vault files
    @Query("SELECT * FROM video_records WHERE isVault = 1 ORDER BY timestamp DESC")
    LiveData<List<VideoRecordEntity>> getVaultVideosLive();

    @Query("SELECT * FROM video_records WHERE isVault = 1 AND filename LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    LiveData<List<VideoRecordEntity>> searchVaultVideosLive(String query);

    @Query("SELECT * FROM video_records WHERE filePath = :filePath LIMIT 1")
    VideoRecordEntity findByFilePath(String filePath);

    @Query("SELECT * FROM video_records WHERE id = :id LIMIT 1")
    VideoRecordEntity getById(long id);

    @Query("UPDATE video_records SET isFavorite = :isFavorite WHERE id = :id")
    void updateFavorite(long id, boolean isFavorite);

    @Query("UPDATE video_records SET isVault = :isVault, filePath = :newPath, uriString = :newUri WHERE id = :id")
    void updateVaultStatus(long id, boolean isVault, String newPath, String newUri);

    @Query("UPDATE video_records SET filename = :newFilename, filePath = :newPath WHERE id = :id")
    void updateFilenameAndPath(long id, String newFilename, String newPath);

    @Query("SELECT * FROM video_records WHERE isFavorite = 0 AND isVault = 0 AND timestamp < :thresholdTime")
    List<VideoRecordEntity> getOldNonFavoriteVideos(long thresholdTime);
}
