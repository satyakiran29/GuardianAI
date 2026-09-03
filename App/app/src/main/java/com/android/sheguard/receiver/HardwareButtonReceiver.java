package com.android.sheguard.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.android.sheguard.R;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.util.SosUtil;

public class HardwareButtonReceiver extends BroadcastReceiver {

    private static final String TAG = "HardwareButtonReceiver";
    private static final long TIME_WINDOW_MS = 3000; // 3 seconds window for 3 clicks
    private static int screenClickCount = 0;
    private static long firstClickTime = 0;
    private static long lastTriggerTime = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        if (Intent.ACTION_SCREEN_OFF.equals(action) || Intent.ACTION_SCREEN_ON.equals(action)) {
            // Check if Power Button SOS is enabled
            String mode = Prefs.getString(Constants.SETTINGS_HARDWARE_TRIGGER_MODE, Constants.HW_MODE_BOTH);
            boolean isPowerSosEnabled = Prefs.getBoolean(Constants.SETTINGS_POWER_BUTTON_SOS, true);
            boolean isMasterHwEnabled = Prefs.getBoolean(Constants.SETTINGS_HARDWARE_BUTTON_SOS, true);

            if (!isMasterHwEnabled || (!isPowerSosEnabled && !Constants.HW_MODE_BOTH.equals(mode) && !Constants.HW_MODE_POWER_ONLY.equals(mode)) || Constants.HW_MODE_DISABLED.equals(mode) || Constants.HW_MODE_VOLUME_ONLY.equals(mode)) {
                return;
            }

            long currentTime = System.currentTimeMillis();

            // Prevent re-triggering within 10 seconds of active SOS
            if (currentTime - lastTriggerTime < 10000) {
                return;
            }

            if (screenClickCount == 0 || (currentTime - firstClickTime) > TIME_WINDOW_MS) {
                screenClickCount = 1;
                firstClickTime = currentTime;
                Log.d(TAG, "Power button click 1/3 at " + currentTime);
            } else {
                screenClickCount++;
                Log.d(TAG, "Power button click " + screenClickCount + "/3");
                if (screenClickCount >= 3) {
                    lastTriggerTime = currentTime;
                    screenClickCount = 0;
                    firstClickTime = 0;
                    Log.w(TAG, "🚨 TRIPLE POWER BUTTON CLICK DETECTED! Triggering Instant Emergency SOS!");

                    try {
                        Toast.makeText(context.getApplicationContext(), context.getString(R.string.hardware_sos_triggered_toast), Toast.LENGTH_LONG).show();
                    } catch (Exception ignored) {}

                    SosUtil.vibrateDevice(context);
                    SosUtil.activateInstantSosMode(context.getApplicationContext());
                }
            }
        }
    }
}
