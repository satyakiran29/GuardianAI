package com.android.sheguard.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.android.sheguard.R;
import com.google.android.material.card.MaterialCardView;

public class AppleGlassManager {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_APPLE_GLASS = "apple_glass";

    public static boolean isAppleGlassEnabled(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_APPLE_GLASS, false);
    }

    public static void setAppleGlassEnabled(Context context, boolean enabled) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_APPLE_GLASS, enabled).apply();
    }

    public static void applyGlassToMainActivity(Activity activity, boolean isNightMode) {
        if (!isAppleGlassEnabled(activity)) return;
        View drawerLayout = activity.findViewById(R.id.drawerLayout);
        if (drawerLayout != null) {
            int canvasRes = isNightMode ? R.drawable.bg_apple_glass_canvas_dark : R.drawable.bg_apple_glass_canvas_light;
            drawerLayout.setBackgroundResource(canvasRes);
        }
    }

    public static void registerFragmentGlassCallbacks(FragmentActivity activity, boolean isNightMode) {
        if (!isAppleGlassEnabled(activity)) return;
        activity.getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull View v, @Nullable Bundle savedInstanceState) {
                        super.onFragmentViewCreated(fm, f, v, savedInstanceState);
                        applyGlassToHierarchy(v, isNightMode);
                        v.post(() -> applyGlassToHierarchy(v, isNightMode));
                    }
                },
                true
        );
    }

    public static void applyGlassToActivity(Activity activity, boolean isNightMode) {
        if (!isAppleGlassEnabled(activity)) return;
        View contentView = activity.findViewById(android.R.id.content);
        if (contentView != null) {
            contentView.post(() -> applyGlassToHierarchy(contentView, isNightMode));
        }
    }

    public static void applyGlassToHierarchy(View rootView, boolean isNightMode) {
        if (rootView == null) return;
        Context context = rootView.getContext();
        if (context == null) return;

        if (rootView.getId() != R.id.drawerLayout) {
            rootView.setBackgroundColor(Color.TRANSPARENT);
        }

        int glassCardBg = isNightMode
                ? Color.argb(70, 30, 34, 53)      // Translucent dark acrylic
                : Color.argb(242, 255, 255, 255); // Translucent frosted milk glass

        int glassCardStroke = isNightMode
                ? Color.argb(55, 255, 255, 255)   // Specular rim highlight for dark mode
                : Color.argb(34, 15, 23, 42);      // Crisp specular slate rim for light mode

        float cardElevationPx = isNightMode ? 0f : dpToPx(context, 1.5f);
        int strokeWidthPx = Math.max(2, (int) dpToPx(context, 1.2f));
        int maxBadgePx = (int) dpToPx(context, 72f);

        traverse(rootView, context, glassCardBg, glassCardStroke, cardElevationPx, strokeWidthPx, maxBadgePx, isNightMode);
    }

    private static void traverse(View view, Context context, int glassCardBg, int glassCardStroke, float cardElevationPx, int strokeWidthPx, int maxBadgePx, boolean isNightMode) {
        if (view == null) return;

        int viewId = view.getId();

        // 1. Bottom Navigation Dock glass styling
        if (viewId == R.id.bottom_nav_dock) {
            int navBgRes = isNightMode ? R.drawable.bg_apple_glass_bottom_nav_dark : R.drawable.bg_apple_glass_bottom_nav_light;
            view.setBackgroundResource(navBgRes);
            view.setElevation(dpToPx(context, isNightMode ? 8f : 4f));
        }

        // 2. Non-MaterialCardView Safety Suite & Trust cards
        if (viewId == R.id.card_fake_call
                || viewId == R.id.card_voice_sos
                || viewId == R.id.card_battery_alert
                || viewId == R.id.card_safe_haven
                || viewId == R.id.card_route_safety
                || viewId == R.id.card_location_replay
                || viewId == R.id.btn_home_forward_whatsapp
                || viewId == R.id.btn_home_fake_call
                || viewId == R.id.btn_home_family_circle
                || viewId == R.id.btn_home_safe_places) {
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            gd.setColor(glassCardBg);
            gd.setStroke(strokeWidthPx, glassCardStroke);
            gd.setCornerRadius(dpToPx(context, 18f));
            view.setBackground(gd);
            view.setElevation(cardElevationPx);
        }

        if (view instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) view;
            int cardId = card.getId();

            boolean isAccentPickerCard = cardId == R.id.card_theme_default
                    || cardId == R.id.card_theme_blue
                    || cardId == R.id.card_theme_violet
                    || cardId == R.id.card_theme_emerald;

            if (isAccentPickerCard) {
                card.setCardBackgroundColor(ColorStateList.valueOf(glassCardBg));
                card.setCardElevation(cardElevationPx);
                return;
            }

            ViewGroup.LayoutParams lp = card.getLayoutParams();
            boolean isSmallDimension = (card.getWidth() > 0 && card.getWidth() <= maxBadgePx && card.getHeight() > 0 && card.getHeight() <= maxBadgePx)
                    || (lp != null && lp.width > 0 && lp.width <= maxBadgePx && lp.height > 0 && lp.height <= maxBadgePx);

            String entryName = "";
            if (cardId != View.NO_ID) {
                try {
                    entryName = context.getResources().getResourceEntryName(cardId).toLowerCase();
                } catch (Exception ignored) {}
            }

            boolean isBadgeOrAvatar = entryName.contains("icon")
                    || entryName.contains("badge")
                    || entryName.contains("avatar")
                    || entryName.contains("category")
                    || entryName.contains("thumb")
                    || entryName.contains("circle")
                    || entryName.contains("dot")
                    || entryName.contains("preview")
                    || entryName.contains("qr");

            if (isSmallDimension || isBadgeOrAvatar) {
                for (int i = 0; i < card.getChildCount(); i++) {
                    traverse(card.getChildAt(i), context, glassCardBg, glassCardStroke, cardElevationPx, strokeWidthPx, maxBadgePx, isNightMode);
                }
                return;
            }

            // Standard container card styling
            card.setCardBackgroundColor(ColorStateList.valueOf(glassCardBg));
            card.setStrokeColor(glassCardStroke);
            card.setStrokeWidth(strokeWidthPx);
            card.setCardElevation(cardElevationPx);
            if (card.getRadius() > 0 && card.getRadius() <= dpToPx(context, 16f)) {
                card.setRadius(dpToPx(context, 18f));
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                traverse(group.getChildAt(i), context, glassCardBg, glassCardStroke, cardElevationPx, strokeWidthPx, maxBadgePx, isNightMode);
            }
        }
    }

    private static float dpToPx(Context context, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}
