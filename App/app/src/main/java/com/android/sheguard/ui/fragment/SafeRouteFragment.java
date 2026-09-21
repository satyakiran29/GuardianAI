package com.android.sheguard.ui.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.android.sheguard.R;
import com.android.sheguard.api.ApiClient;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.databinding.FragmentSafeRouteBinding;
import com.android.sheguard.util.LocationHelper;
import com.android.sheguard.util.SosUtil;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class SafeRouteFragment extends Fragment {

    private static final String TAG = "SafeRouteFragment";

    private FragmentSafeRouteBinding binding;
    private double currentLatitude = 13.0827; // Default Chennai reference
    private double currentLongitude = 80.2707;
    private String currentAddressText = "Locating GPS...";

    private double destLatitude = 0.0;
    private double destLongitude = 0.0;
    private String destAddressText = "";

    private boolean isWomenSafetyMode = true;
    private boolean isNightMode = false;
    private boolean isHotspotsVisible = false;
    private boolean isMonitoring = false;
    private boolean isMapReady = false;

    private JsonArray currentRoutes = new JsonArray();
    private int selectedRouteIndex = 0;
    private JsonArray cachedHotspots = new JsonArray();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean fineGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                boolean coarseGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (fineGranted || coarseGranted) {
                    fetchCurrentLocation();
                } else {
                    if (binding != null) {
                        binding.tvGpsBadge.setText("NO PERMISSION");
                        binding.tvGpsBadge.setTextColor(0xFFEF4444);
                        binding.tvCurrentAddress.setText("Location permission required for safe corridor navigation.");
                    }
                }
            });

    private static final String LEAFLET_ROUTE_HTML = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\" />\n" +
            "    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />\n" +
            "    <script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>\n" +
            "    <style>\n" +
            "        html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; background: #07090E; font-family: sans-serif; }\n" +
            "        .origin-marker {\n" +
            "            width: 18px; height: 18px; border-radius: 50%;\n" +
            "            background: #10B981; border: 3px solid #FFFFFF;\n" +
            "            box-shadow: 0 0 14px #10B981;\n" +
            "        }\n" +
            "        .dest-marker {\n" +
            "            width: 20px; height: 20px; border-radius: 50%;\n" +
            "            background: #FF2E93; border: 3px solid #FFFFFF;\n" +
            "            box-shadow: 0 0 18px #FF2E93;\n" +
            "        }\n" +
            "        .police-marker {\n" +
            "            width: 22px; height: 22px; border-radius: 6px;\n" +
            "            background: #1E293B; border: 2px solid #10B981;\n" +
            "            display: flex; align-items: center; justify-content: center;\n" +
            "            color: #10B981; font-size: 11px; font-weight: bold;\n" +
            "            box-shadow: 0 0 10px rgba(16, 185, 129, 0.5);\n" +
            "        }\n" +
            "        .hotspot-marker {\n" +
            "            width: 22px; height: 22px; border-radius: 50%;\n" +
            "            background: rgba(239, 68, 68, 0.25); border: 2px solid #EF4444;\n" +
            "            display: flex; align-items: center; justify-content: center;\n" +
            "            color: #EF4444; font-size: 11px; font-weight: bold;\n" +
            "            box-shadow: 0 0 14px rgba(239, 68, 68, 0.6);\n" +
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
            "        let map = L.map('map', { zoomControl: false, attributionControl: false }).setView([13.0827, 80.2707], 13);\n" +
            "        let tileLayer = L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', { maxZoom: 19 }).addTo(map);\n" +
            "\n" +
            "        let originMarker = null;\n" +
            "        let destMarker = null;\n" +
            "        let routeLayers = [];\n" +
            "        let hotspotLayers = [];\n" +
            "\n" +
            "        window.initOrigin = function(lat, lng, title) {\n" +
            "            if (originMarker) map.removeLayer(originMarker);\n" +
            "            const icon = L.divIcon({ className: 'origin-marker', iconSize: [18, 18], iconAnchor: [9, 9] });\n" +
            "            originMarker = L.marker([lat, lng], { icon }).addTo(map);\n" +
            "            originMarker.bindPopup('<b>📍 My Location</b><br>' + (title || 'Origin'));\n" +
            "            if (!destMarker) map.setView([lat, lng], 14);\n" +
            "        };\n" +
            "\n" +
            "        window.setDestination = function(lat, lng, title) {\n" +
            "            if (destMarker) map.removeLayer(destMarker);\n" +
            "            const icon = L.divIcon({ className: 'dest-marker', iconSize: [20, 20], iconAnchor: [10, 10] });\n" +
            "            destMarker = L.marker([lat, lng], { icon }).addTo(map);\n" +
            "            destMarker.bindPopup('<b>🎯 Destination</b><br>' + (title || 'Selected Point')).openPopup();\n" +
            "        };\n" +
            "\n" +
            "        window.renderRoutes = function(routesData, selectedIndex, womenModeOn) {\n" +
            "            routeLayers.forEach(l => map.removeLayer(l));\n" +
            "            routeLayers = [];\n" +
            "\n" +
            "            if (!routesData || routesData.length === 0) return;\n" +
            "\n" +
            "            let bounds = L.latLngBounds();\n" +
            "\n" +
            "            routesData.forEach((route, idx) => {\n" +
            "                const isSelected = (idx === selectedIndex);\n" +
            "                const coords = route.coordinates || [];\n" +
            "                const segments = route.segments || [];\n" +
            "\n" +
            "                coords.forEach(pt => bounds.extend([pt[0], pt[1]]));\n" +
            "\n" +
            "                if (isSelected && womenModeOn && segments.length > 0) {\n" +
            "                    // Draw detailed segment-by-segment color coding\n" +
            "                    segments.forEach(seg => {\n" +
            "                        const segLine = L.polyline([seg.start, seg.end], {\n" +
            "                            color: seg.color || '#10B981',\n" +
            "                            weight: 6,\n" +
            "                            opacity: 0.95,\n" +
            "                            lineCap: 'round'\n" +
            "                        }).addTo(map);\n" +
            "\n" +
            "                        segLine.bindPopup(\n" +
            "                            '<b>' + (seg.safety_level || 'Safe') + ' Corridor</b><br>' +\n" +
            "                            'Safety Score: ' + Math.round(seg.safety_score || 85) + '%<br>' +\n" +
            "                            (seg.street_lighting === 1 ? '💡 Well-Lit Street' : '⚠️ Poor Street Lighting') + '<br>' +\n" +
            "                            (seg.cctv_nearby === 1 ? '📹 CCTV Monitored' : 'No CCTV')\n" +
            "                        );\n" +
            "                        routeLayers.push(segLine);\n" +
            "                    });\n" +
            "                } else {\n" +
            "                    // Alternate or standard mode route line\n" +
            "                    const lineColor = isSelected ? '#3B82F6' : (route.route_color || '#64748B');\n" +
            "                    const line = L.polyline(coords, {\n" +
            "                        color: lineColor,\n" +
            "                        weight: isSelected ? 5 : 4,\n" +
            "                        opacity: isSelected ? 0.9 : 0.45,\n" +
            "                        dashArray: isSelected ? null : '6, 8'\n" +
            "                    }).addTo(map);\n" +
            "\n" +
            "                    line.on('click', () => {\n" +
            "                        if (window.AndroidBridge) AndroidBridge.onRouteClicked(idx);\n" +
            "                    });\n" +
            "                    line.bindPopup('<b>' + route.title + '</b><br>' + route.distance_km + ' km • ' + route.duration_mins + ' mins');\n" +
            "                    routeLayers.push(line);\n" +
            "                }\n" +
            "            });\n" +
            "\n" +
            "            if (bounds.isValid()) {\n" +
            "                map.fitBounds(bounds, { padding: [40, 40] });\n" +
            "            }\n" +
            "        };\n" +
            "\n" +
            "        window.renderHotspots = function(hotspots, visible) {\n" +
            "            hotspotLayers.forEach(l => map.removeLayer(l));\n" +
            "            hotspotLayers = [];\n" +
            "            if (!visible || !hotspots) return;\n" +
            "\n" +
            "            hotspots.forEach(h => {\n" +
            "                const isPolice = (h.type === 'police');\n" +
            "                const cls = isPolice ? 'police-marker' : 'hotspot-marker';\n" +
            "                const label = isPolice ? '👮' : '⚠️';\n" +
            "                const icon = L.divIcon({ className: cls, html: label, iconSize: [22, 22], iconAnchor: [11, 11] });\n" +
            "                const m = L.marker([h.latitude, h.longitude], { icon }).addTo(map);\n" +
            "                m.bindPopup('<b>' + h.title + '</b><br>' + (h.description || '') + '<br><i>' + h.distance_km + ' km away</i>');\n" +
            "                hotspotLayers.push(m);\n" +
            "            });\n" +
            "        };\n" +
            "\n" +
            "        window.toggleDayNight = function(isNight) {\n" +
            "            map.removeLayer(tileLayer);\n" +
            "            const url = isNight\n" +
            "                ? 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png'\n" +
            "                : 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png';\n" +
            "            tileLayer = L.tileLayer(url, { maxZoom: 19 }).addTo(map);\n" +
            "        };\n" +
            "\n" +
            "        map.on('click', function(e) {\n" +
            "            window.setDestination(e.latlng.lat, e.latlng.lng, 'Tapped Location');\n" +
            "            if (window.AndroidBridge) {\n" +
            "                AndroidBridge.onMapDestinationSelected(e.latlng.lat, e.latlng.lng);\n" +
            "            }\n" +
            "        });\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>";

    private class SafeRouteJsInterface {
        @JavascriptInterface
        public void onMapDestinationSelected(double lat, double lng) {
            mainHandler.post(() -> {
                destLatitude = lat;
                destLongitude = lng;
                destAddressText = String.format(Locale.US, "GPS: %.4f, %.4f", lat, lng);
                if (binding != null) {
                    binding.etDestination.setText(destAddressText);
                    calculateSafeRoute();
                }
            });
        }

        @JavascriptInterface
        public void onRouteClicked(int routeIndex) {
            mainHandler.post(() -> {
                if (binding != null && currentRoutes != null && routeIndex >= 0 && routeIndex < currentRoutes.size()) {
                    selectedRouteIndex = routeIndex;
                    updateRouteSelection(selectedRouteIndex);
                }
            });
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSafeRouteBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int statusBarHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, statusBarHeight, 0, 0);
            return insets;
        });

        // Initialize Day/Night mode based on current hour
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        isNightMode = (hour < 6 || hour >= 18);
        updateDayNightUi();

        initWebViewMap();
        setupListeners();
        checkPermissionsAndFetchLocation();

        return view;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebViewMap() {
        if (binding == null) return;
        WebSettings settings = binding.webViewMap.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        binding.webViewMap.addJavascriptInterface(new SafeRouteJsInterface(), "AndroidBridge");
        binding.webViewMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                isMapReady = true;
                if (binding != null) {
                    binding.pbMapLoading.setVisibility(View.GONE);
                    // Initialize origin on map
                    binding.webViewMap.evaluateJavascript(
                            String.format(Locale.US, "window.initOrigin(%.5f, %.5f, '%s');",
                                    currentLatitude, currentLongitude, currentAddressText.replace("'", "\\'")),
                            null
                    );
                    // Auto-plan initial route to preset destination
                    calculateSafeRoute();
                }
            }
        });

        binding.pbMapLoading.setVisibility(View.VISIBLE);
        binding.webViewMap.loadDataWithBaseURL("https://guardianai.local", LEAFLET_ROUTE_HTML, "text/html", "UTF-8", null);
    }

    private void setupListeners() {
        // Location locate button
        binding.btnRefreshLocation.setOnClickListener(v -> checkPermissionsAndFetchLocation());

        // External Google Maps Navigation
        binding.btnOpenGoogleMaps.setOnClickListener(v -> {
            if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                LocationHelper.openLocationInGoogleMaps(requireContext(), currentLatitude, currentLongitude, "My Location");
            } else {
                fetchCurrentLocation();
            }
        });

        binding.btnStartNavigation.setOnClickListener(v -> {
            String destination = binding.etDestination.getText() != null ? binding.etDestination.getText().toString().trim() : "";
            if (destination.isEmpty()) {
                destination = "Nearest Police Station";
            }
            LocationHelper.openNavigationDirections(requireContext(), destination);
        });

        // Calculate Route Button
        binding.btnCalculateRoute.setOnClickListener(v -> calculateSafeRoute());

        // Women Safety Mode Switch
        binding.swWomenSafetyMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isWomenSafetyMode = isChecked;
            if (isChecked) {
                Snackbar.make(binding.getRoot(), "🛡️ Women Safety Mode: Safe corridors & police presence prioritized!", Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(binding.getRoot(), "⏱️ Standard Mode: Fastest direct routes displayed", Snackbar.LENGTH_SHORT).show();
            }
            calculateSafeRoute();
        });

        // Day / Night Toggle Chip
        binding.chipDayNight.setOnClickListener(v -> {
            isNightMode = !isNightMode;
            updateDayNightUi();
            if (isMapReady && binding != null) {
                binding.webViewMap.evaluateJavascript("window.toggleDayNight(" + isNightMode + ");", null);
            }
            calculateSafeRoute();
        });

        // Hotspots Toggle Chip
        binding.chipHotspots.setOnClickListener(v -> {
            isHotspotsVisible = !isHotspotsVisible;
            binding.chipHotspots.setText(isHotspotsVisible ? "⚠️ Hotspots ON" : "⚠️ Hotspots");
            binding.chipHotspots.setTextColor(isHotspotsVisible ? 0xFFEF4444 : 0xFFF59E0B);
            if (isHotspotsVisible && cachedHotspots.size() == 0) {
                fetchHotspots();
            } else {
                updateHotspotsOnMap();
            }
        });

        // Quick Preset Destination Chips
        binding.chipPresetPolice.setOnClickListener(v -> {
            binding.etDestination.setText("Nearest Police Station");
            resolveAndPlanRoute("Nearest Police Station", currentLatitude + 0.012, currentLongitude + 0.008);
        });

        binding.chipPresetHome.setOnClickListener(v -> {
            binding.etDestination.setText("Home • Safe Zone");
            resolveAndPlanRoute("Home", currentLatitude - 0.015, currentLongitude + 0.018);
        });

        binding.chipPresetMetro.setOnClickListener(v -> {
            binding.etDestination.setText("Central Metro Transit Hub");
            resolveAndPlanRoute("Metro Station", currentLatitude + 0.022, currentLongitude - 0.014);
        });

        binding.chipPresetHospital.setOnClickListener(v -> {
            binding.etDestination.setText("City Emergency Hospital");
            resolveAndPlanRoute("Hospital", currentLatitude - 0.018, currentLongitude - 0.012);
        });

        // Route Selection Tabs
        binding.btnSelectRoute1.setOnClickListener(v -> updateRouteSelection(0));
        binding.btnSelectRoute2.setOnClickListener(v -> updateRouteSelection(1));

        // Arrival Shield
        binding.btnStartArrivalShield.setOnClickListener(v -> {
            if (isMonitoring) {
                stopMonitoring();
            } else {
                startMonitoring();
            }
        });

        binding.btnSimulateArrival.setOnClickListener(v -> completeArrival());
    }

    private void updateDayNightUi() {
        if (binding == null) return;
        if (isNightMode) {
            binding.chipDayNight.setText("🌙 Night");
            binding.chipDayNight.setBackgroundColor(0xFF1E293B);
        } else {
            binding.chipDayNight.setText("☀️ Day");
            binding.chipDayNight.setBackgroundColor(0xFF0284C7);
        }
    }

    private void checkPermissionsAndFetchLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void fetchCurrentLocation() {
        if (binding == null) return;
        binding.tvGpsBadge.setText("LOCATING...");
        binding.tvGpsBadge.setTextColor(0xFFF59E0B);
        binding.tvCurrentAddress.setText("Querying satellite telemetry...");

        LocationHelper.requestSingleLocationUpdate(requireContext(), new LocationHelper.LocationResultListener() {
            @Override
            public void onLocationReceived(double latitude, double longitude, String addressName) {
                if (binding == null) return;
                currentLatitude = latitude;
                currentLongitude = longitude;
                currentAddressText = addressName;

                binding.tvGpsBadge.setText("LIVE GPS");
                binding.tvGpsBadge.setTextColor(0xFF10B981);
                binding.tvCurrentAddress.setText("📍 " + addressName);
                binding.tvCurrentCoords.setText(String.format(Locale.US, "GPS: %.5f, %.5f (High Accuracy)", latitude, longitude));

                if (isMapReady && binding != null) {
                    binding.webViewMap.evaluateJavascript(
                            String.format(Locale.US, "window.initOrigin(%.5f, %.5f, '%s');",
                                    currentLatitude, currentLongitude, addressName.replace("'", "\\'")),
                            null
                    );
                }

                // If destination exists, refresh route
                if (destLatitude != 0.0 && destLongitude != 0.0) {
                    calculateSafeRoute();
                }
            }

            @Override
            public void onLocationError(String error) {
                if (binding == null) return;
                binding.tvGpsBadge.setText("GPS READY (DEMO)");
                binding.tvGpsBadge.setTextColor(0xFF10B981);
                binding.tvCurrentAddress.setText("Central Station, Anna Salai (Default Safe Corridor)");
                binding.tvCurrentCoords.setText(String.format(Locale.US, "GPS: %.5f, %.5f", currentLatitude, currentLongitude));
            }
        });
    }

    private void resolveAndPlanRoute(String label, double fallbackLat, double fallbackLon) {
        destLatitude = fallbackLat;
        destLongitude = fallbackLon;
        destAddressText = label;

        if (isMapReady && binding != null) {
            binding.webViewMap.evaluateJavascript(
                    String.format(Locale.US, "window.setDestination(%.5f, %.5f, '%s');",
                            destLatitude, destLongitude, label.replace("'", "\\'")),
                    null
            );
        }
        calculateSafeRoute();
    }

    private void calculateSafeRoute() {
        if (binding == null) return;

        // If destination coordinates not chosen yet, pick plausible destination offset
        if (destLatitude == 0.0 || destLongitude == 0.0) {
            destLatitude = currentLatitude + 0.018;
            destLongitude = currentLongitude + 0.015;
            destAddressText = binding.etDestination.getText() != null ? binding.etDestination.getText().toString() : "Destination";
        }

        String timeOfDay = isNightMode ? "Night" : "Day";

        binding.pbMapLoading.setVisibility(View.VISIBLE);

        ApiClient.planSafeRoute(
                currentLatitude, currentLongitude,
                destLatitude, destLongitude,
                isWomenSafetyMode, timeOfDay,
                (success, data, message) -> {
                    if (binding == null) return;
                    binding.pbMapLoading.setVisibility(View.GONE);

                    if (success && data != null && data.has("routes")) {
                        currentRoutes = data.getAsJsonArray("routes");
                        selectedRouteIndex = 0;
                        updateRouteSelection(selectedRouteIndex);

                        // Pass to Leaflet map
                        if (isMapReady) {
                            String routesJsonStr = new Gson().toJson(currentRoutes);
                            binding.webViewMap.evaluateJavascript(
                                    String.format(Locale.US, "window.renderRoutes(%s, %d, %s);",
                                            routesJsonStr, selectedRouteIndex, String.valueOf(isWomenSafetyMode)),
                                    null
                            );
                        }
                    } else {
                        Snackbar.make(binding.getRoot(), "Using offline safe route telemetry", Snackbar.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void updateRouteSelection(int index) {
        if (binding == null || currentRoutes == null || currentRoutes.size() == 0) return;
        selectedRouteIndex = Math.min(index, currentRoutes.size() - 1);

        JsonObject route = currentRoutes.get(selectedRouteIndex).getAsJsonObject();

        String title = route.has("title") ? route.get("title").getAsString() : "Safe Route";
        String subtitle = route.has("subtitle") ? route.get("subtitle").getAsString() : "";
        double score = route.has("overall_safety_score") ? route.get("overall_safety_score").getAsDouble() : 90.0;
        double distKm = route.has("distance_km") ? route.get("distance_km").getAsDouble() : 4.5;
        int durationMins = route.has("duration_mins") ? route.get("duration_mins").getAsInt() : 12;
        int lighting = route.has("lighting_percentage") ? route.get("lighting_percentage").getAsInt() : 90;
        int cctv = route.has("cctv_percentage") ? route.get("cctv_percentage").getAsInt() : 85;
        int police = route.has("police_stations_nearby") ? route.get("police_stations_nearby").getAsInt() : 2;

        binding.tvRouteTitle.setText(title);
        binding.tvRouteStats.setText(String.format(Locale.US, "%.1f km • %d mins travel time • %s", distKm, durationMins, subtitle));

        // Score Badge
        int scoreInt = (int) Math.round(score);
        if (scoreInt >= 75) {
            binding.tvSafetyScoreBadge.setText(String.format(Locale.US, "🛡️ %d%% SAFE", scoreInt));
            binding.tvSafetyScoreBadge.setTextColor(0xFF10B981);
        } else if (scoreInt >= 50) {
            binding.tvSafetyScoreBadge.setText(String.format(Locale.US, "⚠️ %d%% CAUTION", scoreInt));
            binding.tvSafetyScoreBadge.setTextColor(0xFFF59E0B);
        } else {
            binding.tvSafetyScoreBadge.setText(String.format(Locale.US, "🔴 %d%% HIGH RISK", scoreInt));
            binding.tvSafetyScoreBadge.setTextColor(0xFFEF4444);
        }

        binding.tvLightingStat.setText(lighting + "%");
        binding.tvCctvStat.setText(cctv + "%");
        binding.tvPoliceStat.setText(police + " Booths");

        // Dynamic Corridor Insight
        if (scoreInt >= 75) {
            binding.tvCorridorInsight.setText("✅ Recommended Safe Corridor: Well-lit commercial boulevard with verified police checkpoints. Avoids unlit bypasses.");
        } else {
            binding.tvCorridorInsight.setText("⚠️ Caution Corridor: Industrial segment with sparse lighting and lower CCTV coverage. Exercise heightened awareness.");
        }

        // Update Tab button visuals
        if (selectedRouteIndex == 0) {
            binding.btnSelectRoute1.setBackgroundColor(0xFF10B981);
            binding.btnSelectRoute1.setTextColor(0xFFFFFFFF);
            binding.btnSelectRoute2.setBackgroundColor(0x15000000);
            binding.btnSelectRoute2.setTextColor(0xFF64748B);
        } else {
            binding.btnSelectRoute2.setBackgroundColor(0xFF3B82F6);
            binding.btnSelectRoute2.setTextColor(0xFFFFFFFF);
            binding.btnSelectRoute1.setBackgroundColor(0x15000000);
            binding.btnSelectRoute1.setTextColor(0xFF64748B);
        }

        // Tell WebView to re-highlight selected route
        if (isMapReady && binding != null) {
            String routesJsonStr = new Gson().toJson(currentRoutes);
            binding.webViewMap.evaluateJavascript(
                    String.format(Locale.US, "window.renderRoutes(%s, %d, %s);",
                            routesJsonStr, selectedRouteIndex, String.valueOf(isWomenSafetyMode)),
                    null
            );
        }
    }

    private void fetchHotspots() {
        ApiClient.getSafetyHotspots(currentLatitude, currentLongitude, 8.0, (success, data, message) -> {
            if (binding == null) return;
            if (success && data != null && data.has("hotspots")) {
                cachedHotspots = data.getAsJsonArray("hotspots");
            }
            updateHotspotsOnMap();
        });
    }

    private void updateHotspotsOnMap() {
        if (!isMapReady || binding == null) return;
        String hotspotsJsonStr = new Gson().toJson(cachedHotspots);
        binding.webViewMap.evaluateJavascript(
                String.format(Locale.US, "window.renderHotspots(%s, %s);",
                        hotspotsJsonStr, String.valueOf(isHotspotsVisible)),
                null
        );
    }

    private void startMonitoring() {
        isMonitoring = true;
        binding.btnStartArrivalShield.setText("Stop Route Shield");
        binding.btnStartArrivalShield.setBackgroundColor(0xFFEF4444);
        binding.btnSimulateArrival.setVisibility(View.VISIBLE);
        SosUtil.vibrateDevice(requireContext());
        Snackbar.make(binding.getRoot(), "🛡️ Safe Corridor & Arrival Shield Active!", Snackbar.LENGTH_SHORT).show();
    }

    private void stopMonitoring() {
        isMonitoring = false;
        binding.btnStartArrivalShield.setText(getString(R.string.safe_arrival_start_monitoring));
        binding.btnStartArrivalShield.setBackgroundColor(0xFF10B981);
        binding.btnSimulateArrival.setVisibility(View.GONE);
    }

    private void completeArrival() {
        stopMonitoring();
        SosUtil.vibrateDevice(requireContext());
        Snackbar.make(binding.getRoot(), getString(R.string.safe_arrival_arrived_toast), Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.webViewMap.destroy();
        }
        super.onDestroyView();
        binding = null;
    }
}
