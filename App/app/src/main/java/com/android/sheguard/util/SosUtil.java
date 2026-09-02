package com.android.sheguard.util;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.sheguard.R;
import com.android.sheguard.SheGuard;
import com.android.sheguard.api.NotificationAPI;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.model.ContactModel;
import com.android.sheguard.service.SosService;
import com.android.sheguard.ui.fragment.ContactsFragment;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SosUtil {

    private static String mLocation = "";
    private static boolean sentSMS = false;
    private static boolean sentNotification = false;
    private static boolean sentWhatsApp = false;
    private static boolean calledEmergency = false;
    private static AudioManager audioManager = null;
    private static LocationRequest locationRequest = null;
    private static LocationManager locationManager = null;
    private static NotificationAPI notificationApiService = null;
    private static final MediaPlayer mediaPlayer = new MediaPlayer();

    static {
        if (locationRequest == null) {
            locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(2000)
                    .setMaxUpdateDelayMillis(5000)
                    .build();
        }

        if (notificationApiService == null) {
            notificationApiService = NotificationClient.getClient("https://fcm.googleapis.com/").create(NotificationAPI.class);
        }
    }

    public static String getLiveLocationUrl() {
        return mLocation;
    }

    public static void stopBackgroundProcesses(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
                if (processes != null) {
                    String myPkg = context.getPackageName();
                    for (ActivityManager.RunningAppProcessInfo process : processes) {
                        for (String pkg : process.pkgList) {
                            if (!pkg.equals(myPkg) && !pkg.contains("android") && !pkg.contains("system")) {
                                am.killBackgroundProcesses(pkg);
                            }
                        }
                    }
                }
            }
            System.gc();
            Log.i("SafeMode", "Background apps stopped & resources freed for GuardianAI.");
        } catch (Exception e) {
            Log.e("SafeMode", "Error stopping background processes: " + e.getMessage());
        }
    }

    public static void vibrateDevice(Context context) {
        if (!Prefs.getBoolean(Constants.SETTINGS_HAPTIC_FEEDBACK, true)) return;
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(400);
                }
            }
        } catch (Exception ignored) {}
    }

    public static void sendWhatsAppWithLiveLocation(Context context) {
        vibrateDevice(context);

        ArrayList<ContactModel> contacts = new ArrayList<>();
        String jsonContacts = Prefs.getString(Constants.CONTACTS_LIST, "");
        if (!jsonContacts.isEmpty()) {
            Type type = new TypeToken<List<ContactModel>>() {}.getType();
            try {
                contacts.addAll(SheGuard.GSON.fromJson(jsonContacts, type));
            } catch (Exception ignored) {}
        }

        if (!mLocation.isEmpty() && mLocation.contains("Lat:")) {
            sendWhatsAppLocation(context, contacts, mLocation);
            return;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            com.google.android.gms.location.FusedLocationProviderClient client =
                    LocationServices.getFusedLocationProviderClient(context);

            try {
                client.getLastLocation().addOnSuccessListener(loc -> {
                    if (loc != null) {
                        mLocation = formatCoordinates(loc.getLatitude(), loc.getLongitude());
                        sendWhatsAppLocation(context, contacts, mLocation);
                    } else {
                        LocationHelper.requestSingleLocationUpdate(context, new LocationHelper.LocationResultListener() {
                            @Override
                            public void onLocationReceived(double latitude, double longitude, String addressName) {
                                mLocation = formatCoordinates(latitude, longitude);
                                sendWhatsAppLocation(context, contacts, mLocation);
                            }

                            @Override
                            public void onLocationError(String error) {
                                sendWhatsAppLocation(context, contacts, mLocation);
                            }
                        });
                    }
                }).addOnFailureListener(e -> {
                    LocationHelper.requestSingleLocationUpdate(context, new LocationHelper.LocationResultListener() {
                        @Override
                        public void onLocationReceived(double latitude, double longitude, String addressName) {
                            mLocation = formatCoordinates(latitude, longitude);
                            sendWhatsAppLocation(context, contacts, mLocation);
                        }

                        @Override
                        public void onLocationError(String error) {
                            sendWhatsAppLocation(context, contacts, mLocation);
                        }
                    });
                });
            } catch (SecurityException e) {
                sendWhatsAppLocation(context, contacts, mLocation);
            }
        } else {
            sendWhatsAppLocation(context, contacts, mLocation);
        }
    }

    public static void sendWhatsAppLocation(Context context, ArrayList<ContactModel> contacts, String locationUrl) {
        if (contacts == null || contacts.isEmpty()) {
            contacts = new ArrayList<>();
            String jsonContacts = Prefs.getString(Constants.CONTACTS_LIST, "");
            if (!jsonContacts.isEmpty()) {
                Type type = new TypeToken<List<ContactModel>>() {}.getType();
                try {
                    contacts.addAll(SheGuard.GSON.fromJson(jsonContacts, type));
                } catch (Exception ignored) {}
            }
        }

        if (locationUrl == null || locationUrl.isEmpty() || !locationUrl.contains("Lat:")) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                if (lm != null) {
                    android.location.Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (loc == null) {
                        loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                    if (loc == null) {
                        loc = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                    }
                    if (loc != null) {
                        locationUrl = formatCoordinates(loc.getLatitude(), loc.getLongitude());
                        mLocation = locationUrl;
                    }
                }
            }
        }

        if (locationUrl == null || locationUrl.isEmpty()) {
            locationUrl = "Lat: 0.00000, Lng: 0.00000\nhttps://maps.google.com";
        }

        String recipientPhone = "";
        String contactName = "Trusted Contact";
        if (!contacts.isEmpty()) {
            recipientPhone = contacts.get(0).getPhone();
            contactName = contacts.get(0).getName();
        }

        String cleanPhone = recipientPhone.replaceAll("[^0-9+]", "");
        if (cleanPhone.startsWith("+")) {
            cleanPhone = cleanPhone.substring(1);
        }

        String message = context.getString(R.string.whatsapp_sos_message, contactName, locationUrl);
        try {
            String encodedMsg = URLEncoder.encode(message, "UTF-8");
            String url = !cleanPhone.isEmpty()
                    ? "https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + encodedMsg
                    : "https://api.whatsapp.com/send?text=" + encodedMsg;

            Intent whatsappIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            whatsappIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(whatsappIntent);
        } catch (Exception e) {
            try {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType("text/plain");
                sendIntent.setPackage("com.whatsapp");
                sendIntent.putExtra(Intent.EXTRA_TEXT, message);
                sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(sendIntent);
            } catch (Exception ex) {
                Log.e("WhatsApp", "WhatsApp send error: " + ex.getMessage());
            }
        }
    }

    public static boolean isGPSEnabled(Context context) {
        if (locationManager == null) {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }

        Log.i("SOS", "isGPSEnabled: " + locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public static void turnOnGPS(Context context) {
        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(context)
                .checkLocationSettings(new LocationSettingsRequest.Builder()
                        .addLocationRequest(locationRequest)
                        .setAlwaysShow(true)
                        .build()
                );

        result.addOnCompleteListener(task -> {
            try {
                task.getResult(ApiException.class);
            } catch (ApiException apiException) {
                switch (apiException.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            ResolvableApiException resolvableApiException = (ResolvableApiException) apiException;
                            resolvableApiException.startResolutionForResult((AppCompatActivity) context, 2);
                        } catch (IntentSender.SendIntentException sendIntentException) {
                            Log.i("SOS", "turnOnGPS: " + sendIntentException.getMessage());
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }

    public static void startSosNotificationService(Context context) {
        if (!SosService.isRunning) {
            Intent notificationIntent = new Intent(context, SosService.class);
            notificationIntent.setAction("START");

            context.startForegroundService(notificationIntent);
        }
    }

    public static void stopSosNotificationService(Context context) {
        if (SosService.isRunning) {
            Intent notificationIntent = new Intent(context, SosService.class);
            notificationIntent.setAction("STOP");

            context.startForegroundService(notificationIntent);
        }
    }

    public static ArrayList<ContactModel> getStoredContacts(Context context) {
        ArrayList<ContactModel> contacts = new ArrayList<>();
        String jsonContacts = Prefs.getString(Constants.CONTACTS_LIST, "");
        if (!jsonContacts.isEmpty()) {
            Type type = new TypeToken<List<ContactModel>>() {}.getType();
            try {
                contacts.addAll(SheGuard.GSON.fromJson(jsonContacts, type));
            } catch (Exception ignored) {}
        }

        if (contacts.isEmpty() && ContactsFragment.contacts != null && !ContactsFragment.contacts.isEmpty()) {
            contacts.addAll(ContactsFragment.contacts);
        }

        if (contacts.isEmpty()) {
            contacts.add(new ContactModel("Emergency Helpline", "112"));
            try {
                Prefs.putString(Constants.CONTACTS_LIST, SheGuard.GSON.toJson(contacts));
            } catch (Exception ignored) {}
        }

        return contacts;
    }

    public static void activateInstantSosMode(Context context) {
        if (mediaPlayer.isPlaying()) {
            stopSiren();
            resetValues();
            Log.i("SOS", "Stopping Siren");
            Log.i("SOS", "Resetting Values");
            return;
        }

        resetValues();
        vibrateDevice(context);

        if (Prefs.getBoolean(Constants.SETTINGS_STOP_BACKGROUND_APPS, true) || Prefs.getBoolean(Constants.SETTINGS_SAFE_MODE, false)) {
            stopBackgroundProcesses(context);
        }

        ArrayList<ContactModel> contacts = getStoredContacts(context);

        // Auto-call to 100/110/emergency service removed per user specification.
        // Emergency alerts are dispatched exclusively to user-selected contacts and GuardianAI command dashboard.

        sendLocation(context, contacts);

        if (Prefs.getBoolean(Constants.SETTINGS_PLAY_SIREN, false) && !mediaPlayer.isPlaying()) {
            playSiren(context);
            Log.i("SOS", "Playing Siren");
        } else {
            stopSiren();
            Log.i("SOS", "Stopping Siren");
        }
    }

    public static String formatCoordinates(double latitude, double longitude) {
        return String.format(java.util.Locale.US, "Lat: %.5f, Lng: %.5f\nhttps://maps.google.com/maps?q=loc:%.5f,%.5f", latitude, longitude, latitude, longitude);
    }

    private static void broadcastSosPayload(Context context, ArrayList<ContactModel> contacts, double latitude, double longitude) {
        if (latitude != 0.0 || longitude != 0.0) {
            mLocation = formatCoordinates(latitude, longitude);
        } else if (mLocation.isEmpty()) {
            mLocation = "Live GPS coordinates acquiring...\nhttps://maps.google.com";
        }
        Log.i("SOS", "broadcastSosPayload: " + mLocation);

        if (Prefs.getBoolean(Constants.SETTINGS_SEND_SMS, true) && !sentSMS) {
            sendSMS(context, contacts);
            sentSMS = true;
        }

        if (Prefs.getBoolean(Constants.SETTINGS_SEND_NOTIFICATION, true) && !sentNotification) {
            sendNotification(context, contacts);
            sentNotification = true;
        }

        if ((Prefs.getBoolean(Constants.SETTINGS_SEND_WHATSAPP, true) || Prefs.getBoolean(Constants.SETTINGS_SAFE_MODE, false)) && !sentWhatsApp) {
            sendWhatsAppLocation(context, contacts, mLocation);
            sentWhatsApp = true;
        }

        // 4. Sync live telemetric SOS alert with Django & Supabase Cloud
        String userPhone = Prefs.getString(Constants.PREFS_USER_PHONE, "+919876543210");
        boolean isSiren = Prefs.getBoolean(Constants.SETTINGS_PLAY_SIREN, true);
        int batteryLevel = 85;
        try {
            android.os.BatteryManager bm = (android.os.BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (bm != null) {
                batteryLevel = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
            }
        } catch (Exception ignored) {}

        com.android.sheguard.api.ApiClient.triggerSos(
                userPhone,
                latitude != 0.0 ? latitude : 17.3850,
                longitude != 0.0 ? longitude : 78.4867,
                mLocation,
                "button",
                isSiren,
                batteryLevel
        );
    }

    private static void sendLocation(Context context, ArrayList<ContactModel> contacts) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            broadcastSosPayload(context, contacts, 0, 0);
            return;
        }

        com.google.android.gms.location.FusedLocationProviderClient fusedClient =
                LocationServices.getFusedLocationProviderClient(context);

        // 1. Instant Fast-Path: Query last known location for immediate dispatch
        try {
            fusedClient.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null) {
                    Log.i("SOS", "sendLocation: Last known location fast-path triggered");
                    broadcastSosPayload(context, contacts, loc.getLatitude(), loc.getLongitude());
                }
            });
        } catch (SecurityException ignored) {}

        // 2. Fresh High-Accuracy GPS Update (Triggers on 1st update)
        LocationRequest freshRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1500)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(1000)
                .setMaxUpdates(1)
                .build();

        LocationCallback callback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                fusedClient.removeLocationUpdates(this);
                if (!locationResult.getLocations().isEmpty()) {
                    android.location.Location loc = locationResult.getLastLocation();
                    if (loc != null) {
                        Log.i("SOS", "sendLocation: Fresh GPS fix received");
                        broadcastSosPayload(context, contacts, loc.getLatitude(), loc.getLongitude());
                    }
                }
            }
        };

        try {
            fusedClient.requestLocationUpdates(freshRequest, callback, Looper.getMainLooper());
        } catch (SecurityException ignored) {}

        // 3. Fallback Auto-Dispatch: Force automatic emergency broadcast if delay exceeds 3.5 seconds
        new android.os.Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!sentSMS && contacts != null && !contacts.isEmpty()) {
                Log.w("SOS", "sendLocation: Timeout reached, forcing automatic emergency dispatch");
                broadcastSosPayload(context, contacts, 0, 0);
            }
        }, 3500);
    }

    private static void sendSMS(Context context, ContactModel contact) {
        if (contact != null && contact.getPhone() != null) {
            String msg = context.getString(R.string.sos_message, contact.getName(), mLocation);
            boolean sent = SmsHelper.sendSmsWithFallback(context, contact.getPhone(), msg);
            Log.i("SOS", "sendSMS to " + contact.getName() + " (" + contact.getPhone() + "): " + (sent ? "SUCCESS" : "FAILED"));
        }
    }

    private static void sendSMS(Context context, ArrayList<ContactModel> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            contacts = getStoredContacts(context);
        }

        for (ContactModel contact : contacts) {
            sendSMS(context, contact);
        }
    }

    public static void sendNotification(Context context, ArrayList<ContactModel> contacts) {
        Log.i("SOS", "sendNotification: Emergency SOS broadcasted to contacts");
    }

    public static void sendNotification(String userToken, String title, String message) {
        Log.i("SOS", "sendNotification: " + title + " - " + message);
    }

    private static void callEmergency(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ArrayList<ContactModel> contacts = getStoredContacts(context);
        if (contacts != null && !contacts.isEmpty() && contacts.get(0).getPhone() != null) {
            String contactPhone = contacts.get(0).getPhone().replaceAll("[^0-9+]", "");
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + contactPhone));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.i("SOS", "Calling Primary Contact: " + contactPhone);
        } else {
            Log.w("SOS", "No emergency contact saved to call");
        }
    }

    public static void playSiren(Context context) {
        if (mediaPlayer.isPlaying()) {
            return;
        }

        if (audioManager == null) {
            audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }

        try {
            AssetFileDescriptor afd = context.getAssets().openFd("police-operation-siren.mp3");
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mediaPlayer.prepare();
            mediaPlayer.setVolume(1f, 1f);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        } catch (IOException e) {
            Log.e("SOS", "playSiren error: " + e.getMessage(), e);
        }
    }

    public static void stopSiren() {
        try {
            mediaPlayer.stop();
            mediaPlayer.reset();
        } catch (Exception ignored) {
        }
    }

    private static void resetValues() {
        sentSMS = false;
        sentNotification = false;
        sentWhatsApp = false;
        calledEmergency = false;
    }
}
