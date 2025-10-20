package com.example.allreader.data.model;

public class FileItem {
    private String fileName;
    private String fileSize;
    private String fileType;

    public FileItem(String fileName, String fileSize, String fileType) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileType = fileType;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileSize() {
        return fileSize;
    }

    public String getFileType() {
        return fileType;
    }
}
