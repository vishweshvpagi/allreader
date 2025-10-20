package com.example.allreader.data.repository;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.allreader.data.local.AppDatabase;
import com.example.allreader.data.local.dao.FileDao;
import com.example.allreader.data.model.RecentFile;

import java.util.List;

public class FileRepository {
    private static final String TAG = "FileRepository";
    private FileDao fileDao;
    private LiveData<List<RecentFile>> allRecentFiles;

    public FileRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        fileDao = database.fileDao();
        allRecentFiles = fileDao.getAllRecentFiles();
    }

    public void insert(RecentFile file) {
        Log.d(TAG, "Inserting file: " + file.getFileName() + ", type: " + file.getFileType());
        new InsertAsyncTask(fileDao).execute(file);
    }

    public void update(RecentFile file) {
        new UpdateAsyncTask(fileDao).execute(file);
    }

    public void delete(RecentFile file) {
        new DeleteAsyncTask(fileDao).execute(file);
    }

    public void deleteAll() {
        new DeleteAllAsyncTask(fileDao).execute();
    }

    public LiveData<List<RecentFile>> getAllRecentFiles() {
        return allRecentFiles;
    }

    public LiveData<List<RecentFile>> getFilesByType(String type) {
        return fileDao.getFilesByType(type);
    }

    // AsyncTask for Insert
    private static class InsertAsyncTask extends AsyncTask<RecentFile, Void, Void> {
        private FileDao fileDao;

        private InsertAsyncTask(FileDao fileDao) {
            this.fileDao = fileDao;
        }

        @Override
        protected Void doInBackground(RecentFile... recentFiles) {
            fileDao.insert(recentFiles[0]);
            Log.d(TAG, "File inserted to database");
            return null;
        }
    }

    // AsyncTask for Update
    private static class UpdateAsyncTask extends AsyncTask<RecentFile, Void, Void> {
        private FileDao fileDao;

        private UpdateAsyncTask(FileDao fileDao) {
            this.fileDao = fileDao;
        }

        @Override
        protected Void doInBackground(RecentFile... recentFiles) {
            fileDao.update(recentFiles[0]);
            return null;
        }
    }

    // AsyncTask for Delete
    private static class DeleteAsyncTask extends AsyncTask<RecentFile, Void, Void> {
        private FileDao fileDao;

        private DeleteAsyncTask(FileDao fileDao) {
            this.fileDao = fileDao;
        }

        @Override
        protected Void doInBackground(RecentFile... recentFiles) {
            fileDao.delete(recentFiles[0]);
            return null;
        }
    }

    // AsyncTask for Delete All
    private static class DeleteAllAsyncTask extends AsyncTask<Void, Void, Void> {
        private FileDao fileDao;

        private DeleteAllAsyncTask(FileDao fileDao) {
            this.fileDao = fileDao;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            fileDao.deleteAll();
            return null;
        }
    }
}
