package com.skdev.guardianai.data;

import java.io.Serializable;

/**
 * Model representing live crime and hazard alerts.
 */
public class IncidentAlert implements Serializable {
    private String id;
    private String title;
    private String location;
    private String timeAgo;
    private SafetyModelEngine.RiskLevel severity;
    private String crimeType;
    private String advisory;
    private double latitude;
    private double longitude;

    public IncidentAlert(String id, String title, String location, String timeAgo,
                         SafetyModelEngine.RiskLevel severity, String crimeType,
                         String advisory, double latitude, double longitude) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.timeAgo = timeAgo;
        this.severity = severity;
        this.crimeType = crimeType;
        this.advisory = advisory;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getLocation() { return location; }
    public String getTimeAgo() { return timeAgo; }
    public SafetyModelEngine.RiskLevel getSeverity() { return severity; }
    public String getCrimeType() { return crimeType; }
    public String getAdvisory() { return advisory; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
