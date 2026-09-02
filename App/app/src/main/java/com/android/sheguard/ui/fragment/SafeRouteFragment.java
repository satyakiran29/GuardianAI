package com.android.sheguard.ui.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.android.sheguard.R;
import com.android.sheguard.databinding.FragmentSafeRouteBinding;
import com.android.sheguard.util.LocationHelper;
import com.android.sheguard.util.SosUtil;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

public class SafeRouteFragment extends Fragment {

    private FragmentSafeRouteBinding binding;
    private boolean isMonitoring = false;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private String currentAddressText = "";

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
                        binding.tvCurrentAddress.setText("Location access permission is required to calculate safe routes.");
                    }
                }
            });

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

        checkPermissionsAndFetchLocation();

        binding.btnRefreshLocation.setOnClickListener(v -> checkPermissionsAndFetchLocation());

        binding.btnOpenGoogleMaps.setOnClickListener(v -> {
            if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                LocationHelper.openLocationInGoogleMaps(requireContext(), currentLatitude, currentLongitude, "My Current Location");
            } else {
                Snackbar.make(binding.getRoot(), "Acquiring GPS location, please wait...", Snackbar.LENGTH_SHORT).show();
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

        binding.btnStartArrivalShield.setOnClickListener(v -> {
            if (isMonitoring) {
                stopMonitoring();
            } else {
                startMonitoring();
            }
        });

        binding.btnSimulateArrival.setOnClickListener(v -> completeArrival());

        return view;
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
            }

            @Override
            public void onLocationError(String error) {
                if (binding == null) return;
                binding.tvGpsBadge.setText("GPS OFFLINE");
                binding.tvGpsBadge.setTextColor(0xFFEF4444);
                binding.tvCurrentAddress.setText("Turn on device GPS location to view safe route telemetry.");
            }
        });
    }

    private void startMonitoring() {
        isMonitoring = true;
        binding.btnStartArrivalShield.setText("Stop Route Shield");
        binding.btnStartArrivalShield.setBackgroundColor(0xFFEF4444);
        binding.btnSimulateArrival.setVisibility(View.VISIBLE);
        SosUtil.vibrateDevice(requireContext());
        Snackbar.make(binding.getRoot(), "🛡️ Safe Route & Arrival Shield Active!", Snackbar.LENGTH_SHORT).show();
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
        super.onDestroyView();
        binding = null;
    }
}
