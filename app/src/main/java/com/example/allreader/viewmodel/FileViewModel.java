package com.example.allreader.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.allreader.data.model.RecentFile;
import com.example.allreader.data.repository.FileRepository;
import java.util.List;

public class FileViewModel extends AndroidViewModel {
    private FileRepository repository;
    private LiveData<List<RecentFile>> allRecentFiles;

    // Add MutableLiveData for font size
    private final MutableLiveData<Integer> fontSizeLiveData = new MutableLiveData<>();

    public FileViewModel(@NonNull Application application) {
        super(application);
        repository = new FileRepository(application);
        allRecentFiles = repository.getAllRecentFiles();

        // Initialize with default font size 16sp or load from preferences as needed
        fontSizeLiveData.setValue(16);
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

    // Font size observer
    public LiveData<Integer> getFontSizeLiveData() {
        return fontSizeLiveData;
    }

    // Set font size - called by SettingsFragment on change
    public void setFontSize(int fontSize) {
        fontSizeLiveData.setValue(fontSize);
    }
}
