package com.android.sheguard.ui.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.sheguard.R;
import com.android.sheguard.api.ApiClient;
import com.android.sheguard.databinding.FragmentRiskZoneBinding;
import com.android.sheguard.model.RiskZoneModel;
import com.android.sheguard.ui.adapter.RiskZoneAdapter;
import com.android.sheguard.util.LocationHelper;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class RiskZoneFragment extends Fragment implements RiskZoneAdapter.OnRiskZoneClickListener {

    private static final String TAG = "RiskZoneFragment";

    private FragmentRiskZoneBinding binding;
    private RiskZoneAdapter adapter;
    private final List<RiskZoneModel> allRiskZones = new ArrayList<>();
    private final List<RiskZoneModel> filteredRiskZones = new ArrayList<>();

    private double currentLatitude = 18.4550;
    private double currentLongitude = 83.6600;
    private float currentAccuracy = 15.0f;

    private boolean isMapReady = false;
    private boolean isHeatmapActive = false;
    private String activeFilter = "ALL"; // ALL, HIGH, MEDIUM, LOW

    private LocationHelper.LocationTrackingListener trackingListener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fine = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarse = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (Boolean.TRUE.equals(fine) || Boolean.TRUE.equals(coarse)) {
                    startContinuousLocationTracking();
                } else {
                    if (binding != null) {
                        binding.tvGpsTelemetryStatus.setText("Location permission required for live radar");
                    }
                }
            });

    private final String HTML_MAP_LEAFLET = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta charset=\"utf-8\" />\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\" />\n" +
            "    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />\n" +
            "    <script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>\n" +
            "    <script src=\"https://unpkg.com/leaflet.heat@0.2.0/dist/leaflet-heat.js\"></script>\n" +
            "    <style>\n" +
            "        html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; background: #0A0E1A; }\n" +
            "        .user-pulse-marker {\n" +
            "            width: 16px; height: 16px;\n" +
            "            background: #10B981;\n" +
            "            border-radius: 50%;\n" +
            "            border: 2px solid #FFFFFF;\n" +
            "            box-shadow: 0 0 16px #10B981;\n" +
            "        }\n" +
            "        .leaflet-popup-content-wrapper {\n" +
            "            background: #131A2B !important; color: #FFFFFF !important;\n" +
            "            border: 1px solid #2A3650; border-radius: 12px;\n" +
            "            box-shadow: 0 4px 20px rgba(0,0,0,0.6);\n" +
            "        }\n" +
            "        .leaflet-popup-tip { background: #131A2B !important; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div id=\"map\"></div>\n" +
            "    <script>\n" +
            "        let map = L.map('map', { zoomControl: false, attributionControl: false }).setView([18.455, 83.660], 14);\n" +
            "        L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}', { maxZoom: 19 }).addTo(map);\n" +
            "        L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Reference/MapServer/tile/{z}/{y}/{x}', { maxZoom: 19 }).addTo(map);\n" +
            "\n" +
            "        let userMarker = null;\n" +
            "        let accuracyCircle = null;\n" +
            "        let zoneLayers = [];\n" +
            "        let heatLayer = null;\n" +
            "\n" +
            "        window.initUserLocation = function(lat, lng, acc) {\n" +
            "            if (!userMarker) {\n" +
            "                const icon = L.divIcon({ className: 'user-pulse-marker', iconSize: [16, 16], iconAnchor: [8, 8] });\n" +
            "                userMarker = L.marker([lat, lng], { icon, zIndexOffset: 1000 }).addTo(map);\n" +
            "                userMarker.bindPopup('<b>📍 My Live Location</b>');\n" +
            "            } else {\n" +
            "                userMarker.setLatLng([lat, lng]);\n" +
            "            }\n" +
            "            if (acc && acc > 0) {\n" +
            "                if (!accuracyCircle) {\n" +
            "                    accuracyCircle = L.circle([lat, lng], { radius: acc, color: '#10B981', weight: 1, fillColor: '#10B981', fillOpacity: 0.12 }).addTo(map);\n" +
            "                } else {\n" +
            "                    accuracyCircle.setLatLng([lat, lng]);\n" +
            "                    accuracyCircle.setRadius(acc);\n" +
            "                }\n" +
            "            }\n" +
            "        };\n" +
            "\n" +
            "        window.recenterMap = function(lat, lng) {\n" +
            "            map.setView([lat, lng], 15, { animate: true });\n" +
            "        };\n" +
            "\n" +
            "        window.renderRiskZones = function(zonesData) {\n" +
            "            zoneLayers.forEach(l => map.removeLayer(l));\n" +
            "            zoneLayers = [];\n" +
            "            if (!zonesData) return;\n" +
            "\n" +
            "            zonesData.forEach(z => {\n" +
            "                const color = z.color || (z.risk_score >= 61 ? '#EF4444' : (z.risk_score >= 31 ? '#F59E0B' : '#10B981'));\n" +
            "                const r = z.circle_radius || (z.risk_score >= 61 ? 160 : (z.risk_score >= 31 ? 120 : 90));\n" +
            "                const circle = L.circle([z.latitude, z.longitude], {\n" +
            "                    radius: r,\n" +
            "                    color: color,\n" +
            "                    weight: 2,\n" +
            "                    opacity: 0.85,\n" +
            "                    fillColor: color,\n" +
            "                    fillOpacity: 0.25,\n" +
            "                    dashArray: z.risk_score < 61 ? '4, 4' : null\n" +
            "                }).addTo(map);\n" +
            "\n" +
            "                circle.on('click', () => {\n" +
            "                    if (window.AndroidBridge) AndroidBridge.onZoneClicked(JSON.stringify(z));\n" +
            "                });\n" +
            "\n" +
            "                const popupText = '<b>' + (z.risk_score >= 61 ? '🔴 High Risk' : (z.risk_score >= 31 ? '🟡 Caution' : '🟢 Low Risk')) + '</b><br>' +\n" +
            "                    '<b>' + z.name + '</b><br>' +\n" +
            "                    'Risk Score: ' + Math.round(z.risk_score) + '% • Conf: ' + Math.round(z.confidence * 100) + '%<br>' +\n" +
            "                    '<i>Radius: ~' + r + 'm</i>';\n" +
            "                circle.bindPopup(popupText);\n" +
            "                zoneLayers.push(circle);\n" +
            "            });\n" +
            "        };\n" +
            "\n" +
            "        window.renderHeatmap = function(points, visible) {\n" +
            "            if (heatLayer) map.removeLayer(heatLayer);\n" +
            "            heatLayer = null;\n" +
            "            if (!visible || !points || points.length === 0) return;\n" +
            "            heatLayer = L.heatLayer(points, {\n" +
            "                radius: 25,\n" +
            "                blur: 15,\n" +
            "                maxZoom: 17,\n" +
            "                gradient: { 0.2: '#10B981', 0.5: '#F59E0B', 0.8: '#EF4444', 1.0: '#DC2626' }\n" +
            "            }).addTo(map);\n" +
            "        };\n" +
            "\n" +
            "        map.on('click', function(e) {\n" +
            "            if (window.AndroidBridge) AndroidBridge.onMapTapped(e.latlng.lat, e.latlng.lng);\n" +
            "        });\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>";

    private class RiskMapJsInterface {
        @JavascriptInterface
        public void onZoneClicked(String zoneJsonStr) {
            mainHandler.post(() -> {
                try {
                    RiskZoneModel model = new Gson().fromJson(zoneJsonStr, RiskZoneModel.class);
                    populateDetailsCard(model);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing clicked zone: " + e.getMessage());
                }
            });
        }

        @JavascriptInterface
        public void onMapTapped(double lat, double lng) {
            mainHandler.post(() -> evaluateCustomCoordinateRisk(lat, lng));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRiskZoneBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initRecyclerView();
        initWebViewMap();
        initListeners();

        // Immediate fast-path: render cached location & offline baseline in 0ms
        Context ctx = getContext();
        if (ctx != null) {
            Location best = LocationHelper.getBestLastKnownLocation(ctx);
            if (best != null) {
                currentLatitude = best.getLatitude();
                currentLongitude = best.getLongitude();
                currentAccuracy = best.hasAccuracy() ? best.getAccuracy() : 15.0f;
            }
        }

        buildOfflineRegionalRiskZones();
        applyFilterAndRefreshUI();

        // Check permissions and activate continuous real-time GPS telemetry
        if (getContext() != null && LocationHelper.hasLocationPermission(getContext())) {
            startContinuousLocationTracking();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }

        // Fetch fresh online ML predictions from Django backend
        fetchRiskZonesFromBackend();
    }

    private void initRecyclerView() {
        adapter = new RiskZoneAdapter(requireContext(), filteredRiskZones, this);
        binding.rvNearbyRiskZones.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNearbyRiskZones.setAdapter(adapter);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebViewMap() {
        WebSettings ws = binding.webViewRiskMap.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);

        binding.webViewRiskMap.addJavascriptInterface(new RiskMapJsInterface(), "AndroidBridge");
        binding.webViewRiskMap.setWebChromeClient(new WebChromeClient());
        binding.webViewRiskMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                isMapReady = true;
                updateMapUserLocation();
                updateMapRiskZones();
            }
        });

        binding.webViewRiskMap.loadDataWithBaseURL("https://guardianai.local", HTML_MAP_LEAFLET, "text/html", "UTF-8", null);
    }

    private void initListeners() {
        binding.btnBack.setOnClickListener(v -> {
            if (isAdded()) Navigation.findNavController(v).navigateUp();
        });

        binding.btnRecenter.setOnClickListener(v -> {
            if (isMapReady && binding != null) {
                binding.webViewRiskMap.evaluateJavascript(
                        String.format(Locale.US, "window.recenterMap(%.5f, %.5f);", currentLatitude, currentLongitude),
                        null
                );
            }
        });

        binding.btnRefresh.setOnClickListener(v -> {
            fetchRiskZonesFromBackend();
            Snackbar.make(binding.getRoot(), "Refreshing AI Risk Zone telemetry...", Snackbar.LENGTH_SHORT).show();
        });

        binding.chipFilterAll.setOnClickListener(v -> { activeFilter = "ALL"; applyFilterAndRefreshUI(); });
        binding.chipFilterHigh.setOnClickListener(v -> { activeFilter = "HIGH"; applyFilterAndRefreshUI(); });
        binding.chipFilterMedium.setOnClickListener(v -> { activeFilter = "MEDIUM"; applyFilterAndRefreshUI(); });
        binding.chipFilterLow.setOnClickListener(v -> { activeFilter = "LOW"; applyFilterAndRefreshUI(); });

        binding.chipToggleHeatmap.setOnClickListener(v -> {
            isHeatmapActive = !isHeatmapActive;
            binding.chipToggleHeatmap.setChecked(isHeatmapActive);
            if (isHeatmapActive) {
                fetchHeatmapData();
            } else {
                if (isMapReady && binding != null) {
                    binding.webViewRiskMap.evaluateJavascript("window.renderHeatmap([], false);", null);
                }
            }
        });
    }

    private void startContinuousLocationTracking() {
        if (!isAdded() || getContext() == null) return;
        if (!LocationHelper.hasLocationPermission(requireContext())) return;

        if (trackingListener == null) {
            trackingListener = new LocationHelper.LocationTrackingListener() {
                @Override
                public void onLocationUpdated(Location location, double latitude, double longitude, float accuracy, float speed, float bearing) {
                    if (!isAdded() || binding == null) return;
                    currentLatitude = latitude;
                    currentLongitude = longitude;
                    currentAccuracy = accuracy;

                    binding.tvGpsTelemetryStatus.setText(String.format(Locale.US, "GPS Locked • ±%.0fm", accuracy));
                    updateMapUserLocation();

                    // Recalculate distance to each risk zone live
                    for (RiskZoneModel zone : allRiskZones) {
                        zone.setDistanceKm(calculateHaversineDistance(currentLatitude, currentLongitude, zone.getLatitude(), zone.getLongitude()));
                    }
                    Collections.sort(allRiskZones, (a, b) -> Double.compare(b.getRiskScore(), a.getRiskScore()));
                    applyFilterAndRefreshUI();
                }

                @Override
                public void onLocationError(String error) {
                    Log.w(TAG, "Location error: " + error);
                }
            };
        }

        LocationHelper.startLocationTracking(requireContext(), trackingListener);
    }

    private void stopContinuousLocationTracking() {
        if (trackingListener != null && getContext() != null) {
            LocationHelper.stopLocationTracking(requireContext(), trackingListener);
        }
    }

    private void updateMapUserLocation() {
        if (isMapReady && binding != null) {
            binding.webViewRiskMap.evaluateJavascript(
                    String.format(Locale.US, "window.initUserLocation(%.5f, %.5f, %.1f);",
                            currentLatitude, currentLongitude, currentAccuracy), null
            );
        }
    }

    private void updateMapRiskZones() {
        if (isMapReady && binding != null) {
            String zonesJson = new Gson().toJson(filteredRiskZones);
            binding.webViewRiskMap.evaluateJavascript(
                    String.format(Locale.US, "window.renderRiskZones(%s);", zonesJson), null
            );
        }
    }

    private void fetchRiskZonesFromBackend() {
        if (binding == null) return;
        binding.pbMapLoading.setVisibility(View.VISIBLE);

        ApiClient.getRiskZones(currentLatitude, currentLongitude, 25.0, (success, data, message) -> {
            if (!isAdded() || binding == null) return;
            binding.pbMapLoading.setVisibility(View.GONE);

            if (success && data != null && data.has("zones")) {
                allRiskZones.clear();
                JsonArray array = data.getAsJsonArray("zones");
                for (JsonElement el : array) {
                    if (!el.isJsonObject()) continue;
                    JsonObject obj = el.getAsJsonObject();
                    RiskZoneModel model = parseRiskZoneModel(obj);
                    allRiskZones.add(model);
                }
                applyFilterAndRefreshUI();
            }
        });
    }

    private void fetchHeatmapData() {
        if (binding == null) return;
        binding.pbMapLoading.setVisibility(View.VISIBLE);

        ApiClient.getRiskHeatmap(currentLatitude, currentLongitude, 15.0, (success, data, message) -> {
            if (!isAdded() || binding == null) return;
            binding.pbMapLoading.setVisibility(View.GONE);

            if (success && data != null && data.has("heatmap_points")) {
                JsonArray points = data.getAsJsonArray("heatmap_points");
                String pointsJson = new Gson().toJson(points);
                if (isMapReady) {
                    binding.webViewRiskMap.evaluateJavascript(
                            String.format(Locale.US, "window.renderHeatmap(%s, true);", pointsJson), null
                    );
                }
            }
        });
    }

    private void evaluateCustomCoordinateRisk(double lat, double lng) {
        if (binding == null) return;
        binding.pbMapLoading.setVisibility(View.VISIBLE);

        ApiClient.predictLocationRisk(lat, lng, "Night", (success, data, message) -> {
            if (!isAdded() || binding == null) return;
            binding.pbMapLoading.setVisibility(View.GONE);

            if (success && data != null && data.has("risk_assessment")) {
                JsonObject assessment = data.getAsJsonObject("risk_assessment");
                JsonObject hist = data.has("historical_data") ? data.getAsJsonObject("historical_data") : new JsonObject();
                JsonArray factorsArr = data.has("risk_factors") ? data.getAsJsonArray("risk_factors") : new JsonArray();

                double score = assessment.get("risk_score").getAsDouble();
                String category = assessment.get("risk_category").getAsString();
                double confidence = assessment.has("prediction_confidence") ? assessment.get("prediction_confidence").getAsDouble() : 0.88;

                List<String> factors = new ArrayList<>();
                for (JsonElement f : factorsArr) factors.add(f.getAsString());

                List<String> crimes = new ArrayList<>();
                if (hist.has("common_crime_types")) {
                    for (JsonElement c : hist.getAsJsonArray("common_crime_types")) crimes.add(c.getAsString());
                }

                RiskZoneModel model = new RiskZoneModel(
                        "tapped_" + System.currentTimeMillis(),
                        String.format(Locale.US, "Custom Point (%.4f, %.4f)", lat, lng),
                        lat, lng, score, 100.0 - score, category, confidence, 140.0,
                        category.equals("HIGH") ? "#EF4444" : (category.equals("MEDIUM") ? "#F59E0B" : "#10B981"),
                        crimes, factors, calculateHaversineDistance(currentLatitude, currentLongitude, lat, lng)
                );
                populateDetailsCard(model);
            }
        });
    }

    private void applyFilterAndRefreshUI() {
        filteredRiskZones.clear();
        for (RiskZoneModel zone : allRiskZones) {
            if ("ALL".equals(activeFilter)) {
                filteredRiskZones.add(zone);
            } else if ("HIGH".equals(activeFilter) && zone.isHighRisk()) {
                filteredRiskZones.add(zone);
            } else if ("MEDIUM".equals(activeFilter) && zone.isMediumRisk()) {
                filteredRiskZones.add(zone);
            } else if ("LOW".equals(activeFilter) && zone.isLowRisk()) {
                filteredRiskZones.add(zone);
            }
        }

        if (adapter != null) {
            adapter.updateList(filteredRiskZones);
        }

        if (binding != null) {
            binding.tvNearbyCountBadge.setText(filteredRiskZones.size() + " Found");
            if (!filteredRiskZones.isEmpty()) {
                populateDetailsCard(filteredRiskZones.get(0));
            }
        }

        updateMapRiskZones();
    }

    private void populateDetailsCard(RiskZoneModel model) {
        if (binding == null || model == null) return;

        binding.tvDetailZoneTitle.setText(model.getName());
        binding.tvDetailRiskScore.setText(String.format(Locale.US, "%.0f%%", model.getRiskScore()));
        binding.tvDetailConfidence.setText(String.format(Locale.US, "Confidence: %.0f%%", model.getConfidence() * 100));

        int color = 0xFF10B981;
        String badge = "LOW RISK AREA";
        if (model.isHighRisk()) {
            color = 0xFFEF4444;
            badge = "HIGH RISK AREA";
        } else if (model.isMediumRisk()) {
            color = 0xFFF59E0B;
            badge = "CAUTION ZONE";
        }

        binding.tvDetailRiskCategory.setText(badge);
        binding.tvDetailRiskCategory.setTextColor(color);
        binding.tvDetailRiskScore.setTextColor(color);

        GradientDrawable scoreBg = new GradientDrawable();
        scoreBg.setShape(GradientDrawable.OVAL);
        scoreBg.setColor(Color.argb(35, Color.red(color), Color.green(color), Color.blue(color)));
        scoreBg.setStroke(3, color);
        binding.flDetailScoreGauge.setBackground(scoreBg);

        GradientDrawable catBg = new GradientDrawable();
        catBg.setCornerRadius(14f);
        catBg.setColor(Color.argb(35, Color.red(color), Color.green(color), Color.blue(color)));
        binding.tvDetailRiskCategory.setBackground(catBg);

        if (model.getCommonCrimes() != null && !model.getCommonCrimes().isEmpty()) {
            binding.tvDetailCommonCrimes.setText(String.join(", ", model.getCommonCrimes()));
        } else {
            binding.tvDetailCommonCrimes.setText("Historical Baseline");
        }

        StringBuilder factorsText = new StringBuilder();
        if (model.getRiskFactors() != null && !model.getRiskFactors().isEmpty()) {
            for (String factor : model.getRiskFactors()) {
                factorsText.append("• ").append(factor).append("\n");
            }
        } else {
            factorsText.append("• Historical spatial crime patterns evaluated.\n• Proximity to verified police checkpoints applied.");
        }
        binding.tvDetailFactors.setText(factorsText.toString().trim());
    }

    private void buildOfflineRegionalRiskZones() {
        allRiskZones.clear();
        Object[][] data = {
                {"zone_1", "Vangara Road Peripheral Belt", 18.46900, 83.66400, 78.0, "HIGH", 0.91, 160.0, "#EF4444", "Theft, Eve Teasing", "Elevated historical crime frequency • Deficient street lighting at night"},
                {"zone_2", "Cheepurupalli Highway Bypass", 18.44400, 83.65200, 68.0, "HIGH", 0.88, 150.0, "#EF4444", "Highway Assault", "Isolated stretch • 2.5km from nearest police post"},
                {"zone_3", "Santha Bazar Commercial Hub", 18.45200, 83.66800, 48.0, "MEDIUM", 0.85, 120.0, "#F59E0B", "Chain Snatching", "Caution: High crowd density & limited CCTV"},
                {"zone_4", "Palakonda Road Outpost Crossing", 18.45800, 83.65850, 42.0, "MEDIUM", 0.84, 110.0, "#F59E0B", "Harassment", "Moderate illumination • Active police checkpost nearby"},
                {"zone_5", "GMRIT Campus & Women She-Lounge", 18.46556, 83.65946, 12.0, "LOW", 0.96, 90.0, "#10B981", "None Recorded", "Verified Safe Zone: 24/7 security escort, well-lit, CCTV monitored"},
                {"zone_6", "GMR Care Hospital & Emergency Casualty", 18.46150, 83.65830, 15.0, "LOW", 0.94, 90.0, "#10B981", "None Recorded", "Verified Safe Zone: 24/7 emergency casualty & active guard post"},
                {"zone_7", "Rajam Police Station Precinct", 18.44899, 83.65532, 10.0, "LOW", 0.98, 90.0, "#10B981", "None Recorded", "Verified Safe Zone: Round-the-clock staffed law enforcement precinct"},
                {"zone_8", "Rajam RTC Bus Complex", 18.45036, 83.65976, 22.0, "LOW", 0.92, 95.0, "#10B981", "Pickpocketing", "Transit precinct with active police patrol concourse"}
        };

        for (Object[] row : data) {
            String id = (String) row[0];
            String name = (String) row[1];
            double lat = (Double) row[2];
            double lon = (Double) row[3];
            double score = (Double) row[4];
            String cat = (String) row[5];
            double conf = (Double) row[6];
            double rad = (Double) row[7];
            String color = (String) row[8];
            String crimeStr = (String) row[9];
            String factorStr = (String) row[10];

            List<String> crimes = new ArrayList<>();
            crimes.add(crimeStr);
            List<String> factors = new ArrayList<>();
            factors.add(factorStr);

            double d = calculateHaversineDistance(currentLatitude, currentLongitude, lat, lon);
            allRiskZones.add(new RiskZoneModel(id, name, lat, lon, score, 100.0 - score, cat, conf, rad, color, crimes, factors, d));
        }

        Collections.sort(allRiskZones, (a, b) -> Double.compare(b.getRiskScore(), a.getRiskScore()));
    }

    private RiskZoneModel parseRiskZoneModel(JsonObject obj) {
        String id = obj.has("id") ? obj.get("id").getAsString() : "zone_" + System.currentTimeMillis();
        String name = obj.has("name") ? obj.get("name").getAsString() : "Risk Area";
        double lat = obj.has("latitude") ? obj.get("latitude").getAsDouble() : 0.0;
        double lon = obj.has("longitude") ? obj.get("longitude").getAsDouble() : 0.0;
        double score = obj.has("risk_score") ? obj.get("risk_score").getAsDouble() : 50.0;
        double safety = obj.has("safety_score") ? obj.get("safety_score").getAsDouble() : (100.0 - score);
        String cat = obj.has("risk_category") ? obj.get("risk_category").getAsString() : "MEDIUM";
        double conf = obj.has("confidence") ? obj.get("confidence").getAsDouble() : 0.85;
        double rad = obj.has("circle_radius") ? obj.get("circle_radius").getAsDouble() : 120.0;
        String color = obj.has("color") ? obj.get("color").getAsString() : "#F59E0B";

        List<String> crimes = new ArrayList<>();
        if (obj.has("common_crimes")) {
            for (JsonElement c : obj.getAsJsonArray("common_crimes")) crimes.add(c.getAsString());
        }

        List<String> factors = new ArrayList<>();
        if (obj.has("factors")) {
            for (JsonElement f : obj.getAsJsonArray("factors")) factors.add(f.getAsString());
        }

        double dist = calculateHaversineDistance(currentLatitude, currentLongitude, lat, lon);
        return new RiskZoneModel(id, name, lat, lon, score, safety, cat, conf, rad, color, crimes, factors, dist);
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override
    public void onZoneClicked(RiskZoneModel model) {
        populateDetailsCard(model);
        if (isMapReady && binding != null) {
            binding.webViewRiskMap.evaluateJavascript(
                    String.format(Locale.US, "window.recenterMap(%.5f, %.5f);", model.getLatitude(), model.getLongitude()), null
            );
        }
    }

    @Override
    public void onZoneFocusClicked(RiskZoneModel model) {
        onZoneClicked(model);
    }

    @Override
    public void onResume() {
        super.onResume();
        startContinuousLocationTracking();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopContinuousLocationTracking();
    }

    @Override
    public void onDestroyView() {
        stopContinuousLocationTracking();
        if (binding != null) {
            try {
                binding.webViewRiskMap.stopLoading();
                binding.webViewRiskMap.clearHistory();
                binding.webViewRiskMap.destroy();
            } catch (Exception ignored) {}
            binding = null;
        }
        super.onDestroyView();
    }
}

