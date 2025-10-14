package com.example.allreader;

import android.app.Application;
import com.example.allreader.data.local.AppDatabase;

public class MainApplication extends Application {
    private static MainApplication instance;
    private AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        database = AppDatabase.getInstance(this);
    }

    public static MainApplication getInstance() {
        return instance;
    }

    public AppDatabase getDatabase() {
        return database;
    }
}
