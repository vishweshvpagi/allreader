package com.example.allreader.viewmodel;


import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.allreader.data.model.RecentFile;
import com.example.allreader.data.repository.FileRepository;
import java.util.List;

public class FileViewModel extends AndroidViewModel {
    private FileRepository repository;
    private LiveData<List<RecentFile>> allRecentFiles;

    public FileViewModel(@NonNull Application application) {
        super(application);
        repository = new FileRepository(application);
        allRecentFiles = repository.getAllRecentFiles();
    }

    public void insert(RecentFile file) {
        repository.insert(file);
    }

    public void update(RecentFile file) {
        repository.update(file);
    }

    public void delete(RecentFile file) {
        repository.delete(file);
    }

    public void deleteAll() {
        repository.deleteAll();
    }

    public LiveData<List<RecentFile>> getAllRecentFiles() {
        return allRecentFiles;
    }

    public LiveData<List<RecentFile>> getFilesByType(String type) {
        return repository.getFilesByType(type);
    }
}
