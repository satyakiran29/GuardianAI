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

import com.android.sheguard.R;
import com.android.sheguard.databinding.FragmentSafetyTimerBinding;
import com.android.sheguard.service.DeadMansSwitchService;
import com.android.sheguard.util.SosUtil;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

/**
 * Safety Timer — Dead Man's Switch.
 * Slider 1 min → 3 hr (180 min) + quick-pick chips.
 * Countdown runs as a foreground service, survives app kill.
 * Auto-triggers SOS if user doesn't check in before expiry.
 */
public class SafetyTimerFragment extends Fragment {

    private FragmentSafetyTimerBinding binding;
    private int selectedMinutes = 30; // default 30 min

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

        setupSlider();
        setupQuickChips();

        binding.btnToggleTimer.setOnClickListener(v -> {
            if (DeadMansSwitchService.isRunning) {
                stopDeadMansSwitch();
            } else {
                startDeadMansSwitch();
            }
        });

        binding.btnCheckInSafe.setOnClickListener(v -> checkInSafe());

        // Sync UI if service already running
        if (DeadMansSwitchService.isRunning) setRunningState();

        return view;
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
        });

        // Show formatted time as slider label
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
            label = "Duration: " + minutes + " min";
        } else {
            int h = minutes / 60;
            int m = minutes % 60;
            label = m == 0
                    ? "Duration: " + h + " hr"
                    : "Duration: " + h + " hr " + m + " min";
        }
        binding.tvSelectedDuration.setText(label);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Quick-pick chips
    // ─────────────────────────────────────────────────────────────────────────

    private void setupQuickChips() {
        binding.chip5m.setOnClickListener(v -> setFromChip(5));
        binding.chip15m.setOnClickListener(v -> setFromChip(15));
        binding.chip30m.setOnClickListener(v -> setFromChip(30));
        binding.chip45m.setOnClickListener(v -> setFromChip(45));
        binding.chip60m.setOnClickListener(v -> setFromChip(60));
        binding.chip90m.setOnClickListener(v -> setFromChip(90));
        binding.chip120m.setOnClickListener(v -> setFromChip(120));
        binding.chip180m.setOnClickListener(v -> setFromChip(180));
    }

    private void setFromChip(int minutes) {
        if (DeadMansSwitchService.isRunning) return;
        selectedMinutes = minutes;
        binding.sliderDuration.setValue(minutes);
        updateDurationLabel(minutes);
        binding.tvCountdownDigits.setText(formatTime(minutes * 60 * 1000L));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Service control
    // ─────────────────────────────────────────────────────────────────────────

    private void startDeadMansSwitch() {
        if (getContext() == null) return;

        long durationMs = selectedMinutes * 60 * 1000L;
        Intent intent = new Intent(requireContext(), DeadMansSwitchService.class);
        intent.setAction(DeadMansSwitchService.ACTION_START);
        intent.putExtra(DeadMansSwitchService.EXTRA_DURATION_MS, durationMs);
        ContextCompat.startForegroundService(requireContext(), intent);

        setRunningState();
        Snackbar.make(binding.getRoot(),
                "💀 Dead Man's Switch armed for " + formatDurationShort(selectedMinutes) +
                        " — check in before it expires!",
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
        binding.btnCheckInSafe.setVisibility(View.VISIBLE);
        binding.tvTimerStatus.setText(getString(R.string.safety_timer_running));
        binding.tvTimerStatus.setTextColor(0xFF6366F1);
        binding.sliderDuration.setEnabled(false);
        setChipsEnabled(false);
    }

    private void setIdleState() {
        if (binding == null) return;
        binding.btnToggleTimer.setText(getString(R.string.safety_timer_start_btn));
        binding.btnCheckInSafe.setVisibility(View.GONE);
        binding.tvTimerStatus.setTextColor(0xFF6366F1);
        binding.sliderDuration.setEnabled(true);
        setChipsEnabled(true);
        // Reset preview to selected duration
        binding.tvCountdownDigits.setText(formatTime(selectedMinutes * 60 * 1000L));
        if (getContext() != null) {
            binding.tvCountdownDigits.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.text_color_primary));
        }
    }

    private void setChipsEnabled(boolean enabled) {
        binding.chip5m.setEnabled(enabled);
        binding.chip15m.setEnabled(enabled);
        binding.chip30m.setEnabled(enabled);
        binding.chip45m.setEnabled(enabled);
        binding.chip60m.setEnabled(enabled);
        binding.chip90m.setEnabled(enabled);
        binding.chip120m.setEnabled(enabled);
        binding.chip180m.setEnabled(enabled);
    }

    private void updateCountdownDisplay(long ms) {
        if (binding == null) return;
        binding.tvCountdownDigits.setText(formatTime(ms));

        if (ms <= 30_000L) {
            // Warning — less than 30s
            long secs = ms / 1000;
            binding.tvCountdownDigits.setTextColor(0xFFEF4444);
            binding.tvTimerStatus.setText("⚠️ SOS fires in " + secs + "s — tap I'm Safe!");
            binding.tvTimerStatus.setTextColor(0xFFEF4444);
        } else {
            if (getContext() != null) {
                binding.tvCountdownDigits.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.text_color_primary));
            }
            binding.tvTimerStatus.setText(getString(R.string.safety_timer_running));
            binding.tvTimerStatus.setTextColor(0xFF6366F1);
        }
    }

    private void onServiceCancelled() {
        if (binding == null) return;
        if (getContext() != null) SosUtil.vibrateDevice(requireContext());
        binding.tvTimerStatus.setText(getString(R.string.safety_timer_checked_in));
        binding.tvTimerStatus.setTextColor(0xFF10B981);
        setIdleState();
        if (binding != null) {
            Snackbar.make(binding.getRoot(),
                    getString(R.string.safety_timer_checked_in),
                    Snackbar.LENGTH_LONG).show();
        }
    }

    private void onServiceExpired() {
        if (binding == null) return;
        binding.tvCountdownDigits.setText("00:00");
        binding.tvCountdownDigits.setTextColor(0xFFEF4444);
        binding.tvTimerStatus.setText("🚨 EXPIRED — SOS AUTO-TRIGGERED");
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

    /** Short human-readable duration, e.g. "30 min" or "1h 30m" */
    private String formatDurationShort(int minutes) {
        if (minutes < 60) return minutes + " min";
        int h = minutes / 60;
        int m = minutes % 60;
        return m == 0 ? h + " hr" : h + "h " + m + "m";
    }
}
