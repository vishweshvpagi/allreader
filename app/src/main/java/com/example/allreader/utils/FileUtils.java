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
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
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
                ext.equals("png") || ext.equals("gif") || ext.equals("bmp");
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
