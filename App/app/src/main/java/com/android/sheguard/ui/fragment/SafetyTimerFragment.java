package com.android.sheguard.ui.fragment;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.sheguard.R;
import com.android.sheguard.databinding.FragmentSafetyTimerBinding;
import com.android.sheguard.util.SosUtil;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

public class SafetyTimerFragment extends Fragment {

    private FragmentSafetyTimerBinding binding;
    private CountDownTimer countDownTimer;
    private long selectedMillis = 30 * 60 * 1000L;
    private boolean isTimerRunning = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSafetyTimerBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int statusBarHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, statusBarHeight, 0, 0);
            return insets;
        });

        binding.chip15m.setOnClickListener(v -> setDuration(15));
        binding.chip30m.setOnClickListener(v -> setDuration(30));
        binding.chip45m.setOnClickListener(v -> setDuration(45));
        binding.chip60m.setOnClickListener(v -> setDuration(60));

        binding.btnToggleTimer.setOnClickListener(v -> {
            if (isTimerRunning) {
                cancelTimer();
            } else {
                startTimer();
            }
        });

        binding.btnCheckInSafe.setOnClickListener(v -> checkInSafe());

        return view;
    }

    private void setDuration(int minutes) {
        if (isTimerRunning) return;
        selectedMillis = minutes * 60 * 1000L;
        binding.tvCountdownDigits.setText(String.format(Locale.getDefault(), "%02d:00", minutes));
    }

    private void startTimer() {
        isTimerRunning = true;
        binding.btnToggleTimer.setText(getString(R.string.safety_timer_cancel_btn));
        binding.btnCheckInSafe.setVisibility(View.VISIBLE);
        binding.tvTimerStatus.setText(getString(R.string.safety_timer_running));
        binding.chip15m.setEnabled(false);
        binding.chip30m.setEnabled(false);
        binding.chip45m.setEnabled(false);
        binding.chip60m.setEnabled(false);

        countDownTimer = new CountDownTimer(selectedMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                binding.tvCountdownDigits.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                onTimerExpired();
            }
        }.start();
    }

    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        binding.btnToggleTimer.setText(getString(R.string.safety_timer_start_btn));
        binding.btnCheckInSafe.setVisibility(View.GONE);
        binding.tvTimerStatus.setText("Timer cancelled");
        binding.chip15m.setEnabled(true);
        binding.chip30m.setEnabled(true);
        binding.chip45m.setEnabled(true);
        binding.chip60m.setEnabled(true);
        binding.tvCountdownDigits.setText(String.format(Locale.getDefault(), "%02d:00", (int) (selectedMillis / 60000)));
    }

    private void checkInSafe() {
        cancelTimer();
        SosUtil.vibrateDevice(requireContext());
        Snackbar.make(binding.getRoot(), getString(R.string.safety_timer_checked_in), Snackbar.LENGTH_LONG).show();
    }

    private void onTimerExpired() {
        isTimerRunning = false;
        binding.tvCountdownDigits.setText("00:00");
        binding.tvTimerStatus.setText("EXPIRED");
        SosUtil.vibrateDevice(requireContext());
        SosUtil.playSiren(requireContext());
        SosUtil.activateInstantSosMode(requireContext());
        Snackbar.make(binding.getRoot(), getString(R.string.safety_timer_expired_alert), Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        super.onDestroyView();
        binding = null;
    }
}
