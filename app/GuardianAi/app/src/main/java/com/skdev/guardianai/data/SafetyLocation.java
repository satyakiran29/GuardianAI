package com.skdev.guardianai.data;

import java.io.Serializable;

/**
 * Represents a geographical locality with safety attributes and predictions.
 */
public class SafetyLocation implements Serializable {
    private String id;
    private String city;
    private String area;
    private double latitude;
    private double longitude;
    private double lightingScore;
    private double policeDistanceKm;
    private int crimeCount;
    private int crowdDensity;
    private String prevalentCrime;
    private SafetyModelEngine.SafetyPrediction prediction;

    public SafetyLocation(String id, String city, String area, double latitude, double longitude,
                          double lightingScore, double policeDistanceKm, int crimeCount,
                          int crowdDensity, String prevalentCrime) {
        this.id = id;
        this.city = city;
        this.area = area;
        this.latitude = latitude;
        this.longitude = longitude;
        this.lightingScore = lightingScore;
        this.policeDistanceKm = policeDistanceKm;
        this.crimeCount = crimeCount;
        this.crowdDensity = crowdDensity;
        this.prevalentCrime = prevalentCrime;
        this.prediction = SafetyModelEngine.evaluateSafety(lightingScore, policeDistanceKm, crimeCount);
    }

    public String getId() { return id; }
    public String getCity() { return city; }
    public String getArea() { return area; }
    public String getFullName() { return area + ", " + city; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getLightingScore() { return lightingScore; }
    public double getPoliceDistanceKm() { return policeDistanceKm; }
    public int getCrimeCount() { return crimeCount; }
    public int getCrowdDensity() { return crowdDensity; }
    public String getPrevalentCrime() { return prevalentCrime; }
    public SafetyModelEngine.SafetyPrediction getPrediction() { return prediction; }
    public double getSafetyScore() { return prediction != null ? prediction.score : 0.7; }
    public SafetyModelEngine.RiskLevel getRiskLevel() { return prediction != null ? prediction.riskLevel : SafetyModelEngine.RiskLevel.LOW; }
}
