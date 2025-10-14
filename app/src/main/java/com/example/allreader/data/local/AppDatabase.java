package com.example.allreader.data.local;


import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.allreader.data.local.dao.FileDao;
import com.example.allreader.data.model.RecentFile;

@Database(entities = {RecentFile.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract FileDao fileDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "allreader_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}

