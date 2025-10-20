package com.example.allreader.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Locale;

public class FileUtils {

    public static String getFileName(Context context, Uri uri) {
        String fileName = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (fileName == null) {
            fileName = uri.getLastPathSegment();
        }

        return fileName != null ? fileName : "Unknown File";  // Never return null
    }

    public static String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase(Locale.ROOT);
        }
        return "";
    }

    public static String getFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static boolean isPdfFile(String fileName) {
        return getFileExtension(fileName).equals("pdf");
    }

    public static boolean isEpubFile(String fileName) {
        return getFileExtension(fileName).equals("epub");
    }

    public static boolean isExcelFile(String fileName) {
        String ext = getFileExtension(fileName);
        return ext.equals("xls") || ext.equals("xlsx");
    }

    public static boolean isImageFile(String fileName) {
        String ext = getFileExtension(fileName);
        return ext.equals("jpg") || ext.equals("jpeg") ||
                ext.equals("png") || ext.equals("gif") ||
                ext.equals("bmp") || ext.equals("webp");  // Added webp support
    }
    public static boolean isPowerPointFile(String fileName) {
        String ext = getFileExtension(fileName);
        return ext.equals("ppt") || ext.equals("pptx");
    }public static boolean isTxtFile(String fileName) {
        String ext = getFileExtension(fileName);
        return ext.equals("txt") || ext.equals("log") || ext.equals("md");
    }public static boolean isVideoFile(String fileName) {
        String ext = getFileExtension(fileName);
        return ext.equals("mp4") || ext.equals("avi") || ext.equals("mkv") ||
                ext.equals("mov") || ext.equals("wmv") || ext.equals("3gp") ||
                ext.equals("flv") || ext.equals("webm");
    }

    public static boolean isArchiveFile(String fileName) {
        String ext = getFileExtension(fileName);
        return ext.equals("zip") || ext.equals("rar") ||
                ext.equals("jar") || ext.equals("7z");
    }


    public static long getFileSize(Context context, Uri uri) {
        long size = 0;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (uri.getScheme().equals("file")) {
            try {
                File file = new File(uri.getPath());
                size = file.length();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return size;
    }


    public static boolean isWordFile(String fileName) {
        String ext = getFileExtension(fileName);
        return ext.equals("doc") || ext.equals("docx");
    }


    public static String getMimeType(String url) {
        String type = null;
        String extension = getFileExtension(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }
}
