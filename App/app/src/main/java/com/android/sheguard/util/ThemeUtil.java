package com.android.sheguard.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.view.View;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.sheguard.R;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;

public class ThemeUtil {

    public static final String PREFS_NAME = "theme_prefs";
    public static final String KEY_THEME_COLOR = "theme_color_name";
    public static final String KEY_THEME_MODE = "theme_mode";
    public static final String KEY_APPLE_GLASS = "apple_glass";
    public static final String KEY_PURE_BLACK = "pure_black";

    public static final String COLOR_DEFAULT = "default";
    public static final String COLOR_BLUE = "blue";
    public static final String COLOR_VIOLET = "violet";
    public static final String COLOR_EMERALD = "emerald";

    public static final String MODE_SYSTEM = "system";
    public static final String MODE_LIGHT = "light";
    public static final String MODE_DARK = "dark";
    public static final String MODE_AMOLED = "amoled";

    /**
     * Applies full theme (Accent color, Night mode, Apple Glass, Pure Black AMOLED) to an Activity.
     * Should be called in Activity.onCreate() before setContentView().
     */
    public static void applyTheme(Activity activity) {
        if (activity == null) return;

        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String themeColorName = prefs.getString(KEY_THEME_COLOR, COLOR_DEFAULT);
        String themeMode = prefs.getString(KEY_THEME_MODE, MODE_SYSTEM);
        boolean isAppleGlass = prefs.getBoolean(KEY_APPLE_GLASS, false);
        boolean isPureBlack = prefs.getBoolean(KEY_PURE_BLACK, false);

        // Apply night mode delegate
        applyNightMode(themeMode);

        // Apply base Activity theme with chosen accent color
        int themeId;
        switch (themeColorName) {
            case COLOR_BLUE:
                themeId = R.style.Theme_SheGuard_Blue;
                break;
            case COLOR_VIOLET:
                themeId = R.style.Theme_SheGuard_Violet;
                break;
            case COLOR_EMERALD:
                themeId = R.style.Theme_SheGuard_Emerald;
                break;
            case COLOR_DEFAULT:
            default:
                themeId = R.style.Theme_SheGuard;
                break;
        }
        activity.setTheme(themeId);

        boolean isNightMode = isNightMode(activity, themeMode);

        if (isAppleGlass) {
            activity.getTheme().applyStyle(R.style.ThemeOverlay_AppleGlass, true);
            int canvasBg = isNightMode ? R.drawable.bg_apple_glass_canvas_dark : R.drawable.bg_apple_glass_canvas_light;
            activity.getWindow().getDecorView().setBackgroundResource(canvasBg);
            activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
            activity.getWindow().setNavigationBarColor(Color.TRANSPARENT);

            if (activity instanceof FragmentActivity) {
                AppleGlassManager.registerFragmentGlassCallbacks((FragmentActivity) activity, isNightMode);
            }
            AppleGlassManager.applyGlassToActivity(activity, isNightMode);
        } else if (isPureBlack && isNightMode) {
            activity.getTheme().applyStyle(R.style.ThemeOverlay_PureBlack, true);
            activity.getWindow().getDecorView().setBackgroundColor(Color.BLACK);
        } else {
            activity.getWindow().getDecorView().setBackgroundColor(Color.TRANSPARENT);
        }

        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(!isNightMode);
            insetsController.setAppearanceLightNavigationBars(!isNightMode);
        }

        if (activity instanceof FragmentActivity) {
            registerFullUiThemeCallbacks((FragmentActivity) activity);
        }

