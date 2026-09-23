package com.android.sheguard.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Resilient Dual-Provider GPS & Location Tracking Engine for GuardianAI.
 * Combines Google Play Services FusedLocationProviderClient with native Android LocationManager,
 * ensuring high-accuracy fixes, freshness validation, and real-time continuous corridor tracking.
 */
public class LocationHelper {

    private static final String TAG = "LocationHelper";

    public interface LocationResultListener {
        void onLocationReceived(double latitude, double longitude, String addressName);
        void onLocationError(String error);
    }

    public interface LocationTrackingListener {
        void onLocationUpdated(Location location, double latitude, double longitude, float accuracy, float speed, float bearing);
        void onLocationError(String error);
    }

    private static final Map<LocationTrackingListener, LocationCallback> activeFusedCallbacks = new ConcurrentHashMap<>();
    private static final Map<LocationTrackingListener, LocationListener> activeNativeListeners = new ConcurrentHashMap<>();

    public static boolean hasLocationPermission(Context context) {
        if (context == null) return false;
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Verifies if any system location provider is enabled (GPS, Cellular, Wi-Fi).
     */
    public static boolean isLocationEnabled(Context context) {
        if (context == null) return false;
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return lm.isLocationEnabled();
        } else {
            return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }
    }

    /**
     * Prompts the user to turn on GPS / Location using standard system resolution dialog.
     */
    public static void promptEnableLocation(Activity activity) {
        if (activity == null) return;
        try {
            LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).build();
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(request)
                    .setAlwaysShow(true);

            LocationServices.getSettingsClient(activity)
                    .checkLocationSettings(builder.build())
                    .addOnCompleteListener(task -> {
                        try {
                            task.getResult(ApiException.class);
                        } catch (ApiException e) {
                            if (e.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                                try {
                                    ResolvableApiException resolvable = (ResolvableApiException) e;
                                    resolvable.startResolutionForResult(activity, 1001);
                                } catch (IntentSender.SendIntentException ex) {
                                    Log.w(TAG, "Error resolving location settings: " + ex.getMessage());
                                }
                            } else {
                                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                activity.startActivity(intent);
                            }
                        }
                    });
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                activity.startActivity(intent);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Requests a fresh single location fix with freshness checking and dual-provider fallback.
     */
    @SuppressLint("MissingPermission")
    public static void requestSingleLocationUpdate(Context context, LocationResultListener listener) {
        if (context == null || listener == null) return;

        if (!hasLocationPermission(context)) {
            listener.onLocationError("Location permission not granted");
            return;
        }

        Handler mainHandler = new Handler(Looper.getMainLooper());
        AtomicBoolean isResolved = new AtomicBoolean(false);

        FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(context);
        LocationManager nativeLm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // 1. Check last known location for freshness (< 45s old and accuracy < 60m)
        try {
            fusedClient.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null && !isResolved.get()) {
                    long age = System.currentTimeMillis() - loc.getTime();
                    if (age < 45000 && loc.getAccuracy() <= 60.0f) {
                        if (isResolved.compareAndSet(false, true)) {
                            Log.d(TAG, "Fast fresh last-known fix: " + loc.getLatitude() + ", " + loc.getLongitude() + " (age " + (age / 1000) + "s)");
                            resolveAddressAndNotify(context, loc.getLatitude(), loc.getLongitude(), listener, mainHandler);
                        }
                    }
                }
            });
        } catch (Exception ignored) {}

        // 2. Request fresh high-accuracy satellite update via FusedClient
        LocationRequest freshRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1500)
                .setWaitForAccurateLocation(false)
                .setMaxUpdates(1)
                .build();

        LocationCallback fusedCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                try {
                    fusedClient.removeLocationUpdates(this);
                } catch (Exception ignored) {}

                if (!locationResult.getLocations().isEmpty()) {
                    Location loc = locationResult.getLastLocation();
                    if (loc != null && isResolved.compareAndSet(false, true)) {
                        Log.d(TAG, "Fresh Fused fix: " + loc.getLatitude() + ", " + loc.getLongitude() + " (acc " + loc.getAccuracy() + "m)");
                        resolveAddressAndNotify(context, loc.getLatitude(), loc.getLongitude(), listener, mainHandler);
                    }
                }
            }
        };

        try {
            fusedClient.requestLocationUpdates(freshRequest, fusedCallback, Looper.getMainLooper());
        } catch (Exception e) {
            Log.w(TAG, "Fused location request failed: " + e.getMessage());
        }

        // 3. Concurrently register native LocationManager listener as hardware fallback
        LocationListener nativeListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location loc) {
                if (nativeLm != null) {
                    try {
                        nativeLm.removeUpdates(this);
                    } catch (Exception ignored) {}
                }
                if (isResolved.compareAndSet(false, true)) {
                    try {
                        fusedClient.removeLocationUpdates(fusedCallback);
                    } catch (Exception ignored) {}
                    Log.d(TAG, "Fresh Native LM fix: " + loc.getLatitude() + ", " + loc.getLongitude());
                    resolveAddressAndNotify(context, loc.getLatitude(), loc.getLongitude(), listener, mainHandler);
                }
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(@NonNull String provider) {}
            @Override public void onProviderDisabled(@NonNull String provider) {}
        };

        if (nativeLm != null) {
            try {
                if (nativeLm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    nativeLm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1.0f, nativeListener, Looper.getMainLooper());
                }
                if (nativeLm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    nativeLm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1.0f, nativeListener, Looper.getMainLooper());
                }
            } catch (Exception ignored) {}
        }

        // 4. 8-Second Safety Watchdog: If GPS fix takes longer, fall back to best known or notify
        mainHandler.postDelayed(() -> {
            if (!isResolved.get()) {
                try {
                    fusedClient.removeLocationUpdates(fusedCallback);
                } catch (Exception ignored) {}
                if (nativeLm != null) {
                    try {
                        nativeLm.removeUpdates(nativeListener);
                    } catch (Exception ignored) {}
                }

                Location bestKnown = getBestLastKnownLocation(context);
                if (bestKnown != null && isResolved.compareAndSet(false, true)) {
                    Log.w(TAG, "Watchdog timeout: utilizing best available location: " + bestKnown.getLatitude() + ", " + bestKnown.getLongitude());
                    resolveAddressAndNotify(context, bestKnown.getLatitude(), bestKnown.getLongitude(), listener, mainHandler);
                } else if (isResolved.compareAndSet(false, true)) {
                    if (!isLocationEnabled(context)) {
                        listener.onLocationError("GPS/Location is turned off on your device. Please enable Location in Settings.");
                    } else {
                        listener.onLocationError("Acquiring GPS fix... Satellite lock taking longer than usual.");
                    }
                }
            }
        }, 8000);
    }

    /**
     * Starts continuous real-time GPS tracking for active screens (Safe Route navigation, corridor monitoring).
     */
    @SuppressLint("MissingPermission")
    public static void startLocationTracking(Context context, LocationTrackingListener listener) {
        if (context == null || listener == null) return;
        if (!hasLocationPermission(context)) {
            listener.onLocationError("Location permission not granted");
            return;
        }

        stopLocationTracking(context, listener);

        FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(context);
        LocationManager nativeLm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // Immediate initial fix if available
        Location initialLoc = getBestLastKnownLocation(context);
        if (initialLoc != null) {
            listener.onLocationUpdated(
                    initialLoc,
                    initialLoc.getLatitude(),
                    initialLoc.getLongitude(),
                    initialLoc.getAccuracy(),
                    initialLoc.getSpeed(),
                    initialLoc.getBearing()
            );
        }

        LocationRequest continuousRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2500)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(1500)
                .setMinUpdateDistanceMeters(1.0f)
                .build();

        LocationCallback fusedCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!locationResult.getLocations().isEmpty()) {
                    Location loc = locationResult.getLastLocation();
                    if (loc != null) {
                        listener.onLocationUpdated(
                                loc,
                                loc.getLatitude(),
                                loc.getLongitude(),
                                loc.getAccuracy(),
                                loc.getSpeed(),
                                loc.getBearing()
                        );
                    }
                }
            }
        };

        try {
            fusedClient.requestLocationUpdates(continuousRequest, fusedCallback, Looper.getMainLooper());
            activeFusedCallbacks.put(listener, fusedCallback);
        } catch (Exception e) {
            Log.w(TAG, "Error starting fused tracking: " + e.getMessage());
        }

        // Native LocationManager backup stream
        LocationListener nativeListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location loc) {
                listener.onLocationUpdated(
                        loc,
                        loc.getLatitude(),
                        loc.getLongitude(),
                        loc.getAccuracy(),
                        loc.getSpeed(),
                        loc.getBearing()
                );
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(@NonNull String provider) {}
            @Override public void onProviderDisabled(@NonNull String provider) {}
        };

        if (nativeLm != null) {
            try {
                if (nativeLm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    nativeLm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2500, 1.0f, nativeListener, Looper.getMainLooper());
                    activeNativeListeners.put(listener, nativeListener);
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * Stops continuous real-time tracking for the given listener.
     */
    public static void stopLocationTracking(Context context, LocationTrackingListener listener) {
        if (listener == null) return;

        LocationCallback cb = activeFusedCallbacks.remove(listener);
        if (cb != null && context != null) {
            try {
                FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(context);
                client.removeLocationUpdates(cb);
            } catch (Exception ignored) {}
        }

        LocationListener nl = activeNativeListeners.remove(listener);
        if (nl != null && context != null) {
            try {
                LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                if (lm != null) lm.removeUpdates(nl);
            } catch (Exception ignored) {}
        }
    }

    @SuppressLint("MissingPermission")
    public static Location getBestLastKnownLocation(Context context) {
        if (!hasLocationPermission(context)) return null;

        Location best = null;
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm != null) {
            try {
                Location gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Location netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                Location passLoc = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

                if (gpsLoc != null) best = gpsLoc;
                if (netLoc != null && (best == null || netLoc.getTime() > best.getTime())) best = netLoc;
                if (passLoc != null && (best == null || passLoc.getTime() > best.getTime())) best = passLoc;
            } catch (Exception ignored) {}
        }
        return best;
    }

    private static void resolveAddressAndNotify(Context context, double lat, double lng, LocationResultListener listener, Handler mainHandler) {
        Executors.newSingleThreadExecutor().execute(() -> {
            String addressText = "Lat: " + String.format(Locale.US, "%.5f", lat) + ", Lng: " + String.format(Locale.US, "%.5f", lng);
            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    if (addr.getFeatureName() != null) sb.append(addr.getFeatureName()).append(", ");
                    if (addr.getLocality() != null) sb.append(addr.getLocality()).append(", ");
                    if (addr.getAdminArea() != null) sb.append(addr.getAdminArea());
                    if (sb.length() > 0) {
                        addressText = sb.toString();
                    }
                }
            } catch (IOException e) {
                Log.w(TAG, "Geocoder notice: " + e.getMessage());
            }

            final String finalAddress = addressText;
            mainHandler.post(() -> listener.onLocationReceived(lat, lng, finalAddress));
        });
    }

    public static void openLocationInGoogleMaps(Context context, double lat, double lng, String label) {
        try {
            Uri gmmIntentUri = Uri.parse("geo:" + lat + "," + lng + "?q=" + lat + "," + lng + "(" + Uri.encode(label) + ")");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
            } else {
                Intent webMapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/maps?q=loc:" + lat + "," + lng));
                context.startActivity(webMapIntent);
            }
        } catch (Exception e) {
            Intent webMapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/maps?q=loc:" + lat + "," + lng));
            context.startActivity(webMapIntent);
        }
    }

    public static void openNavigationDirections(Context context, String destination) {
        try {
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(destination));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
            } else {
                Intent webMapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(destination)));
                context.startActivity(webMapIntent);
            }
        } catch (Exception e) {
            Intent webMapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(destination)));
            context.startActivity(webMapIntent);
        }
    }
}
