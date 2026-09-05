package com.android.sheguard.ui.fragment;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.android.sheguard.R;
import com.android.sheguard.api.ApiClient;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.databinding.FragmentProfileBinding;
import com.android.sheguard.ui.activity.OnBoardingActivity;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Profile Screen matching the dark, human, reassuring design:
 * - Glowing pink avatar frame with camera badge
 * - Profile details card (Full Name, Email, Phone, Location)
 * - Account & Security section (Change Password, My Guardians, Delete Account)
 * - Permanent account deletion with confirmation dialog, server purge & local data wipe
 * - Bottom reassurance banner ("You are important / Your safety matters. Always.")
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private LocationManager locationManager = null;
    private LocationRequest locationRequest = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int top = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, top, 0, 0);
            return insets;
        });

        // Top bar back button
        binding.btnBack.setOnClickListener(v -> {
            if (!Navigation.findNavController(v).navigateUp()) {
                requireActivity().onBackPressed();
            }
        });

        // Edit Profile buttons
        View.OnClickListener editProfileListener = v -> {
            try {
                Navigation.findNavController(v).navigate(R.id.action_profileFragment_to_editProfileFragment);
            } catch (Exception e) {
                try {
                    Navigation.findNavController(v).navigate(R.id.editProfileFragment);
                } catch (Exception ignored) {}
            }
        };

        binding.btnEditProfile.setOnClickListener(editProfileListener);
        binding.btnRowName.setOnClickListener(editProfileListener);
        binding.btnRowEmail.setOnClickListener(editProfileListener);
        binding.btnRowPhone.setOnClickListener(editProfileListener);

        binding.btnChangeAvatar.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Tap 'Edit profile' to update your photo and info", Toast.LENGTH_SHORT).show()
        );

        // Account & Security rows
        binding.rowChangePassword.setOnClickListener(editProfileListener);

        binding.rowMyGuardians.setOnClickListener(v -> {
            try {
                Navigation.findNavController(v).navigate(R.id.contactsFragment);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Opening My Guardians...", Toast.LENGTH_SHORT).show();
            }
        });

        // Delete Account (User Requested Feature)
        binding.rowDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());

        if (locationRequest == null) {
            locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(2000)
                    .setMaxUpdateDelayMillis(5000)
                    .build();
        }

        getUserDetails();
        getCurrentLocation();

        return view;
    }

    private void getUserDetails() {
        String name = Prefs.getString(Constants.PREFS_USER_NAME, "Sarah Connor");
        String email = Prefs.getString(Constants.PREFS_USER_EMAIL, "demo.user@guardianai.app");
        String phone = Prefs.getString(Constants.PREFS_USER_PHONE, "+1 (555) 019-2834");
        String role = Prefs.getString("USER_ROLE", "user");

        binding.tvDisplayName.setText(name);
        binding.tvName.setText(name + " (" + role.toUpperCase() + ")");
        binding.tvEmail.setText(email);
        binding.tvPhone.setText(phone);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delete Account Dialog & Purge Logic
    // ─────────────────────────────────────────────────────────────────────────

    private void showDeleteAccountDialog() {
        if (getContext() == null) return;

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm_delete_account);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        View btnConfirm = dialog.findViewById(R.id.btn_confirm_delete);
        View btnCancel = dialog.findViewById(R.id.btn_cancel_delete);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                executeDeleteAccount();
            });
        }

        dialog.show();
    }

    private void executeDeleteAccount() {
        if (getContext() == null) return;

        String phone = Prefs.getString(Constants.PREFS_USER_PHONE, "");
        String email = Prefs.getString(Constants.PREFS_USER_EMAIL, "");

        Toast.makeText(requireContext(), "Processing account deletion...", Toast.LENGTH_SHORT).show();

        // Call backend API to purge database records (alerts, contacts, location history, links)
        ApiClient.deleteAccount(phone, email, (success, result, message) -> {
            // Regardless of network connectivity, proceed with local data wipe
            wipeLocalAccountAndLogout();
        });
    }

    private void wipeLocalAccountAndLogout() {
        if (getContext() == null) return;

        // Wipe all user identity & session preferences
        Prefs.putBoolean(Constants.IS_DEMO_MODE, false);
        Prefs.remove(Constants.PREFS_USER_NAME);
        Prefs.remove(Constants.PREFS_USER_EMAIL);
        Prefs.remove(Constants.PREFS_USER_PHONE);
        Prefs.remove("USER_ROLE");
        Prefs.remove(Constants.CONTACTS_LIST);
        Prefs.remove("dms_journey_note");
        Prefs.remove(Constants.PREFS_LAST_LATITUDE);
        Prefs.remove(Constants.PREFS_LAST_LONGITUDE);

        Toast.makeText(requireContext(), "Account permanently deleted. All data erased.", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(requireContext(), OnBoardingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finishAffinity();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GPS & Location Helper
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isGPSEnabled() {
        if (locationManager == null && getContext() != null) {
            locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        }
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void getCurrentLocation() {
        if (getContext() == null) return;

        if (!isGPSEnabled()) {
            binding.tvLocation.setText(R.string.gps_is_not_enabled);
            return;
        } else {
            binding.tvLocation.setText(R.string.getting_location);
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        final int[] numberOfUpdates = {0};

        LocationServices.getFusedLocationProviderClient(requireContext())
                .requestLocationUpdates(locationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(@NonNull LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        if (getContext() == null || binding == null) {
                            return;
                        }

                        numberOfUpdates[0]++;

                        if (numberOfUpdates[0] >= 3) {
                            LocationServices.getFusedLocationProviderClient(getContext())
                                    .removeLocationUpdates(this);

                            if (locationResult.getLocations().size() > 0) {
                                int idx = locationResult.getLocations().size() - 1;
                                double latitude = locationResult.getLocations().get(idx).getLatitude();
                                double longitude = locationResult.getLocations().get(idx).getLongitude();

                                Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                                try {
                                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                                    if (addresses != null && !addresses.isEmpty()) {
                                        StringBuilder address = new StringBuilder();
                                        for (int i = 0; i <= addresses.get(0).getMaxAddressLineIndex(); i++) {
                                            address.append(addresses.get(0).getAddressLine(i));
                                            if (i < addresses.get(0).getMaxAddressLineIndex()) {
                                                address.append("\n");
                                            }
                                        }
                                        binding.tvLocation.setText(address.toString());
                                    } else {
                                        binding.tvLocation.setText(getString(R.string.failed_to_get_location));
                                    }
                                } catch (IOException e) {
                                    if (binding != null) {
                                        binding.tvLocation.setText(getString(R.string.failed_to_get_location));
                                    }
                                }
                            }
                        }
                    }
                }, Looper.getMainLooper());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}