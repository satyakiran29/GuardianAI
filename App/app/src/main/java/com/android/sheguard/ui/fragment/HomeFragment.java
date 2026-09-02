package com.android.sheguard.ui.fragment;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.android.sheguard.R;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.databinding.FragmentHomeBinding;
import com.android.sheguard.service.SosService;
import com.android.sheguard.ui.activity.LoginRegisterActivity;
import com.android.sheguard.ui.activity.MainActivity;
import com.android.sheguard.model.ContactModel;
import com.android.sheguard.util.AppUtil;
import com.android.sheguard.util.SmsHelper;
import com.android.sheguard.util.SosUtil;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        try {
            ((AppCompatActivity) requireActivity()).setSupportActionBar(binding.header.toolbar);
        } catch (Exception ignored) {}
        setUserNameOnTitle();

        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel1 = new NotificationChannel(getString(R.string.notification_channel_push), getString(R.string.notification_channel_push), NotificationManager.IMPORTANCE_HIGH);
        NotificationChannel channel2 = new NotificationChannel(getString(R.string.notification_channel_emergency), getString(R.string.notification_channel_emergency), NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(channel1);
        notificationManager.createNotificationChannel(channel2);

        binding.sosButton.setOnClickListener(v -> {
            if (!AppUtil.permissionsGranted(getContext())) {
                requestAppPermissions();
            }
            if (SosUtil.isGPSEnabled(requireContext())) {
                SosUtil.activateInstantSosMode(requireContext());
            } else {
                SosUtil.turnOnGPS(requireContext());
                SosUtil.activateInstantSosMode(requireContext());
            }
        });

        binding.sosCircleBadge.setOnClickListener(v -> binding.sosButton.performClick());
        binding.btnTopDrawer.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).toggleDrawer();
            }
        });
        binding.btnTopProfile.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_profileFragment));
        binding.btnTopAlertInfo.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_helplineFragment));
        binding.btnTopSettings.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_settingsFragment));
        binding.btnTopLogout.setOnClickListener(v -> showLogoutDialog());
        binding.cardCrowdSafety.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_safeRouteFragment));

        binding.btnSafeCheckIn.setOnClickListener(v -> {
            SosUtil.vibrateDevice(requireContext());
            ArrayList<ContactModel> contacts = SosUtil.getStoredContacts(requireContext());
            if (contacts.isEmpty()) {
                Snackbar.make(requireActivity().findViewById(android.R.id.content), "Your contacts list is empty! Please add emergency contacts.", Snackbar.LENGTH_LONG).show();
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Your contacts list is empty! Please add emergency contacts.", Toast.LENGTH_LONG).show();
                }
                return;
            }

            String locationUrl = SosUtil.getLiveLocationUrl();
            if (locationUrl.isEmpty()) {
                locationUrl = "https://maps.google.com";
            }
            String checkInMsg = "✅ I am safe and everything is okay! My current location:\n" + locationUrl;
            int sentCount = SmsHelper.sendEmergencySmsToContacts(requireContext(), contacts, checkInMsg);
            Snackbar.make(requireActivity().findViewById(android.R.id.content), "Safety check-in sent to " + sentCount + " contact(s)! ✅", Snackbar.LENGTH_LONG).show();
        });

        MainActivity.shakeDetection.setValue(Prefs.getBoolean(Constants.SETTINGS_SHAKE_DETECTION, false));
        MainActivity.shakeDetection.setOnChangeListener(newValue -> {
            binding.btnShakeDetection.setVisibility(newValue ? View.VISIBLE : View.GONE);
            updateButtonText();
            if (!newValue) {
                SosUtil.stopSosNotificationService(requireContext());
            }
        });
        binding.btnShakeDetection.setVisibility(Prefs.getBoolean(Constants.SETTINGS_SHAKE_DETECTION, false) ? View.VISIBLE : View.GONE);

        updateButtonText();

        binding.btnShakeDetection.setOnClickListener(v -> {
            if (!SosService.isRunning) {
                if (AppUtil.permissionsGranted(getContext()) && SosUtil.isGPSEnabled(requireContext())) {
                    SosUtil.startSosNotificationService(requireContext());
                    Snackbar.make(requireActivity().findViewById(android.R.id.content), getString(R.string.service_started), Snackbar.LENGTH_LONG).show();
                } else if (!AppUtil.permissionsGranted(getContext())) {
                    multiplePermissions.launch(AppUtil.REQUIRED_PERMISSIONS);
                } else {
                    SosUtil.turnOnGPS(requireContext());
                }
            } else {
                SosUtil.stopSosNotificationService(requireContext());
                Snackbar.make(requireActivity().findViewById(android.R.id.content), getString(R.string.service_stopped), Snackbar.LENGTH_LONG).show();
            }

            updateButtonText();
        });

        binding.contacts.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_contactsFragment));
        binding.helpline.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_helplineFragment));
        binding.safetyTips.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_safetyTipsFragment));
        binding.about.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_aboutFragment));
        binding.cardSafeRide.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_safeRideFragment));

        // 15-Feature Safety Suite Navigation
        binding.cardSafetyTimer.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_safetyTimerFragment));
        binding.cardFakeCall.setOnClickListener(v -> showFakeCallLauncherDialog());
        binding.cardAiAssistant.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_aiAssistantFragment));
        binding.cardSafeRoute.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_safeRouteFragment));
        binding.cardTripMonitor.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_tripMonitorFragment));
        binding.cardVoiceSos.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_voiceSosFragment));

        boolean isSafeMode = Prefs.getBoolean(Constants.SETTINGS_SAFE_MODE, false);
        binding.switchSafeModeHome.setChecked(isSafeMode);
        binding.tvSafeModeStatus.setText(isSafeMode ? R.string.safe_mode_home_desc_active : R.string.safe_mode_home_desc_inactive);

        binding.switchSafeModeHome.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Prefs.putBoolean(Constants.SETTINGS_SAFE_MODE, isChecked);
            binding.tvSafeModeStatus.setText(isChecked ? R.string.safe_mode_home_desc_active : R.string.safe_mode_home_desc_inactive);

            if (isChecked) {
                SosUtil.vibrateDevice(requireContext());
                SosUtil.stopBackgroundProcesses(requireContext());
                Snackbar.make(requireActivity().findViewById(android.R.id.content), getString(R.string.safe_mode_activated_toast), Snackbar.LENGTH_LONG).show();

                if (AppUtil.permissionsGranted(getContext()) && SosUtil.isGPSEnabled(requireContext())) {
                    SosUtil.activateInstantSosMode(requireContext());
                }
            } else {
                Snackbar.make(requireActivity().findViewById(android.R.id.content), getString(R.string.safe_mode_deactivated_toast), Snackbar.LENGTH_SHORT).show();
            }
        });

        binding.btnHomeForwardWhatsapp.setOnClickListener(v -> {
            SosUtil.sendWhatsAppWithLiveLocation(requireContext());
            Snackbar.make(requireActivity().findViewById(android.R.id.content), getString(R.string.whatsapp_sent_toast), Snackbar.LENGTH_SHORT).show();
        });

        initializeDrawerItems();

        if (!AppUtil.permissionsGranted(getContext())) {
            multiplePermissions.launch(AppUtil.REQUIRED_PERMISSIONS);
        }

        return view;
    }

    private void initializeDrawerItems() {
        ((NavigationView) requireActivity().findViewById(R.id.navView)).setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            NavOptions navOptions = new NavOptions.Builder()
                    .setEnterAnim(0)
                    .setExitAnim(0)
                    .setPopEnterAnim(R.anim.slide_out)
                    .setPopExitAnim(R.anim.fade_in)
                    .build();

            if (id == R.id.nav_profile) {
                Navigation.findNavController(binding.getRoot()).navigate(R.id.action_homeFragment_to_profileFragment, null, navOptions);
            } else if (id == R.id.nav_safe_ride) {
                Navigation.findNavController(binding.getRoot()).navigate(R.id.action_homeFragment_to_safeRideFragment, null, navOptions);
            } else if (id == R.id.nav_settings) {
                Navigation.findNavController(binding.getRoot()).navigate(R.id.action_homeFragment_to_settingsFragment, null, navOptions);
            } else if (id == R.id.nav_logout) {
                Prefs.putBoolean(Constants.IS_DEMO_MODE, false);
                Prefs.remove(Constants.PREFS_USER_NAME);
                Prefs.remove(Constants.PREFS_USER_EMAIL);
                Prefs.remove(Constants.PREFS_USER_PHONE);
                Prefs.remove("USER_ROLE");
                Intent intent = new Intent(getContext(), LoginRegisterActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            ((MainActivity) requireActivity()).toggleDrawer();
            return true;
        });
    }

    public void setUserNameOnTitle() {
        String userName = Prefs.getString(Constants.PREFS_USER_NAME, "Guardian User");
        if (getContext() != null && binding.header.collapsingToolbar != null) {
            try {
                binding.header.collapsingToolbar.setSubtitle(getString(R.string.activity_home_desc, userName));
            } catch (Exception ignored) {}
        }
    }

    private void updateButtonText() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (getContext() != null) {
                binding.btnShakeDetection.setText(SosService.isRunning ? getString(R.string.btn_stop_service) : getString(R.string.btn_start_service));
            }
        }, 200);
    }

    private void requestAppPermissions() {
        if (multiplePermissions != null) {
            multiplePermissions.launch(AppUtil.REQUIRED_PERMISSIONS);
        }
    }

    private final ActivityResultLauncher<String[]> multiplePermissions = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
        boolean allGranted = true;
        for (Map.Entry<String, Boolean> entry : result.entrySet()) {
            if (!entry.getValue()) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted && getContext() != null) {
            Snackbar snackbar = Snackbar.make(binding.getRoot(), R.string.permission_must_be_granted, Snackbar.LENGTH_SHORT);
            snackbar.setAction(R.string.grant, v -> requestAppPermissions());
            snackbar.show();
        } else if (allGranted && AppUtil.permissionsGranted(getActivity())) {
            if (SosService.isRunning) {
                updateButtonText();
            }
        }
    });

    private void showFakeCallLauncherDialog() {
        String[] delays = {
                getString(R.string.fake_call_now),
                getString(R.string.fake_call_10s),
                getString(R.string.fake_call_30s)
        };

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.fake_call_title))
                .setItems(delays, (dialog, which) -> {
                    int delayMs = 0;
                    if (which == 1) delayMs = 10000;
                    else if (which == 2) delayMs = 30000;

                    if (delayMs == 0) {
                        launchFakeCall();
                    } else {
                        Snackbar.make(binding.getRoot(), "Fake call scheduled in " + (delayMs / 1000) + " seconds", Snackbar.LENGTH_SHORT).show();
                        new Handler(Looper.getMainLooper()).postDelayed(this::launchFakeCall, delayMs);
                    }
                })
                .setNegativeButton(getString(R.string.btn_later), null)
                .show();
    }

    private void launchFakeCall() {
        if (getActivity() != null) {
            Intent intent = new Intent(requireContext(), com.android.sheguard.ui.activity.FakeCallActivity.class);
            intent.putExtra("caller_name", getString(R.string.fake_call_default_caller));
            startActivity(intent);
        }
    }

    private void showLogoutDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of GuardianAI?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    Prefs.clearPref(Constants.IS_DEMO_MODE);
                    Prefs.remove(Constants.PREFS_USER_NAME);
                    Prefs.remove(Constants.PREFS_USER_EMAIL);
                    Prefs.remove(Constants.PREFS_USER_PHONE);
                    Prefs.remove("user_role");

                    Intent intent = new Intent(requireContext(), com.android.sheguard.ui.activity.OnBoardingActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        requireActivity().finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        syncLiveLocationTelemetry();
    }

    private void syncLiveLocationTelemetry() {
        if (getContext() == null) return;
        if (com.android.sheguard.util.LocationHelper.hasLocationPermission(requireContext())) {
            com.android.sheguard.util.LocationHelper.requestSingleLocationUpdate(requireContext(), new com.android.sheguard.util.LocationHelper.LocationResultListener() {
                @Override
                public void onLocationReceived(double latitude, double longitude, String addressName) {
                    if (getContext() == null) return;
                    String phone = Prefs.getString(Constants.PREFS_USER_PHONE, "");
                    int battery = 85;
                    try {
                        android.os.BatteryManager bm = (android.os.BatteryManager) requireContext().getSystemService(Context.BATTERY_SERVICE);
                        if (bm != null) {
                            battery = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
                        }
                    } catch (Exception ignored) {}
                    if (!phone.isEmpty()) {
                        com.android.sheguard.api.ApiClient.pingLocation(phone, latitude, longitude, addressName, battery);
                    }
                }

                @Override
                public void onLocationError(String error) {}
            });
        }
    }
}
