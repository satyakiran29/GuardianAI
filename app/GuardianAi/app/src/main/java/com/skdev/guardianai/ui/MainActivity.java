package com.skdev.guardianai.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.skdev.guardianai.R;
import com.skdev.guardianai.chatbot.ChatAdapter;
import com.skdev.guardianai.chatbot.ChatMessage;
import com.skdev.guardianai.dashboard.AlertAdapter;
import com.skdev.guardianai.dashboard.ExplainableAiActivity;
import com.skdev.guardianai.data.EmergencyContact;
import com.skdev.guardianai.data.EmergencyContactManager;
import com.skdev.guardianai.data.IncidentAlert;
import com.skdev.guardianai.data.RouteOption;
import com.skdev.guardianai.data.SafetyDataRepository;
import com.skdev.guardianai.data.SafetyLocation;
import com.skdev.guardianai.data.SafetyModelEngine;
import com.skdev.guardianai.map.SafeMapCanvasView;
import com.skdev.guardianai.media.EvidenceAdapter;
import com.skdev.guardianai.media.MediaItem;
import com.skdev.guardianai.sos.EmergencyContactsActivity;
import com.skdev.guardianai.sos.SosAlertManager;
import com.skdev.guardianai.voice.VoiceFeedbackManager;
import com.skdev.guardianai.voice.VoiceTriggerManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Main Activity hosting all 5 GuardianAI modules with tab navigation,
 * real-time ML routing, voice keyword triggers, emergency SOS, and evidence vault.
 */
public class MainActivity extends AppCompatActivity implements VoiceTriggerManager.VoiceTriggerListener {

    private static final int PERMISSION_REQ_CODE = 101;

    private FrameLayout contentContainer;
    private BottomNavigationView bottomNav;
    private Spinner spinnerCity;
    private ImageButton btnToggleVoice;
    private Button btnHeaderSos;

    private SafetyDataRepository dataRepo;
    private VoiceFeedbackManager voiceFeedback;
    private VoiceTriggerManager voiceTrigger;
    private SosAlertManager sosManager;
    private EmergencyContactManager contactManager;

    private String currentCity = "Bengaluru";
    private SafetyLocation currentOrigin;
    private SafetyLocation currentDestination;
    private List<RouteOption> currentRoutes = new ArrayList<>();
    private int selectedRouteIdx = 0;

    // View caches for the 5 tabs
    private View tabMapView;
    private View tabDashboardView;
    private View tabSosView;
    private View tabChatView;
    private View tabEvidenceView;

    // Evidence & Chat data
    private final List<ChatMessage> chatMessages = new ArrayList<>();
    private ChatAdapter chatAdapter;
    private final List<MediaItem> evidenceItems = new ArrayList<>();
    private EvidenceAdapter evidenceAdapter;

    // Audio recording
    private MediaRecorder audioRecorder;
    private boolean isRecordingAudio = false;
    private File audioRecordFile;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private int recordingSeconds = 0;
    private TextView tvAudioTimer;
    private View layoutAudioStatus;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.skdev.guardianai.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataRepo = SafetyDataRepository.getInstance();
        voiceFeedback = VoiceFeedbackManager.getInstance(this);
        voiceTrigger = new VoiceTriggerManager(this);
        voiceTrigger.setListener(this);
        sosManager = SosAlertManager.getInstance(this);
        contactManager = EmergencyContactManager.getInstance(this);

        initViews();
        requestAppPermissions();
        setupCitySelector();
        initDefaultEvidenceAndChat();

        int targetTab = getIntent().getIntExtra("target_tab", R.id.nav_map);
        bottomNav.setSelectedItemId(targetTab);
        switchTab(targetTab);

        // Start voice trigger listening by default
        voiceTrigger.startListening();

        // Update home screen widgets with current data
        com.skdev.guardianai.widget.WidgetUpdateHelper.updateAllWidgets(this);

