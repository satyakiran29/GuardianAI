package com.skdev.guardianai.media;

import java.io.Serializable;

/**
 * Model representing an emergency evidence record (Photo, Video, or Audio).
 */
public class MediaItem implements Serializable {
    public enum MediaType {
        PHOTO, VIDEO, AUDIO
    }

    private String id;
    private MediaType type;
    private String title;
    private String filePath;
    private String timestamp;
    private String locationLabel;
    private long fileSizeBytes;

    public MediaItem(String id, MediaType type, String title, String filePath,
                     String timestamp, String locationLabel, long fileSizeBytes) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.filePath = filePath;
        this.timestamp = timestamp;
        this.locationLabel = locationLabel;
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getId() { return id; }
    public MediaType getType() { return type; }
    public String getTitle() { return title; }
    public String getFilePath() { return filePath; }
    public String getTimestamp() { return timestamp; }
    public String getLocationLabel() { return locationLabel; }
    public long getFileSizeBytes() { return fileSizeBytes; }

    public String getFormattedSize() {
        if (fileSizeBytes <= 0) return "0 KB";
        if (fileSizeBytes < 1024 * 1024) {
            return (fileSizeBytes / 1024) + " KB";
        }
        return String.format(java.util.Locale.US, "%.1f MB", fileSizeBytes / (1024.0 * 1024.0));
    }
}
