package com.android.sheguard;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.android.sheguard.receiver.BatteryMonitorReceiver;
import com.android.sheguard.util.ThemeUtil;
import com.google.android.material.color.DynamicColors;
import com.google.gson.Gson;

public class SheGuard extends Application {

    public static final Gson GSON = new Gson();
    @SuppressLint("StaticFieldLeak")
    public static Context context;

    public static Context getAppContext() {
        return context;
    }

    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        ThemeUtil.applySavedTheme();
        DynamicColors.applyToActivitiesIfAvailable(this);

        try {
            IntentFilter batteryFilter = new IntentFilter();
            batteryFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
            batteryFilter.addAction(Intent.ACTION_BATTERY_LOW);
            batteryFilter.addAction(Intent.ACTION_POWER_CONNECTED);
            registerReceiver(new BatteryMonitorReceiver(), batteryFilter);
        } catch (Exception ignored) {}
    }
}