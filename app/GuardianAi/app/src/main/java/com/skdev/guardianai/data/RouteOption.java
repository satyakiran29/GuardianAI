package com.skdev.guardianai.data;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a route alternative evaluated by GuardianAI's ML safety engine.
 */
public class RouteOption implements Serializable {
    public enum RouteType {
        SAFEST("Guardian AI Safest Route", "#10B981", "Optimal well-lit avenues, CCTV coverage, nearest police stations"),
        MODERATE("Moderate Risk Route", "#F59E0B", "Standard transit path with intermediate lighting"),
        HIGH_RISK("High Risk Shortcut", "#EF4444", "Unlit alleys and remote areas with high historical incident count");

        private final String title;
        private final String colorHex;
        private final String description;

        RouteType(String title, String colorHex, String description) {
            this.title = title;
            this.colorHex = colorHex;
            this.description = description;
        }

        public String getTitle() { return title; }
        public String getColorHex() { return colorHex; }
        public String getDescription() { return description; }
    }

    public static class Waypoint implements Serializable {
        public final double lat;
        public final double lng;
        public final String label;
        public final double safetyScore;

        public Waypoint(double lat, double lng, String label, double safetyScore) {
            this.lat = lat;
            this.lng = lng;
            this.label = label;
            this.safetyScore = safetyScore;
        }
    }

    private String id;
    private RouteType type;
    private double distanceKm;
    private int estimatedMinutes;
    private double avgLightingScore;
    private double avgPoliceDistKm;
    private int totalHistoricalCrimes;
    private SafetyModelEngine.SafetyPrediction prediction;
    private List<Waypoint> waypoints;

    public RouteOption(String id, RouteType type, double distanceKm, int estimatedMinutes,
                       double avgLightingScore, double avgPoliceDistKm, int totalHistoricalCrimes,
                       List<Waypoint> waypoints) {
        this.id = id;
        this.type = type;
        this.distanceKm = distanceKm;
        this.estimatedMinutes = estimatedMinutes;
        this.avgLightingScore = avgLightingScore;
        this.avgPoliceDistKm = avgPoliceDistKm;
        this.totalHistoricalCrimes = totalHistoricalCrimes;
        this.waypoints = waypoints;
        this.prediction = SafetyModelEngine.evaluateSafety(avgLightingScore, avgPoliceDistKm, totalHistoricalCrimes);
    }

    public String getId() { return id; }
    public RouteType getType() { return type; }
    public double getDistanceKm() { return distanceKm; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public double getAvgLightingScore() { return avgLightingScore; }
    public double getAvgPoliceDistKm() { return avgPoliceDistKm; }
    public int getTotalHistoricalCrimes() { return totalHistoricalCrimes; }
    public SafetyModelEngine.SafetyPrediction getPrediction() { return prediction; }
    public List<Waypoint> getWaypoints() { return waypoints; }
    public double getSafetyScore() { return prediction != null ? prediction.score : 0.75; }
}
