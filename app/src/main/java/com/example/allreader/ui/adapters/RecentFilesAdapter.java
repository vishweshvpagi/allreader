package com.example.allreader.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.allreader.R;
import com.example.allreader.data.model.RecentFile;
import com.example.allreader.ui.activities.*;
import com.example.allreader.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentFilesAdapter extends RecyclerView.Adapter<RecentFilesAdapter.RecentFileViewHolder> {

    private List<RecentFile> files = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    @NonNull
    @Override
    public RecentFileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new RecentFileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentFileViewHolder holder, int position) {
        RecentFile file = files.get(position);
        holder.tvFileName.setText(file.getFileName());
        holder.tvFileType.setText(file.getFileType().toUpperCase());
        holder.tvFileSize.setText(dateFormat.format(new Date(file.getLastOpened())));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public void setFiles(List<RecentFile> files) {
        this.files = files;
        notifyDataSetChanged();
    }

    class RecentFileViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName, tvFileSize, tvFileType;

        public RecentFileViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            tvFileType = itemView.findViewById(R.id.tvFileType);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    RecentFile file = files.get(position);
                    openFile(itemView.getContext(), file);
                }
            });
        }

        private void openFile(Context context, RecentFile file) {
            String fileName = file.getFileName();
            String filePath = file.getFilePath();

            if (fileName == null || fileName.isEmpty()) {
                Toast.makeText(context, "Invalid file", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.e("RECENT_OPEN", "==================");
            Log.e("RECENT_OPEN", "File: " + fileName);
            Log.e("RECENT_OPEN", "Path: " + filePath);

            Uri fileUri = Uri.parse(filePath);
            Intent intent = null;

            // Get lowercase extension
            String lowerFileName = fileName.toLowerCase();

            // Check extension directly
            if (lowerFileName.endsWith(".mp3") || lowerFileName.endsWith(".m4a") ||
                    lowerFileName.endsWith(".wav") || lowerFileName.endsWith(".ogg") ||
                    lowerFileName.endsWith(".flac") || lowerFileName.endsWith(".aac")) {

                Log.e("RECENT_OPEN", "Detected: AUDIO");
                intent = new Intent(context, AudioPlayerActivity.class);
                intent.setData(fileUri);
                intent.putExtra("AUDIO_URI", filePath);
            }
            else if (lowerFileName.endsWith(".pdf")) {
                Log.e("RECENT_OPEN", "Detected: PDF");
                intent = new Intent(context, PdfReaderActivity.class);
                intent.setData(fileUri);
                intent.putExtra(Constants.EXTRA_FILE_PATH, filePath);
                intent.putExtra(Constants.EXTRA_FILE_NAME, fileName);
            }
            else if (lowerFileName.endsWith(".epub")) {
                intent = new Intent(context, EpubReaderActivity.class);
                intent.setData(fileUri);
                intent.putExtra(Constants.EXTRA_FILE_PATH, filePath);
                intent.putExtra(Constants.EXTRA_FILE_NAME, fileName);
            }
            else if (lowerFileName.endsWith(".doc") || lowerFileName.endsWith(".docx")) {
                intent = new Intent(context, WordReaderActivity.class);
                intent.setData(fileUri);
                intent.putExtra(Constants.EXTRA_FILE_PATH, filePath);
                intent.putExtra(Constants.EXTRA_FILE_NAME, fileName);
            }
            else if (lowerFileName.endsWith(".xls") || lowerFileName.endsWith(".xlsx")) {
                intent = new Intent(context, ExcelReaderActivity.class);
                intent.setData(fileUri);
                intent.putExtra(Constants.EXTRA_FILE_PATH, filePath);
                intent.putExtra(Constants.EXTRA_FILE_NAME, fileName);
            }
            else if (lowerFileName.endsWith(".ppt") || lowerFileName.endsWith(".pptx")) {
                intent = new Intent(context, PowerPointReaderActivity.class);
                intent.setData(fileUri);
                intent.putExtra(Constants.EXTRA_FILE_PATH, filePath);
                intent.putExtra(Constants.EXTRA_FILE_NAME, fileName);
            }
            else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg") ||
                    lowerFileName.endsWith(".png") || lowerFileName.endsWith(".gif") ||
                    lowerFileName.endsWith(".bmp") || lowerFileName.endsWith(".webp")) {
                intent = new Intent(context, ImageViewerActivity.class);
                intent.setData(fileUri);
                intent.putExtra(Constants.EXTRA_FILE_PATH, filePath);
                intent.putExtra(Constants.EXTRA_FILE_NAME, fileName);
            }
            else if (lowerFileName.endsWith(".mp4") || lowerFileName.endsWith(".avi") ||
                    lowerFileName.endsWith(".mkv") || lowerFileName.endsWith(".mov")) {
                intent = new Intent(context, VideoPlayerActivity.class);
                intent.setData(fileUri);
                intent.putExtra(Constants.EXTRA_FILE_PATH, filePath);
                intent.putExtra(Constants.EXTRA_FILE_NAME, fileName);
            }
            else if (lowerFileName.endsWith(".txt") || lowerFileName.endsWith(".log")) {
                intent = new Intent(context, TxtReaderActivity.class);
                intent.setData(fileUri);
                intent.putExtra(Constants.EXTRA_FILE_PATH, filePath);
                intent.putExtra(Constants.EXTRA_FILE_NAME, fileName);
            }
            else if (lowerFileName.endsWith(".zip") || lowerFileName.endsWith(".rar")) {
                intent = new Intent(context, ArchiveViewerActivity.class);
                intent.setData(fileUri);
                intent.putExtra(Constants.EXTRA_FILE_PATH, filePath);
                intent.putExtra(Constants.EXTRA_FILE_NAME, fileName);
            }
            else {
                Log.e("RECENT_OPEN", "NOT SUPPORTED!");
                Toast.makeText(context, "Unsupported: " + fileName, Toast.LENGTH_LONG).show();
                return;
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                try {
                    Log.e("RECENT_OPEN", "Starting activity...");
                    context.startActivity(intent);
                } catch (Exception e) {
                    Log.e("RECENT_OPEN", "ERROR: " + e.getMessage());
                    Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            Log.e("RECENT_OPEN", "==================");
        }
    }
}
