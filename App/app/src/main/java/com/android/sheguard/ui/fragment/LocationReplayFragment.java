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
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import java.util.Locale;

import com.android.sheguard.R;
import com.android.sheguard.api.ApiClient;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.databinding.FragmentLocationReplayBinding;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class LocationReplayFragment extends Fragment {

    private static final String TAG = "LocationReplay";
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
            "            width: 22px;\n" +
            "            height: 22px;\n" +
            "            border-radius: 50%;\n" +
            "            background: #FF2E93;\n" +
            "            border: 3px solid #FFFFFF;\n" +
            "            box-shadow: 0 0 16px #FF2E93, 0 0 32px rgba(255, 46, 147, 0.6);\n" +
            "            animation: pulseWard 2s infinite;\n" +
            "        }\n" +
            "        @keyframes pulseWard {\n" +
            "            0% { transform: scale(1); }\n" +
            "            50% { transform: scale(1.15); }\n" +
            "            100% { transform: scale(1); }\n" +
            "        }\n" +
            "        .incident-marker {\n" +
            "            width: 26px;\n" +
            "            height: 26px;\n" +
            "            border-radius: 50%;\n" +
            "            background: #EF4444;\n" +
            "            border: 3px solid #FFFFFF;\n" +
            "            box-shadow: 0 0 20px #EF4444;\n" +
            "            display: flex;\n" +
            "            align-items: center;\n" +
            "            justify-content: center;\n" +
            "            font-size: 14px;\n" +
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
            "        let map = L.map('map', { zoomControl: false, attributionControl: false }).setView([__INIT_LAT__, __INIT_LNG__], 14);\n" +
            "        L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}', { maxZoom: 19, maxNativeZoom: 16 }).addTo(map);\n" +
            "        L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Reference/MapServer/tile/{z}/{y}/{x}', { maxZoom: 19, maxNativeZoom: 16 }).addTo(map);\n" +
            "\n" +
            "        let trail = [];\n" +
            "        let polyline = null;\n" +
            "        let movingMarker = null;\n" +
            "        let incidentMarkers = [];\n" +
            "        let breadcrumbCircles = [];\n" +
            "\n" +
            "        window.initDefaultLocation = function(lat, lng, name) {\n" +
            "            if (movingMarker) map.removeLayer(movingMarker);\n" +
            "            const wardIcon = L.divIcon({ className: 'ward-marker', iconSize: [22, 22], iconAnchor: [11, 11] });\n" +
            "            movingMarker = L.marker([lat, lng], { icon: wardIcon, zIndexOffset: 1000 }).addTo(map);\n" +
            "            movingMarker.bindPopup('<b>' + (name || 'Current Position') + '</b><br>Live GPS Anchor').openPopup();\n" +
            "            map.setView([lat, lng], 15);\n" +
            "        };\n" +
            "\n" +
            "        window.initReplayMap = function(data) {\n" +
            "            trail = data || [];\n" +
            "            if (polyline) { map.removeLayer(polyline); polyline = null; }\n" +
            "            if (movingMarker) { map.removeLayer(movingMarker); movingMarker = null; }\n" +
            "            incidentMarkers.forEach(m => map.removeLayer(m));\n" +
            "            incidentMarkers = [];\n" +
            "            breadcrumbCircles.forEach(c => map.removeLayer(c));\n" +
            "            breadcrumbCircles = [];\n" +
            "\n" +
            "            if (!trail || trail.length === 0) {\n" +
            "                window.initDefaultLocation(__INIT_LAT__, __INIT_LNG__, 'Protected User');\n" +
            "                return;\n" +
            "            }\n" +
            "\n" +
            "            const latlngs = trail.map(pt => [pt.latitude, pt.longitude]);\n" +
            "            if (latlngs.length > 1) {\n" +
            "                polyline = L.polyline(latlngs, {\n" +
            "                    color: '#FF2E93',\n" +
            "                    weight: 4,\n" +
            "                    opacity: 0.85,\n" +
            "                    dashArray: '8, 6'\n" +
            "                }).addTo(map);\n" +
            "            }\n" +
            "\n" +
            "            trail.forEach((pt, idx) => {\n" +
            "                if (pt.is_incident) {\n" +
            "                    const incIcon = L.divIcon({ className: 'incident-marker', html: '🚨', iconSize: [26, 26], iconAnchor: [13, 13] });\n" +
            "                    const m = L.marker([pt.latitude, pt.longitude], { icon: incIcon, zIndexOffset: 900 }).addTo(map);\n" +
            "                    m.bindPopup('<b>🚨 SOS Incident Point</b><br>' + (pt.formatted_time || '') + '<br>' + (pt.address || ''));\n" +
            "                    incidentMarkers.push(m);\n" +
            "                } else {\n" +
            "                    const c = L.circleMarker([pt.latitude, pt.longitude], {\n" +
            "                        radius: 5,\n" +
            "                        color: '#FF2E93',\n" +
            "                        fillColor: '#FF65A7',\n" +
            "                        fillOpacity: 0.75,\n" +
            "                        weight: 1.5\n" +
            "                    }).addTo(map);\n" +
            "                    c.bindPopup('<b>Point ' + (idx + 1) + '</b><br>' + (pt.formatted_time || '') + '<br>' + (pt.address || ''));\n" +
            "                    breadcrumbCircles.push(c);\n" +
            "                }\n" +
            "            });\n" +
            "\n" +
            "            const wardIcon = L.divIcon({ className: 'ward-marker', iconSize: [22, 22], iconAnchor: [11, 11] });\n" +
            "            movingMarker = L.marker([trail[0].latitude, trail[0].longitude], { icon: wardIcon, zIndexOffset: 1000 }).addTo(map);\n" +
            "            movingMarker.bindPopup('<b>' + (trail[0].formatted_time || 'Starting Point') + '</b><br>' + (trail[0].address || ''));\n" +
            "\n" +
            "            if (polyline && latlngs.length > 1) {\n" +
            "                map.fitBounds(polyline.getBounds(), { padding: [50, 50] });\n" +
            "            } else if (trail.length === 1) {\n" +
            "                map.setView([trail[0].latitude, trail[0].longitude], 16);\n" +
            "            }\n" +
            "        };\n" +
            "\n" +
            "        window.seekToPoint = function(idx) {\n" +
            "            if (!trail || !trail[idx] || !movingMarker) return;\n" +
            "            const pt = trail[idx];\n" +
            "            movingMarker.setLatLng([pt.latitude, pt.longitude]);\n" +
            "            movingMarker.bindPopup('<b>' + (pt.formatted_time || '') + '</b><br>' + (pt.address || '')).openPopup();\n" +
            "            if (!map.getBounds().contains([pt.latitude, pt.longitude])) {\n" +
            "                map.panTo([pt.latitude, pt.longitude], { animate: true });\n" +
            "            }\n" +
            "        };\n" +
            "\n" +
            "        window.recenterMap = function() {\n" +
            "            if (polyline && trail && trail.length > 1) {\n" +
            "                map.fitBounds(polyline.getBounds(), { padding: [50, 50] });\n" +
            "            } else if (trail && trail.length >= 1) {\n" +
            "                map.setView([trail[0].latitude, trail[0].longitude], 16);\n" +
            "            } else {\n" +
            "                map.setView([%.5f, %.5f], 16);\n" +
            "            }\n" +
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
            wardLat = getArguments().getDouble("WARD_LAT", 0.0);
            wardLng = getArguments().getDouble("WARD_LNG", 0.0);
        }

        // If ward coordinates not passed or are 0, use user's local GPS cache or best last known
        if (wardLat == 0.0 || wardLng == 0.0) {
            wardLat = Prefs.getFloat(Constants.PREFS_LAST_LATITUDE, 0f);
            wardLng = Prefs.getFloat(Constants.PREFS_LAST_LONGITUDE, 0f);
        }
        if (wardLat == 0.0 || wardLng == 0.0) {
            if (getContext() != null) {
                android.location.Location best = com.android.sheguard.util.LocationHelper.getBestLastKnownLocation(requireContext());
                if (best != null && (best.getLatitude() != 0.0 || best.getLongitude() != 0.0)) {
                    wardLat = best.getLatitude();
                    wardLng = best.getLongitude();
                } else {
                    wardLat = 17.4482;
                    wardLng = 78.3914;
                }
            }
        }
        if (wardPhone == null || wardPhone.trim().isEmpty()) {
            wardPhone = Prefs.getString(Constants.PREFS_USER_PHONE, "");
            if (wardPhone.isEmpty()) {
                wardPhone = Prefs.getString(Constants.PREFS_USER_EMAIL, "+919876543210");
            }
        }

        binding.tvReplayTitle.setText(wardName);
        binding.tvReplaySubtitle.setText("24h Telemetry Replay • Movement Review");

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.layoutTopBar, (v, insets) -> {
            int statusBarHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), statusBarHeight, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

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
                } else if (binding != null) {
                    binding.webViewMap.evaluateJavascript(
                            String.format(Locale.US, "window.initDefaultLocation(%.5f, %.5f, '%s');",
                                    wardLat, wardLng, wardName.replace("'", "\\'")),
                            null
                    );
                }
            }
        });

        try {
            String mapHtml = LEAFLET_HTML
                    .replace("__INIT_LAT__", String.format(Locale.US, "%.5f", wardLat))
                    .replace("__INIT_LNG__", String.format(Locale.US, "%.5f", wardLng));
            binding.webViewMap.loadDataWithBaseURL("https://guardianai.app", mapHtml, "text/html", "UTF-8", null);
        } catch (Exception e) {
            Log.e(TAG, "Error loading replay map: " + e.getMessage(), e);
        }
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

        binding.btnExportPdf.setOnClickListener(v -> exportForensicPdf());

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

    private void exportForensicPdf() {
        if (getContext() == null || getActivity() == null) return;

        String guardianPhone = Prefs.getString(Constants.PREFS_USER_PHONE, "");
        if (guardianPhone.isEmpty()) {
            guardianPhone = wardPhone;
        }
        String debriefUrl = ApiClient.getBaseUrl() + "location/debrief/?ward_phone=" + Uri.encode(wardPhone) +
                "&guardian_phone=" + Uri.encode(guardianPhone) + "&hours=24&format=html";

        Toast.makeText(getContext(), "Generating Forensic PDF Debrief...", Toast.LENGTH_SHORT).show();

        try {
            PrintManager printManager = (PrintManager) requireActivity().getSystemService(Context.PRINT_SERVICE);
            if (printManager != null) {
                WebView printWebView = new WebView(requireContext());
                printWebView.getSettings().setJavaScriptEnabled(true);
                printWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        try {
                            String safeName = (wardName != null ? wardName.replaceAll("[^a-zA-Z0-9]", "_") : "Ward");
                            String jobName = "Forensic_Debrief_" + safeName;
                            PrintDocumentAdapter printAdapter = printWebView.createPrintDocumentAdapter(jobName);
                            printManager.print(jobName, printAdapter, new PrintAttributes.Builder()
                                     .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                    .build());
                        } catch (Exception e) {
                            openDebriefInBrowser(debriefUrl);
                        }
                    }

                    @Override
                    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                        openDebriefInBrowser(debriefUrl);
                    }
                });
                printWebView.loadUrl(debriefUrl);
            } else {
                openDebriefInBrowser(debriefUrl);
            }
        } catch (Exception e) {
            openDebriefInBrowser(debriefUrl);
        }
    }

    private void openDebriefInBrowser(String url) {
        if (getContext() == null) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Could not open document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchLocationTrail() {
        String guardianPhone = Prefs.getString(Constants.PREFS_USER_PHONE, "");
        if (guardianPhone.isEmpty()) {
            guardianPhone = wardPhone;
        }
        if (wardPhone.isEmpty()) {
            wardPhone = guardianPhone;
        }
        if (wardPhone.isEmpty()) {
            wardPhone = Constants.DEMO_USER_PHONE;
            guardianPhone = Constants.DEMO_USER_PHONE;
        }
        binding.pbLoading.setVisibility(View.VISIBLE);

        ApiClient.getLocationHistory(wardPhone, guardianPhone, 24, (success, data, message) -> {
            if (binding == null || getContext() == null) return;
            binding.pbLoading.setVisibility(View.GONE);

            if (success && data != null && data.has("trail")) {
                trailData = data.getAsJsonArray("trail");
                int totalPoints = trailData.size();
                binding.tvPointsBadge.setText(totalPoints + " Checkpoints");

                if (totalPoints > 0) {
                    binding.seekbarReplay.setEnabled(totalPoints > 1);
                    binding.btnPlayPause.setEnabled(totalPoints > 1);
                    binding.btnStepPrev.setEnabled(totalPoints > 1);
                    binding.btnStepNext.setEnabled(totalPoints > 1);
                    binding.seekbarReplay.setMax(Math.max(1, totalPoints - 1));
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
                } else {
                    binding.tvTelemetryTime.setText("No movement in last 24h");
                    binding.tvTelemetryBattery.setText(Prefs.getInt(Constants.PREFS_BATTERY_LEVEL, 85) + "%");
                    binding.tvTelemetryAddress.setText("Showing last known GPS coordinates");
                    binding.tvTelemetryCheckpoint.setText("0 Checkpoints");
                    binding.tvTimeStart.setText("--");
                    binding.tvTimeEnd.setText("Now");
                    binding.seekbarReplay.setEnabled(false);
                    binding.btnPlayPause.setEnabled(false);
                    binding.btnStepPrev.setEnabled(false);
                    binding.btnStepNext.setEnabled(false);
                    if (isMapReady && binding.webViewMap != null) {
                        binding.webViewMap.evaluateJavascript(
                                String.format(Locale.US, "window.initDefaultLocation(%.5f, %.5f, '%s');",
                                        wardLat, wardLng, wardName.replace("'", "\\'")),
                                null
                        );
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
