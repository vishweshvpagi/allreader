package com.example.allreader.ui.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.allreader.R;
import com.example.allreader.data.model.RecentFile;
import com.example.allreader.data.model.FileItem;
import com.example.allreader.ui.adapters.FileListAdapter;
import com.example.allreader.ui.activities.*;
import com.example.allreader.utils.Constants;
import com.example.allreader.viewmodel.FileViewModel;

import java.util.List;
import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private TextView tvWelcome, tvRecentFiles;
    private RecyclerView recyclerRecentFiles;
    private FileListAdapter adapter;
    private FileViewModel viewModel;
    private SharedPreferences prefs;
    private List<RecentFile> currentFiles = new ArrayList<>();

    private static final int DEFAULT_FONT_SIZE = 16;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvWelcome = view.findViewById(R.id.welcomeText);
        tvRecentFiles = view.findViewById(R.id.recentFilesTitle);
        recyclerRecentFiles = view.findViewById(R.id.recyclerRecentFiles);

        prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, 0);
        viewModel = new ViewModelProvider(requireActivity()).get(FileViewModel.class);

        setupRecyclerView();
        observeRecentFiles();
        applyFontSize();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        applyFontSize();
    }

    private void setupRecyclerView() {
        recyclerRecentFiles.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FileListAdapter();
        recyclerRecentFiles.setAdapter(adapter);

        adapter.setOnItemClickListener(fileItem -> {
            try {
                openRecentFile(fileItem);
            } catch (Exception e) {
                Log.e(TAG, "Error opening file", e);
                Toast.makeText(getContext(), "Error opening file", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void observeRecentFiles() {
        viewModel.getAllRecentFiles().observe(getViewLifecycleOwner(), this::updateRecentFiles);
    }

    private void updateRecentFiles(List<RecentFile> files) {
        if (files == null || files.isEmpty()) {
            currentFiles.clear();
            adapter.setFiles(new ArrayList<>());
            return;
        }

        currentFiles = new ArrayList<>(files);

        List<FileItem> fileItems = new ArrayList<>();
        for (RecentFile rf : files) {
            String name = rf.getFileName() != null ? rf.getFileName() : "Unknown File";
            String size = formatSize(rf.getSize());
            String type = rf.getFileType() != null ? rf.getFileType() : "file";

            FileItem fi = new FileItem(name, size, type);
            fileItems.add(fi);
        }

        adapter.setFiles(fileItems);
    }

    private void openRecentFile(FileItem fileItem) {
        if (currentFiles == null || currentFiles.isEmpty()) {
            Toast.makeText(getContext(), "File not found", Toast.LENGTH_SHORT).show();
            return;
        }

        for (RecentFile rf : currentFiles) {
            if (rf.getFileName() != null && rf.getFileName().equals(fileItem.getFileName())) {
                openFileByType(rf);
                return;
            }
        }

        Toast.makeText(getContext(), "Cannot find file", Toast.LENGTH_SHORT).show();
    }

    private void openFileByType(RecentFile recentFile) {
        if (recentFile == null || recentFile.getFileType() == null) {
            Toast.makeText(getContext(), "Invalid file", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = null;
        String fileType = recentFile.getFileType();

        switch (fileType) {
            case Constants.FILE_TYPE_PDF:
                intent = new Intent(getActivity(), PdfReaderActivity.class);
                break;
            case Constants.FILE_TYPE_EPUB:
                intent = new Intent(getActivity(), EpubReaderActivity.class);
                break;
            case Constants.FILE_TYPE_WORD:
                intent = new Intent(getActivity(), WordReaderActivity.class);
                break;
            case Constants.FILE_TYPE_EXCEL:
                intent = new Intent(getActivity(), ExcelReaderActivity.class);
                break;
            case Constants.FILE_TYPE_POWERPOINT:
                intent = new Intent(getActivity(), PowerPointReaderActivity.class);
                break;
            case Constants.FILE_TYPE_TXT:
                intent = new Intent(getActivity(), TxtReaderActivity.class);
                break;
            case Constants.FILE_TYPE_VIDEO:
                intent = new Intent(getActivity(), VideoPlayerActivity.class);
                break;
            case Constants.FILE_TYPE_ARCHIVE:
                intent = new Intent(getActivity(), ArchiveViewerActivity.class);
                break;
            case Constants.FILE_TYPE_IMAGE:
                intent = new Intent(getActivity(), ImageViewerActivity.class);
                break;
        }

        if (intent != null) {
            intent.putExtra(Constants.EXTRA_FILE_PATH, recentFile.getFilePath());
            intent.putExtra(Constants.EXTRA_FILE_NAME, recentFile.getFileName());
            intent.putExtra(Constants.EXTRA_FILE_TYPE, recentFile.getFileType());
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), "Unsupported file type", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int kb = (int) (bytes / 1024);
        if (kb < 1024) return kb + " KB";
        int mb = kb / 1024;
        return mb + " MB";
    }

    private void applyFontSize() {
        int fontSize = prefs.getInt(Constants.PREF_FONT_SIZE, DEFAULT_FONT_SIZE);
        tvWelcome.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        tvRecentFiles.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        adapter.setFontSize(fontSize);
    }
}
