package com.example.allreader.ui.adapters;


import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.allreader.R;
import com.example.allreader.data.model.RecentFile;
import com.example.allreader.ui.activities.EpubReaderActivity;
import com.example.allreader.ui.activities.ExcelReaderActivity;
import com.example.allreader.ui.activities.ImageViewerActivity;
import com.example.allreader.ui.activities.PdfReaderActivity;
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
            Intent intent = null;

            switch (file.getFileType()) {
                case Constants.FILE_TYPE_PDF:
                    intent = new Intent(context, PdfReaderActivity.class);
                    break;
                case Constants.FILE_TYPE_EPUB:
                    intent = new Intent(context, EpubReaderActivity.class);
                    break;
                case Constants.FILE_TYPE_EXCEL:
                    intent = new Intent(context, ExcelReaderActivity.class);
                    break;
                case Constants.FILE_TYPE_IMAGE:
                    intent = new Intent(context, ImageViewerActivity.class);
                    break;
            }

            if (intent != null) {
                intent.putExtra(Constants.EXTRA_FILE_PATH, file.getFilePath());
                intent.putExtra(Constants.EXTRA_FILE_NAME, file.getFileName());
                intent.putExtra(Constants.EXTRA_FILE_TYPE, file.getFileType());
                context.startActivity(intent);
            }
        }
    }
}
