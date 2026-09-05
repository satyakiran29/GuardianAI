package com.android.sheguard.ui.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.android.sheguard.R;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.databinding.FragmentSafetyTimerBinding;
import com.android.sheguard.model.ContactModel;
import com.android.sheguard.service.DeadMansSwitchService;
import com.android.sheguard.util.SosUtil;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Arrival & Safety Check-In (Dead Man's Switch).
 * Real-world human scenario presets: Cab Ride (15m), Walking (20m), Solo Run (35m),
 * Meeting (45m), Late Transit (60m), Custom Time (1m-180m).
 * Displays active emergency contacts preview and optional trip note.
 * Auto-triggers SOS if user fails to check in before countdown expiry.
 */
public class SafetyTimerFragment extends Fragment {

    private enum Scenario {
        CAB(15, "e.g. Uber White Dzire AP 39 X 1234 to Hitech City"),
        WALK(20, "e.g. Walking from Metro Gate 2 to Home"),
        RUN(35, "e.g. Central Park 5km outer trail"),
        MEETING(45, "e.g. Coffee meetup with Alex at Third Wave"),
        TRANSIT(60, "e.g. Route 42 Bus / Train Coach B3"),
        CUSTOM(35, "e.g. Destination, vehicle number, or route note");

        final int durationMinutes;
        final String defaultHint;

        Scenario(int durationMinutes, String defaultHint) {
            this.durationMinutes = durationMinutes;
            this.defaultHint = defaultHint;
        }
    }

    private FragmentSafetyTimerBinding binding;
    private int selectedMinutes = 35; // default 35 min (Solo Run)
    private Scenario activeScenario = Scenario.RUN;

    // ─── BroadcastReceiver — service ticks / events ───────────────────────────
    private final BroadcastReceiver dmsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || binding == null) return;
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case DeadMansSwitchService.BROADCAST_TICK:
                    long ms = intent.getLongExtra(DeadMansSwitchService.EXTRA_MILLIS_LEFT, 0);
                    updateCountdownDisplay(ms);
                    break;
                case DeadMansSwitchService.BROADCAST_CANCELLED:
                    onServiceCancelled();
                    break;
                case DeadMansSwitchService.BROADCAST_EXPIRED:
                    onServiceExpired();
                    break;
            }
        }
    };

    // ─────────────────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSafetyTimerBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int top = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, top, 0, 0);
            return insets;
        });

        loadTrustedCirclePreview();
        setupScenarios();
        setupSlider();

        binding.btnToggleTimer.setOnClickListener(v -> {
            if (DeadMansSwitchService.isRunning) {
                stopDeadMansSwitch();
            } else {
                startDeadMansSwitch();
            }
        });

        binding.btnCheckInSafe.setOnClickListener(v -> checkInSafe());

        binding.cardTrustedCircle.setOnClickListener(v -> {
            try {
                Navigation.findNavController(v).navigate(R.id.action_safetyTimerFragment_to_contactsFragment);
            } catch (Exception e) {
                try {
                    Navigation.findNavController(v).navigate(R.id.contactsFragment);
                } catch (Exception ignored) {}
            }
        });

        // Restore active trip note if service already running
        String savedNote = Prefs.getString("dms_journey_note", "");
        if (!savedNote.isEmpty()) {
            binding.etJourneyNote.setText(savedNote);
        }

        // Sync UI if service already running
        if (DeadMansSwitchService.isRunning) {
            setRunningState();
        } else {
            setIdleState();
        }

        return view;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Trusted Circle Preview
    // ─────────────────────────────────────────────────────────────────────────

    private void loadTrustedCirclePreview() {
        if (getContext() == null || binding == null) return;
        ArrayList<ContactModel> contacts = SosUtil.getStoredContacts(requireContext());

        if (contacts.isEmpty()) {
            binding.ivCircleBadge.setText("⚠️");
            binding.tvCircleStatus.setText("No Emergency Contacts Saved");
            binding.tvCircleStatus.setTextColor(0xFFF59E0B); // Amber
            binding.tvCircleDetail.setText("Tap to add trusted contacts so GuardianAI knows who to alert.");
        } else {
            binding.ivCircleBadge.setText("👥");
            binding.tvCircleStatus.setText("Alerts Will Notify " + contacts.size() + " Contact" + (contacts.size() > 1 ? "s" : ""));
            binding.tvCircleStatus.setTextColor(0xFF38BDF8); // Calm sky blue

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < contacts.size(); i++) {
                ContactModel c = contacts.get(i);
                if (i > 0) sb.append(" • ");
                sb.append(c.getName());
                if (c.isPrimary()) sb.append(" (Primary)");
            }
            sb.append(" · Live GPS coordinates & trip note attached");
            binding.tvCircleDetail.setText(sb.toString());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario Presets Setup
    // ─────────────────────────────────────────────────────────────────────────

    private void setupScenarios() {
        binding.cardScenarioCab.setOnClickListener(v -> selectScenario(Scenario.CAB));
        binding.cardScenarioWalk.setOnClickListener(v -> selectScenario(Scenario.WALK));
        binding.cardScenarioRun.setOnClickListener(v -> selectScenario(Scenario.RUN));
        binding.cardScenarioMeeting.setOnClickListener(v -> selectScenario(Scenario.MEETING));
        binding.cardScenarioTransit.setOnClickListener(v -> selectScenario(Scenario.TRANSIT));
        binding.cardScenarioCustom.setOnClickListener(v -> selectScenario(Scenario.CUSTOM));

        // Initial selection: RUN (35m)
        applyScenarioSelection(Scenario.RUN);
    }

    private void selectScenario(Scenario scenario) {
        if (DeadMansSwitchService.isRunning) return;
        applyScenarioSelection(scenario);

        selectedMinutes = scenario.durationMinutes;
        binding.sliderDuration.setValue(selectedMinutes);
        updateDurationLabel(selectedMinutes);
        binding.tvCountdownDigits.setText(formatTime(selectedMinutes * 60 * 1000L));
    }

    private void applyScenarioSelection(Scenario scenario) {
        activeScenario = scenario;

        // Reset all backgrounds
        binding.cardScenarioCab.setBackgroundResource(R.drawable.bg_scenario_card_normal);
        binding.cardScenarioWalk.setBackgroundResource(R.drawable.bg_scenario_card_normal);
        binding.cardScenarioRun.setBackgroundResource(R.drawable.bg_scenario_card_normal);
        binding.cardScenarioMeeting.setBackgroundResource(R.drawable.bg_scenario_card_normal);
        binding.cardScenarioTransit.setBackgroundResource(R.drawable.bg_scenario_card_normal);
        binding.cardScenarioCustom.setBackgroundResource(R.drawable.bg_scenario_card_normal);

        // Highlight selected
        switch (scenario) {
            case CAB:
                binding.cardScenarioCab.setBackgroundResource(R.drawable.bg_scenario_card_selected);
                break;
            case WALK:
                binding.cardScenarioWalk.setBackgroundResource(R.drawable.bg_scenario_card_selected);
                break;
            case RUN:
                binding.cardScenarioRun.setBackgroundResource(R.drawable.bg_scenario_card_selected);
                break;
            case MEETING:
                binding.cardScenarioMeeting.setBackgroundResource(R.drawable.bg_scenario_card_selected);
                break;
            case TRANSIT:
                binding.cardScenarioTransit.setBackgroundResource(R.drawable.bg_scenario_card_selected);
                break;
            case CUSTOM:
                binding.cardScenarioCustom.setBackgroundResource(R.drawable.bg_scenario_card_selected);
                break;
        }

        // Update hint in journey note
        if (binding.etJourneyNote.getText() == null || binding.etJourneyNote.getText().toString().trim().isEmpty()) {
            binding.etJourneyNote.setHint(scenario.defaultHint);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Slider setup
    // ─────────────────────────────────────────────────────────────────────────

    private void setupSlider() {
        binding.sliderDuration.setValue(selectedMinutes);
        updateDurationLabel(selectedMinutes);

        binding.sliderDuration.addOnChangeListener((slider, value, fromUser) -> {
            if (DeadMansSwitchService.isRunning) {
                // Snap back — can't change while armed
                slider.setValue(selectedMinutes);
                return;
            }
            selectedMinutes = (int) value;
            updateDurationLabel(selectedMinutes);
            // Update countdown preview
            binding.tvCountdownDigits.setText(formatTime(selectedMinutes * 60 * 1000L));

            if (fromUser && activeScenario != Scenario.CUSTOM) {
                // If user touches slider directly, switch active scenario to CUSTOM
                applyScenarioSelection(Scenario.CUSTOM);
            }
        });

        // Show formatted time as slider floating label
        binding.sliderDuration.setLabelFormatter(value -> {
            int m = (int) value;
            if (m < 60) return m + " min";
            int h = m / 60;
            int rem = m % 60;
            return rem == 0 ? h + " hr" : h + "h " + rem + "m";
        });
    }

    private void updateDurationLabel(int minutes) {
        String label;
        if (minutes < 60) {
            label = "Estimated Arrival: " + minutes + " min";
        } else {
            int h = minutes / 60;
            int m = minutes % 60;
            label = m == 0
                    ? "Estimated Arrival: " + h + " hr"
                    : "Estimated Arrival: " + h + " hr " + m + " min";
        }
        binding.tvSelectedDuration.setText(label);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Service control
    // ─────────────────────────────────────────────────────────────────────────

    private void startDeadMansSwitch() {
        if (getContext() == null || binding == null) return;

        // Save optional trip / vehicle note
        String note = binding.etJourneyNote.getText() != null
                ? binding.etJourneyNote.getText().toString().trim()
                : "";
        Prefs.putString("dms_journey_note", note);

        String triggerSource = "Safety Check-In Expired (No Check-In)";
        if (!note.isEmpty()) {
            triggerSource += " · Note: " + note;
        }
        Prefs.putString(Constants.DMS_TRIGGER_SOURCE, triggerSource);

        long durationMs = selectedMinutes * 60 * 1000L;
        Intent intent = new Intent(requireContext(), DeadMansSwitchService.class);
        intent.setAction(DeadMansSwitchService.ACTION_START);
        intent.putExtra(DeadMansSwitchService.EXTRA_DURATION_MS, durationMs);
        ContextCompat.startForegroundService(requireContext(), intent);

        setRunningState();
        Snackbar.make(binding.getRoot(),
                "🛡️ Safety Check-In armed for " + formatDurationShort(selectedMinutes) + " — tap 'I Am Safe' when you arrive.",
                Snackbar.LENGTH_LONG).show();
    }

    private void stopDeadMansSwitch() {
        sendServiceAction(DeadMansSwitchService.ACTION_CANCEL);
    }

    private void checkInSafe() {
        sendServiceAction(DeadMansSwitchService.ACTION_CANCEL);
    }

    private void sendServiceAction(String action) {
        if (getContext() == null) return;
        Intent intent = new Intent(requireContext(), DeadMansSwitchService.class);
        intent.setAction(action);
        requireContext().startService(intent);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle — register/unregister broadcast receiver
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onResume() {
        super.onResume();
        loadTrustedCirclePreview();
        IntentFilter filter = new IntentFilter();
        filter.addAction(DeadMansSwitchService.BROADCAST_TICK);
        filter.addAction(DeadMansSwitchService.BROADCAST_CANCELLED);
        filter.addAction(DeadMansSwitchService.BROADCAST_EXPIRED);
        if (getContext() != null) {
            requireContext().registerReceiver(dmsReceiver, filter,
                    Context.RECEIVER_NOT_EXPORTED);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            if (getContext() != null) requireContext().unregisterReceiver(dmsReceiver);
        } catch (IllegalArgumentException ignored) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI state helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void setRunningState() {
        if (binding == null) return;
        binding.btnToggleTimer.setText(getString(R.string.safety_timer_cancel_btn));
        binding.btnToggleTimer.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.surface_card_stroke));
        binding.btnCheckInSafe.setVisibility(View.VISIBLE);
        binding.tvTimerStatus.setText("🟢 Check-In Guard Active — Tap below when you arrive safely");
        binding.tvTimerStatus.setTextColor(0xFF10B981);
        binding.sliderDuration.setEnabled(false);
        binding.etJourneyNote.setEnabled(false);
        setScenariosEnabled(false);
    }

    private void setIdleState() {
        if (binding == null) return;
        binding.btnToggleTimer.setText("🛡️ Start Safety Check-In");
        binding.btnToggleTimer.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.colorPrimaryDark));
        binding.btnCheckInSafe.setVisibility(View.GONE);
        binding.tvTimerStatus.setText("Ready · Select a preset or adjust duration below");
        binding.tvTimerStatus.setTextColor(0xFF38BDF8);
        binding.sliderDuration.setEnabled(true);
        binding.etJourneyNote.setEnabled(true);
        setScenariosEnabled(true);

        // Reset preview to selected duration
        binding.tvCountdownDigits.setText(formatTime(selectedMinutes * 60 * 1000L));
        binding.tvCountdownDigits.setTextColor(0xFFFFFFFF);
    }

    private void setScenariosEnabled(boolean enabled) {
        binding.cardScenarioCab.setEnabled(enabled);
        binding.cardScenarioWalk.setEnabled(enabled);
        binding.cardScenarioRun.setEnabled(enabled);
        binding.cardScenarioMeeting.setEnabled(enabled);
        binding.cardScenarioTransit.setEnabled(enabled);
        binding.cardScenarioCustom.setEnabled(enabled);
        float alpha = enabled ? 1.0f : 0.6f;
        binding.cardScenarioCab.setAlpha(alpha);
        binding.cardScenarioWalk.setAlpha(alpha);
        binding.cardScenarioRun.setAlpha(alpha);
        binding.cardScenarioMeeting.setAlpha(alpha);
        binding.cardScenarioTransit.setAlpha(alpha);
        binding.cardScenarioCustom.setAlpha(alpha);
    }

    private void updateCountdownDisplay(long ms) {
        if (binding == null) return;
        binding.tvCountdownDigits.setText(formatTime(ms));

        if (ms <= 30_000L) {
            // Escalation warning — less than 30s
            long secs = ms / 1000;
            binding.tvCountdownDigits.setTextColor(0xFFEF4444);
            binding.tvTimerStatus.setText("⚠️ SOS dispatches in " + secs + "s — tap I Am Safe!");
            binding.tvTimerStatus.setTextColor(0xFFEF4444);
        } else {
            binding.tvCountdownDigits.setTextColor(0xFFFFFFFF);
            binding.tvTimerStatus.setText("🟢 Check-In Guard Active — Tap below when you arrive safely");
            binding.tvTimerStatus.setTextColor(0xFF10B981);
        }
    }

    private void onServiceCancelled() {
        if (binding == null) return;
        if (getContext() != null) SosUtil.vibrateDevice(requireContext());
        Prefs.remove("dms_journey_note");
        binding.tvTimerStatus.setText("✨ Glad you're safe! Check-in confirmed.");
        binding.tvTimerStatus.setTextColor(0xFF10B981);
        setIdleState();
        Snackbar.make(binding.getRoot(),
                "✨ Checked in safely! Timer disarmed.",
                Snackbar.LENGTH_LONG).show();
    }

    private void onServiceExpired() {
        if (binding == null) return;
        binding.tvCountdownDigits.setText("00:00");
        binding.tvCountdownDigits.setTextColor(0xFFEF4444);
        binding.tvTimerStatus.setText("🚨 EXPIRED — EMERGENCY SOS DISPATCHED TO CONTACTS");
        binding.tvTimerStatus.setTextColor(0xFFEF4444);
        setIdleState();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Formatting helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Format ms → "HH:MM:SS" or "MM:SS" depending on total duration */
    private String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long hours   = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    /** Short human-readable duration, e.g. "35 min" or "1h 30m" */
    private String formatDurationShort(int minutes) {
        if (minutes < 60) return minutes + " min";
        int h = minutes / 60;
        int m = minutes % 60;
        return m == 0 ? h + " hr" : h + "h " + m + "m";
    }
}
