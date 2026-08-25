package com.securityrecorder.app.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import androidx.core.content.ContextCompat;
import java.util.Locale;

/**
 * GPS location helper for geotagging video metadata.
 */
public class LocationHelper {

    @SuppressLint("MissingPermission")
    public static Location getLastKnownLocation(Context context) {
        if (!hasLocationPermission(context)) {
            return null;
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) return null;

        Location bestLocation = null;
        try {
            Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Location passiveLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

            if (gpsLocation != null) {
                bestLocation = gpsLocation;
            } else if (networkLocation != null) {
                bestLocation = networkLocation;
            } else {
                bestLocation = passiveLocation;
            }
        } catch (Exception ignored) {}

        return bestLocation;
    }

    public static String formatCoordinates(Location location) {
        if (location == null) return "Unknown";
        return String.format(Locale.US, "%.5f, %.5f", location.getLatitude(), location.getLongitude());
    }

    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}
