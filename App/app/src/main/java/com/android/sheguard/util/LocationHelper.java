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
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
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

        // 1. Fast-path: Check fusedClient.getLastLocation() for fresh fix (< 90s)
        try {
            fusedClient.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null && !isResolved.get()) {
                    long age = System.currentTimeMillis() - loc.getTime();
                    if (age < 90000 && loc.getAccuracy() <= 100.0f) {
                        if (isResolved.compareAndSet(false, true)) {
                            Log.d(TAG, "Fast fresh last-known fix: " + loc.getLatitude() + ", " + loc.getLongitude());
                            resolveAddressAndNotify(context, loc.getLatitude(), loc.getLongitude(), listener, mainHandler);
                        }
                    }
                }
            });
        } catch (Exception ignored) {}

        // 2. Request fresh high-accuracy satellite update via FusedClient (0m threshold)
        LocationRequest freshRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(500)
                .setMinUpdateDistanceMeters(0f)
                .build();

        LocationCallback fusedCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!locationResult.getLocations().isEmpty()) {
                    Location loc = locationResult.getLastLocation();
                    if (loc != null && isResolved.compareAndSet(false, true)) {
                        try {
                            fusedClient.removeLocationUpdates(this);
                        } catch (Exception ignored) {}
                        Log.d(TAG, "Fresh Fused fix: " + loc.getLatitude() + ", " + loc.getLongitude());
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

        // 3. Concurrently register native LocationManager listener (GPS + NETWORK) as hardware fallback (0m threshold)
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
        };

        if (nativeLm != null) {
            try {
                if (nativeLm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    nativeLm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0f, nativeListener, Looper.getMainLooper());
                }
                if (nativeLm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    nativeLm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0f, nativeListener, Looper.getMainLooper());
                }
            } catch (Exception ignored) {}
        }

        // 4. 6-Second Safety Watchdog: If GPS fix takes longer, fall back to best known
        mainHandler.postDelayed(() -> {
            if (!isResolved.get()) {
                Location bestKnown = getBestLastKnownLocation(context);
                if (bestKnown != null && isResolved.compareAndSet(false, true)) {
                    try {
                        fusedClient.removeLocationUpdates(fusedCallback);
                        if (nativeLm != null) nativeLm.removeUpdates(nativeListener);
                    } catch (Exception ignored) {}
                    Log.w(TAG, "Watchdog timeout: utilizing best available location: " + bestKnown.getLatitude() + ", " + bestKnown.getLongitude());
                    resolveAddressAndNotify(context, bestKnown.getLatitude(), bestKnown.getLongitude(), listener, mainHandler);
                } else if (isResolved.compareAndSet(false, true)) {
                    try {
                        fusedClient.removeLocationUpdates(fusedCallback);
                        if (nativeLm != null) nativeLm.removeUpdates(nativeListener);
                    } catch (Exception ignored) {}
                    if (!isLocationEnabled(context)) {
                        listener.onLocationError("GPS/Location is turned off on your device. Please enable Location in Settings.");
                    } else {
                        listener.onLocationError("Acquiring GPS fix... Satellite lock taking longer than usual.");
                    }
                }
            }
        }, 6000);
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

        // Immediate initial fix if available (from cache or prefs)
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

        // Query fused client last location fast-path
        try {
            fusedClient.getLastLocation().addOnSuccessListener(loc -> {
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
            });
        } catch (Exception ignored) {}

        // High accuracy real-time request (0m distance threshold allows stationary updates)
        LocationRequest continuousRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(1000)
                .setMinUpdateDistanceMeters(0f)
                .build();

        LocationCallback fusedCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!locationResult.getLocations().isEmpty()) {
                    Location loc = locationResult.getLastLocation();
                    if (loc != null) {
                        try {
                            Prefs.putFloat(Constants.PREFS_LAST_LATITUDE, (float) loc.getLatitude());
                            Prefs.putFloat(Constants.PREFS_LAST_LONGITUDE, (float) loc.getLongitude());
                        } catch (Exception ignored) {}
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

        // Native LocationManager dual-provider backup stream (GPS + NETWORK)
        LocationListener nativeListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location loc) {
                try {
                    Prefs.putFloat(Constants.PREFS_LAST_LATITUDE, (float) loc.getLatitude());
                    Prefs.putFloat(Constants.PREFS_LAST_LONGITUDE, (float) loc.getLongitude());
                } catch (Exception ignored) {}
                listener.onLocationUpdated(
                        loc,
                        loc.getLatitude(),
                        loc.getLongitude(),
                        loc.getAccuracy(),
                        loc.getSpeed(),
                        loc.getBearing()
                );
            }
        };

        if (nativeLm != null) {
            try {
                if (nativeLm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    nativeLm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0f, nativeListener, Looper.getMainLooper());
                }
                if (nativeLm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    nativeLm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 0f, nativeListener, Looper.getMainLooper());
                }
                activeNativeListeners.put(listener, nativeListener);
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

        // Fallback to locally persisted last known location
        if (best == null && context != null) {
            try {
                float savedLat = Prefs.getFloat(Constants.PREFS_LAST_LATITUDE, 0f);
                float savedLng = Prefs.getFloat(Constants.PREFS_LAST_LONGITUDE, 0f);
                if (savedLat != 0f && savedLng != 0f) {
                    Location loc = new Location("cached_prefs");
                    loc.setLatitude(savedLat);
                    loc.setLongitude(savedLng);
                    loc.setTime(System.currentTimeMillis());
                    loc.setAccuracy(50.0f);
                    best = loc;
                }
            } catch (Exception ignored) {}
        }
        return best;
    }

    private static void resolveAddressAndNotify(Context context, double lat, double lng, LocationResultListener listener, Handler mainHandler) {
        // Save to prefs immediately
        try {
            Prefs.putFloat(Constants.PREFS_LAST_LATITUDE, (float) lat);
            Prefs.putFloat(Constants.PREFS_LAST_LONGITUDE, (float) lng);
        } catch (Exception ignored) {}

        // Immediate callback with coordinates so UI updates instantly without network latency
        String fallbackAddress = String.format(Locale.US, "GPS: %.4f, %.4f", lat, lng);
        mainHandler.post(() -> listener.onLocationReceived(lat, lng, fallbackAddress));

        // Resolve reverse geocode asynchronously in background
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    if (addr.getFeatureName() != null && !addr.getFeatureName().matches("\\d+")) sb.append(addr.getFeatureName()).append(", ");
                    if (addr.getThoroughfare() != null) sb.append(addr.getThoroughfare()).append(", ");
                    if (addr.getLocality() != null) sb.append(addr.getLocality()).append(", ");
                    if (addr.getAdminArea() != null) sb.append(addr.getAdminArea());
                    String resolved = sb.length() > 0 ? sb.toString() : addr.getAddressLine(0);
                    if (resolved != null && !resolved.isEmpty()) {
                        mainHandler.post(() -> listener.onLocationReceived(lat, lng, resolved));
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Geocoder notice: " + e.getMessage());
            }
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
