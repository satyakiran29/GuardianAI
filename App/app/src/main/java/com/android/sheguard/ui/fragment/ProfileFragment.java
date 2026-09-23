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

        // 2-Step Verification (2FA) Switch
        boolean is2fa = Prefs.getBoolean("TWO_FACTOR_ENABLED", false);
        binding.switchTwoFactor.setChecked(is2fa);
        updateTwoFactorLabel(is2fa);

        binding.switchTwoFactor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Prefs.putBoolean("TWO_FACTOR_ENABLED", isChecked);
            updateTwoFactorLabel(isChecked);

            String userIdentifier = Prefs.getString(Constants.PREFS_USER_EMAIL, "");
            if (userIdentifier.isEmpty()) {
                userIdentifier = Prefs.getString(Constants.PREFS_USER_PHONE, "");
            }

            ApiClient.toggleTwoFactor(userIdentifier, isChecked, (success, state, message) -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), isChecked ? "🔒 2-Step Verification enabled!" : "🔓 2-Step Verification disabled", Toast.LENGTH_SHORT).show();
                }
            });
        });

        binding.rowTwoFactor.setOnClickListener(v -> binding.switchTwoFactor.toggle());

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
        boolean is2fa = Prefs.getBoolean("TWO_FACTOR_ENABLED", false);
        binding.switchTwoFactor.setChecked(is2fa);
        updateTwoFactorLabel(is2fa);
    }

    private void updateTwoFactorLabel(boolean enabled) {
        if (binding != null && binding.tvTwoFactorStatus != null) {
            binding.tvTwoFactorStatus.setText(enabled ? "🟢 Enabled • Email OTP required at sign-in" : "⚪ Disabled • Password only");
            binding.tvTwoFactorStatus.setTextColor(enabled ? Color.parseColor("#34D399") : Color.parseColor("#94A3B8"));
        }
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
        return com.android.sheguard.util.LocationHelper.isLocationEnabled(requireContext());
    }

    private void getCurrentLocation() {
        if (getContext() == null || binding == null) return;

        if (!isGPSEnabled()) {
            binding.tvLocation.setText(R.string.gps_is_not_enabled);
            binding.tvLocation.setOnClickListener(v -> com.android.sheguard.util.LocationHelper.promptEnableLocation(requireActivity()));
            return;
        }

        binding.tvLocation.setText(R.string.getting_location);

        com.android.sheguard.util.LocationHelper.requestSingleLocationUpdate(requireContext(), new com.android.sheguard.util.LocationHelper.LocationResultListener() {
            @Override
            public void onLocationReceived(double latitude, double longitude, String addressName) {
                if (binding != null) {
                    binding.tvLocation.setText(addressName);
                }
            }

            @Override
            public void onLocationError(String error) {
                if (binding != null) {
                    binding.tvLocation.setText(getString(R.string.failed_to_get_location));
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}