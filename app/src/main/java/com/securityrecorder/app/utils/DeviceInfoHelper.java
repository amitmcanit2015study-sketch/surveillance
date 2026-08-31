package com.securityrecorder.app.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import androidx.core.content.ContextCompat;
import java.util.List;
import java.util.Locale;

/**
 * Helper to retrieve comprehensive device hardware, OS version, device owner name, and SIM/carrier information.
 */
public class DeviceInfoHelper {

    public static String getDeviceModel() {
        String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER.toUpperCase(Locale.US) : "ANDROID";
        String model = Build.MODEL != null ? Build.MODEL : "DEVICE";
        if (model.toUpperCase(Locale.US).startsWith(manufacturer)) {
            return model;
        }
        return manufacturer + " " + model;
    }

    public static String getOsVersion() {
        return "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";
    }

    public static String getDeviceUserName(Context context) {
        if (context == null) return "Owner";
        String name = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                name = Settings.Global.getString(context.getContentResolver(), "device_name");
            }
            if (name == null || name.trim().isEmpty()) {
                name = Settings.Secure.getString(context.getContentResolver(), "bluetooth_name");
            }
            if (name == null || name.trim().isEmpty()) {
                name = Settings.System.getString(context.getContentResolver(), "device_name");
            }
        } catch (Throwable ignored) {}

        if (name == null || name.trim().isEmpty()) {
            try {
                name = Build.USER != null && !Build.USER.equalsIgnoreCase("root") ? Build.USER : "Device Owner";
            } catch (Throwable ignored) {
                name = "Device Owner";
            }
        }
        return name;
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
    public static String getSimInfo(Context context) {
        if (context == null) return "Cellular / Standby";
        StringBuilder sb = new StringBuilder();
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                String operatorName = null;
                try {
                    operatorName = tm.getNetworkOperatorName();
                    if (operatorName == null || operatorName.isEmpty()) {
                        operatorName = tm.getSimOperatorName();
                    }
                } catch (Throwable ignored) {}

                String country = null;
                try {
                    country = tm.getNetworkCountryIso();
                    if (country != null && !country.isEmpty()) {
                        country = country.toUpperCase(Locale.US);
                    }
                } catch (Throwable ignored) {}

                String line1Number = null;
                try {
                    boolean hasPhonePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                            || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED);

                    if (hasPhonePerm) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            SubscriptionManager sm = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                            if (sm != null) {
                                List<SubscriptionInfo> subList = sm.getActiveSubscriptionInfoList();
                                if (subList != null && !subList.isEmpty()) {
                                    for (SubscriptionInfo info : subList) {
                                        CharSequence carrier = info.getCarrierName();
                                        String num = info.getNumber();
                                        if (carrier != null && carrier.length() > 0) {
                                            operatorName = carrier.toString();
                                        }
                                        if (num != null && !num.trim().isEmpty()) {
                                            line1Number = num;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        if (line1Number == null || line1Number.isEmpty()) {
                            line1Number = tm.getLine1Number();
                        }
                    }
                } catch (Throwable ignored) {}

                if (operatorName != null && !operatorName.isEmpty()) {
                    sb.append(operatorName);
                } else {
                    sb.append("SIM Card Active");
                }

                if (country != null && !country.isEmpty()) {
                    sb.append(" (").append(country).append(")");
                }

                if (line1Number != null && !line1Number.trim().isEmpty()) {
                    sb.append(" · ").append(line1Number);
                }
            }
        } catch (Throwable ignored) {}

        return sb.length() > 0 ? sb.toString() : "SIM Card Active";
    }
}
