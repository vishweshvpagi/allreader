package com.example.allreader.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.example.allreader.ui.activities.ImageViewerActivity;
import com.example.allreader.ui.activities.PdfReaderActivity;
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
                            openFile(uri);
                        }
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_picker, container, false);

        btnPickFile = view.findViewById(R.id.btnPickFile);
        viewModel = new ViewModelProvider(this).get(FileViewModel.class);

        btnPickFile.setOnClickListener(v -> pickFile());

        return view;
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        // TEMPORARILY DISABLED: Excel and EPUB file types
        String[] mimeTypes = {"application/pdf",
                // "application/epub+zip",  // EPUB DISABLED
                // "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",  // EXCEL DISABLED
                "image/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        filePickerLauncher.launch(intent);
    }

    private void openFile(Uri uri) {
        String fileName = FileUtils.getFileName(getContext(), uri);
        String filePath = uri.toString();

        Intent intent = null;
        String fileType = "";

        if (FileUtils.isPdfFile(fileName)) {
            intent = new Intent(getActivity(), PdfReaderActivity.class);
            fileType = Constants.FILE_TYPE_PDF;
        }
        // TEMPORARILY DISABLED: EPUB handling
        // else if (FileUtils.isEpubFile(fileName)) {
        //     intent = new Intent(getActivity(), EpubReaderActivity.class);
        //     fileType = Constants.FILE_TYPE_EPUB;
        // }
        // TEMPORARILY DISABLED: Excel handling
        // else if (FileUtils.isExcelFile(fileName)) {
        //     intent = new Intent(getActivity(), ExcelReaderActivity.class);
        //     fileType = Constants.FILE_TYPE_EXCEL;
        // }
        else if (FileUtils.isImageFile(fileName)) {
            intent = new Intent(getActivity(), ImageViewerActivity.class);
            fileType = Constants.FILE_TYPE_IMAGE;
        } else {
            Toast.makeText(getContext(), "Unsupported file type", Toast.LENGTH_SHORT).show();
            return;
        }

        intent.putExtra(Constants.EXTRA_FILE_PATH, filePath);
        intent.putExtra(Constants.EXTRA_FILE_NAME, fileName);
        intent.putExtra(Constants.EXTRA_FILE_TYPE, fileType);

        RecentFile recentFile = new RecentFile(fileName, filePath, fileType, System.currentTimeMillis(), 0);
        viewModel.insert(recentFile);

        startActivity(intent);
    }
}
