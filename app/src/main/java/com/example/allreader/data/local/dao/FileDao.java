package com.example.allreader.data.local.dao;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.allreader.data.model.RecentFile;
import java.util.List;

@Dao
public interface FileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFile(RecentFile file);

    @Update
    void updateFile(RecentFile file);

    @Delete
    void deleteFile(RecentFile file);

    @Query("SELECT * FROM recent_files ORDER BY lastOpened DESC")
    LiveData<List<RecentFile>> getAllRecentFiles();

    @Query("SELECT * FROM recent_files WHERE filePath = :path LIMIT 1")
    RecentFile getFileByPath(String path);

    @Query("DELETE FROM recent_files")
    void deleteAllFiles();

    @Query("SELECT * FROM recent_files WHERE fileType = :type ORDER BY lastOpened DESC")
    LiveData<List<RecentFile>> getFilesByType(String type);
}
