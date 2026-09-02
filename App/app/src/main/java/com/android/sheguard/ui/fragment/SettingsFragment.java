package com.android.sheguard.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.android.sheguard.BuildConfig;
import com.android.sheguard.R;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.databinding.FragmentSettingsBinding;
import com.android.sheguard.service.SosService;
import com.android.sheguard.ui.activity.MainActivity;
import com.android.sheguard.util.SosUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        ((AppCompatActivity) requireActivity()).setSupportActionBar(binding.header.toolbar);
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            binding.header.collapsingToolbar.setTitle(getString(R.string.activity_settings_title));
            binding.header.collapsingToolbar.setSubtitle(getString(R.string.activity_settings_desc));
        }

        // Profile & Account Settings
        String userName = Prefs.getString(Constants.PREFS_USER_NAME, "My Profile");
        String userPhone = Prefs.getString(Constants.PREFS_USER_PHONE, "");
        binding.tvProfileName.setText(userName);
        if (!userPhone.isEmpty()) {
            binding.tvProfileDesc.setText(userPhone + " • " + getString(R.string.activity_edit_profile_desc));
        }
        binding.profileContainer.setOnClickListener(v -> {
            try {
                androidx.navigation.Navigation.findNavController(view).navigate(R.id.action_settingsFragment_to_editProfileFragment);
            } catch (Exception e) {
                // Fallback
            }
        });

        // Shake Detection
        binding.switchShakeDetection.setChecked(Prefs.getBoolean(Constants.SETTINGS_SHAKE_DETECTION, false));
        binding.switchShakeDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Prefs.putBoolean(Constants.SETTINGS_SHAKE_DETECTION, isChecked);
            MainActivity.shakeDetection.setValue(isChecked);
        });
        binding.shakeDetectionContainer.setOnClickListener(v -> binding.switchShakeDetection.toggle());

        // Send SMS
        binding.switchSendSms.setChecked(Prefs.getBoolean(Constants.SETTINGS_SEND_SMS, true));
        binding.switchSendSms.setOnCheckedChangeListener((buttonView, isChecked) -> Prefs.putBoolean(Constants.SETTINGS_SEND_SMS, isChecked));
        binding.sendSmsContainer.setOnClickListener(v -> binding.switchSendSms.toggle());

        // Send Push Notification
        binding.switchSendNotification.setChecked(Prefs.getBoolean(Constants.SETTINGS_SEND_NOTIFICATION, true));
        binding.switchSendNotification.setOnCheckedChangeListener((buttonView, isChecked) -> Prefs.putBoolean(Constants.SETTINGS_SEND_NOTIFICATION, isChecked));
        binding.sendNotificationContainer.setOnClickListener(v -> binding.switchSendNotification.toggle());

        // Play Siren
        binding.switchPlaySiren.setChecked(Prefs.getBoolean(Constants.SETTINGS_PLAY_SIREN, false));
        binding.switchPlaySiren.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Prefs.putBoolean(Constants.SETTINGS_PLAY_SIREN, isChecked);
            if (!isChecked) {
                SosService.stopSiren();
                SosUtil.stopSiren();
            }
        });
        binding.playSirenContainer.setOnClickListener(v -> binding.switchPlaySiren.toggle());

        // Call Emergency
        binding.switchCallEmergencyService.setChecked(Prefs.getBoolean(Constants.SETTINGS_CALL_EMERGENCY_SERVICE, false));
        binding.switchCallEmergencyService.setOnCheckedChangeListener((buttonView, isChecked) -> Prefs.putBoolean(Constants.SETTINGS_CALL_EMERGENCY_SERVICE, isChecked));
        binding.callEmergencyServiceContainer.setOnClickListener(v -> binding.switchCallEmergencyService.toggle());

        // Safe Mode
        binding.switchSafeMode.setChecked(Prefs.getBoolean(Constants.SETTINGS_SAFE_MODE, false));
        binding.switchSafeMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Prefs.putBoolean(Constants.SETTINGS_SAFE_MODE, isChecked);
            if (isChecked) {
                SosUtil.vibrateDevice(requireContext());
                SosUtil.stopBackgroundProcesses(requireContext());
                Snackbar.make(view, getString(R.string.safe_mode_activated_toast), Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(view, getString(R.string.safe_mode_deactivated_toast), Snackbar.LENGTH_SHORT).show();
            }
        });
        binding.safeModeContainer.setOnClickListener(v -> binding.switchSafeMode.toggle());

        // Send WhatsApp
        binding.switchSendWhatsapp.setChecked(Prefs.getBoolean(Constants.SETTINGS_SEND_WHATSAPP, true));
        binding.switchSendWhatsapp.setOnCheckedChangeListener((buttonView, isChecked) -> Prefs.putBoolean(Constants.SETTINGS_SEND_WHATSAPP, isChecked));
        binding.sendWhatsappContainer.setOnClickListener(v -> binding.switchSendWhatsapp.toggle());

        // Stop Background Apps
        binding.switchStopBackgroundApps.setChecked(Prefs.getBoolean(Constants.SETTINGS_STOP_BACKGROUND_APPS, true));
        binding.switchStopBackgroundApps.setOnCheckedChangeListener((buttonView, isChecked) -> Prefs.putBoolean(Constants.SETTINGS_STOP_BACKGROUND_APPS, isChecked));
        binding.stopBackgroundAppsContainer.setOnClickListener(v -> binding.switchStopBackgroundApps.toggle());

        // 15% Low Battery Guardian Alert
        binding.switchLowBatteryAlert.setChecked(Prefs.getBoolean(Constants.SETTINGS_LOW_BATTERY_ALERT, true));
        binding.switchLowBatteryAlert.setOnCheckedChangeListener((buttonView, isChecked) -> Prefs.putBoolean(Constants.SETTINGS_LOW_BATTERY_ALERT, isChecked));
        binding.lowBatteryAlertContainer.setOnClickListener(v -> binding.switchLowBatteryAlert.toggle());

        // Haptic Feedback
        binding.switchHapticFeedback.setChecked(Prefs.getBoolean(Constants.SETTINGS_HAPTIC_FEEDBACK, true));
        binding.switchHapticFeedback.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Prefs.putBoolean(Constants.SETTINGS_HAPTIC_FEEDBACK, isChecked);
            if (isChecked) {
                SosUtil.vibrateDevice(requireContext());
            }
        });
        binding.hapticFeedbackContainer.setOnClickListener(v -> binding.switchHapticFeedback.toggle());

        // Accessibility Settings Link
        binding.accessibilityContainer.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            } catch (Exception e) {
                Snackbar.make(view, "Could not open Accessibility Settings", Snackbar.LENGTH_SHORT).show();
            }
        });

        // App Theme & Display Mode
        binding.tvThemeDesc.setText(com.android.sheguard.util.ThemeUtil.getThemeModeName(requireContext()));
        binding.themeContainer.setOnClickListener(v -> showThemeChooserDialog());

        // App Language Selection
        binding.tvLanguageDesc.setText(com.android.sheguard.util.LocaleUtil.getLanguageName(requireContext()));
        binding.languageContainer.setOnClickListener(v -> showLanguageChooserDialog());

        // App Updater
        binding.tvUpdaterDesc.setText(getString(R.string.app_updater_desc, BuildConfig.VERSION_NAME));
        binding.appUpdaterContainer.setOnClickListener(v -> checkForUpdates(view));

        // Version Changelog
        binding.tvChangelogDesc.setText(getString(R.string.changelog_desc, BuildConfig.VERSION_NAME));
        binding.changelogContainer.setOnClickListener(v -> showChangelogDialog());

        // Log Out
        binding.logoutContainer.setOnClickListener(v -> showLogoutConfirmationDialog());

        return view;
    }

    private void showLogoutConfirmationDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.btn_log_out))
                .setMessage("Are you sure you want to log out of GuardianAI?")
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    Prefs.putBoolean(Constants.IS_DEMO_MODE, false);
                    Prefs.remove(Constants.PREFS_USER_NAME);
                    Prefs.remove(Constants.PREFS_USER_EMAIL);
                    Prefs.remove(Constants.PREFS_USER_PHONE);
                    Prefs.remove("USER_ROLE");

                    Intent intent = new Intent(requireContext(), com.android.sheguard.ui.activity.OnBoardingActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finishAffinity();
                    }
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    private void showLanguageChooserDialog() {
        String[] languages = {
                getString(R.string.language_system),
                getString(R.string.language_english),
                getString(R.string.language_hindi),
                getString(R.string.language_telugu)
        };
        int currentIndex = com.android.sheguard.util.LocaleUtil.getLanguageIndex();

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.choose_language))
                .setSingleChoiceItems(languages, currentIndex, (dialog, which) -> {
                    String selectedLang;
                    switch (which) {
                        case 1:
                            selectedLang = Constants.LANG_EN;
                            break;
                        case 2:
                            selectedLang = Constants.LANG_HI;
                            break;
                        case 3:
                            selectedLang = Constants.LANG_TE;
                            break;
                        case 0:
                        default:
                            selectedLang = Constants.LANG_SYSTEM;
                            break;
                    }
                    com.android.sheguard.util.LocaleUtil.setLocale(requireContext(), selectedLang);
                    dialog.dismiss();
                    if (getActivity() != null) {
                        getActivity().recreate();
                    }
                })
                .setNegativeButton(getString(R.string.btn_later), null)
                .show();
    }

    private void showThemeChooserDialog() {
        String[] themes = {
                getString(R.string.theme_system),
                getString(R.string.theme_light),
                getString(R.string.theme_dark),
                getString(R.string.theme_amoled)
        };
        int currentIndex = com.android.sheguard.util.ThemeUtil.getThemeIndex();

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.choose_theme))
                .setSingleChoiceItems(themes, currentIndex, (dialog, which) -> {
                    String selectedMode;
                    switch (which) {
                        case 1:
                            selectedMode = Constants.THEME_LIGHT;
                            break;
                        case 2:
                            selectedMode = Constants.THEME_DARK;
                            break;
                        case 3:
                            selectedMode = Constants.THEME_AMOLED;
                            break;
                        case 0:
                        default:
                            selectedMode = Constants.THEME_SYSTEM;
                            break;
                    }
                    Prefs.putString(Constants.SETTINGS_THEME_MODE, selectedMode);
                    com.android.sheguard.util.ThemeUtil.applyTheme(selectedMode);
                    binding.tvThemeDesc.setText(com.android.sheguard.util.ThemeUtil.getThemeModeName(requireContext()));
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.btn_later), null)
                .show();
    }

    private void showChangelogDialog() {
        android.text.Spanned styledMessage;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            styledMessage = android.text.Html.fromHtml(getString(R.string.changelog_v1_0_1_content).replace("\n", "<br/>"), android.text.Html.FROM_HTML_MODE_LEGACY);
        } else {
            styledMessage = android.text.Html.fromHtml(getString(R.string.changelog_v1_0_1_content).replace("\n", "<br/>"));
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.changelog_v1_0_1_title))
                .setMessage(styledMessage)
                .setPositiveButton(getString(R.string.btn_ok), null)
                .show();
    }

    private void checkForUpdates(View view) {
        if (getActivity() != null) {
            com.android.sheguard.util.AppUpdateManager.checkForUpdates(requireActivity(), true);
        }
    }
}