package com.android.sheguard.receiver;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.BatteryManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.android.sheguard.R;
import com.android.sheguard.SheGuard;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.model.ContactModel;
import com.android.sheguard.util.SosUtil;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class BatteryMonitorReceiver extends BroadcastReceiver {

    private static final String TAG = "BatteryMonitor";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();

        if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            Log.i(TAG, "Device charging. Resetting low battery alert cycle flag.");
            Prefs.putBoolean(Constants.KEY_BATTERY_ALERT_SENT, false);
            return;
        }

        if (Intent.ACTION_BATTERY_CHANGED.equals(action) || Intent.ACTION_BATTERY_LOW.equals(action)) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);

            if (scale <= 0) return;
            int batteryPct = (int) ((level / (float) scale) * 100);

            if (isCharging || batteryPct > 20) {
                Prefs.putBoolean(Constants.KEY_BATTERY_ALERT_SENT, false);
                return;
            }

            boolean alertEnabled = Prefs.getBoolean(Constants.SETTINGS_LOW_BATTERY_ALERT, true);
            boolean alreadySent = Prefs.getBoolean(Constants.KEY_BATTERY_ALERT_SENT, false);

            if (alertEnabled && !alreadySent && batteryPct <= Constants.BATTERY_ALERT_THRESHOLD && batteryPct > 0) {
                Log.i(TAG, "Battery dropped to " + batteryPct + "%. Triggering guardian auto-alert!");
                Prefs.putBoolean(Constants.KEY_BATTERY_ALERT_SENT, true);

                // Stop background apps to conserve power
                SosUtil.stopBackgroundProcesses(context);
                SosUtil.vibrateDevice(context);

                dispatchLowBatteryLocation(context, batteryPct);

                try {
                    Toast.makeText(context, context.getString(R.string.low_battery_alert_sent_toast), Toast.LENGTH_LONG).show();
                } catch (Exception ignored) {}
            }
        }
    }

    private void dispatchLowBatteryLocation(Context context, int batteryPct) {
        ArrayList<ContactModel> contacts = new ArrayList<>();
        Gson gson = SheGuard.GSON;
        String jsonContacts = Prefs.getString(Constants.CONTACTS_LIST, "");

        if (!jsonContacts.isEmpty()) {
            Type type = new TypeToken<List<ContactModel>>() {}.getType();
            contacts.addAll(gson.fromJson(jsonContacts, type));
        }

        if (contacts.isEmpty()) {
            Log.w(TAG, "No emergency contacts configured for low battery dispatch.");
            return;
        }

        // Fetch last known location
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            Task<Location> lastLocationTask = LocationServices.getFusedLocationProviderClient(context).getLastLocation();
            lastLocationTask.addOnSuccessListener(location -> {
                String locationUrl = (location != null)
                        ? "https://maps.google.com/maps?q=loc:" + location.getLatitude() + "," + location.getLongitude()
                        : (SosUtil.getLiveLocationUrl().isEmpty() ? "https://maps.google.com" : SosUtil.getLiveLocationUrl());

                sendLowBatteryMessages(context, contacts, locationUrl, batteryPct);
            }).addOnFailureListener(e -> {
                String locationUrl = SosUtil.getLiveLocationUrl().isEmpty() ? "https://maps.google.com" : SosUtil.getLiveLocationUrl();
                sendLowBatteryMessages(context, contacts, locationUrl, batteryPct);
            });
        } else {
            String locationUrl = SosUtil.getLiveLocationUrl().isEmpty() ? "https://maps.google.com" : SosUtil.getLiveLocationUrl();
            sendLowBatteryMessages(context, contacts, locationUrl, batteryPct);
        }
    }

    private void sendLowBatteryMessages(Context context, ArrayList<ContactModel> contacts, String locationUrl, int batteryPct) {
        if (!com.android.sheguard.util.SmsHelper.isSmsPermissionGranted(context) || contacts == null) {
            return;
        }

        for (ContactModel contact : contacts) {
            if (contact != null && contact.getPhone() != null) {
                String msg = context.getString(R.string.low_battery_sms_message, contact.getName(), batteryPct, locationUrl);
                boolean sent = com.android.sheguard.util.SmsHelper.sendSms(context, contact.getPhone(), msg);
                if (sent) {
                    Log.i(TAG, "Low battery SMS dispatched to: " + contact.getName());
                }
            }
        }
    }
}
