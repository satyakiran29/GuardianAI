package com.android.sheguard.ui.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.android.sheguard.R;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.databinding.ActivityMainBinding;
import com.android.sheguard.util.ObservableVariable;

@SuppressWarnings("FieldCanBeLocal")
public class MainActivity extends AppCompatActivity {

    public static ObservableVariable<Boolean> shakeDetection = new ObservableVariable<>();
    private ActivityMainBinding binding;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.android.sheguard.util.LocaleUtil.setLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout, (v, insets) -> {
            androidx.core.view.ViewCompat.dispatchApplyWindowInsets(binding.fragmentContainerView, insets);
            return insets;
        });

        binding.navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            binding.drawerLayout.closeDrawer(binding.navView);
            NavController navController = Navigation.findNavController(this, R.id.fragmentContainerView);
            if (id == R.id.nav_profile) {
                navController.navigate(R.id.profileFragment);
                return true;
            } else if (id == R.id.nav_safe_ride) {
                navController.navigate(R.id.safeRideFragment);
                return true;
            } else if (id == R.id.nav_settings) {
                navController.navigate(R.id.settingsFragment);
                return true;
            } else if (id == R.id.nav_logout) {
                Prefs.putBoolean(Constants.IS_DEMO_MODE, false);
                Prefs.remove(Constants.PREFS_USER_NAME);
                Prefs.remove(Constants.PREFS_USER_EMAIL);
                Prefs.remove(Constants.PREFS_USER_PHONE);
                Prefs.remove("USER_ROLE");

                Intent intent = new Intent(MainActivity.this, OnBoardingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finishAffinity();
                return true;
            }
            return false;
        });

        // Silent in-app update check on startup
        com.android.sheguard.util.AppUpdateManager.checkForUpdates(this, false);
    }

    public void toggleDrawer() {
        if (binding.drawerLayout.isDrawerOpen(binding.navView)) {
            binding.drawerLayout.closeDrawer(binding.navView);
        } else {
            binding.drawerLayout.openDrawer(binding.navView);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.fragmentContainerView);
        return NavigationUI.navigateUp(navController, binding.drawerLayout) || super.onSupportNavigateUp();
    }

    private int volumeKeyPressCount = 0;
    private long firstVolumeKeyPressTime = 0;
    private long lastVolumeKeyTriggerTime = 0;

    @Override
    public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getAction() == android.view.KeyEvent.ACTION_DOWN &&
                (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN)) {

            String mode = Prefs.getString(Constants.SETTINGS_HARDWARE_TRIGGER_MODE, Constants.HW_MODE_BOTH);
            boolean isVolumeEnabled = Prefs.getBoolean(Constants.SETTINGS_VOLUME_BUTTON_SOS, true);
            boolean isMasterHwEnabled = Prefs.getBoolean(Constants.SETTINGS_HARDWARE_BUTTON_SOS, true);

            if (isMasterHwEnabled && (isVolumeEnabled || Constants.HW_MODE_BOTH.equals(mode) || Constants.HW_MODE_VOLUME_ONLY.equals(mode)) && !Constants.HW_MODE_DISABLED.equals(mode) && !Constants.HW_MODE_POWER_ONLY.equals(mode)) {
                long now = System.currentTimeMillis();
                if (now - lastVolumeKeyTriggerTime < 10000) {
                    return super.dispatchKeyEvent(event);
                }

                if (volumeKeyPressCount == 0 || (now - firstVolumeKeyPressTime) > 2000) {
                    volumeKeyPressCount = 1;
                    firstVolumeKeyPressTime = now;
                } else {
                    volumeKeyPressCount++;
                    if (volumeKeyPressCount >= 3) {
                        lastVolumeKeyTriggerTime = now;
                        volumeKeyPressCount = 0;
                        firstVolumeKeyPressTime = 0;
                        android.util.Log.w("MainActivity", "🚨 VOLUME KEY TRIPLE-CLICK DETECTED in Foreground!");
                        com.android.sheguard.util.SosUtil.vibrateDevice(this);
                        try {
                            android.widget.Toast.makeText(this, getString(R.string.hardware_sos_triggered_toast), android.widget.Toast.LENGTH_LONG).show();
                        } catch (Exception ignored) {}
                        com.android.sheguard.util.SosUtil.activateInstantSosMode(this);
                        return true;
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onStart() {
        super.onStart();

        boolean isDemoMode = Prefs.getBoolean(Constants.IS_DEMO_MODE, false);
        boolean isUserLoggedIn = Prefs.getString(Constants.PREFS_USER_PHONE, null) != null || Prefs.getString(Constants.PREFS_USER_EMAIL, null) != null;

        if (!isDemoMode && !isUserLoggedIn) {
            startActivity(new Intent(MainActivity.this, OnBoardingActivity.class));
            finishAffinity();
            return;
        }

        // Always ensure 24/7 background safety protection service is running with notification
        if (!com.android.sheguard.service.SosService.isRunning) {
            if (com.android.sheguard.util.AppUtil.permissionsGranted(this)) {
                com.android.sheguard.util.SosUtil.startSosNotificationService(this);
            }
        }
    }
}