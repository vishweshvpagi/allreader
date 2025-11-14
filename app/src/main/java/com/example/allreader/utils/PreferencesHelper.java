package com.example.allreader.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesHelper {
    private static final String PREFS_NAME = "AllReaderPrefs";
    private static final String KEY_NIGHT_MODE = "night_mode";
    private static final String KEY_TEXT_SIZE = "text_size";
    private static final String KEY_FULLSCREEN = "fullscreen_mode";

    public static boolean isNightMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_NIGHT_MODE, false);
    }

    public static void setNightMode(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_NIGHT_MODE, enabled).apply();
    }

    public static int getTextSize(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_TEXT_SIZE, 16);
    }

    public static void setTextSize(Context context, int size) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_TEXT_SIZE, size).apply();
    }

    public static boolean isFullscreenMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_FULLSCREEN, false);
    }

    public static void setFullscreenMode(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_FULLSCREEN, enabled).apply();
    }
}
