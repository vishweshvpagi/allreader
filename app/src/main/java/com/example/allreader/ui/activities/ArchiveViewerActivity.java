package com.example.allreader.ui.activities;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.allreader.R;
import com.example.allreader.utils.Constants;
import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ArchiveViewerActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyText;
    private Uri archiveUri;
    private List<ArchiveItem> fileList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive_viewer);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyText = findViewById(R.id.emptyText);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Intent intent = getIntent();
        String fileName;

        if (intent.getData() != null) {
            archiveUri = intent.getData();
            fileName = getFileNameFromUri(archiveUri);
        } else {
            String filePath = intent.getStringExtra(Constants.EXTRA_FILE_PATH);
            fileName = intent.getStringExtra(Constants.EXTRA_FILE_NAME);
            if (filePath != null) archiveUri = Uri.parse(filePath);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(fileName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        loadArchive(fileName);
    }

    private String getFileNameFromUri(Uri uri) {
        try {
            Cursor c = getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx != -1) return c.getString(idx);
            }
        } catch (Exception e) {}
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
                    } else {
                        ArchiveAdapter adapter = new ArchiveAdapter(fileList);
                        recyclerView.setAdapter(adapter);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        }

        zis.close();
        is.close();
    }

    private void loadRar() throws Exception {
        // Copy RAR to cache
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

        // Read RAR
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

    // Archive Item Model
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

    // RecyclerView Adapter
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
            holder.btnExtract.setOnClickListener(v ->
                    Toast.makeText(ArchiveViewerActivity.this, "Extract: " + item.name, Toast.LENGTH_SHORT).show()
            );
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