        View contentView = activity.findViewById(android.R.id.content);
        if (contentView != null) {
            contentView.post(() -> applyFullUiTheme(contentView, activity));
        }
    }

    public static void registerFullUiThemeCallbacks(FragmentActivity activity) {
        if (activity == null) return;
        activity.getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentViewCreated(androidx.fragment.app.FragmentManager fm, androidx.fragment.app.Fragment f, View v, android.os.Bundle savedInstanceState) {
                        super.onFragmentViewCreated(fm, f, v, savedInstanceState);
                        applyFullUiTheme(v, activity);
                        v.post(() -> applyFullUiTheme(v, activity));
                    }
                },
                true
        );
    }

    public static void applyFullUiTheme(View rootView, Context context) {
        if (rootView == null || context == null) return;
        int accentColor = getAccentColor(context);
        traverseAndTintUi(rootView, context, accentColor);
    }

    private static void traverseAndTintUi(View view, Context context, int accentColor) {
        if (view == null) return;

        int viewId = view.getId();
        String entryName = "";
        if (viewId != View.NO_ID) {
            try {
                entryName = context.getResources().getResourceEntryName(viewId).toLowerCase();
            } catch (Exception ignored) {}
        }

        if (view instanceof com.google.android.material.button.MaterialButton) {
            com.google.android.material.button.MaterialButton mb = (com.google.android.material.button.MaterialButton) view;
            if (entryName.contains("login") || entryName.contains("register") || entryName.contains("next") || entryName.contains("play_pause")) {
                mb.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
            }
        } else if (view instanceof com.google.android.material.floatingactionbutton.FloatingActionButton) {
            com.google.android.material.floatingactionbutton.FloatingActionButton fab = (com.google.android.material.floatingactionbutton.FloatingActionButton) view;
            fab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
        } else if (view instanceof android.widget.ProgressBar) {
            android.widget.ProgressBar pb = (android.widget.ProgressBar) view;
            pb.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(accentColor));
            pb.setProgressTintList(android.content.res.ColorStateList.valueOf(accentColor));
        }

        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                traverseAndTintUi(vg.getChildAt(i), context, accentColor);
            }
        }
    }

    public static void applyNightMode(String mode) {
        if (mode == null) {
            mode = MODE_SYSTEM;
        }

        switch (mode) {
            case MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case MODE_DARK:
            case MODE_AMOLED:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case MODE_SYSTEM:
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
        Context context = com.android.sheguard.SheGuard.getAppContext();
        String savedMode = MODE_SYSTEM;
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            savedMode = prefs.getString(KEY_THEME_MODE, Prefs.getString(Constants.SETTINGS_THEME_MODE, MODE_SYSTEM));
        }
        applyNightMode(savedMode);
    }

    public static boolean isNightMode(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String mode = prefs.getString(KEY_THEME_MODE, MODE_SYSTEM);
        return isNightMode(context, mode);
    }

    public static boolean isNightMode(Context context, String themeMode) {
        if (MODE_LIGHT.equals(themeMode)) {
            return false;
        } else if (MODE_DARK.equals(themeMode) || MODE_AMOLED.equals(themeMode)) {
            return true;
        } else {
            if (context == null || context.getResources() == null) return false;
            int nightMask = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return nightMask == Configuration.UI_MODE_NIGHT_YES;
        }
    }

    public static int getAccentColor(Context context) {
        if (context == null) return Color.parseColor("#FF2E93");
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String colorName = prefs.getString(KEY_THEME_COLOR, COLOR_DEFAULT);
        switch (colorName) {
            case COLOR_BLUE:
                return ContextCompat.getColor(context, R.color.accent_blue);
            case COLOR_VIOLET:
                return ContextCompat.getColor(context, R.color.accent_violet);
            case COLOR_EMERALD:
                return ContextCompat.getColor(context, R.color.accent_emerald);
            case COLOR_DEFAULT:
            default:
                return ContextCompat.getColor(context, R.color.accent_pink);
        }
    }

    public static String getThemeSummary(Context context) {
        if (context == null) return "System Default";
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String colorName = prefs.getString(KEY_THEME_COLOR, COLOR_DEFAULT);
        String mode = prefs.getString(KEY_THEME_MODE, MODE_SYSTEM);
        boolean isGlass = prefs.getBoolean(KEY_APPLE_GLASS, false);

        String colorTitle;
        switch (colorName) {
            case COLOR_BLUE:
                colorTitle = "Blue";
                break;
            case COLOR_VIOLET:
                colorTitle = "Violet";
                break;
            case COLOR_EMERALD:
                colorTitle = "Emerald";
                break;
            case COLOR_DEFAULT:
            default:
                colorTitle = "Default (Pink)";
                break;
        }

        String modeTitle;
        switch (mode) {
            case MODE_LIGHT:
                modeTitle = "Light";
                break;
            case MODE_DARK:
                modeTitle = "Dark";
                break;
            case MODE_AMOLED:
                modeTitle = "AMOLED";
                break;
            case MODE_SYSTEM:
            default:
                modeTitle = "System";
                break;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(colorTitle).append(" • ").append(modeTitle);
        if (isGlass) {
            sb.append(" • Glass UI");
        }
        return sb.toString();
    }

    // Legacy compatibility methods
    public static void applyTheme(String mode) {
        applyNightMode(mode);
    }

    public static String getThemeMode() {
        Context context = com.android.sheguard.SheGuard.getAppContext();
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getString(KEY_THEME_MODE, Prefs.getString(Constants.SETTINGS_THEME_MODE, MODE_SYSTEM));
        }
        return Prefs.getString(Constants.SETTINGS_THEME_MODE, MODE_SYSTEM);
    }

    public static boolean isAmoledMode() {
        Context context = com.android.sheguard.SheGuard.getAppContext();
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getBoolean(KEY_PURE_BLACK, false);
        }
        return MODE_AMOLED.equals(getThemeMode());
    }

    public static int getThemeIndex() {
        String mode = getThemeMode();
        switch (mode) {
            case MODE_LIGHT:
                return 1;
            case MODE_DARK:
                return 2;
            case MODE_AMOLED:
                return 3;
            case MODE_SYSTEM:
            default:
                return 0;
        }
    }

    public static String getThemeModeName(Context context) {
        return getThemeSummary(context);
    }
}
