package com.example.allreader.ui.activities;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.allreader.R;
import com.example.allreader.readers.PdfReader;
import com.github.barteksc.pdfviewer.PDFView;

public class PdfReaderActivity extends AppCompatActivity {

    private PDFView pdfView;
    private String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_reader);

        pdfView = findViewById(R.id.pdfView);

        Intent intent = getIntent();
        filePath = intent.getStringExtra("FILE_PATH");

        if (filePath != null) {
            loadPdf();
        } else {
            Toast.makeText(this, "No file path provided", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadPdf() {
        PdfReader reader = new PdfReader(this);
        reader.loadPdf(pdfView, filePath);
    }
}

