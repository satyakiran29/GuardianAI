package com.skdev.guardianai.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Embedded repository containing geospatial safety datasets for 10 Indian metropolitan cities,
 * city matrices, route generation algorithms, and real-time hazard alerts.
 */
public class SafetyDataRepository {

    private static SafetyDataRepository instance;
    private final List<SafetyLocation> locations = new ArrayList<>();
    private final List<IncidentAlert> liveAlerts = new ArrayList<>();
    private final List<String> cities = Arrays.asList(
            "Bengaluru", "Bhopal", "Chennai", "Delhi", "Hyderabad",
            "Jaipur", "Kolkata", "Lucknow", "Mumbai", "Patna"
    );

    private SafetyDataRepository() {
        populateLocations();
        populateLiveAlerts();
    }

    public static synchronized SafetyDataRepository getInstance() {
        if (instance == null) {
            instance = new SafetyDataRepository();
        }
        return instance;
    }

    public List<String> getCities() {
        return cities;
    }

    public List<SafetyLocation> getAllLocations() {
        return new ArrayList<>(locations);
    }

    public List<SafetyLocation> getLocationsForCity(String city) {
        List<SafetyLocation> result = new ArrayList<>();
        for (SafetyLocation loc : locations) {
            if (loc.getCity().equalsIgnoreCase(city)) {
                result.add(loc);
            }
        }
        return result;
    }

    public SafetyLocation findLocationByName(String query) {
        if (query == null || query.trim().isEmpty()) return null;
        String q = query.trim().toLowerCase(Locale.ROOT);
        for (SafetyLocation loc : locations) {
            if (loc.getArea().toLowerCase(Locale.ROOT).contains(q) ||
                loc.getFullName().toLowerCase(Locale.ROOT).contains(q)) {
                return loc;
            }
        }
        return null;
    }

    public List<SafetyLocation> searchLocations(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(locations.subList(0, Math.min(10, locations.size())));
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<SafetyLocation> results = new ArrayList<>();
        for (SafetyLocation loc : locations) {
            if (loc.getArea().toLowerCase(Locale.ROOT).contains(q) ||
                loc.getCity().toLowerCase(Locale.ROOT).contains(q) ||
                loc.getPrevalentCrime().toLowerCase(Locale.ROOT).contains(q)) {
                results.add(loc);
            }
        }
        return results;
    }

    public List<IncidentAlert> getLiveAlerts() {
        return new ArrayList<>(liveAlerts);
    }

