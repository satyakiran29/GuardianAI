package com.android.sheguard.ui.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.android.sheguard.R;
import com.android.sheguard.api.ApiClient;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.databinding.FragmentLocationReplayBinding;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class LocationReplayFragment extends Fragment {

    private FragmentLocationReplayBinding binding;
    private String wardPhone = "";
    private String wardName = "Protected User";
    private double wardLat = 17.4482;
    private double wardLng = 78.3914;

    private JsonArray trailData = new JsonArray();
    private int currentPointIndex = 0;
    private boolean isPlaying = false;
    private int playbackSpeed = 1; // 1x, 2x, 5x
    private boolean isMapReady = false;

    private final Handler playbackHandler = new Handler(Looper.getMainLooper());
    private Runnable playbackRunnable;

    private static final String LEAFLET_HTML = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\" />\n" +
            "    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />\n" +
            "    <script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>\n" +
            "    <style>\n" +
            "        html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; background: #07090e; }\n" +
            "        .ward-marker {\n" +
            "            width: 20px;\n" +
            "            height: 20px;\n" +
            "            border-radius: 50%;\n" +
            "            background: #FF2E93;\n" +
            "            border: 3px solid #FFFFFF;\n" +
            "            box-shadow: 0 0 16px #FF2E93;\n" +
            "        }\n" +
            "        .incident-marker {\n" +
            "            width: 24px;\n" +
            "            height: 24px;\n" +
            "            border-radius: 50%;\n" +
            "            background: #EF4444;\n" +
            "            border: 3px solid #FFFFFF;\n" +
            "            box-shadow: 0 0 20px #EF4444;\n" +
            "        }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div id=\"map\"></div>\n" +
            "    <script>\n" +
            "        let map = L.map('map', { zoomControl: false, attributionControl: false }).setView([17.4482, 78.3914], 14);\n" +
            "        L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', { maxZoom: 19 }).addTo(map);\n" +
            "\n" +
            "        let trail = [];\n" +
            "        let polyline = null;\n" +
            "        let movingMarker = null;\n" +
            "        let incidentMarkers = [];\n" +
            "\n" +
            "        window.initReplayMap = function(data) {\n" +
            "            trail = data;\n" +
            "            if (polyline) map.removeLayer(polyline);\n" +
            "            if (movingMarker) map.removeLayer(movingMarker);\n" +
            "            incidentMarkers.forEach(m => map.removeLayer(m));\n" +
            "            incidentMarkers = [];\n" +
            "\n" +
            "            if (!trail || trail.length === 0) return;\n" +
            "\n" +
            "            const latlngs = trail.map(pt => [pt.latitude, pt.longitude]);\n" +
            "            polyline = L.polyline(latlngs, {\n" +
            "                color: '#FF2E93',\n" +
            "                weight: 4,\n" +
            "                opacity: 0.85,\n" +
            "                dashArray: '8, 6'\n" +
            "            }).addTo(map);\n" +
            "\n" +
            "            trail.forEach(pt => {\n" +
            "                if (pt.is_incident) {\n" +
            "                    const incIcon = L.divIcon({ className: 'incident-marker', iconSize: [24, 24], iconAnchor: [12, 12] });\n" +
            "                    const m = L.marker([pt.latitude, pt.longitude], { icon: incIcon }).addTo(map);\n" +
            "                    incidentMarkers.push(m);\n" +
            "                } else {\n" +
            "                    L.circleMarker([pt.latitude, pt.longitude], {\n" +
            "                        radius: 4,\n" +
            "                        color: '#FF2E93',\n" +
            "                        fillColor: '#FF65A7',\n" +
            "                        fillOpacity: 0.7,\n" +
            "                        weight: 1\n" +
            "                    }).addTo(map);\n" +
            "                }\n" +
            "            });\n" +
            "\n" +
            "            const wardIcon = L.divIcon({ className: 'ward-marker', iconSize: [20, 20], iconAnchor: [10, 10] });\n" +
            "            movingMarker = L.marker([trail[0].latitude, trail[0].longitude], { icon: wardIcon }).addTo(map);\n" +
            "\n" +
            "            map.fitBounds(polyline.getBounds(), { padding: [50, 50] });\n" +
            "        };\n" +
            "\n" +
            "        window.seekToPoint = function(idx) {\n" +
            "            if (!trail || !trail[idx] || !movingMarker) return;\n" +
            "            const pt = trail[idx];\n" +
            "            movingMarker.setLatLng([pt.latitude, pt.longitude]);\n" +
            "            if (!map.getBounds().contains([pt.latitude, pt.longitude])) {\n" +
            "                map.panTo([pt.latitude, pt.longitude], { animate: true });\n" +
            "            }\n" +
            "        };\n" +
            "\n" +
            "        window.recenterMap = function() {\n" +
            "            if (polyline) map.fitBounds(polyline.getBounds(), { padding: [50, 50] });\n" +
            "        };\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLocationReplayBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            wardPhone = getArguments().getString("WARD_PHONE", "");
            wardName = getArguments().getString("WARD_NAME", "Protected User");
            wardLat = getArguments().getDouble("WARD_LAT", 17.4482);
            wardLng = getArguments().getDouble("WARD_LNG", 78.3914);
        }

        binding.tvReplayTitle.setText(wardName);
        binding.tvReplaySubtitle.setText("24h Telemetry Replay • Movement Review");

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        initWebView();
        setupControls();
        fetchLocationTrail();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        WebSettings settings = binding.webViewMap.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        binding.webViewMap.setBackgroundColor(0xFF07090E);

        binding.webViewMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                isMapReady = true;
                if (trailData != null && trailData.size() > 0) {
                    injectTrailToMap();
                }
            }
        });

        binding.webViewMap.loadDataWithBaseURL("https://guardianai.app", LEAFLET_HTML, "text/html", "UTF-8", null);
    }

    private void setupControls() {
        binding.btnPlayPause.setOnClickListener(v -> togglePlayPause());

        binding.btnStepPrev.setOnClickListener(v -> {
            pausePlayback();
            if (currentPointIndex > 0) {
                currentPointIndex--;
                displayPoint(currentPointIndex);
            }
        });

        binding.btnStepNext.setOnClickListener(v -> {
            pausePlayback();
            if (currentPointIndex < trailData.size() - 1) {
                currentPointIndex++;
                displayPoint(currentPointIndex);
            }
        });

        binding.btnSpeedToggle.setOnClickListener(v -> {
            if (playbackSpeed == 1) playbackSpeed = 2;
            else if (playbackSpeed == 2) playbackSpeed = 5;
            else playbackSpeed = 1;

            binding.btnSpeedToggle.setText(playbackSpeed + "x");
            if (isPlaying) {
                startPlayback();
            }
        });

        binding.fabRecenter.setOnClickListener(v -> {
            if (binding != null && binding.webViewMap != null) {
                binding.webViewMap.evaluateJavascript("window.recenterMap();", null);
            }
        });

        binding.seekbarReplay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && progress >= 0 && progress < trailData.size()) {
                    pausePlayback();
                    currentPointIndex = progress;
                    displayPoint(currentPointIndex);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                pausePlayback();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void fetchLocationTrail() {
        String guardianPhone = Prefs.getString(Constants.PREFS_USER_PHONE, "");
        binding.pbLoading.setVisibility(View.VISIBLE);

        ApiClient.getLocationHistory(wardPhone, guardianPhone, 24, (success, data, message) -> {
            if (binding == null || getContext() == null) return;
            binding.pbLoading.setVisibility(View.GONE);

            if (success && data != null && data.has("trail")) {
                trailData = data.getAsJsonArray("trail");
                int totalPoints = trailData.size();
                binding.tvPointsBadge.setText(totalPoints + " Checkpoints");

                if (totalPoints > 0) {
                    binding.seekbarReplay.setMax(totalPoints - 1);
                    binding.seekbarReplay.setProgress(0);

                    JsonObject firstPt = trailData.get(0).getAsJsonObject();
                    JsonObject lastPt = trailData.get(totalPoints - 1).getAsJsonObject();

                    if (firstPt.has("formatted_time")) {
                        binding.tvTimeStart.setText(firstPt.get("formatted_time").getAsString());
                    }
                    if (lastPt.has("formatted_time")) {
                        binding.tvTimeEnd.setText(lastPt.get("formatted_time").getAsString());
                    }

                    currentPointIndex = 0;
                    displayPoint(0);

                    if (isMapReady) {
                        injectTrailToMap();
                    }
                }
            } else {
                Toast.makeText(getContext(), message != null ? message : "Failed to load location trail", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void injectTrailToMap() {
        if (binding == null || binding.webViewMap == null || trailData == null || trailData.size() == 0) return;
        String js = "window.initReplayMap(" + trailData.toString() + ");";
        binding.webViewMap.evaluateJavascript(js, null);
    }

    private void displayPoint(int index) {
        if (trailData == null || index < 0 || index >= trailData.size() || binding == null) return;

        JsonObject pt = trailData.get(index).getAsJsonObject();

        String time = pt.has("formatted_time") ? pt.get("formatted_time").getAsString() : "--";
        String date = pt.has("formatted_date") ? pt.get("formatted_date").getAsString() : "";
        int battery = pt.has("battery_level") ? pt.get("battery_level").getAsInt() : 75;
        String address = pt.has("address") ? pt.get("address").getAsString() : "Live Coordinates";
        boolean isIncident = pt.has("is_incident") && pt.get("is_incident").getAsBoolean();

        binding.tvTelemetryTime.setText(date.isEmpty() ? time : time + " • " + date);
        binding.tvTelemetryBattery.setText(battery + "%");
        binding.tvTelemetryAddress.setText(address);
        binding.tvTelemetryCheckpoint.setText("Point " + (index + 1) + " of " + trailData.size());
        binding.seekbarReplay.setProgress(index);

        if (isIncident) {
            binding.layoutIncidentBanner.setVisibility(View.VISIBLE);
            binding.tvIncidentHeadline.setText("🚨 EMERGENCY SOS TRIGGERED HERE");
            binding.tvIncidentDetails.setText("Distress incident beacon recorded at " + time + " (" + address + ")");
        } else {
            binding.layoutIncidentBanner.setVisibility(View.GONE);
        }

        if (binding.webViewMap != null) {
            binding.webViewMap.evaluateJavascript("window.seekToPoint(" + index + ");", null);
        }
    }

    private void togglePlayPause() {
        if (isPlaying) {
            pausePlayback();
        } else {
            startPlayback();
        }
    }

    private void startPlayback() {
        if (trailData == null || trailData.size() == 0) return;
        if (currentPointIndex >= trailData.size() - 1) {
            currentPointIndex = 0;
        }

        isPlaying = true;
        binding.btnPlayPause.setText("Pause");

        playbackHandler.removeCallbacks(playbackRunnable);
        int stepDelay = Math.max(200, 1000 / playbackSpeed);

        playbackRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPlaying || binding == null) return;
                currentPointIndex++;
                if (currentPointIndex >= trailData.size()) {
                    pausePlayback();
                    currentPointIndex = trailData.size() - 1;
                    return;
                }
                displayPoint(currentPointIndex);
                playbackHandler.postDelayed(this, stepDelay);
            }
        };
        playbackHandler.postDelayed(playbackRunnable, stepDelay);
    }

    private void pausePlayback() {
        isPlaying = false;
        if (binding != null) {
            binding.btnPlayPause.setText("Play Replay");
        }
        playbackHandler.removeCallbacks(playbackRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pausePlayback();
        binding = null;
    }
}
