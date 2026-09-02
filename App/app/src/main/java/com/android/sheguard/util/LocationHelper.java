package com.android.sheguard.util;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class LocationHelper {

    private static final String TAG = "LocationHelper";

    public interface LocationResultListener {
        void onLocationReceived(double latitude, double longitude, String addressName);
        void onLocationError(String error);
    }

    public static boolean hasLocationPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestSingleLocationUpdate(Context context, LocationResultListener listener) {
        if (!hasLocationPermission(context)) {
            listener.onLocationError("Location permission not granted");
            return;
        }

        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(context);
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setWaitForAccurateLocation(false)
                .setMaxUpdates(1)
                .build();

        Handler mainHandler = new Handler(Looper.getMainLooper());

        try {
            client.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    resolveAddressAndNotify(context, location.getLatitude(), location.getLongitude(), listener, mainHandler);
                } else {
                    try {
                        client.requestLocationUpdates(request, new LocationCallback() {
                            @Override
                            public void onLocationResult(LocationResult locationResult) {
                                client.removeLocationUpdates(this);
                                if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                                    Location loc = locationResult.getLastLocation();
                                    if (loc != null) {
                                        resolveAddressAndNotify(context, loc.getLatitude(), loc.getLongitude(), listener, mainHandler);
                                        return;
                                    }
                                }
                                mainHandler.post(() -> listener.onLocationError("Unable to acquire GPS fix"));
                            }
                        }, Looper.getMainLooper());
                    } catch (SecurityException e) {
                        mainHandler.post(() -> listener.onLocationError(e.getMessage()));
                    }
                }
            }).addOnFailureListener(e -> mainHandler.post(() -> listener.onLocationError(e.getMessage())));
        } catch (SecurityException e) {
            listener.onLocationError(e.getMessage());
        }
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
                Log.e(TAG, "Geocoder error: " + e.getMessage());
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
