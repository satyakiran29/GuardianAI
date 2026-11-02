package com.android.sheguard.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RiskZoneModel implements Serializable {

    public static final String CATEGORY_LOW = "LOW";
    public static final String CATEGORY_MEDIUM = "MEDIUM";
    public static final String CATEGORY_HIGH = "HIGH";

    private String id;
    private String name;
    private double latitude;
    private double longitude;
    private double riskScore;
    private double safetyScore;
    private String riskCategory;
    private double confidence;
    private double circleRadius;
    private String color;
    private List<String> commonCrimes = new ArrayList<>();
    private List<String> riskFactors = new ArrayList<>();
    private double distanceKm;

    public RiskZoneModel() {
    }

    public RiskZoneModel(String id, String name, double latitude, double longitude,
                         double riskScore, double safetyScore, String riskCategory,
                         double confidence, double circleRadius, String color,
                         List<String> commonCrimes, List<String> riskFactors, double distanceKm) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.riskScore = riskScore;
        this.safetyScore = safetyScore;
        this.riskCategory = riskCategory;
        this.confidence = confidence;
        this.circleRadius = circleRadius;
        this.color = color;
        if (commonCrimes != null) this.commonCrimes = commonCrimes;
        if (riskFactors != null) this.riskFactors = riskFactors;
        this.distanceKm = distanceKm;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }

    public double getSafetyScore() { return safetyScore; }
    public void setSafetyScore(double safetyScore) { this.safetyScore = safetyScore; }

    public String getRiskCategory() { return riskCategory; }
    public void setRiskCategory(String riskCategory) { this.riskCategory = riskCategory; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public double getCircleRadius() { return circleRadius; }
    public void setCircleRadius(double circleRadius) { this.circleRadius = circleRadius; }

    public String getColor() { return color != null ? color : (isHighRisk() ? "#EF4444" : (isMediumRisk() ? "#F59E0B" : "#10B981")); }
    public void setColor(String color) { this.color = color; }

    public List<String> getCommonCrimes() { return commonCrimes; }
    public void setCommonCrimes(List<String> commonCrimes) { this.commonCrimes = commonCrimes; }

    public List<String> getRiskFactors() { return riskFactors; }
    public void setRiskFactors(List<String> riskFactors) { this.riskFactors = riskFactors; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public boolean isHighRisk() { return CATEGORY_HIGH.equalsIgnoreCase(riskCategory) || riskScore >= 61.0; }
    public boolean isMediumRisk() { return CATEGORY_MEDIUM.equalsIgnoreCase(riskCategory) || (riskScore >= 31.0 && riskScore <= 60.0); }
    public boolean isLowRisk() { return CATEGORY_LOW.equalsIgnoreCase(riskCategory) || riskScore <= 30.0; }

    public String getFormattedDistance() {
        if (distanceKm < 1.0) {
            return String.format(Locale.US, "%.0f m away", distanceKm * 1000);
        }
        return String.format(Locale.US, "%.1f km away", distanceKm);
    }
}

