package com.securityrecorder.app.data.local;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room database entity storing recorded security media (video, audio, photo) metadata and vault status.
 */
@Entity(tableName = "video_records", indices = {@Index(value = {"filePath"}, unique = true)})
public class VideoRecordEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String filename;
    private String filePath;
    private String uriString;
    private long timestamp;
    private long durationMs;
    private long sizeBytes;
    private boolean isFavorite;
    private String resolution;
    private String location;
    private String codec;
    private boolean isVault;
    private String mediaType; // "video", "audio", "image"

    public VideoRecordEntity(String filename, String filePath, String uriString,
                             long timestamp, long durationMs, long sizeBytes,
                             boolean isFavorite, String resolution, String location, String codec,
                             boolean isVault, String mediaType) {
        this.filename = filename;
        this.filePath = filePath;
        this.uriString = uriString;
        this.timestamp = timestamp;
        this.durationMs = durationMs;
        this.sizeBytes = sizeBytes;
        this.isFavorite = isFavorite;
        this.resolution = resolution;
        this.location = location;
        this.codec = codec;
        this.isVault = isVault;
        this.mediaType = mediaType;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getUriString() {
        return uriString;
    }

    public void setUriString(String uriString) {
        this.uriString = uriString;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        this.isFavorite = favorite;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public boolean isVault() {
        return isVault;
    }

    public void setVault(boolean vault) {
        isVault = vault;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }
}
