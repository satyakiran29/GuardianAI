package com.skdev.guardianai.updater;

import java.io.Serializable;
import java.util.List;

/**
 * Model representing update metadata parsed from update.json.
 */
public class UpdateInfo implements Serializable {
    private int versionCode;
    private String versionName;
    private String appName;
    private String releaseDate;
    private String apkFileName;
    private String downloadUrl;
    private long fileSizeBytes;
    private String fileSizeFormatted;
    private boolean isMandatory;
    private String title;
    private List<String> changelog;

    public int getVersionCode() { return versionCode; }
    public String getVersionName() { return versionName; }
    public String getAppName() { return appName; }
    public String getReleaseDate() { return releaseDate; }
    public String getApkFileName() { return apkFileName; }
    public String getDownloadUrl() { return downloadUrl; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public String getFileSizeFormatted() { return fileSizeFormatted != null ? fileSizeFormatted : "5.96 MB"; }
    public boolean isMandatory() { return isMandatory; }
    public String getTitle() { return title != null ? title : "Update Available"; }
    public List<String> getChangelog() { return changelog; }
}
