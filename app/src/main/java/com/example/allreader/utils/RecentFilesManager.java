package com.example.allreader.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RecentFilesManager {
    private static final String PREFS_NAME = "AllReaderPrefs";
    private static final String KEY_RECENT_FILES = "recent_files";
    private static final int MAX_RECENT_FILES = 15;

    public static class RecentFile {
        public String name;
        public String path;
        public String type;
        public long size;
        public long lastOpened;

        public RecentFile(String name, String path, String type, long size) {
            this.name = name;
            this.path = path;
            this.type = type;
            this.size = size;
            this.lastOpened = System.currentTimeMillis();
        }
    }

    public static void addRecentFile(Context context, String name, String path, String type, long size) {
        List<RecentFile> recentFiles = getRecentFiles(context);

        // Remove if already exists
        for (int i = 0; i < recentFiles.size(); i++) {
            if (recentFiles.get(i).path.equals(path)) {
                recentFiles.remove(i);
                break;
            }
        }

        // Add to beginning
        recentFiles.add(0, new RecentFile(name, path, type, size));

        // Keep only MAX_RECENT_FILES
        if (recentFiles.size() > MAX_RECENT_FILES) {
            recentFiles = recentFiles.subList(0, MAX_RECENT_FILES);
        }

        saveRecentFiles(context, recentFiles);
    }

    public static List<RecentFile> getRecentFiles(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_RECENT_FILES, "[]");

        Gson gson = new Gson();
        Type type = new TypeToken<List<RecentFile>>(){}.getType();
        List<RecentFile> files = gson.fromJson(json, type);

        return files != null ? files : new ArrayList<>();
    }

    private static void saveRecentFiles(Context context, List<RecentFile> files) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = gson.toJson(files);
        prefs.edit().putString(KEY_RECENT_FILES, json).apply();
    }

    public static void clearRecentFiles(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_RECENT_FILES).apply();
    }
}
