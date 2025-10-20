package com.example.allreader.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.allreader.R;
import com.example.allreader.data.model.RecentFile;
import com.example.allreader.ui.activities.*;
import com.example.allreader.utils.Constants;
import com.example.allreader.utils.FileUtils;
import com.example.allreader.viewmodel.FileViewModel;

public class FilePickerFragment extends Fragment {

    private Button btnPickFile;
    private FileViewModel viewModel;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            try {
                                // IMPORTANT: Take persistent URI permission
                                takePersistableUriPermission(uri);
                                openFile(uri);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(getContext(), "Error opening file", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_picker, container, false);

        btnPickFile = view.findViewById(R.id.btnPickFile);
        viewModel = new ViewModelProvider(requireActivity()).get(FileViewModel.class);

        btnPickFile.setOnClickListener(v -> pickFile());

        return view;
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        // Request persistable permission
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        filePickerLauncher.launch(intent);
    }

    private void takePersistableUriPermission(Uri uri) {
        try {
            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
            requireContext().getContentResolver().takePersistableUriPermission(uri, takeFlags);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void openFile(Uri uri) {
        String fileName = FileUtils.getFileName(getContext(), uri);
        String filePath = uri.toString();
        long fileSize = getFileSizeFromUri(uri);

        Intent intent = null;
        String fileType = "";

        if (FileUtils.isPdfFile(fileName)) {
            intent = new Intent(getActivity(), PdfReaderActivity.class);
            fileType = Constants.FILE_TYPE_PDF;
        }
        else if (FileUtils.isEpubFile(fileName)) {
            intent = new Intent(getActivity(), EpubReaderActivity.class);
            fileType = Constants.FILE_TYPE_EPUB;
        }
        else if (FileUtils.isWordFile(fileName)) {
            intent = new Intent(getActivity(), WordReaderActivity.class);
            fileType = Constants.FILE_TYPE_WORD;
        }
        else if (FileUtils.isExcelFile(fileName)) {
            intent = new Intent(getActivity(), ExcelReaderActivity.class);
            fileType = Constants.FILE_TYPE_EXCEL;
        }
        else if (FileUtils.isPowerPointFile(fileName)) {
            intent = new Intent(getActivity(), PowerPointReaderActivity.class);
            fileType = Constants.FILE_TYPE_POWERPOINT;
        }
        else if (FileUtils.isTxtFile(fileName)) {
            intent = new Intent(getActivity(), TxtReaderActivity.class);
            fileType = Constants.FILE_TYPE_TXT;
        }
        else if (FileUtils.isVideoFile(fileName)) {
            intent = new Intent(getActivity(), VideoPlayerActivity.class);
            fileType = Constants.FILE_TYPE_VIDEO;
        }
        else if (FileUtils.isArchiveFile(fileName)) {
            intent = new Intent(getActivity(), ArchiveViewerActivity.class);
            fileType = Constants.FILE_TYPE_ARCHIVE;
        }
        else if (FileUtils.isImageFile(fileName)) {
            intent = new Intent(getActivity(), ImageViewerActivity.class);
            fileType = Constants.FILE_TYPE_IMAGE;
        }
        else {
            Toast.makeText(getContext(), "Unsupported file type: " + fileName, Toast.LENGTH_SHORT).show();
            return;
        }

        intent.putExtra(Constants.EXTRA_FILE_PATH, filePath);
        intent.putExtra(Constants.EXTRA_FILE_NAME, fileName);
        intent.putExtra(Constants.EXTRA_FILE_TYPE, fileType);

        // Save to recent files
        RecentFile recentFile = new RecentFile(
                fileName,
                filePath,
                fileType,
                System.currentTimeMillis(),
                0
        );
        recentFile.setFileSize(fileSize);

        viewModel.insert(recentFile);

        startActivity(intent);
    }

    private long getFileSizeFromUri(Uri uri) {
        long size = 0;
        try {
            if (uri.getScheme().equals("content")) {
                Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex);
                    }
                    cursor.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }
}
