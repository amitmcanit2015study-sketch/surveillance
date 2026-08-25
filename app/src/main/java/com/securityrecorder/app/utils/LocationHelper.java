package com.securityrecorder.app.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.os.Build;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * GPS location helper for geotagging, reverse geocoding, and stamping visual GPS metadata overlays on captured photos.
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
        if (location == null) return "Not available";
        return String.format(Locale.US, "%.5f, %.5f", location.getLatitude(), location.getLongitude());
    }

    public static String getFullLocationString(Context context, Location location) {
        if (location == null) return "Not available";
        String coords = formatCoordinates(location);
        String address = getAddressString(context, location);
        if (!address.isEmpty() && !address.equals(coords)) {
            return address + " (" + coords + ")";
        }
        return coords;
    }

    public static String getAddressString(Context context, Location location) {
        if (location == null) return "";
        try {
            if (Geocoder.isPresent()) {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    if (addr.getLocality() != null) {
                        sb.append(addr.getLocality());
                    } else if (addr.getSubAdminArea() != null) {
                        sb.append(addr.getSubAdminArea());
                    }
                    if (addr.getAdminArea() != null) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(addr.getAdminArea());
                    }
                    if (addr.getCountryName() != null) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(addr.getCountryName());
                    }
                    if (sb.length() > 0) return sb.toString();
                }
            }
        } catch (Exception ignored) {}
        return formatCoordinates(location);
    }

    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Stamping Geotag info overlay directly on the bottom-right corner of the image bitmap
     * and saving EXIF metadata into the file.
     */
    public static String stampGeoTagOnImage(Context context, File imageFile, Location location) {
        if (imageFile == null || !imageFile.exists()) return "Not available";

        String dateTimeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        String deviceName = Build.MANUFACTURER.toUpperCase(Locale.US) + " " + Build.MODEL;
        String coordsStr = location != null ? formatCoordinates(location) : "GPS: Unavailable";
        String placeStr = location != null ? getAddressString(context, location) : "Location: Standard";

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap original = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
            if (original == null) return coordsStr;

            int width = original.getWidth();
            int height = original.getHeight();

            Bitmap mutableBitmap = original.copy(Bitmap.Config.ARGB_8888, true);
            original.recycle();

            Canvas canvas = new Canvas(mutableBitmap);

            // Calculate scale-independent font size based on image width
            float textSize = Math.max(22f, width * 0.022f);
            float lineSpacing = textSize * 0.45f;
            float padding = textSize * 0.9f;

            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(textSize);
            textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            textPaint.setShadowLayer(4f, 2f, 2f, Color.BLACK);

            List<String> lines = new ArrayList<>();
            lines.add("📍 " + placeStr);
            lines.add("🌐 GPS: " + coordsStr);
            lines.add("🕒 " + dateTimeStr);
            lines.add("📱 " + deviceName);

            float maxLineWidth = 0;
            for (String line : lines) {
                float w = textPaint.measureText(line);
                if (w > maxLineWidth) maxLineWidth = w;
            }

            float blockWidth = maxLineWidth + (padding * 2);
            float blockHeight = (lines.size() * textSize) + ((lines.size() - 1) * lineSpacing) + (padding * 2);

            float right = width - (padding * 0.8f);
            float bottom = height - (padding * 0.8f);
            float left = right - blockWidth;
            float top = bottom - blockHeight;

            // Draw dark rounded translucent badge in the bottom right corner
            Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setColor(Color.argb(175, 10, 15, 25));
            RectF bgRect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(bgRect, textSize * 0.6f, textSize * 0.6f, bgPaint);

            // Draw subtle cyan border for security look
            Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(Math.max(2f, textSize * 0.08f));
            strokePaint.setColor(Color.argb(200, 56, 189, 248)); // #38BDF8
            canvas.drawRoundRect(bgRect, textSize * 0.6f, textSize * 0.6f, strokePaint);

            // Draw text lines
            float currentY = top + padding + textSize * 0.85f;
            for (String line : lines) {
                canvas.drawText(line, left + padding, currentY, textPaint);
                currentY += textSize + lineSpacing;
            }

            // Save back to file
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
            }
            mutableBitmap.recycle();

            // Write EXIF tags into image
            writeExifMetadata(imageFile, location, dateTimeStr, deviceName);

        } catch (Exception ignored) {}

        return location != null ? getFullLocationString(context, location) : "Not available";
    }

    private static void writeExifMetadata(File file, Location location, String dateTimeStr, String deviceName) {
        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            exif.setAttribute(ExifInterface.TAG_DATETIME, dateTimeStr);
            exif.setAttribute(ExifInterface.TAG_MAKE, Build.MANUFACTURER);
            exif.setAttribute(ExifInterface.TAG_MODEL, Build.MODEL);
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "Surveillance Geotagged Security Capture");

            if (location != null) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();

                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, convertToDegreeMinuteSeconds(lat));
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, lat >= 0 ? "N" : "S");
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convertToDegreeMinuteSeconds(lon));
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, lon >= 0 ? "E" : "W");
                if (location.hasAltitude()) {
                    exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, String.valueOf(Math.abs(location.getAltitude())));
                    exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, location.getAltitude() >= 0 ? "0" : "1");
                }
            }
            exif.saveAttributes();
        } catch (Exception ignored) {}
    }

    private static String convertToDegreeMinuteSeconds(double coordinate) {
        coordinate = Math.abs(coordinate);
        int degrees = (int) coordinate;
        coordinate = (coordinate - degrees) * 60;
        int minutes = (int) coordinate;
        coordinate = (coordinate - minutes) * 60;
        int seconds = (int) (coordinate * 1000);
        return degrees + "/1," + minutes + "/1," + seconds + "/1000";
    }
}
