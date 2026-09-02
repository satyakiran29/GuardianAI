package com.android.sheguard.ui.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.databinding.ActivityOnboardingBinding;
import com.android.sheguard.ui.view.OnBoardingView;

public class OnBoardingActivity extends AppCompatActivity {

    ActivityOnboardingBinding binding;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.android.sheguard.util.LocaleUtil.setLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void onStart() {
        super.onStart();

        boolean isDemoMode = Prefs.getBoolean(Constants.IS_DEMO_MODE, false);
        boolean isUserLoggedIn = Prefs.getString(Constants.PREFS_USER_PHONE, null) != null || Prefs.getString(Constants.PREFS_USER_EMAIL, null) != null;

        if (isDemoMode || isUserLoggedIn) {
            startActivity(new Intent(OnBoardingActivity.this, MainActivity.class));
            finishAffinity();
        }
    }

    @Override
    public void onBackPressed() {
        try {
            OnBoardingView.navigateToPrevSlide();
        } catch (Exception ignored) {
            OnBoardingActivity.this.finish();
            System.exit(0);
            super.onBackPressed();
        }
    }
}