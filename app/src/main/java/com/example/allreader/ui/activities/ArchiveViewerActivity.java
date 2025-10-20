package com.example.allreader.ui.activities;

import static com.example.allreader.R.*;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.allreader.R;
import com.example.allreader.utils.Constants;
import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ArchiveViewerActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_EXTRACT_FOLDER = 100;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyText;
    private Button btnExtractAll;
    private Uri archiveUri;
    private String archiveFileName;
    private List<ArchiveItem> fileList = new ArrayList<>();
    private ArchiveItem pendingExtractItem;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive_viewer);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyText = findViewById(R.id.emptyText);
        btnExtractAll = findViewById(R.id.btnExtractAll);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Intent intent = getIntent();

        if (intent.getData() != null) {
            archiveUri = intent.getData();
            archiveFileName = getFileNameFromUri(archiveUri);
        } else {
            String filePath = intent.getStringExtra(Constants.EXTRA_FILE_PATH);
            archiveFileName = intent.getStringExtra(Constants.EXTRA_FILE_NAME);
            if (filePath != null) archiveUri = Uri.parse(filePath);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(archiveFileName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        btnExtractAll.setOnClickListener(v -> extractAllFiles());

        loadArchive(archiveFileName);
    }

    private String getFileNameFromUri(Uri uri) {
        try {
            Cursor c = getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx != -1) {
                    String name = c.getString(idx);
                    c.close();
                    return name;
                }
                c.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Archive";
    }

    private void loadArchive(String fileName) {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                if (fileName.endsWith(".zip") || fileName.endsWith(".jar")) {
                    loadZip();
                } else if (fileName.endsWith(".rar")) {
                    loadRar();
                }

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (fileList.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                        btnExtractAll.setEnabled(false);
                    } else {
                        ArchiveAdapter adapter = new ArchiveAdapter(fileList);
                        recyclerView.setAdapter(adapter);
                        btnExtractAll.setEnabled(true);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading archive: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void loadZip() throws Exception {
        InputStream is = getContentResolver().openInputStream(archiveUri);
        ZipInputStream zis = new ZipInputStream(is);
        ZipEntry entry;

        while ((entry = zis.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                fileList.add(new ArchiveItem(
                        entry.getName(),
                        entry.getSize(),
                        getFileIcon(entry.getName())
                ));
            }
            zis.closeEntry();
        }

        zis.close();
        is.close();
    }

    private void loadRar() throws Exception {
        File cacheFile = new File(getCacheDir(), "temp.rar");
        InputStream is = getContentResolver().openInputStream(archiveUri);
        FileOutputStream fos = new FileOutputStream(cacheFile);
        byte[] buffer = new byte[8192];
        int length;
        while ((length = is.read(buffer)) > 0) {
            fos.write(buffer, 0, length);
        }
        fos.close();
        is.close();

        Archive archive = new Archive(cacheFile);
        for (FileHeader fh : archive.getFileHeaders()) {
            if (!fh.isDirectory()) {
                fileList.add(new ArchiveItem(
                        fh.getFileName(),
                        fh.getFullUnpackSize(),
                        getFileIcon(fh.getFileName())
                ));
            }
        }
        archive.close();
        cacheFile.delete();
    }

    private void extractSingleFile(ArchiveItem item) {
        pendingExtractItem = item;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_CODE_EXTRACT_FOLDER);
    }

    private void extractAllFiles() {
        pendingExtractItem = null;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_CODE_EXTRACT_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_EXTRACT_FOLDER && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri folderUri = data.getData();

                // Grant persistent permission
                getContentResolver().takePersistableUriPermission(
                        folderUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                );

                if (pendingExtractItem != null) {
                    extractFile(folderUri, pendingExtractItem);
                } else {
                    extractAll(folderUri);
                }
            }
        }
    }

    private void extractFile(Uri folderUri, ArchiveItem item) {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                DocumentFile folder = DocumentFile.fromTreeUri(this, folderUri);

                if (archiveFileName.endsWith(".zip") || archiveFileName.endsWith(".jar")) {
                    extractSingleZipFile(folder, item);
                } else if (archiveFileName.endsWith(".rar")) {
                    extractSingleRarFile(folder, item);
                }

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Extracted: " + item.name, Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error extracting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void extractAll(Uri folderUri) {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                DocumentFile folder = DocumentFile.fromTreeUri(this, folderUri);
                int count = 0;

                if (archiveFileName.endsWith(".zip") || archiveFileName.endsWith(".jar")) {
                    count = extractAllZipFiles(folder);
                } else if (archiveFileName.endsWith(".rar")) {
                    count = extractAllRarFiles(folder);
                }

                final int finalCount = count;
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Extracted " + finalCount + " files", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error extracting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void extractSingleZipFile(DocumentFile folder, ArchiveItem item) throws Exception {
        InputStream is = getContentResolver().openInputStream(archiveUri);
        ZipInputStream zis = new ZipInputStream(is);
        ZipEntry entry;

        while ((entry = zis.getNextEntry()) != null) {
            if (!entry.isDirectory() && entry.getName().equals(item.name)) {
                String fileName = getFileNameFromPath(entry.getName());
                DocumentFile file = folder.createFile(getMimeType(fileName), fileName);

                if (file != null) {
                    OutputStream os = getContentResolver().openOutputStream(file.getUri());
                    BufferedOutputStream bos = new BufferedOutputStream(os);

                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = zis.read(buffer)) > 0) {
                        bos.write(buffer, 0, length);
                    }

                    bos.flush();
                    bos.close();
                    os.close();
                }
                break;
            }
            zis.closeEntry();
        }

        zis.close();
        is.close();
    }

    private int extractAllZipFiles(DocumentFile folder) throws Exception {
        InputStream is = getContentResolver().openInputStream(archiveUri);
        ZipInputStream zis = new ZipInputStream(is);
        ZipEntry entry;
        int count = 0;

        while ((entry = zis.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                String fileName = getFileNameFromPath(entry.getName());
                DocumentFile file = folder.createFile(getMimeType(fileName), fileName);

                if (file != null) {
                    OutputStream os = getContentResolver().openOutputStream(file.getUri());
                    BufferedOutputStream bos = new BufferedOutputStream(os);

                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = zis.read(buffer)) > 0) {
                        bos.write(buffer, 0, length);
                    }

                    bos.flush();
                    bos.close();
                    os.close();
                    count++;
                }
            }
            zis.closeEntry();
        }

        zis.close();
        is.close();
        return count;
    }

    private void extractSingleRarFile(DocumentFile folder, ArchiveItem item) throws Exception {
        File cacheFile = new File(getCacheDir(), "temp.rar");
        InputStream is = getContentResolver().openInputStream(archiveUri);
        FileOutputStream fos = new FileOutputStream(cacheFile);
        byte[] buffer = new byte[8192];
        int length;
        while ((length = is.read(buffer)) > 0) {
            fos.write(buffer, 0, length);
        }
        fos.close();
        is.close();

        Archive archive = new Archive(cacheFile);
        FileHeader fileHeader = archive.nextFileHeader();

        while (fileHeader != null) {
            if (!fileHeader.isDirectory() && fileHeader.getFileName().equals(item.name)) {
                String fileName = getFileNameFromPath(fileHeader.getFileName());
                DocumentFile file = folder.createFile(getMimeType(fileName), fileName);

                if (file != null) {
                    OutputStream os = getContentResolver().openOutputStream(file.getUri());
                    archive.extractFile(fileHeader, os);
                    os.close();
                }
                break;
            }
            fileHeader = archive.nextFileHeader();
        }

        archive.close();
        cacheFile.delete();
    }

    private int extractAllRarFiles(DocumentFile folder) throws Exception {
        File cacheFile = new File(getCacheDir(), "temp.rar");
        InputStream is = getContentResolver().openInputStream(archiveUri);
        FileOutputStream fos = new FileOutputStream(cacheFile);
        byte[] buffer = new byte[8192];
        int length;
        while ((length = is.read(buffer)) > 0) {
            fos.write(buffer, 0, length);
        }
        fos.close();
        is.close();

        Archive archive = new Archive(cacheFile);
        FileHeader fileHeader = archive.nextFileHeader();
        int count = 0;

        while (fileHeader != null) {
            if (!fileHeader.isDirectory()) {
                String fileName = getFileNameFromPath(fileHeader.getFileName());
                DocumentFile file = folder.createFile(getMimeType(fileName), fileName);

                if (file != null) {
                    OutputStream os = getContentResolver().openOutputStream(file.getUri());
                    archive.extractFile(fileHeader, os);
                    os.close();
                    count++;
                }
            }
            fileHeader = archive.nextFileHeader();
        }

        archive.close();
        cacheFile.delete();
        return count;
    }

    private String getFileNameFromPath(String path) {
        if (path.contains("/")) {
            return path.substring(path.lastIndexOf("/") + 1);
        }
        return path;
    }

    private String getMimeType(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (ext) {
            case "pdf": return "application/pdf";
            case "doc": return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls": return "application/vnd.ms-excel";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt": return "application/vnd.ms-powerpoint";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt": return "text/plain";
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "mp3": return "audio/mpeg";
            case "mp4": return "video/mp4";
            default: return "application/octet-stream";
        }
    }

    private String getFileIcon(String name) {
        String ext = name.substring(name.lastIndexOf(".") + 1).toLowerCase();
        switch (ext) {
            case "pdf": return "📕";
            case "doc": case "docx": return "📘";
            case "xls": case "xlsx": return "📗";
            case "ppt": case "pptx": return "📙";
            case "txt": return "📄";
            case "jpg": case "png": case "gif": return "🖼️";
            case "mp3": case "wav": return "🎵";
            case "mp4": case "avi": return "🎬";
            default: return "📄";
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    static class ArchiveItem {
        String name;
        long size;
        String icon;

        ArchiveItem(String name, long size, String icon) {
            this.name = name;
            this.size = size;
            this.icon = icon;
        }
    }

    class ArchiveAdapter extends RecyclerView.Adapter<ArchiveAdapter.ViewHolder> {
        List<ArchiveItem> items;

        ArchiveAdapter(List<ArchiveItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_archive_file, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ArchiveItem item = items.get(position);
            holder.fileName.setText(item.name);
            holder.fileSize.setText(formatSize(item.size));
            holder.fileIcon.setText(item.icon);
            holder.btnExtract.setOnClickListener(v -> extractSingleFile(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        String formatSize(long size) {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return (size / 1024) + " KB";
            return (size / (1024 * 1024)) + " MB";
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView fileName, fileSize, fileIcon;
            Button btnExtract;

            ViewHolder(View v) {
                super(v);
                fileName = v.findViewById(R.id.fileName);
                fileSize = v.findViewById(R.id.fileSize);
                fileIcon = v.findViewById(R.id.fileIcon);
                btnExtract = v.findViewById(R.id.btnExtract);
            }
        }
    }
}
