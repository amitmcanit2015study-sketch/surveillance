package com.securityrecorder.app.data.model;

import java.io.Serializable;

/**
 * Representation of a security media item (Video, Audio, or Image) for UI presentation.
 */
public class VideoItem implements Serializable {
    private long id;
    private String title;
    private String filePath;
    private String uriString;
    private long timestamp;
    private long durationMs;
    private long sizeBytes;
    private boolean isFavorite;
    private boolean isSelected;
    private String resolution;
    private String location;
    private String codec;
    private boolean isVault;
    private String mediaType; // "video", "audio", "image"

    public VideoItem() {}

    public VideoItem(long id, String title, String filePath, String uriString,
                     long timestamp, long durationMs, long sizeBytes,
                     boolean isFavorite, String resolution, String location, String codec) {
        this(id, title, filePath, uriString, timestamp, durationMs, sizeBytes, isFavorite, resolution, location, codec, false, inferMediaType(filePath));
    }

    public VideoItem(long id, String title, String filePath, String uriString,
                     long timestamp, long durationMs, long sizeBytes,
                     boolean isFavorite, String resolution, String location, String codec,
                     boolean isVault, String mediaType) {
        this.id = id;
        this.title = title;
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
        this.mediaType = mediaType != null ? mediaType : inferMediaType(filePath);
    }

    public static String inferMediaType(String path) {
        if (path == null) return "video";
        String lower = path.toLowerCase();
        if (lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".mp3") || lower.endsWith(".wav")) {
            return "audio";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")) {
            return "image";
        }
        return "video";
    }

    public boolean isAudio() {
        return "audio".equalsIgnoreCase(mediaType) || (filePath != null && (filePath.endsWith(".m4a") || filePath.endsWith(".aac") || filePath.endsWith(".mp3")));
    }

    public boolean isImage() {
        return "image".equalsIgnoreCase(mediaType) || (filePath != null && (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg") || filePath.endsWith(".png") || filePath.endsWith(".webp")));
    }

    public boolean isVideo() {
        return !isAudio() && !isImage();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
        if (this.mediaType == null) {
            this.mediaType = inferMediaType(filePath);
        }
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
        isFavorite = favorite;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
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
