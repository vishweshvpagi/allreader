package com.example.allreader.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recent_files")
public class RecentFile {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String fileName;
    private String filePath;
    private String fileType;
    private long lastOpened;
    private int pageNumber;
    private long fileSize;

    public RecentFile(String fileName, String filePath, String fileType, long lastOpened, int pageNumber) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.lastOpened = lastOpened;
        this.pageNumber = pageNumber;
        this.fileSize = 0;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public long getLastOpened() {
        return lastOpened;
    }

    public void setLastOpened(long lastOpened) {
        this.lastOpened = lastOpened;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    // getName() returns fileName - THIS IS THE KEY FIX
    public String getName() {
        return this.fileName;  // Changed from just 'return fileName;' to be explicit
    }

    // getSize() returns fileSize
    public long getSize() {
        return this.fileSize;
    }
}