    /**
     * Generates 3 realistic alternative route paths between start and destination,
     * evaluating them with the GuardianAI ML regression equation.
     */
    public List<RouteOption> generateRoutes(SafetyLocation origin, SafetyLocation destination) {
        List<RouteOption> routes = new ArrayList<>();
        if (origin == null || destination == null) {
            return routes;
        }

        double oLat = origin.getLatitude();
        double oLng = origin.getLongitude();
        double dLat = destination.getLatitude();
        double dLng = destination.getLongitude();

        // Calculate direct Euclidean approximate distance in KM
        double approxDist = Math.hypot((dLat - oLat) * 111.0, (dLng - oLng) * 111.0);
        approxDist = Math.max(2.5, approxDist);

        // 1. SAFEST ROUTE (Well lit avenues, CCTV, near police stations)
        double safeDist = approxDist * 1.15; // slightly longer via main avenues
        int safeTime = (int) Math.round(safeDist * 2.8);
        double safeLighting = Math.min(9.5, Math.max(7.5, (origin.getLightingScore() + destination.getLightingScore()) / 2.0 + 2.0));
        double safePoliceDist = Math.max(0.6, (origin.getPoliceDistanceKm() + destination.getPoliceDistanceKm()) / 2.0 * 0.4);
        int safeCrimes = Math.max(5, (int)((origin.getCrimeCount() + destination.getCrimeCount()) * 0.3));

        List<RouteOption.Waypoint> safeWaypoints = new ArrayList<>();
        safeWaypoints.add(new RouteOption.Waypoint(oLat, oLng, origin.getArea() + " (Start)", origin.getSafetyScore()));
        safeWaypoints.add(new RouteOption.Waypoint(oLat + (dLat - oLat) * 0.35 + 0.005, oLng + (dLng - oLng) * 0.25 - 0.004, "Main Blvd / Metro Corridor", 0.92));
        safeWaypoints.add(new RouteOption.Waypoint(oLat + (dLat - oLat) * 0.70 + 0.004, oLng + (dLng - oLng) * 0.75 + 0.003, "Police Checkpoint / Well-Lit Plaza", 0.89));
        safeWaypoints.add(new RouteOption.Waypoint(dLat, dLng, destination.getArea() + " (Destination)", destination.getSafetyScore()));

        routes.add(new RouteOption(
                "route_safe",
                RouteOption.RouteType.SAFEST,
                Math.round(safeDist * 10.0) / 10.0,
                safeTime,
                safeLighting,
                safePoliceDist,
                safeCrimes,
                safeWaypoints
        ));

        // 2. MODERATE ROUTE (Standard urban route)
        double modDist = approxDist * 1.05;
        int modTime = (int) Math.round(modDist * 2.5);
        double modLighting = Math.min(6.5, Math.max(4.5, (origin.getLightingScore() + destination.getLightingScore()) / 2.0));
        double modPoliceDist = Math.max(2.0, (origin.getPoliceDistanceKm() + destination.getPoliceDistanceKm()) / 2.0);
        int modCrimes = Math.max(20, (int)((origin.getCrimeCount() + destination.getCrimeCount()) * 0.7));

        List<RouteOption.Waypoint> modWaypoints = new ArrayList<>();
        modWaypoints.add(new RouteOption.Waypoint(oLat, oLng, origin.getArea() + " (Start)", origin.getSafetyScore()));
        modWaypoints.add(new RouteOption.Waypoint(oLat + (dLat - oLat) * 0.50, oLng + (dLng - oLng) * 0.50, "Central Market / Transit Hub", 0.68));
        modWaypoints.add(new RouteOption.Waypoint(dLat, dLng, destination.getArea() + " (Destination)", destination.getSafetyScore()));

        routes.add(new RouteOption(
                "route_moderate",
                RouteOption.RouteType.MODERATE,
                Math.round(modDist * 10.0) / 10.0,
                modTime,
                modLighting,
                modPoliceDist,
                modCrimes,
                modWaypoints
        ));

        // 3. HIGH RISK SHORTCUT (Direct back alleys, dark lanes, remote)
        double riskDist = approxDist * 0.88; // shorter shortcut
        int riskTime = (int) Math.round(riskDist * 2.2);
        double riskLighting = Math.min(3.2, Math.max(1.5, (origin.getLightingScore() + destination.getLightingScore()) / 2.0 - 2.5));
        double riskPoliceDist = Math.min(8.5, Math.max(5.5, (origin.getPoliceDistanceKm() + destination.getPoliceDistanceKm()) / 2.0 + 3.0));
        int riskCrimes = Math.max(55, (int)((origin.getCrimeCount() + destination.getCrimeCount()) * 1.3));

        List<RouteOption.Waypoint> riskWaypoints = new ArrayList<>();
        riskWaypoints.add(new RouteOption.Waypoint(oLat, oLng, origin.getArea() + " (Start)", origin.getSafetyScore()));
        riskWaypoints.add(new RouteOption.Waypoint(oLat + (dLat - oLat) * 0.40 - 0.008, oLng + (dLng - oLng) * 0.45 + 0.007, "Unlit Industrial Lane / Isolated Bypass", 0.38));
        riskWaypoints.add(new RouteOption.Waypoint(oLat + (dLat - oLat) * 0.75 - 0.006, oLng + (dLng - oLng) * 0.80 - 0.005, "Deserted Railway Underpass", 0.28));
        riskWaypoints.add(new RouteOption.Waypoint(dLat, dLng, destination.getArea() + " (Destination)", destination.getSafetyScore()));

        routes.add(new RouteOption(
                "route_risky",
                RouteOption.RouteType.HIGH_RISK,
                Math.round(riskDist * 10.0) / 10.0,
                riskTime,
                riskLighting,
                riskPoliceDist,
                riskCrimes,
                riskWaypoints
        ));

        return routes;
    }

