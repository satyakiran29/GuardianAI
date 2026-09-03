package com.android.sheguard.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.android.sheguard.R;
import com.android.sheguard.SheGuard;
import com.android.sheguard.api.NotificationAPI;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.model.ContactModel;
import com.android.sheguard.receiver.HardwareButtonReceiver;
import com.android.sheguard.ui.activity.MainActivity;
import com.android.sheguard.util.FirebaseUtil;
import com.android.sheguard.util.NotificationClient;
import com.android.sheguard.util.SosUtil;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("FieldCanBeLocal")
public class SosService extends Service implements SensorEventListener {

    private String mLocation = "";
    private long lastShakeTime = 0;
    private boolean sentSMS = false;
    private boolean sentWhatsApp = false;
    public static boolean isRunning = false;
    private boolean calledEmergency = false;
    private boolean sentNotification = false;
    private AudioManager audioManager = null;
    private final Float shakeThreshold = 10.2f;
    private SensorManager sensorManager = null;
    private LocationManager locationManager = null;
    private LocationRequest locationRequest = null;
    private static final int MIN_TIME_BETWEEN_SHAKES = 1000;
    private static NotificationAPI notificationApiService = null;
    private static final MediaPlayer mediaPlayer = new MediaPlayer();

    // Hardware Button Listeners
    private HardwareButtonReceiver hardwareButtonReceiver = null;
    private android.database.ContentObserver volumeObserver = null;
    private int volumeClickCount = 0;
    private long firstVolumeClickTime = 0;
    private long lastVolumeTriggerTime = 0;

    // 5-Second Background Telemetry & Supabase Live Stream
    private com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient = null;
    private com.google.android.gms.location.LocationCallback locationStreamCallback = null;
    private double currentLatitude = 17.3850;
    private double currentLongitude = 78.4867;
    private final android.os.Handler telemetryStreamHandler = new android.os.Handler(Looper.getMainLooper());
    private boolean isTelemetryStreamActive = false;

