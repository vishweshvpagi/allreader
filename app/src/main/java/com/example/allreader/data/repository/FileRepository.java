package com.example.allreader.data.repository;


import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.allreader.MainApplication;
import com.example.allreader.data.local.AppDatabase;
import com.example.allreader.data.local.dao.FileDao;
import com.example.allreader.data.model.RecentFile;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileRepository {
    private FileDao fileDao;
    private LiveData<List<RecentFile>> allRecentFiles;
    private ExecutorService executorService;

    public FileRepository(Application application) {
        AppDatabase database = MainApplication.getInstance().getDatabase();
        fileDao = database.fileDao();
        allRecentFiles = fileDao.getAllRecentFiles();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void insert(RecentFile file) {
        executorService.execute(() -> fileDao.insertFile(file));
    }

    public void update(RecentFile file) {
        executorService.execute(() -> fileDao.updateFile(file));
    }

    public void delete(RecentFile file) {
        executorService.execute(() -> fileDao.deleteFile(file));
    }

    public void deleteAll() {
        executorService.execute(() -> fileDao.deleteAllFiles());
    }

    public LiveData<List<RecentFile>> getAllRecentFiles() {
        return allRecentFiles;
    }

    public LiveData<List<RecentFile>> getFilesByType(String type) {
        return fileDao.getFilesByType(type);
    }

    public RecentFile getFileByPath(String path) {
        return fileDao.getFileByPath(path);
    }
}

