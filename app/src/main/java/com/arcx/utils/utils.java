package com.arcx.utils;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public class utils {

    private final Context context;
    private final SharedPreferences prefs;

    public utils(Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences(
                "arcx_settings",
                Context.MODE_PRIVATE
        );
    }

    public String getSt(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }

    public void setSt(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    public boolean getBool(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }

    public void setBool(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    public int getInt(String key, int defaultValue) {
        return prefs.getInt(key, defaultValue);
    }

    public void setInt(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }

    public void setSt(String file, String key, String value) {
        context.getSharedPreferences(file, Context.MODE_PRIVATE)
                .edit()
                .putString(key, value)
                .apply();
    }

    public String getSt(String file, String key, String defaultValue) {
        return context.getSharedPreferences(file, Context.MODE_PRIVATE)
                .getString(key, defaultValue);
    }

    public void setBool(String file, String key, boolean value) {
        context.getSharedPreferences(file, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(key, value)
                .apply();
    }

    public void setInt(String file, String key, int value) {
        context.getSharedPreferences(file, Context.MODE_PRIVATE)
                .edit()
                .putInt(key, value)
                .apply();
    }

    public int getInt(String file, String key, int defaultValue) {
        return context.getSharedPreferences(file, Context.MODE_PRIVATE)
                .getInt(key, defaultValue);
    }

    public void setLocale(Activity activity, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = activity.getResources();
        Configuration configuration = resources.getConfiguration();

        configuration.setLocale(locale);
        resources.updateConfiguration(
                configuration,
                resources.getDisplayMetrics()
        );
    }

    public void setLocale(Service service, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = service.getResources();
        Configuration configuration = resources.getConfiguration();

        configuration.setLocale(locale);
        resources.updateConfiguration(
                configuration,
                resources.getDisplayMetrics()
        );
    }
}