    private final Runnable telemetryStreamRunnable = new Runnable() {
        @Override
        public void run() {
            sendLiveTelemetryPing();
            if (isTelemetryStreamActive) {
                telemetryStreamHandler.postDelayed(this, 5000); // 5 seconds interval
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

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

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        if (sensorManager != null) {
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // Register Power Button Screen State Receiver
        try {
            hardwareButtonReceiver = new HardwareButtonReceiver();
            android.content.IntentFilter filter = new android.content.IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            registerReceiver(hardwareButtonReceiver, filter);
            Log.i("SosService", "HardwareButtonReceiver registered for Power button triple-click");
        } catch (Exception e) {
            Log.e("SosService", "Error registering HardwareButtonReceiver: " + e.getMessage());
        }

        // Register Volume Observer for background volume triggers
        try {
            volumeObserver = new android.database.ContentObserver(new android.os.Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    handleVolumeChangeTrigger();
                }
            };
            getContentResolver().registerContentObserver(
                    android.provider.Settings.System.CONTENT_URI,
                    true,
                    volumeObserver
            );
            Log.i("SosService", "Volume ContentObserver registered for Volume SOS");
        } catch (Exception e) {
            Log.e("SosService", "Error registering volume observer: " + e.getMessage());
        }
    }

    private void handleVolumeChangeTrigger() {
        String mode = Prefs.getString(Constants.SETTINGS_HARDWARE_TRIGGER_MODE, Constants.HW_MODE_BOTH);
        boolean isVolumeEnabled = Prefs.getBoolean(Constants.SETTINGS_VOLUME_BUTTON_SOS, true);
        boolean isMasterHwEnabled = Prefs.getBoolean(Constants.SETTINGS_HARDWARE_BUTTON_SOS, true);

        if (!isMasterHwEnabled || (!isVolumeEnabled && !Constants.HW_MODE_BOTH.equals(mode) && !Constants.HW_MODE_VOLUME_ONLY.equals(mode)) || Constants.HW_MODE_DISABLED.equals(mode) || Constants.HW_MODE_POWER_ONLY.equals(mode)) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastVolumeTriggerTime < 10000) {
            return;
        }

        if (volumeClickCount == 0 || (now - firstVolumeClickTime) > 2500) {
            volumeClickCount = 1;
            firstVolumeClickTime = now;
        } else {
            volumeClickCount++;
            if (volumeClickCount >= 3) {
                lastVolumeTriggerTime = now;
                volumeClickCount = 0;
                firstVolumeClickTime = 0;
                Log.w("SosService", "🚨 VOLUME BUTTON TRIPLE-PRESS DETECTED! Triggering Emergency SOS!");
                SosUtil.vibrateDevice(this);
                activateSosMode();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equalsIgnoreCase("STOP")) {
                if (isRunning) {
                    stopTelemetryStream();
                    this.stopForeground(true);
                    this.stopSelf();

                    stopSiren();
                    resetValues();
                    Log.i("SosService", "Service Stopped");
                    return START_NOT_STICKY;
                }
            } else if (intent.getAction().equalsIgnoreCase("TRIGGER_SOS")) {
                Log.w("SosService", "🚨 SOS Triggered via Notification Action Button");
                activateSosMode();
            }
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        // Action: Quick SOS Button
        Intent sosActionIntent = new Intent(this, SosService.class);
        sosActionIntent.setAction("TRIGGER_SOS");
        PendingIntent sosPendingIntent = PendingIntent.getService(this, 2, sosActionIntent, PendingIntent.FLAG_IMMUTABLE);
        Notification.Action sosAction = new Notification.Action.Builder(
                R.drawable.ic_launcher_notification,
                "🚨 Quick SOS",
                sosPendingIntent
        ).build();

        // Action: Stop Service
        Intent stopActionIntent = new Intent(this, SosService.class);
        stopActionIntent.setAction("STOP");
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 3, stopActionIntent, PendingIntent.FLAG_IMMUTABLE);
        Notification.Action stopAction = new Notification.Action.Builder(
                R.drawable.ic_remove,
                "🛑 Stop",
                stopPendingIntent
        ).build();

        NotificationChannel channel = new NotificationChannel(
                getString(R.string.notification_channel_emergency),
                "GuardianAI 24/7 Shield",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Shows active 24/7 background safety protection and live GPS telemetry updates.");
        channel.setShowBadge(false);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new Notification.Builder(this, getString(R.string.notification_channel_emergency))
                .setContentTitle("🛡️ GuardianAI is Running in Background")
                .setContentText("24/7 safety protection, shake detection & live GPS telemetry active.")
                .setSubText("Protection Active")
                .setSmallIcon(R.drawable.ic_launcher_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .addAction(sosAction)
                .addAction(stopAction)
                .build();

        this.startForeground(1, notification);
        if (notificationManager != null) {
            notificationManager.notify(1, notification);
        }

        isRunning = true;
        Log.i("SosService", "Service Started - Running in background with notification & 5s Supabase telemetry stream");
        startContinuousLocationAndTelemetryStream();

        return START_STICKY;
    }

    private void startContinuousLocationAndTelemetryStream() {
        if (isTelemetryStreamActive) return;
        isTelemetryStreamActive = true;

        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        }

        // Fast-path last known location fix
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
                    if (loc != null) {
                        currentLatitude = loc.getLatitude();
                        currentLongitude = loc.getLongitude();
                        mLocation = SosUtil.formatCoordinates(currentLatitude, currentLongitude);
                        sendLiveTelemetryPing();
                    }
                });
            } catch (SecurityException ignored) {}
        }

        // High accuracy 5-second continuous GPS stream
        LocationRequest continuousRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(3000)
                .setMaxUpdateDelayMillis(5000)
                .build();

        locationStreamCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!locationResult.getLocations().isEmpty()) {
                    android.location.Location loc = locationResult.getLastLocation();
                    if (loc != null) {
                        currentLatitude = loc.getLatitude();
                        currentLongitude = loc.getLongitude();
                        mLocation = SosUtil.formatCoordinates(currentLatitude, currentLongitude);
                        Log.d("SosService", "Fresh 5s GPS location: " + currentLatitude + ", " + currentLongitude);
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                fusedLocationClient.requestLocationUpdates(continuousRequest, locationStreamCallback, Looper.getMainLooper());
            } catch (SecurityException ignored) {}
        }

        // Launch 5s heartbeat loop to backend and Supabase
        telemetryStreamHandler.removeCallbacks(telemetryStreamRunnable);
        telemetryStreamHandler.post(telemetryStreamRunnable);
    }

    private void sendLiveTelemetryPing() {
        String userPhone = Prefs.getString(Constants.PREFS_USER_PHONE, "+919876543210");
        int batteryLevel = 85;
        try {
            android.os.BatteryManager bm = (android.os.BatteryManager) getSystemService(Context.BATTERY_SERVICE);
            if (bm != null) {
                batteryLevel = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
            }
        } catch (Exception ignored) {}

        String address = mLocation;
        if (address.isEmpty()) {
            address = SosUtil.formatCoordinates(currentLatitude, currentLongitude);
        }

        Log.d("SosService", "⚡ 5s Background Telemetry Stream -> Lat: " + currentLatitude + ", Lng: " + currentLongitude + ", Battery: " + batteryLevel + "% to Backend & Supabase");

        com.android.sheguard.api.ApiClient.pingLocation(
                userPhone,
                currentLatitude,
                currentLongitude,
                address,
                batteryLevel
        );
    }

    private void stopTelemetryStream() {
        isTelemetryStreamActive = false;
        telemetryStreamHandler.removeCallbacks(telemetryStreamRunnable);
        if (fusedLocationClient != null && locationStreamCallback != null) {
            try {
                fusedLocationClient.removeLocationUpdates(locationStreamCallback);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long curTime = System.currentTimeMillis();
            if ((curTime - lastShakeTime) > MIN_TIME_BETWEEN_SHAKES) {

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                double acceleration = Math.sqrt(Math.pow(x, 2) +
                        Math.pow(y, 2) +
                        Math.pow(z, 2)) - SensorManager.GRAVITY_EARTH;

                if (acceleration > shakeThreshold) {
                    lastShakeTime = curTime;
                    deviceShaken();
                    Log.i("SosService", "Device Shaken");
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

    private void deviceShaken() {
        if (!Prefs.getBoolean(Constants.SETTINGS_SHAKE_DETECTION, false)) {
            stopSiren();
            Log.i("SosService", "Stopped Siren");
            return;
        }

        activateSosMode();
    }

    private void activateSosMode() {
        SosUtil.vibrateDevice(this);

        if (Prefs.getBoolean(Constants.SETTINGS_STOP_BACKGROUND_APPS, true) || Prefs.getBoolean(Constants.SETTINGS_SAFE_MODE, false)) {
            SosUtil.stopBackgroundProcesses(this);
        }

        ArrayList<ContactModel> contacts = SosUtil.getStoredContacts(this);

        // Auto-call to 100/110 removed per user specification
        sendLocation(contacts);

        if (Prefs.getBoolean(Constants.SETTINGS_PLAY_SIREN, false)) {
            playSiren();
            Log.i("SosService", "Playing Siren");
        } else {
            stopSiren();
            Log.i("SosService", "Stopped Siren");
        }
    }

    private void broadcastSosPayload(ArrayList<ContactModel> contacts, double latitude, double longitude) {
        if (latitude != 0.0 || longitude != 0.0) {
            mLocation = SosUtil.formatCoordinates(latitude, longitude);
        } else if (mLocation.isEmpty()) {
            mLocation = "Live GPS coordinates acquiring...\nhttps://maps.google.com";
        }
        Log.i("SosService", "broadcastSosPayload: " + mLocation);

        if (Prefs.getBoolean(Constants.SETTINGS_SEND_SMS, true) && !sentSMS) {
            sendSMS(contacts);
            sentSMS = true;
        }

        if (Prefs.getBoolean(Constants.SETTINGS_SEND_NOTIFICATION, true) && !sentNotification) {
            sendNotification(contacts);
            sentNotification = true;
        }

        if ((Prefs.getBoolean(Constants.SETTINGS_SEND_WHATSAPP, true) || Prefs.getBoolean(Constants.SETTINGS_SAFE_MODE, false)) && !sentWhatsApp) {
            SosUtil.sendWhatsAppLocation(SosService.this, contacts, mLocation);
            sentWhatsApp = true;
        }

        // 4. Sync live telemetric SOS alert with Django & Supabase Cloud
        String userPhone = Prefs.getString(Constants.PREFS_USER_PHONE, "+919876543210");
        boolean isSiren = Prefs.getBoolean(Constants.SETTINGS_PLAY_SIREN, false);
        int batteryLevel = 85;
        try {
            android.os.BatteryManager bm = (android.os.BatteryManager) getSystemService(Context.BATTERY_SERVICE);
            if (bm != null) {
                batteryLevel = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
            }
        } catch (Exception ignored) {}

        com.android.sheguard.api.ApiClient.triggerSos(
                userPhone,
                latitude != 0.0 ? latitude : 17.3850,
                longitude != 0.0 ? longitude : 78.4867,
                mLocation,
                "shake",
                isSiren,
                batteryLevel
        );
        com.android.sheguard.api.ApiClient.pingLocation(
                userPhone,
                latitude != 0.0 ? latitude : 17.3850,
                longitude != 0.0 ? longitude : 78.4867,
                mLocation,
                batteryLevel
        );
    }

    private void sendLocation(ArrayList<ContactModel> contacts) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            broadcastSosPayload(contacts, 0, 0);
            return;
        }

        com.google.android.gms.location.FusedLocationProviderClient fusedClient =
                LocationServices.getFusedLocationProviderClient(this);

        // 1. Instant Fast-Path: Query last known location for immediate dispatch
        try {
            fusedClient.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null) {
                    Log.i("SosService", "sendLocation: Last known location fast-path triggered");
                    broadcastSosPayload(contacts, loc.getLatitude(), loc.getLongitude());
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
                        Log.i("SosService", "sendLocation: Fresh GPS fix received");
                        broadcastSosPayload(contacts, loc.getLatitude(), loc.getLongitude());
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
                Log.w("SosService", "sendLocation: Timeout reached, forcing automatic emergency dispatch");
                broadcastSosPayload(contacts, 0, 0);
            }
        }, 3500);
    }

    private boolean isGPSEnabled() {
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }

        Log.i("SosService", "Location: GPS Enabled: " + locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void sendSMS(ContactModel contact) {
        if (contact != null && contact.getPhone() != null) {
            String msg = getString(R.string.sos_message, contact.getName(), mLocation);
            com.android.sheguard.util.SmsHelper.sendSmsWithFallback(this, contact.getPhone(), msg);
            Log.i("SosService", "SMS dispatched to: " + contact.getPhone());
        }
    }

    private void sendSMS(ArrayList<ContactModel> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            contacts = SosUtil.getStoredContacts(this);
        }

        for (ContactModel contact : contacts) {
            sendSMS(contact);
        }
    }

    private void sendNotification(ArrayList<ContactModel> contacts) {
        Log.i("SosService", "Notification: Emergency SOS broadcasted to contacts");
    }

    private static void sendNotification(String userToken, String title, String message) {
        Log.i("SosService", "Notification: " + title + " - " + message);
    }

    private void callEmergency() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ArrayList<ContactModel> contacts = SosUtil.getStoredContacts(this);
        if (contacts != null && !contacts.isEmpty() && contacts.get(0).getPhone() != null) {
            String contactPhone = contacts.get(0).getPhone().replaceAll("[^0-9+]", "");
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + contactPhone));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Log.i("SosService", "Call: called primary contact: " + contactPhone);
        } else {
            Log.w("SosService", "No emergency contact saved to call");
        }
    }

    private void playSiren() {
        if (mediaPlayer.isPlaying()) {
            return;
        }

        try {
            AssetFileDescriptor afd = getAssets().openFd("police-operation-siren.mp3");
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

    private void resetValues() {
        isRunning = false;
        sentSMS = false;
        sentWhatsApp = false;
        sentNotification = false;
        calledEmergency = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTelemetryStream();

        if (hardwareButtonReceiver != null) {
            try {
                unregisterReceiver(hardwareButtonReceiver);
            } catch (Exception ignored) {}
            hardwareButtonReceiver = null;
        }

        if (volumeObserver != null) {
            try {
                getContentResolver().unregisterContentObserver(volumeObserver);
            } catch (Exception ignored) {}
            volumeObserver = null;
        }

        if (sensorManager != null) {
            try {
                sensorManager.unregisterListener(this);
            } catch (Exception ignored) {}
        }
        Log.i("SosService", "SosService destroyed and listeners unregistered");
    }
}