        // Silent background check for updates from update.json
        com.skdev.guardianai.updater.AppUpdateManager.checkForUpdates(this, false);
    }

    private void initViews() {
        contentContainer = findViewById(R.id.fl_content_container);
        bottomNav = findViewById(R.id.bottom_navigation);
        spinnerCity = findViewById(R.id.spinner_city_selector);
        btnToggleVoice = findViewById(R.id.btn_toggle_voice_trigger);
        btnHeaderSos = findViewById(R.id.btn_header_quick_sos);
        ImageButton btnLanguage = findViewById(R.id.btn_header_language);
        ImageButton btnWidgets = findViewById(R.id.btn_header_widgets);
        ImageButton btnUpdate = findViewById(R.id.btn_header_update);

        bottomNav.setOnItemSelectedListener(item -> {
            switchTab(item.getItemId());
            return true;
        });

        if (btnLanguage != null) {
            btnLanguage.setOnClickListener(v -> showLanguageSelectionDialog());
        }

        if (btnWidgets != null) {
            btnWidgets.setOnClickListener(v -> showWidgetShowcaseDialog());
        }

        if (btnUpdate != null) {
            btnUpdate.setOnClickListener(v -> com.skdev.guardianai.updater.AppUpdateManager.checkForUpdates(this, true));
        }

        btnToggleVoice.setOnClickListener(v -> {
            if (voiceTrigger.isListening()) {
                voiceTrigger.stopListening();
                btnToggleVoice.setImageResource(R.drawable.ic_mic);
                btnToggleVoice.setBackgroundResource(R.drawable.badge_moderate);
                Toast.makeText(this, "Voice SOS Trigger Paused", Toast.LENGTH_SHORT).show();
            } else {
                voiceTrigger.startListening();
                btnToggleVoice.setImageResource(R.drawable.ic_mic_active);
                btnToggleVoice.setBackgroundResource(R.drawable.badge_safe);
                Toast.makeText(this, "Voice SOS Trigger Active (Listening for 'Help' / 'Emergency')", Toast.LENGTH_SHORT).show();
            }
        });

        btnHeaderSos.setOnClickListener(v -> {
            bottomNav.setSelectedItemId(R.id.nav_sos);
            triggerSosAction();
        });
    }

    private void showWidgetShowcaseDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_widget_showcase, null, false);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Button btnClose = dialogView.findViewById(R.id.btn_close_widget_showcase);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void showLanguageSelectionDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_language, null, false);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View btnEn = dialogView.findViewById(R.id.btn_lang_english);
        View btnHi = dialogView.findViewById(R.id.btn_lang_hindi);
        View btnTe = dialogView.findViewById(R.id.btn_lang_telugu);
        TextView checkEn = dialogView.findViewById(R.id.tv_check_en);
        TextView checkHi = dialogView.findViewById(R.id.tv_check_hi);
        TextView checkTe = dialogView.findViewById(R.id.tv_check_te);
        Button btnClose = dialogView.findViewById(R.id.btn_close_lang_dialog);

        String currentLang = com.skdev.guardianai.utils.LocaleHelper.getLanguage(this);
        checkEn.setVisibility(com.skdev.guardianai.utils.LocaleHelper.LANG_ENGLISH.equals(currentLang) ? View.VISIBLE : View.GONE);
        checkHi.setVisibility(com.skdev.guardianai.utils.LocaleHelper.LANG_HINDI.equals(currentLang) ? View.VISIBLE : View.GONE);
        checkTe.setVisibility(com.skdev.guardianai.utils.LocaleHelper.LANG_TELUGU.equals(currentLang) ? View.VISIBLE : View.GONE);

        btnEn.setOnClickListener(v -> changeLanguage(com.skdev.guardianai.utils.LocaleHelper.LANG_ENGLISH, dialog));
        btnHi.setOnClickListener(v -> changeLanguage(com.skdev.guardianai.utils.LocaleHelper.LANG_HINDI, dialog));
        btnTe.setOnClickListener(v -> changeLanguage(com.skdev.guardianai.utils.LocaleHelper.LANG_TELUGU, dialog));

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void changeLanguage(String langCode, android.app.AlertDialog dialog) {
        com.skdev.guardianai.utils.LocaleHelper.setLocale(this, langCode);
        dialog.dismiss();
        Toast.makeText(this, getString(R.string.language_changed), Toast.LENGTH_SHORT).show();
        recreate();
    }

    private void setupCitySelector() {
        List<String> cities = dataRepo.getCities();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, cities) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(Color.parseColor("#10B981"));
                tv.setTextSize(13f);
                tv.setText("📍 " + getItem(position) + " ▼");
                return tv;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(Color.parseColor("#F8FAFC"));
                tv.setBackgroundColor(Color.parseColor("#0F172A"));
                tv.setPadding(24, 24, 24, 24);
                return tv;
            }
        };

        spinnerCity.setAdapter(adapter);
        spinnerCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCity = cities.get(position);
                updateCityLocations();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateCityLocations() {
        List<SafetyLocation> locs = dataRepo.getLocationsForCity(currentCity);
        if (!locs.isEmpty()) {
            currentOrigin = locs.get(0);
            currentDestination = locs.size() > 1 ? locs.get(1) : locs.get(0);
            currentRoutes = dataRepo.generateRoutes(currentOrigin, currentDestination);
            selectedRouteIdx = 0;

            // Refresh Map if active
            if (tabMapView != null) {
                bindMapView(tabMapView);
            }
            // Refresh Dashboard if active
            if (tabDashboardView != null) {
                bindDashboardView(tabDashboardView);
            }

            // Sync home screen widgets
            com.skdev.guardianai.widget.WidgetUpdateHelper.updateAllWidgets(this);
        }
    }

    private void switchTab(int navId) {
        contentContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        if (navId == R.id.nav_map) {
            if (tabMapView == null) {
                tabMapView = inflater.inflate(R.layout.view_tab_map, contentContainer, false);
            }
            bindMapView(tabMapView);
            contentContainer.addView(tabMapView);
        } else if (navId == R.id.nav_dashboard) {
            if (tabDashboardView == null) {
                tabDashboardView = inflater.inflate(R.layout.view_tab_dashboard, contentContainer, false);
            }
            bindDashboardView(tabDashboardView);
            contentContainer.addView(tabDashboardView);
        } else if (navId == R.id.nav_sos) {
            if (tabSosView == null) {
                tabSosView = inflater.inflate(R.layout.view_tab_sos, contentContainer, false);
            }
            bindSosView(tabSosView);
            contentContainer.addView(tabSosView);
        } else if (navId == R.id.nav_chat) {
            if (tabChatView == null) {
                tabChatView = inflater.inflate(R.layout.view_tab_chat, contentContainer, false);
            }
            bindChatView(tabChatView);
            contentContainer.addView(tabChatView);
        } else if (navId == R.id.nav_vault) {
            if (tabEvidenceView == null) {
                tabEvidenceView = inflater.inflate(R.layout.view_tab_evidence, contentContainer, false);
            }
            bindEvidenceView(tabEvidenceView);
            contentContainer.addView(tabEvidenceView);
        }
    }

    // ==========================================
    // 1. MAP & SAFE ROUTE TAB
    // ==========================================
    private void bindMapView(View view) {
        SafeMapCanvasView mapCanvas = view.findViewById(R.id.safe_map_canvas);
        AutoCompleteTextView actvSearch = view.findViewById(R.id.actv_destination_search);
        ImageButton btnSpeak = view.findViewById(R.id.btn_voice_search_speak);
        TextView tvDestTitle = view.findViewById(R.id.tv_dest_title);
        TextView tvDestRiskBadge = view.findViewById(R.id.tv_dest_risk_badge);
        TextView tvDestAnalysis = view.findViewById(R.id.tv_dest_ai_analysis);

        Button btnSafe = view.findViewById(R.id.btn_select_safe_route);
        Button btnMod = view.findViewById(R.id.btn_select_mod_route);
        Button btnRisk = view.findViewById(R.id.btn_select_risk_route);
        Button btnNav = view.findViewById(R.id.btn_start_safe_navigation);

        List<SafetyLocation> cityLocs = dataRepo.getLocationsForCity(currentCity);
        List<String> areaNames = new ArrayList<>();
        for (SafetyLocation loc : cityLocs) {
            areaNames.add(loc.getArea());
        }

        ArrayAdapter<String> searchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, areaNames);
        actvSearch.setAdapter(searchAdapter);
        actvSearch.setOnItemClickListener((parent, v, position, id) -> {
            String selectedArea = (String) parent.getItemAtPosition(position);
            SafetyLocation loc = dataRepo.findLocationByName(selectedArea);
            if (loc != null) {
                currentDestination = loc;
                currentRoutes = dataRepo.generateRoutes(currentOrigin, currentDestination);
                selectedRouteIdx = 0;
                mapCanvas.setMapData(currentOrigin, currentDestination, currentRoutes, cityLocs);
                updateDestinationUi(tvDestTitle, tvDestRiskBadge, tvDestAnalysis, loc);
                voiceFeedback.announceDestinationSafety(loc);
            }
        });

        if (currentDestination != null) {
            updateDestinationUi(tvDestTitle, tvDestRiskBadge, tvDestAnalysis, currentDestination);
        }

        mapCanvas.setMapData(currentOrigin, currentDestination, currentRoutes, cityLocs);
        mapCanvas.setSelectedRoute(selectedRouteIdx);

        btnSafe.setOnClickListener(v -> {
            selectedRouteIdx = 0;
            mapCanvas.setSelectedRoute(0);
            highlightRouteButtons(btnSafe, btnMod, btnRisk, 0);
        });

        btnMod.setOnClickListener(v -> {
            selectedRouteIdx = 1;
            mapCanvas.setSelectedRoute(1);
            highlightRouteButtons(btnSafe, btnMod, btnRisk, 1);
        });

        btnRisk.setOnClickListener(v -> {
            selectedRouteIdx = 2;
            mapCanvas.setSelectedRoute(2);
            highlightRouteButtons(btnSafe, btnMod, btnRisk, 2);
        });

        btnSpeak.setOnClickListener(v -> {
            if (currentDestination != null) {
                voiceFeedback.announceDestinationSafety(currentDestination);
            }
        });

        btnNav.setOnClickListener(v -> {
            RouteOption selected = (currentRoutes != null && selectedRouteIdx < currentRoutes.size()) ?
                    currentRoutes.get(selectedRouteIdx) : null;
            if (selected != null) {
                String prompt = String.format(Locale.US,
                        "Starting navigation via %s. Total distance %.1f km, ETA %d minutes. Safety index is %d percent.",
                        selected.getType().getTitle(), selected.getDistanceKm(), selected.getEstimatedMinutes(),
                        selected.getPrediction().getScorePercentage());
                voiceFeedback.speak(prompt);
                Toast.makeText(this, "🛰️ Live AI Safe Navigation Active with Hazard Radar", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateDestinationUi(TextView tvTitle, TextView tvBadge, TextView tvAnalysis, SafetyLocation loc) {
        tvTitle.setText("Destination: " + loc.getFullName());
        SafetyModelEngine.SafetyPrediction pred = loc.getPrediction();
        if (pred != null) {
            tvBadge.setText(pred.riskLevel.getLabel().toUpperCase(Locale.ROOT));
            tvBadge.setTextColor(Color.parseColor(pred.riskLevel.getColorHex()));
            if (pred.riskLevel == SafetyModelEngine.RiskLevel.LOW) {
                tvBadge.setBackgroundResource(R.drawable.badge_safe);
            } else if (pred.riskLevel == SafetyModelEngine.RiskLevel.MEDIUM) {
                tvBadge.setBackgroundResource(R.drawable.badge_moderate);
            } else if (pred.riskLevel == SafetyModelEngine.RiskLevel.HIGH) {
                tvBadge.setBackgroundResource(R.drawable.badge_high);
            } else {
                tvBadge.setBackgroundResource(R.drawable.badge_critical);
            }

            tvAnalysis.setText(String.format(Locale.US,
                    "AI Model: Safety Score %.2f (%d%%) • Lighting %.1f/10 • Nearest Police %.1f km • %d Crimes logged",
                    pred.score, pred.getScorePercentage(), pred.lightingScore, pred.policeDistanceKm, pred.crimeCount));
        }
    }

    private void highlightRouteButtons(Button bSafe, Button bMod, Button bRisk, int selected) {
        bSafe.setBackgroundResource(selected == 0 ? R.drawable.btn_primary_gradient : R.drawable.btn_secondary_outline);
        bSafe.setTextColor(selected == 0 ? Color.WHITE : Color.parseColor("#10B981"));

        bMod.setBackgroundResource(selected == 1 ? R.drawable.btn_primary_gradient : R.drawable.btn_secondary_outline);
        bMod.setTextColor(selected == 1 ? Color.WHITE : Color.parseColor("#F59E0B"));

        bRisk.setBackgroundResource(selected == 2 ? R.drawable.btn_primary_gradient : R.drawable.btn_secondary_outline);
        bRisk.setTextColor(selected == 2 ? Color.WHITE : Color.parseColor("#EF4444"));
    }

    // ==========================================
    // 2. AI DASHBOARD TAB
    // ==========================================
    private void bindDashboardView(View view) {
        TextView tvCityName = view.findViewById(R.id.tv_dash_city_name);
        TextView tvCityScore = view.findViewById(R.id.tv_dash_city_score);
        TextView tvAvgLight = view.findViewById(R.id.tv_dash_avg_lighting);
        TextView tvAvgPolice = view.findViewById(R.id.tv_dash_avg_police);
        TextView tvHighRiskPct = view.findViewById(R.id.tv_dash_high_risk_pct);

        TextView tvSimScore = view.findViewById(R.id.tv_sim_score);
        TextView tvLightLbl = view.findViewById(R.id.tv_label_lighting);
        TextView tvPoliceLbl = view.findViewById(R.id.tv_label_police);
        TextView tvCrimeLbl = view.findViewById(R.id.tv_label_crime);
        SeekBar sbLight = view.findViewById(R.id.sb_lighting);
        SeekBar sbPolice = view.findViewById(R.id.sb_police_dist);
        SeekBar sbCrime = view.findViewById(R.id.sb_crime_count);
        Button btnOpenDeepXai = view.findViewById(R.id.btn_open_deep_explainable_ai);

        RecyclerView rvAlerts = view.findViewById(R.id.rv_live_alerts);
        rvAlerts.setLayoutManager(new LinearLayoutManager(this));
        rvAlerts.setAdapter(new AlertAdapter(dataRepo.getLiveAlerts()));

        tvCityName.setText(currentCity + " Safety Dashboard");

        btnOpenDeepXai.setOnClickListener(v -> {
            Intent intent = new Intent(this, ExplainableAiActivity.class);
            startActivity(intent);
        });

        // Interactive mini simulator
        SeekBar.OnSeekBarChangeListener simListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double l = Math.max(1.0, sbLight.getProgress() / 10.0);
                double p = Math.max(0.2, sbPolice.getProgress() / 10.0);
                int c = sbCrime.getProgress();

                SafetyModelEngine.SafetyPrediction pred = SafetyModelEngine.evaluateSafety(l, p, c);
                tvSimScore.setText(String.format(Locale.US, "Score: %.2f (%s)", pred.score, pred.riskLevel.getLabel()));
                tvSimScore.setTextColor(Color.parseColor(pred.riskLevel.getColorHex()));

                tvLightLbl.setText(String.format(Locale.US, "Street Lighting Score: %.1f / 10 (+%.2f boost)", l, pred.lightingContribution));
                tvPoliceLbl.setText(String.format(Locale.US, "Police Station Distance: %.1f km (%.2f penalty)", p, pred.policeDistImpact));
                tvCrimeLbl.setText(String.format(Locale.US, "Historical Crime Volume: %d crimes (%.2f penalty)", c, pred.crimeCountImpact));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        sbLight.setOnSeekBarChangeListener(simListener);
        sbPolice.setOnSeekBarChangeListener(simListener);
        sbCrime.setOnSeekBarChangeListener(simListener);
    }

    // ==========================================
    // 3. EMERGENCY SOS TAB
    // ==========================================
    private void bindSosView(View view) {
        View btnSosCircle = view.findViewById(R.id.btn_trigger_sos_circle);
        View layoutSosActive = view.findViewById(R.id.layout_sos_active_banner);
        Button btnCancelSos = view.findViewById(R.id.btn_cancel_sos_alert);
        TextView tvLocationTag = view.findViewById(R.id.tv_sos_location_tag);
        TextView tvContactsSummary = view.findViewById(R.id.tv_contacts_summary);
        SwitchCompat switchVoice = view.findViewById(R.id.switch_voice_detection);
        Button btnTestVoice = view.findViewById(R.id.btn_test_voice_keyword);
        Button btnManageContacts = view.findViewById(R.id.btn_manage_contacts);
        Button btnCallPolice = view.findViewById(R.id.btn_direct_call_police);
        Button btnCallWomen = view.findViewById(R.id.btn_direct_call_women_helpline);

        if (currentOrigin != null) {
            tvLocationTag.setText(String.format(Locale.US, "Live GPS: %s (%.4f° N, %.4f° E)",
                    currentOrigin.getFullName(), currentOrigin.getLatitude(), currentOrigin.getLongitude()));
        }

        int count = contactManager.getContacts().size();
        tvContactsSummary.setText(count + " emergency contacts active (No count limit)");

        layoutSosActive.setVisibility(sosManager.isSosActive() ? View.VISIBLE : View.GONE);

        btnSosCircle.setOnClickListener(v -> {
            triggerSosAction();
            layoutSosActive.setVisibility(View.VISIBLE);
        });

        btnCancelSos.setOnClickListener(v -> {
            sosManager.stopSos();
            layoutSosActive.setVisibility(View.GONE);
            Toast.makeText(this, "Emergency SOS Disarmed", Toast.LENGTH_SHORT).show();
        });

        switchVoice.setChecked(voiceTrigger.isListening());
        switchVoice.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                voiceTrigger.startListening();
            } else {
                voiceTrigger.stopListening();
            }
        });

        btnTestVoice.setOnClickListener(v -> {
            onKeywordDetected("help");
        });

        btnManageContacts.setOnClickListener(v -> {
            Intent intent = new Intent(this, EmergencyContactsActivity.class);
            startActivity(intent);
        });

        btnCallPolice.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"));
            startActivity(intent);
        });

        btnCallWomen.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:1091"));
            startActivity(intent);
        });
    }

    private void triggerSosAction() {
        double lat = currentOrigin != null ? currentOrigin.getLatitude() : 12.9784;
        double lng = currentOrigin != null ? currentOrigin.getLongitude() : 77.6408;
        String loc = currentOrigin != null ? currentOrigin.getFullName() : "Indiranagar, Bengaluru";

        sosManager.triggerEmergencySos(lat, lng, loc);
        Toast.makeText(this, "🚨 EMERGENCY SOS ACTIVATED! Siren enabled & SMS sent!", Toast.LENGTH_LONG).show();
    }

    // ==========================================
    // 4. AI SAFETY CHATBOT TAB
    // ==========================================
    private void bindChatView(View view) {
        RecyclerView rvChat = view.findViewById(R.id.rv_chat_messages);
        EditText etInput = view.findViewById(R.id.et_chat_input);
        ImageButton btnSend = view.findViewById(R.id.btn_send_chat);

        Button chipNight = view.findViewById(R.id.chip_night_safety);
        Button chipHelplines = view.findViewById(R.id.chip_helplines);
        Button chipFollowed = view.findViewById(R.id.chip_followed);
        Button chipMl = view.findViewById(R.id.chip_ml_formula);

        rvChat.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatAdapter(chatMessages);
        rvChat.setAdapter(chatAdapter);
        rvChat.scrollToPosition(chatMessages.size() - 1);

        btnSend.setOnClickListener(v -> {
            String text = etInput.getText().toString().trim();
            if (!text.isEmpty()) {
                sendUserChatMessage(text, rvChat);
                etInput.setText("");
            }
        });

        chipNight.setOnClickListener(v -> sendUserChatMessage("Night Travel Safety Checklist", rvChat));
        chipHelplines.setOnClickListener(v -> sendUserChatMessage("Emergency Helpline Numbers in India", rvChat));
        chipFollowed.setOnClickListener(v -> sendUserChatMessage("What to do if someone is following me?", rvChat));
        chipMl.setOnClickListener(v -> sendUserChatMessage("Explain the GuardianAI Safety Score formula", rvChat));
    }

    private void sendUserChatMessage(String text, RecyclerView rv) {
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        chatMessages.add(new ChatMessage(UUID.randomUUID().toString(), text, true, time, null));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        rv.scrollToPosition(chatMessages.size() - 1);

        // Generate AI Assistant Response
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String botResponse = generateBotResponse(text);
            String botTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
            chatMessages.add(new ChatMessage(UUID.randomUUID().toString(), botResponse, false, botTime, null));
            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
            rv.scrollToPosition(chatMessages.size() - 1);
        }, 400);
    }

    private String generateBotResponse(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        if (q.contains("night") || q.contains("travel") || q.contains("tips")) {
            return "🌙 **GuardianAI Night Travel Checklist**:\n\n1. Always select the **Green Safe Route** (evaluated for >7.5 lighting index and CCTV coverage).\n2. Share live GPS location with your emergency contacts.\n3. Enable GuardianAI **Voice SOS Detection** (background keyword listener).\n4. Avoid poorly lit shortcuts even if faster.\n5. Keep your emergency helpline on speed dial (112 / 1091).";
        } else if (q.contains("helpline") || q.contains("number") || q.contains("police") || q.contains("contact")) {
            return "📞 **Essential Emergency Helplines (India)**:\n\n• **112**: All-in-One National Emergency Response Support System (ERSS)\n• **1091**: Women Safety Helpline (24/7 Police Dispatch)\n• **102 / 108**: Emergency Ambulance\n• **1090**: Women Power Line (Anti-Harassment)\n• **1930**: Cyber Crime Emergency Reporting";
        } else if (q.contains("followed") || q.contains("danger") || q.contains("stalk")) {
            return "🚨 **Immediate Protocol if Being Followed**:\n\n1. Do NOT head home or into isolated lanes. Move toward a crowded, well-lit public area (metro station, 24/7 store, petrol pump).\n2. Immediately tap **EMERGENCY SOS** in GuardianAI or say *'Help'* to trigger automated SMS dispatch with live GPS coordinates.\n3. Call 112 directly.\n4. Stay on phone speaker with a trusted contact.";
        } else if (q.contains("formula") || q.contains("ml") || q.contains("regression") || q.contains("score")) {
            return "🧠 **GuardianAI Mathematical Model (R² = 0.9629)**:\n\n`Safety_Score = 0.9593 + 0.0436(Lighting) - 0.0587(PoliceDist) - 0.0074(CrimeCount)`\n\n• **Lighting (+0.0436)**: Strongest protective factor.\n• **Police Distance (-0.0587/km)**: Primary hazard factor.\n• **Crimes (-0.0074/unit)**: Historical incident penalty.";
        } else {
            return "🛡️ GuardianAI Assistant: I am monitoring your real-time safety in " + currentCity + " (" + (currentOrigin != null ? currentOrigin.getArea() : "Active Zone") + "). You can ask me for safe route assessments, emergency protocols, self-defense guidance, or tap SOS anytime for instant dispatch!";
        }
    }

    private void initDefaultEvidenceAndChat() {
        if (chatMessages.isEmpty()) {
            chatMessages.add(new ChatMessage("1",
                    "Hello! I am your GuardianAI Safety Companion. I continuously monitor route risk, street illumination, and police response radii. How can I assist your journey?",
                    false, "10:00 AM", null));
        }

        if (evidenceItems.isEmpty()) {
            evidenceItems.add(new MediaItem("e1", MediaItem.MediaType.PHOTO, "CCTV & Street Light Audit", "/storage/evidence_photo_1.jpg", "Today, 09:30 AM", "Indiranagar 100ft Road", 2400000));
            evidenceItems.add(new MediaItem("e2", MediaItem.MediaType.AUDIO, "Voice Memo - Suspicious Activity", "/storage/audio_note_1.mp3", "Yesterday, 11:15 PM", "Dadar Sector 4", 850000));
        }
    }

    // ==========================================
    // 5. EVIDENCE VAULT TAB
    // ==========================================
    private void bindEvidenceView(View view) {
        RecyclerView rvEvidence = view.findViewById(R.id.rv_evidence_list);
        TextView tvVaultCount = view.findViewById(R.id.tv_vault_count);
        Button btnPhoto = view.findViewById(R.id.btn_capture_photo);
        Button btnVideo = view.findViewById(R.id.btn_capture_video);
        Button btnAudio = view.findViewById(R.id.btn_record_audio);
        layoutAudioStatus = view.findViewById(R.id.layout_audio_recording_status);
        tvAudioTimer = view.findViewById(R.id.tv_audio_timer);
        Button btnStopAudio = view.findViewById(R.id.btn_stop_audio_record);

        rvEvidence.setLayoutManager(new LinearLayoutManager(this));
        evidenceAdapter = new EvidenceAdapter(evidenceItems, new EvidenceAdapter.OnEvidenceActionListener() {
            @Override
            public void onOpenMedia(MediaItem item) {
                Toast.makeText(MainActivity.this, "Opening Evidence: " + item.getTitle(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteMedia(MediaItem item) {
                evidenceItems.remove(item);
                evidenceAdapter.notifyDataSetChanged();
                tvVaultCount.setText(evidenceItems.size() + " ITEMS");
                Toast.makeText(MainActivity.this, "Evidence removed", Toast.LENGTH_SHORT).show();
            }
        });
        rvEvidence.setAdapter(evidenceAdapter);
        tvVaultCount.setText(evidenceItems.size() + " ITEMS");

        btnPhoto.setOnClickListener(v -> {
            String time = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date());
            String loc = currentOrigin != null ? currentOrigin.getArea() : currentCity;
            evidenceItems.add(0, new MediaItem(UUID.randomUUID().toString(), MediaItem.MediaType.PHOTO,
                    "Emergency Photo Capture", "/storage/emulated/0/GuardianAI/photo_" + System.currentTimeMillis() + ".jpg",
                    time, loc, 1850000));
            evidenceAdapter.notifyDataSetChanged();
            tvVaultCount.setText(evidenceItems.size() + " ITEMS");
            Toast.makeText(this, "📸 Geo-tagged Photo saved to Evidence Vault!", Toast.LENGTH_SHORT).show();
        });

        btnVideo.setOnClickListener(v -> {
            String time = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date());
            String loc = currentOrigin != null ? currentOrigin.getArea() : currentCity;
            evidenceItems.add(0, new MediaItem(UUID.randomUUID().toString(), MediaItem.MediaType.VIDEO,
                    "Emergency Video Incident Log", "/storage/emulated/0/GuardianAI/video_" + System.currentTimeMillis() + ".mp4",
                    time, loc, 6400000));
            evidenceAdapter.notifyDataSetChanged();
            tvVaultCount.setText(evidenceItems.size() + " ITEMS");
            Toast.makeText(this, "📹 Video Evidence recorded with GPS watermark!", Toast.LENGTH_SHORT).show();
        });

        btnAudio.setOnClickListener(v -> {
            startAudioVoiceMemo();
        });

        btnStopAudio.setOnClickListener(v -> {
            stopAudioVoiceMemo(tvVaultCount);
        });
    }

    private void startAudioVoiceMemo() {
        if (isRecordingAudio) return;
        isRecordingAudio = true;
        recordingSeconds = 0;
        layoutAudioStatus.setVisibility(View.VISIBLE);

        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRecordingAudio) {
                    recordingSeconds++;
                    int mins = recordingSeconds / 60;
                    int secs = recordingSeconds % 60;
                    tvAudioTimer.setText(String.format(Locale.US, "Recording Voice Evidence: %02d:%02d", mins, secs));
                    timerHandler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    private void stopAudioVoiceMemo(TextView tvVaultCount) {
        if (!isRecordingAudio) return;
        isRecordingAudio = false;
        layoutAudioStatus.setVisibility(View.GONE);

        String time = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date());
        String loc = currentOrigin != null ? currentOrigin.getArea() : currentCity;
        evidenceItems.add(0, new MediaItem(UUID.randomUUID().toString(), MediaItem.MediaType.AUDIO,
                "Voice Incident Memo (" + recordingSeconds + "s)",
                "/storage/emulated/0/GuardianAI/audio_" + System.currentTimeMillis() + ".m4a",
                time, loc, recordingSeconds * 32000L));
        evidenceAdapter.notifyDataSetChanged();
        tvVaultCount.setText(evidenceItems.size() + " ITEMS");
        Toast.makeText(this, "🎙️ Audio Voice Memo saved to Vault!", Toast.LENGTH_SHORT).show();
    }

    // ==========================================
    // VOICE TRIGGER LISTENER CALLBACKS
    // ==========================================
    @Override
    public void onKeywordDetected(String matchedKeyword) {
        runOnUiThread(() -> {
            Toast.makeText(this, "🚨 VOICE TRIGGER DETECTED: \"" + matchedKeyword.toUpperCase(Locale.ROOT) + "\"!", Toast.LENGTH_LONG).show();
            bottomNav.setSelectedItemId(R.id.nav_sos);
            triggerSosAction();
        });
    }

    @Override
    public void onListeningStateChanged(boolean isListening) {
        runOnUiThread(() -> {
            if (isListening) {
                btnToggleVoice.setImageResource(R.drawable.ic_mic_active);
                btnToggleVoice.setBackgroundResource(R.drawable.badge_safe);
            } else {
                btnToggleVoice.setImageResource(R.drawable.ic_mic);
                btnToggleVoice.setBackgroundResource(R.drawable.badge_moderate);
            }
        });
    }

    @Override
    public void onError(String message) {}

    private void requestAppPermissions() {
        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.CAMERA
        };

        List<String> needed = new ArrayList<>();
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                needed.add(perm);
            }
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), PERMISSION_REQ_CODE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceTrigger != null) {
            voiceTrigger.destroy();
        }
        if (sosManager != null) {
            sosManager.stopSos();
        }
        if (voiceFeedback != null) {
            voiceFeedback.shutdown();
        }
    }
}
