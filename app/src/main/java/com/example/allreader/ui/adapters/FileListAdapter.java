package com.example.allreader.ui.adapters;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.allreader.R;
import com.example.allreader.data.model.FileItem;
import java.util.ArrayList;
import java.util.List;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileViewHolder> {

    private List<FileItem> files = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(FileItem file);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
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
        holder.tvFileName.setText(file.getFileName());
        holder.tvFileSize.setText(file.getFileSize());
        holder.tvFileType.setText(file.getFileType().toUpperCase());
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
        TextView tvFileName, tvFileSize, tvFileType;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
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

