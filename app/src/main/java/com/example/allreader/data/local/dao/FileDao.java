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
    void insert(RecentFile file);

    @Update
    void update(RecentFile file);

    @Delete
    void delete(RecentFile file);

    @Query("DELETE FROM recent_files")
    void deleteAll();

    @Query("SELECT * FROM recent_files ORDER BY lastOpened DESC")
    LiveData<List<RecentFile>> getAllRecentFiles();

    @Query("SELECT * FROM recent_files WHERE fileType = :type ORDER BY lastOpened DESC")
    LiveData<List<RecentFile>> getFilesByType(String type);
}
