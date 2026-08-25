package com.securityrecorder.app.data.model;

/**
 * Storage metrics model holding capacity and partition data.
 */
public class StorageInfo {
    private final long totalInternalBytes;
    private final long freeInternalBytes;
    private final long totalExternalBytes;
    private final long freeExternalBytes;
    private final long recordingsTotalBytes;
    private final int recordingsCount;

    public StorageInfo(long totalInternalBytes, long freeInternalBytes,
                       long totalExternalBytes, long freeExternalBytes,
                       long recordingsTotalBytes, int recordingsCount) {
        this.totalInternalBytes = totalInternalBytes;
        this.freeInternalBytes = freeInternalBytes;
        this.totalExternalBytes = totalExternalBytes;
        this.freeExternalBytes = freeExternalBytes;
        this.recordingsTotalBytes = recordingsTotalBytes;
        this.recordingsCount = recordingsCount;
    }

    public long getTotalInternalBytes() {
        return totalInternalBytes;
    }

    public long getFreeInternalBytes() {
        return freeInternalBytes;
    }

    public long getUsedInternalBytes() {
        return Math.max(0, totalInternalBytes - freeInternalBytes);
    }

    public long getTotalExternalBytes() {
        return totalExternalBytes;
    }

    public long getFreeExternalBytes() {
        return freeExternalBytes;
    }

    public long getRecordingsTotalBytes() {
        return recordingsTotalBytes;
    }

    public int getRecordingsCount() {
        return recordingsCount;
    }

    public boolean isCriticallyLow() {
        // Less than 100 MB free
        return freeInternalBytes < (100L * 1024 * 1024);
    }
}
