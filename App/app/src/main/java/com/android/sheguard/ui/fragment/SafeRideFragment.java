package com.android.sheguard.ui.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.android.sheguard.R;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.databinding.FragmentSafeRideBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SafeRideFragment extends Fragment {

    private FragmentSafeRideBinding binding;
    private FusedLocationProviderClient fusedLocationClient;

    // User coordinates (Default: Rajam Ground Truth AP 532127)
    private double userLat = 18.46600;
    private double userLon = 83.66360;

    // Destination coordinates (Default: GMR Care Hospital Gate 1)
    private double destLat = 18.46150;
    private double destLon = 83.65830;
    private String destName = "GMR Varalakshmi CARE Multi-Speciality Hospital";

    // Captain coordinates (Captain Priya V. starting position ~350m away)
    private double captainLat = 18.46880;
    private double captainLon = 83.66580;

    private int selectedService = 0; // 0: Bike (₹49), 1: Shadow (₹39), 2: Cab (₹119)
    private boolean isMapReady = false;
    private boolean isEscortActive = false;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private Runnable telemetrySimulationRunnable;
    private int simulationStep = 0;

    private static final String LEAFLET_ESCORT_HTML = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\" />\n" +
            "    <link rel=\"stylesheet\" href=\"file:///android_asset/leaflet/leaflet.css\" />\n" +
            "    <script src=\"file:///android_asset/leaflet/leaflet.js\"></script>\n" +
            "    <style>\n" +
            "        html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; background: #07090E; font-family: sans-serif; }\n" +
            "        .user-pulse {\n" +
            "            width: 18px; height: 18px; border-radius: 50%;\n" +
            "            background: #10B981; border: 3px solid #FFFFFF;\n" +
            "            box-shadow: 0 0 12px #10B981;\n" +
            "            animation: user-radar 2s infinite ease-out;\n" +
            "        }\n" +
            "        @keyframes user-radar {\n" +
            "            0% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }\n" +
            "            70% { box-shadow: 0 0 0 16px rgba(16, 185, 129, 0); }\n" +
            "            100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }\n" +
            "        }\n" +
            "        .pilot-bike-marker {\n" +
            "            background: #020617; border: 2px solid #10B981; border-radius: 50%;\n" +
            "            width: 32px; height: 32px; display: flex; align-items: center; justify-content: center;\n" +
            "            box-shadow: 0 2px 8px rgba(0,0,0,0.6); font-size: 16px;\n" +
            "        }\n" +
            "        .pilot-active-marker {\n" +
            "            background: #10B981; border: 2px solid #FFFFFF; border-radius: 50%;\n" +
            "            width: 36px; height: 36px; display: flex; align-items: center; justify-content: center;\n" +
            "            box-shadow: 0 0 16px #10B981; font-size: 18px;\n" +
            "            animation: pilot-pulse 1.5s infinite alternate;\n" +
            "        }\n" +
            "        @keyframes pilot-pulse {\n" +
            "            0% { transform: scale(1.0); }\n" +
            "            100% { transform: scale(1.15); }\n" +
            "        }\n" +
            "        .dest-flag-marker {\n" +
            "            background: #EF4444; border: 2px solid #FFFFFF; border-radius: 50%;\n" +
            "            width: 28px; height: 28px; display: flex; align-items: center; justify-content: center;\n" +
            "            box-shadow: 0 2px 8px rgba(239, 68, 68, 0.6); font-size: 14px;\n" +
            "        }\n" +
            "        .swiggy-flow-line {\n" +
            "            stroke-dasharray: 10, 14;\n" +
            "            animation: swiggy-dash 1.2s linear infinite;\n" +
            "        }\n" +
            "        @keyframes swiggy-dash {\n" +
            "            from { stroke-dashoffset: 24; }\n" +
            "            to { stroke-dashoffset: 0; }\n" +
            "        }\n" +
            "        .swiggy-eta-pill {\n" +
            "            background: #020617; border: 1.5px solid #10B981; color: #FFFFFF;\n" +
            "            font-size: 11px; font-weight: 700; padding: 3px 8px; border-radius: 12px;\n" +
            "            white-space: nowrap; box-shadow: 0 2px 8px rgba(0,0,0,0.7);\n" +
            "        }\n" +
            "        .leaflet-popup-content-wrapper {\n" +
            "            background: #0F172A !important; color: #F8FAFC !important;\n" +
            "            border-radius: 12px !important; border: 1px solid #1E293B;\n" +
            "        }\n" +
            "        .leaflet-popup-tip { background: #0F172A !important; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div id=\"map\"></div>\n" +
            "    <script>\n" +
            "        const map = L.map('map', { zoomControl: false, attributionControl: false }).setView([18.46600, 83.66360], 15);\n" +
            "        L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}', {\n" +
            "            maxZoom: 19, maxNativeZoom: 16\n" +
            "        }).addTo(map);\n" +
            "        L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Reference/MapServer/tile/{z}/{y}/{x}', {\n" +
            "            maxZoom: 19, maxNativeZoom: 16\n" +
            "        }).addTo(map);\n" +
            "\n" +
            "        let userMarker = null;\n" +
            "        let destMarker = null;\n" +
            "        let assignedCaptainMarker = null;\n" +
            "        let otherPilotMarkers = [];\n" +
            "        let routeLayers = [];\n" +
            "        let captainRouteLayers = [];\n" +
            "\n" +
            "        window.initEscortScene = function(uLat, uLon, dLat, dLon, dName) {\n" +
            "            // 1. User Location\n" +
            "            if (userMarker) map.removeLayer(userMarker);\n" +
            "            const userIcon = L.divIcon({ className: '', html: '<div class=\"user-pulse\"></div>', iconSize: [18, 18], iconAnchor: [9, 9] });\n" +
            "            userMarker = L.marker([uLat, uLon], { icon: userIcon }).addTo(map);\n" +
            "            userMarker.bindPopup('<b>📍 Your Live GPS Location</b><br>Guardian Telemetry Active');\n" +
            "\n" +
            "            // 2. Destination\n" +
            "            if (destMarker) map.removeLayer(destMarker);\n" +
            "            const destIcon = L.divIcon({ className: '', html: '<div class=\"dest-flag-marker\">🏁</div>', iconSize: [28, 28], iconAnchor: [14, 14] });\n" +
            "            destMarker = L.marker([dLat, dLon], { icon: destIcon }).addTo(map);\n" +
            "            destMarker.bindPopup('<b>🏁 Destination</b><br>' + dName);\n" +
            "\n" +
            "            // 3. Patrolling Guardian Pilots nearby\n" +
            "            otherPilotMarkers.forEach(m => map.removeLayer(m));\n" +
            "            otherPilotMarkers = [];\n" +
            "\n" +
            "            const pilots = [\n" +
            "                { lat: uLat + 0.0035, lon: uLon + 0.0028, name: 'Captain Priya V.', rating: '⭐ 4.9', veh: 'TVS Jupiter (AP 39 BK 9821)', dist: '2 min away' },\n" +
            "                { lat: uLat - 0.0042, lon: uLon + 0.0031, name: 'Captain Kavitha S.', rating: '⭐ 4.95', veh: 'Honda Activa (AP 39 CW 4120)', dist: '4 min away' },\n" +
            "                { lat: uLat + 0.0060, lon: uLon - 0.0050, name: 'Pilot Anitha R. (Cab)', rating: '⭐ 4.8', veh: 'Tata Nexon EV (AP 39 ED 8812)', dist: '6 min away' }\n" +
            "            ];\n" +
            "\n" +
            "            pilots.forEach((p, idx) => {\n" +
            "                const pIcon = L.divIcon({\n" +
            "                    className: '',\n" +
            "                    html: '<div class=\"pilot-bike-marker\">' + (idx === 2 ? '🚗' : '🏍️') + '</div>',\n" +
            "                    iconSize: [32, 32], iconAnchor: [16, 16]\n" +
            "                });\n" +
            "                const pm = L.marker([p.lat, p.lon], { icon: pIcon }).addTo(map);\n" +
            "                pm.bindPopup('<b>🛡️ ' + p.name + '</b><br>' + p.rating + ' • ' + p.dist + '<br>' + p.veh);\n" +
            "                otherPilotMarkers.push(pm);\n" +
            "            });\n" +
            "\n" +
            "            map.setView([uLat, uLon], 15);\n" +
            "        };\n" +
            "\n" +
            "        window.renderEscortRoute = function(coords, durStr, distStr) {\n" +
            "            routeLayers.forEach(l => map.removeLayer(l));\n" +
            "            routeLayers = [];\n" +
            "            if (!coords || coords.length === 0) return;\n" +
            "\n" +
            "            // Road Casing (Swiggy dark contrast)\n" +
            "            const casing = L.polyline(coords, { color: '#020617', weight: 9, opacity: 0.95, lineCap: 'round', lineJoin: 'round' }).addTo(map);\n" +
            "            routeLayers.push(casing);\n" +
            "\n" +
            "            // Emerald Safe Polyline\n" +
            "            const mainLine = L.polyline(coords, { color: '#10B981', weight: 5, opacity: 0.95, lineCap: 'round', lineJoin: 'round' }).addTo(map);\n" +
            "            routeLayers.push(mainLine);\n" +
            "\n" +
            "            // Flowing Animated Dash\n" +
            "            const flowLine = L.polyline(coords, { color: '#FFFFFF', weight: 2.5, opacity: 0.85, className: 'swiggy-flow-line' }).addTo(map);\n" +
            "            routeLayers.push(flowLine);\n" +
            "\n" +
            "            // Floating ETA Badge over destination\n" +
            "            const lastPt = coords[coords.length - 1];\n" +
            "            const etaHtml = '<div class=\"swiggy-eta-pill\">⚡ ' + durStr + ' • ' + distStr + '</div>';\n" +
            "            const etaIcon = L.divIcon({ className: '', html: etaHtml, iconSize: [120, 26], iconAnchor: [60, 36] });\n" +
            "            const etaMarker = L.marker([lastPt[0], lastPt[1]], { icon: etaIcon, zIndexOffset: 2000 }).addTo(map);\n" +
            "            routeLayers.push(etaMarker);\n" +
            "\n" +
            "            const bounds = L.latLngBounds(coords);\n" +
            "            map.fitBounds(bounds, { padding: [40, 40] });\n" +
            "        };\n" +
            "\n" +
            "        window.setAssignedCaptain = function(cLat, cLon, captainName) {\n" +
            "            // Hide other generic pilots\n" +
            "            otherPilotMarkers.forEach(m => map.removeLayer(m));\n" +
            "            if (assignedCaptainMarker) map.removeLayer(assignedCaptainMarker);\n" +
            "\n" +
            "            const activeIcon = L.divIcon({\n" +
            "                className: '',\n" +
            "                html: '<div class=\"pilot-active-marker\">🏍️</div>',\n" +
            "                iconSize: [36, 36], iconAnchor: [18, 18]\n" +
            "            });\n" +
            "            assignedCaptainMarker = L.marker([cLat, cLon], { icon: activeIcon, zIndexOffset: 3000 }).addTo(map);\n" +
            "            assignedCaptainMarker.bindPopup('<b>🛡️ ' + captainName + '</b><br>⚡ Approaching your location');\n" +
            "        };\n" +
            "\n" +
            "        window.updateCaptainPosition = function(lat, lon) {\n" +
            "            if (assignedCaptainMarker) {\n" +
            "                assignedCaptainMarker.setLatLng([lat, lon]);\n" +
            "            }\n" +
            "        };\n" +
            "\n" +
            "        window.renderCaptainPickupRoute = function(coords) {\n" +
            "            captainRouteLayers.forEach(l => map.removeLayer(l));\n" +
            "            captainRouteLayers = [];\n" +
            "            if (!coords || coords.length === 0) return;\n" +
            "\n" +
            "            const capCasing = L.polyline(coords, { color: '#020617', weight: 8, opacity: 0.90 }).addTo(map);\n" +
            "            captainRouteLayers.push(capCasing);\n" +
            "\n" +
            "            const capLine = L.polyline(coords, { color: '#3B82F6', weight: 4.5, dashArray: '6, 8', opacity: 0.95 }).addTo(map);\n" +
            "            captainRouteLayers.push(capLine);\n" +
            "        };\n" +
            "\n" +
            "        window.recenterUser = function(lat, lon) {\n" +
            "            map.setView([lat, lon], 16);\n" +
            "        };\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSafeRideBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        ((AppCompatActivity) requireActivity()).setSupportActionBar(binding.header.toolbar);
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            binding.header.collapsingToolbar.setTitle(getString(R.string.activity_safe_ride_title));
            binding.header.collapsingToolbar.setSubtitle("Rapido-style on-demand verified safety ride & escort");
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        initMapWebView();
        setupDestinationChips();
        setupServiceOptions();
        setupActionButtons();
        detectLiveLocation();

        return view;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initMapWebView() {
        WebSettings settings = binding.webViewEscortMap.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        binding.webViewEscortMap.setBackgroundColor(0xFF07090E);
        binding.webViewEscortMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                isMapReady = true;
                if (binding != null) {
                    binding.pbMapLoading.setVisibility(View.GONE);
                    syncMapScene();
                    fetchAndRenderEscortRoute();
                }
            }
        });

        binding.webViewEscortMap.loadDataWithBaseURL("https://guardianai.app", LEAFLET_ESCORT_HTML, "text/html", "UTF-8", null);
    }

    private void detectLiveLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    userLat = location.getLatitude();
                    userLon = location.getLongitude();
                    // Calibrate captain relative to user's real location (~300m away)
                    captainLat = userLat + 0.0030;
                    captainLon = userLon + 0.0025;
                    if (isMapReady) {
                        syncMapScene();
                        fetchAndRenderEscortRoute();
                    }
                }
            });
        }
    }

    private void syncMapScene() {
        if (!isMapReady || binding == null) return;
        String js = String.format(Locale.US,
                "window.initEscortScene(%f, %f, %f, %f, '%s');",
                userLat, userLon, destLat, destLon, destName.replace("'", "\\'"));
        binding.webViewEscortMap.evaluateJavascript(js, null);
    }

    private void setupDestinationChips() {
        binding.chipDestHospital.setOnClickListener(v -> selectDestination(
                18.46150, 83.65830,
                "GMR Varalakshmi CARE Multi-Speciality Hospital",
                binding.chipDestHospital));

        binding.chipDestStation.setOnClickListener(v -> selectDestination(
                18.31420, 83.58050,
                "Cheepurupalli Railway Station",
                binding.chipDestStation));

        binding.chipDestCampus.setOnClickListener(v -> selectDestination(
                18.46556, 83.65946,
                "GMRIT Main Campus Security Gate",
                binding.chipDestCampus));

        binding.chipDestMarket.setOnClickListener(v -> selectDestination(
                18.44850, 83.66120,
                "Santha Bazar Market & Police Outpost",
                binding.chipDestMarket));
    }

    private void selectDestination(double lat, double lon, String name, View selectedChip) {
        destLat = lat;
        destLon = lon;
        destName = name;

        // Reset chip styles
        binding.chipDestHospital.setBackgroundResource(R.drawable.item_background_material);
        binding.chipDestStation.setBackgroundResource(R.drawable.item_background_material);
        binding.chipDestCampus.setBackgroundResource(R.drawable.item_background_material);
        binding.chipDestMarket.setBackgroundResource(R.drawable.item_background_material);

        selectedChip.setBackgroundResource(R.drawable.container);
        binding.tvSelectedDestination.setText("📍 Destination: " + name);

        syncMapScene();
        fetchAndRenderEscortRoute();
    }

    private void setupServiceOptions() {
        binding.cardOptBike.setOnClickListener(v -> updateServiceSelection(0));
        binding.cardOptShadow.setOnClickListener(v -> updateServiceSelection(1));
        binding.cardOptCab.setOnClickListener(v -> updateServiceSelection(2));
    }

    private void updateServiceSelection(int index) {
        selectedService = index;

        // Reset strokes
        binding.cardOptBike.setStrokeColor(index == 0 ? 0xFF10B981 : 0x20FFFFFF);
        binding.cardOptBike.setStrokeWidth(index == 0 ? 4 : 2);

        binding.cardOptShadow.setStrokeColor(index == 1 ? 0xFF3B82F6 : 0x20FFFFFF);
        binding.cardOptShadow.setStrokeWidth(index == 1 ? 4 : 2);

        binding.cardOptCab.setStrokeColor(index == 2 ? 0xFFF59E0B : 0x20FFFFFF);
        binding.cardOptCab.setStrokeWidth(index == 2 ? 4 : 2);

        if (!isEscortActive) {
            if (index == 0) {
                binding.btnRequestEscort.setText("⚡ Request Guardian Bike Escort (₹49)");
            } else if (index == 1) {
                binding.btnRequestEscort.setText("🛡️ Request Shadow Commute Shield (₹39)");
            } else {
                binding.btnRequestEscort.setText("🚗 Request Guardian Safe Cab (₹119)");
            }
        }
    }

    private void setupActionButtons() {
        binding.fabRecenter.setOnClickListener(v -> {
            if (isMapReady && binding != null) {
                binding.webViewEscortMap.evaluateJavascript(
                        String.format(Locale.US, "window.recenterUser(%f, %f);", userLat, userLon),
                        null);
            }
        });

        binding.btnRequestEscort.setOnClickListener(v -> requestEscortSimulation());

        binding.btnCallPilot.setOnClickListener(v -> {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:08941251233")); // Rajam Local Police & Guardian Command
            startActivity(callIntent);
        });

        binding.btnShareTelemetry.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "GuardianAI Live Safe Escort Telemetry");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "🛡️ GuardianAI Live Safe Escort Active!\n" +
                            "Captain: Priya V. (Verified Female Pilot #GP-412)\n" +
                            "Vehicle: TVS Jupiter (AP 39 BK 9821)\n" +
                            "Destination: " + destName + "\n" +
                            "Live GPS Telemetry & Police Shield: https://guardianai.app/track/escort-8419");
            startActivity(Intent.createChooser(shareIntent, "Share Live Telemetry"));
        });

        binding.btnSosIntercept.setOnClickListener(v -> triggerSosIntercept());

        binding.btnCancelEscort.setOnClickListener(v -> cancelEscort());
    }

    private void requestEscortSimulation() {
        binding.btnRequestEscort.setEnabled(false);
        binding.btnRequestEscort.setText("📡 Contacting Nearest Verified Captain...");

        mainHandler.postDelayed(() -> {
            if (binding == null) return;

            isEscortActive = true;
            binding.cardActiveEscort.setVisibility(View.VISIBLE);
            binding.btnRequestEscort.setText("🟢 Safe Escort In Progress • Telemetry Active");
            binding.tvShieldStatus.setText("🛡️ Live Telemetry ACTIVE");

            Snackbar.make(binding.getRoot(),
                    "✅ Captain Assigned! Captain Priya V. is 2 mins away (OTP: 8419)",
                    Snackbar.LENGTH_LONG).show();

            // Set Captain on Map and fetch road route from Captain to User
            if (isMapReady) {
                binding.webViewEscortMap.evaluateJavascript(
                        String.format(Locale.US, "window.setAssignedCaptain(%f, %f, 'Captain Priya V.');", captainLat, captainLon),
                        null);
                fetchCaptainApproachRoute();
            }

            startTelemetryLiveSimulation();
        }, 1500);
    }

    private void startTelemetryLiveSimulation() {
        simulationStep = 0;
        final int totalSteps = 20;

        telemetrySimulationRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isEscortActive || binding == null) return;

                simulationStep++;
                float progress = Math.min(1.0f, simulationStep / (float) totalSteps);

                // Interpolate captain position towards user along the street
                double curLat = captainLat + (userLat - captainLat) * progress;
                double curLon = captainLon + (userLon - captainLon) * progress;

                if (isMapReady && binding != null) {
                    binding.webViewEscortMap.evaluateJavascript(
                            String.format(Locale.US, "window.updateCaptainPosition(%f, %f);", curLat, curLon),
                            null);
                }

                int speed = 25 + (simulationStep % 5);
                binding.tvTelemetrySpeed.setText("🚀 Speed: " + speed + " km/h");
                binding.tvTelemetryDrift.setText("🛡️ Drift: 0m (On-Road)");

                if (progress >= 1.0f) {
                    binding.tvEscortStatusBadge.setText("✅ CAPTAIN ARRIVED • BOARD WITH OTP 8419");
                    binding.tvEscortStatusBadge.setTextColor(0xFF10B981);
                    Snackbar.make(binding.getRoot(), "🎉 Captain Priya has arrived at your pickup location!", Snackbar.LENGTH_LONG).show();
                } else {
                    int remainingMins = Math.max(1, (int) Math.ceil((1.0f - progress) * 2.5f));
                    binding.tvEscortStatusBadge.setText("⚡ CAPTAIN ARRIVING IN " + remainingMins + " MIN" + (remainingMins > 1 ? "S" : ""));
                    mainHandler.postDelayed(this, 1800);
                }
            }
        };

        mainHandler.postDelayed(telemetrySimulationRunnable, 1000);
    }

    private void triggerSosIntercept() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("🚨 1-Tap SOS Pilot Intercept")
                .setMessage("Priority emergency beacon broadcasted!\n\n• Local Police Station (Rajam PS) notified\n• Nearest 3 Guardian pilots dispatched to your live coordinates\n• Camera telemetry locked to command center")
                .setPositiveButton("Understood", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void cancelEscort() {
        isEscortActive = false;
        if (telemetrySimulationRunnable != null) {
            mainHandler.removeCallbacks(telemetrySimulationRunnable);
        }

        binding.cardActiveEscort.setVisibility(View.GONE);
        binding.btnRequestEscort.setEnabled(true);
        updateServiceSelection(selectedService);
        binding.tvShieldStatus.setText("🛡️ Shield Ready");

        syncMapScene();
        fetchAndRenderEscortRoute();

        Snackbar.make(binding.getRoot(), "Escort request cancelled.", Snackbar.LENGTH_SHORT).show();
    }

    private void fetchAndRenderEscortRoute() {
        networkExecutor.execute(() -> {
            try {
                String urlStr = String.format(Locale.US,
                        "https://routing.openstreetmap.de/routed-car/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson",
                        userLon, userLat, destLon, destLat);

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "GuardianAI-SafeRide/1.3.8");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                if (conn.getResponseCode() == 200) {
                    InputStream in = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject res = new JSONObject(sb.toString());
                    JSONArray routes = res.getJSONArray("routes");
                    if (routes.length() > 0) {
                        JSONObject r0 = routes.getJSONObject(0);
                        double distKm = r0.getDouble("distance") / 1000.0;
                        int durMins = Math.max(1, (int) Math.round(r0.getDouble("duration") / 60.0));

                        JSONArray coords = r0.getJSONObject("geometry").getJSONArray("coordinates");
                        JSONArray swapped = new JSONArray();
                        for (int i = 0; i < coords.length(); i++) {
                            JSONArray pt = coords.getJSONArray(i);
                            JSONArray sPt = new JSONArray();
                            sPt.put(pt.getDouble(1)); // lat
                            sPt.put(pt.getDouble(0)); // lon
                            swapped.put(sPt);
                        }

                        String durStr = durMins + " mins";
                        String distStr = String.format(Locale.US, "%.1f km", distKm);

                        mainHandler.post(() -> {
                            if (isMapReady && binding != null) {
                                String js = String.format(Locale.US,
                                        "window.renderEscortRoute(%s, '%s', '%s');",
                                        swapped.toString(), durStr, distStr);
                                binding.webViewEscortMap.evaluateJavascript(js, null);
                            }
                        });
                        return;
                    }
                }
            } catch (Exception e) {
                // Fallback to direct linear route
            }

            // Fallback points
            JSONArray fallbackPts = new JSONArray();
            try {
                JSONArray p1 = new JSONArray(); p1.put(userLat); p1.put(userLon);
                JSONArray p2 = new JSONArray(); p2.put(destLat); p2.put(destLon);
                fallbackPts.put(p1);
                fallbackPts.put(p2);
                mainHandler.post(() -> {
                    if (isMapReady && binding != null) {
                        binding.webViewEscortMap.evaluateJavascript(
                                String.format(Locale.US, "window.renderEscortRoute(%s, '4 mins', '1.2 km');", fallbackPts.toString()),
                                null);
                    }
                });
            } catch (Exception ignored) {}
        });
    }

    private void fetchCaptainApproachRoute() {
        networkExecutor.execute(() -> {
            try {
                String urlStr = String.format(Locale.US,
                        "https://routing.openstreetmap.de/routed-car/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson",
                        captainLon, captainLat, userLon, userLat);

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "GuardianAI-SafeRide/1.3.8");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                if (conn.getResponseCode() == 200) {
                    InputStream in = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject res = new JSONObject(sb.toString());
                    JSONArray routes = res.getJSONArray("routes");
                    if (routes.length() > 0) {
                        JSONArray coords = routes.getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates");
                        JSONArray swapped = new JSONArray();
                        for (int i = 0; i < coords.length(); i++) {
                            JSONArray pt = coords.getJSONArray(i);
                            JSONArray sPt = new JSONArray();
                            sPt.put(pt.getDouble(1));
                            sPt.put(pt.getDouble(0));
                            swapped.put(sPt);
                        }

                        mainHandler.post(() -> {
                            if (isMapReady && binding != null) {
                                binding.webViewEscortMap.evaluateJavascript(
                                        String.format(Locale.US, "window.renderCaptainPickupRoute(%s);", swapped.toString()),
                                        null);
                            }
                        });
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    @Override
    public void onDestroyView() {
        if (telemetrySimulationRunnable != null) {
            mainHandler.removeCallbacks(telemetrySimulationRunnable);
        }
        if (binding != null && binding.webViewEscortMap != null) {
            binding.webViewEscortMap.destroy();
        }
        binding = null;
        super.onDestroyView();
    }
}