    private void populateLocations() {
        // Bengaluru Localities
        locations.add(new SafetyLocation("blr_1", "Bengaluru", "Indiranagar", 12.9784, 77.6408, 8.8, 1.2, 12, 650, "Night Safety Complaint"));
        locations.add(new SafetyLocation("blr_2", "Bengaluru", "Koramangala", 12.9352, 77.6245, 8.4, 1.5, 18, 720, "Harassment"));
        locations.add(new SafetyLocation("blr_3", "Bengaluru", "Whitefield", 12.9698, 77.7500, 5.3, 4.1, 48, 380, "Stalking"));
        locations.add(new SafetyLocation("blr_4", "Bengaluru", "Electronic City", 12.8452, 77.6602, 6.2, 3.8, 35, 420, "Unsafe Transport"));
        locations.add(new SafetyLocation("blr_5", "Bengaluru", "Yelahanka", 13.1007, 77.5963, 5.5, 4.2, 42, 310, "Night Safety Complaint"));
        locations.add(new SafetyLocation("blr_6", "Bengaluru", "MG Road / Brigade", 12.9756, 77.6066, 9.2, 0.8, 14, 920, "Verbal Abuse"));

        // Mumbai Localities
        locations.add(new SafetyLocation("mum_1", "Mumbai", "Bandra West", 19.0596, 72.8295, 8.6, 1.1, 15, 850, "Stalking"));
        locations.add(new SafetyLocation("mum_2", "Mumbai", "Dadar", 19.0178, 72.8478, 5.2, 4.3, 54, 910, "Chain Snatching"));
        locations.add(new SafetyLocation("mum_3", "Mumbai", "Andheri East", 19.1136, 72.8697, 5.4, 4.2, 49, 780, "Night Safety Complaint"));
        locations.add(new SafetyLocation("mum_4", "Mumbai", "Kurla", 19.0726, 72.8845, 3.8, 5.8, 62, 890, "Assault"));
        locations.add(new SafetyLocation("mum_5", "Mumbai", "Colaba", 18.9067, 72.8147, 8.9, 1.0, 11, 600, "Verbal Abuse"));

        // Delhi Localities
        locations.add(new SafetyLocation("del_1", "Delhi", "Connaught Place", 28.6315, 77.2167, 9.1, 0.9, 16, 950, "Verbal Abuse"));
        locations.add(new SafetyLocation("del_2", "Delhi", "Hauz Khas", 28.5494, 77.2001, 8.2, 1.6, 22, 680, "Harassment"));
        locations.add(new SafetyLocation("del_3", "Delhi", "Rohini", 28.7495, 77.0565, 4.6, 4.9, 52, 530, "Unsafe Transport"));
        locations.add(new SafetyLocation("del_4", "Delhi", "Dwarka", 28.5921, 77.0460, 6.0, 3.5, 38, 490, "Night Safety Complaint"));
        locations.add(new SafetyLocation("del_5", "Delhi", "Lajpat Nagar", 28.5700, 77.2400, 7.8, 2.1, 28, 800, "Chain Snatching"));

        // Hyderabad Localities
        locations.add(new SafetyLocation("hyd_1", "Hyderabad", "Banjara Hills", 17.4156, 78.4357, 8.7, 1.3, 14, 520, "Verbal Abuse"));
        locations.add(new SafetyLocation("hyd_2", "Hyderabad", "Gachibowli", 17.4401, 78.3489, 5.3, 4.1, 46, 410, "Night Safety Complaint"));
        locations.add(new SafetyLocation("hyd_3", "Hyderabad", "HITEC City", 17.4474, 78.3762, 8.5, 1.8, 19, 760, "Stalking"));
        locations.add(new SafetyLocation("hyd_4", "Hyderabad", "Secunderabad", 17.4399, 78.4983, 7.1, 2.4, 33, 670, "Chain Snatching"));
        locations.add(new SafetyLocation("hyd_5", "Hyderabad", "Charminar / Old City", 17.3616, 78.4747, 6.4, 2.9, 44, 980, "Harassment"));

        // Jaipur Localities
        locations.add(new SafetyLocation("jpr_1", "Jaipur", "C-Scheme", 26.9075, 75.7985, 8.5, 1.4, 15, 460, "Verbal Abuse"));
        locations.add(new SafetyLocation("jpr_2", "Jaipur", "Vaishali Nagar", 26.9124, 75.7433, 4.2, 5.5, 58, 380, "Kidnapping"));
        locations.add(new SafetyLocation("jpr_3", "Jaipur", "Malviya Nagar", 26.8549, 75.8243, 6.1, 3.9, 39, 510, "Chain Snatching"));
        locations.add(new SafetyLocation("jpr_4", "Jaipur", "Mansarovar", 26.8644, 75.7687, 5.8, 4.0, 41, 490, "Night Safety Complaint"));

        // Chennai Localities
        locations.add(new SafetyLocation("chn_1", "Chennai", "Anna Nagar", 13.0850, 80.2101, 8.7, 1.5, 16, 610, "Harassment"));
        locations.add(new SafetyLocation("chn_2", "Chennai", "T. Nagar", 13.0418, 80.2341, 8.1, 1.9, 26, 920, "Chain Snatching"));
        locations.add(new SafetyLocation("chn_3", "Chennai", "Tambaram", 12.9249, 80.1000, 5.6, 4.4, 45, 430, "Unsafe Transport"));
        locations.add(new SafetyLocation("chn_4", "Chennai", "Velachery", 12.9815, 80.2180, 7.3, 2.7, 30, 580, "Stalking"));

        // Kolkata Localities
        locations.add(new SafetyLocation("kol_1", "Kolkata", "Park Street", 22.5535, 88.3516, 9.0, 1.0, 14, 880, "Domestic Violence"));
        locations.add(new SafetyLocation("kol_2", "Kolkata", "Dum Dum", 22.6420, 88.4312, 4.8, 5.1, 56, 670, "Assault"));
        locations.add(new SafetyLocation("kol_3", "Kolkata", "Salt Lake Sector V", 22.5804, 88.4378, 8.6, 1.7, 18, 590, "Cyber Crime"));
        locations.add(new SafetyLocation("kol_4", "Kolkata", "Howrah", 22.5958, 88.2636, 5.1, 4.8, 51, 940, "Chain Snatching"));

        // Lucknow Localities
        locations.add(new SafetyLocation("lko_1", "Lucknow", "Hazratganj", 26.8467, 80.9462, 8.8, 1.2, 17, 750, "Harassment"));
        locations.add(new SafetyLocation("lko_2", "Lucknow", "Aliganj", 26.8893, 80.9431, 4.9, 4.8, 53, 440, "Stalking"));
        locations.add(new SafetyLocation("lko_3", "Lucknow", "Gomti Nagar", 26.8530, 80.9984, 8.4, 1.8, 20, 580, "Night Safety Complaint"));

        // Bhopal Localities
        locations.add(new SafetyLocation("bhp_1", "Bhopal", "MP Nagar", 23.2332, 77.4343, 8.3, 1.6, 18, 620, "Verbal Abuse"));
        locations.add(new SafetyLocation("bhp_2", "Bhopal", "Kolar", 23.1765, 77.4182, 5.2, 4.5, 44, 310, "Night Safety Complaint"));
        locations.add(new SafetyLocation("bhp_3", "Bhopal", "Arera Colony", 23.2156, 77.4358, 8.6, 1.4, 15, 480, "Cyber Crime"));

        // Patna Localities
        locations.add(new SafetyLocation("pat_1", "Patna", "Boring Road", 25.6174, 85.1221, 7.9, 2.2, 25, 700, "Stalking"));
        locations.add(new SafetyLocation("pat_2", "Patna", "Rajendra Nagar", 25.6015, 85.1582, 5.0, 4.7, 52, 610, "Chain Snatching"));
        locations.add(new SafetyLocation("pat_3", "Patna", "Danapur", 25.6333, 85.0500, 4.5, 5.6, 59, 450, "Kidnapping"));
    }

