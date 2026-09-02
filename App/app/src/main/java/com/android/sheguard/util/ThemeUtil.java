package com.android.sheguard.util;

import android.content.Context;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

import com.android.sheguard.R;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;

public class ThemeUtil {

    public static void applyTheme(String mode) {
        if (mode == null) {
            mode = Constants.THEME_SYSTEM;
        }

        switch (mode) {
            case Constants.THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case Constants.THEME_DARK:
            case Constants.THEME_AMOLED:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case Constants.THEME_SYSTEM:
            default:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                }
                break;
        }
    }

    public static void applySavedTheme() {
        String savedMode = Prefs.getString(Constants.SETTINGS_THEME_MODE, Constants.THEME_SYSTEM);
        applyTheme(savedMode);
    }

    public static String getThemeMode() {
        return Prefs.getString(Constants.SETTINGS_THEME_MODE, Constants.THEME_SYSTEM);
    }

    public static boolean isAmoledMode() {
        return Constants.THEME_AMOLED.equals(getThemeMode());
    }

    public static int getThemeIndex() {
        String mode = getThemeMode();
        switch (mode) {
            case Constants.THEME_LIGHT:
                return 1;
            case Constants.THEME_DARK:
                return 2;
            case Constants.THEME_AMOLED:
                return 3;
            case Constants.THEME_SYSTEM:
            default:
                return 0;
        }
    }

    public static String getThemeModeName(Context context) {
        String mode = getThemeMode();
        switch (mode) {
            case Constants.THEME_LIGHT:
                return context.getString(R.string.theme_light);
            case Constants.THEME_DARK:
                return context.getString(R.string.theme_dark);
            case Constants.THEME_AMOLED:
                return context.getString(R.string.theme_amoled);
            case Constants.THEME_SYSTEM:
            default:
                return context.getString(R.string.theme_system);
        }
    }
}
