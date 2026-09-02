package com.securityrecorder.app.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

public class LocaleHelper {

    private static final String PREFS_NAME = "app_language_prefs";
    private static final String KEY_LANGUAGE = "selected_language";

    public static boolean isHindi(Context context) {
        return getLanguage(context).startsWith("hi");
    }

    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, "en");
    }

    public static Context wrapContext(Context context) {
        String lang = getLanguage(context);
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            config.setLocales(localeList);
            return context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
            return context;
        }
    }

    public static void applyAppLanguage(Context context) {
        // wrapContext(Context) in attachBaseContext properly handles per-app locale
        // on Android N+ (API 24+) without triggering infinite activity recreation loops.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            String lang = getLanguage(context);
            Locale locale = new Locale(lang);
            Locale.setDefault(locale);
            Resources res = context.getResources();
            Configuration config = new Configuration(res.getConfiguration());
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
        }
    }

    public static void setLocale(Activity activity, String languageTag) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, languageTag).apply();

        Locale locale = new Locale(languageTag);
        Locale.setDefault(locale);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Resources res = activity.getResources();
            Configuration config = new Configuration(res.getConfiguration());
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
            activity.getApplicationContext().getResources().updateConfiguration(config, activity.getApplicationContext().getResources().getDisplayMetrics());
        }

        try {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag));
        } catch (Exception ignored) {
        }

        activity.recreate();
    }
}