    private void populateLiveAlerts() {
        liveAlerts.add(new IncidentAlert(
                "alert_1",
                "Poor Illumination Alert",
                "Vaishali Nagar (Sector 4)",
                "8 mins ago",
                SafetyModelEngine.RiskLevel.HIGH,
                "Lighting Hazard",
                "Street lighting failure reported. GuardianAI dynamic routing suggests main avenue detour.",
                26.9124, 75.7433
        ));
        liveAlerts.add(new IncidentAlert(
                "alert_2",
                "Police Patrol Active",
                "Indiranagar 100ft Road",
                "15 mins ago",
                SafetyModelEngine.RiskLevel.LOW,
                "Patrol Checkpoint",
                "Active police PCR vans stationed near metro junction. Enhanced safety zone.",
                12.9784, 77.6408
        ));
        liveAlerts.add(new IncidentAlert(
                "alert_3",
                "Elevated Night Risk Zone",
                "Dadar Railway Sub-Lane",
                "22 mins ago",
                SafetyModelEngine.RiskLevel.CRITICAL,
                "Night Incident",
                "Chain snatching attempt reported near unlit lane. Avoid foot transit after 10 PM.",
                19.0178, 72.8478
        ));
        liveAlerts.add(new IncidentAlert(
                "alert_4",
                "Well-Lit Safe Corridor",
                "Connaught Place Inner Circle",
                "35 mins ago",
                SafetyModelEngine.RiskLevel.LOW,
                "CCTV Active Zone",
                "Full high-lux illumination and continuous CCTV coverage operational.",
                28.6315, 77.2167
        ));
        liveAlerts.add(new IncidentAlert(
                "alert_5",
                "Transit Advisory",
                "Gachibowli Outer Ring Road",
                "45 mins ago",
                SafetyModelEngine.RiskLevel.MEDIUM,
                "Transport Advisory",
                "Low crowd density reported. Use verified cabs and keep GuardianAI Live SOS active.",
                17.4401, 78.3489
        ));
    }
}
