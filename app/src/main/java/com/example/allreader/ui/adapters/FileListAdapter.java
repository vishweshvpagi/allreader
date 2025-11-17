package com.example.allreader.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.allreader.R;
import com.example.allreader.data.model.FileItem;
import com.example.allreader.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileViewHolder> {

    private List<FileItem> files = new ArrayList<>();
    private OnItemClickListener listener;
    private int fontSize = 16;

    public interface OnItemClickListener {
        void onItemClick(FileItem file);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem file = files.get(position);

        holder.tvFileName.setText(file.getFileName() != null ? file.getFileName() : "Unknown");
        holder.tvFileSize.setText(file.getFileSize() != null ? file.getFileSize() : "0 B");
        holder.tvFileType.setText(file.getFileType() != null ? file.getFileType().toUpperCase() : "FILE");

        // Set icon based on file type
        holder.ivFileIcon.setImageResource(getIconForFileType(file.getFileType()));

        holder.tvFileName.setTextSize(fontSize);
        holder.tvFileSize.setTextSize(fontSize - 4);
        holder.tvFileType.setTextSize(fontSize - 4);
    }

    private int getIconForFileType(String fileType) {
        if (fileType == null) return R.drawable.ic_file;

        switch (fileType.toLowerCase()) {
            case Constants.FILE_TYPE_PDF:
                return R.drawable.ic_pdf;
            case Constants.FILE_TYPE_WORD:
                return R.drawable.ic_word;
            case Constants.FILE_TYPE_EXCEL:
                return R.drawable.ic_excel;
            case Constants.FILE_TYPE_IMAGE:
                return R.drawable.ic_image;
            case Constants.FILE_TYPE_VIDEO:
                return R.drawable.ic_video;
            case Constants.FILE_TYPE_AUDIO:
                return R.drawable.ic_audio_file;
            case Constants.FILE_TYPE_ARCHIVE:
                return R.drawable.ic_archive;
            case Constants.FILE_TYPE_TXT:
                return R.drawable.ic_txt;
            default:
                return R.drawable.ic_file;
        }
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public void setFiles(List<FileItem> files) {
        this.files = files;
        notifyDataSetChanged();
    }

    class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFileIcon;
        TextView tvFileName, tvFileSize, tvFileType;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            tvFileType = itemView.findViewById(R.id.tvFileType);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(files.get(position));
                }
            });
        }
    }
}
