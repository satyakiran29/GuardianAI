package com.android.sheguard.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.sheguard.R;
import com.android.sheguard.databinding.FragmentTripMonitorBinding;
import com.android.sheguard.util.SosUtil;
import com.google.android.material.snackbar.Snackbar;

public class TripMonitorFragment extends Fragment {

    private FragmentTripMonitorBinding binding;
    private boolean isMonitoring = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTripMonitorBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int statusBarHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, statusBarHeight, 0, 0);
            return insets;
        });

        binding.btnStartTripGuard.setOnClickListener(v -> {
            if (isMonitoring) {
                stopTripGuard();
            } else {
                startTripGuard();
            }
        });

        return view;
    }

    private void startTripGuard() {
        isMonitoring = true;
        binding.btnStartTripGuard.setText("Stop Trip Guard");
        binding.btnStartTripGuard.setBackgroundColor(0xFFEF4444);
        SosUtil.vibrateDevice(requireContext());
        Snackbar.make(binding.getRoot(), getString(R.string.trip_active_shield), Snackbar.LENGTH_SHORT).show();
    }

    private void stopTripGuard() {
        isMonitoring = false;
        binding.btnStartTripGuard.setText(getString(R.string.trip_start_btn));
        binding.btnStartTripGuard.setBackgroundColor(0xFFF59E0B);
        Snackbar.make(binding.getRoot(), "Trip Guard ended.", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
