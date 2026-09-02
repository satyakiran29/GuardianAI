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

    @Override
    protected void onStart() {
        super.onStart();

        boolean isDemoMode = Prefs.getBoolean(Constants.IS_DEMO_MODE, false);
        boolean isUserLoggedIn = Prefs.getString(Constants.PREFS_USER_PHONE, null) != null || Prefs.getString(Constants.PREFS_USER_EMAIL, null) != null;

        if (!isDemoMode && !isUserLoggedIn) {
            startActivity(new Intent(MainActivity.this, OnBoardingActivity.class));
            finishAffinity();
        }
    }
}