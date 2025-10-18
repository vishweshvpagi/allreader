package com.example.allreader.ui.activities;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.allreader.R;
import com.example.allreader.utils.Constants;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class EpubReaderActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private Button prevButton, nextButton;
    private TextView pageIndicator;

    private Uri epubUri;
    private List<String> chapters = new ArrayList<>();
    private int currentChapter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_epub_reader);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);
        pageIndicator = findViewById(R.id.pageIndicator);

        // Configure WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        // Handle both internal and external intents
        Intent intent = getIntent();
        String fileName;

        if (intent.getData() != null) {
            epubUri = intent.getData();
            fileName = getFileNameFromUri(epubUri);
        } else {
            String filePath = intent.getStringExtra(Constants.EXTRA_FILE_PATH);
            fileName = intent.getStringExtra(Constants.EXTRA_FILE_NAME);

            if (filePath != null) {
                epubUri = Uri.parse(filePath);
            } else {
                Toast.makeText(this, "No EPUB file to open", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(fileName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        prevButton.setOnClickListener(v -> previousChapter());
        nextButton.setOnClickListener(v -> nextChapter());

        loadEpub();
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = "Book.epub";
        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileName;
    }

    private void loadEpub() {
        if (epubUri == null) {
            Toast.makeText(this, "Invalid EPUB file", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(epubUri);
                if (inputStream == null) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Cannot open EPUB file", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                // Parse EPUB (it's a ZIP file)
                ZipInputStream zipInputStream = new ZipInputStream(inputStream);
                ZipEntry zipEntry;

                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    String entryName = zipEntry.getName();

                    // Look for HTML/XHTML content files
                    if ((entryName.endsWith(".html") || entryName.endsWith(".xhtml") || entryName.endsWith(".htm"))
                            && !entryName.contains("nav.xhtml") && !entryName.contains("toc.")) {

                        // Read the chapter content
                        BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream));
                        StringBuilder content = new StringBuilder();
                        String line;

                        while ((line = reader.readLine()) != null) {
                            content.append(line).append("\n");
                        }

                        chapters.add(wrapContent(content.toString()));
                    }
                }

                zipInputStream.close();
                inputStream.close();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    if (chapters.isEmpty()) {
                        Toast.makeText(this, "No readable content found in EPUB", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    webView.setVisibility(View.VISIBLE);
                    displayChapter(0);
                    updateNavigationButtons();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading EPUB: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private String wrapContent(String htmlContent) {
        // Wrap content in proper HTML with styling for better readability
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0, user-scalable=yes'>" +
                "<style>" +
                "body { " +
                "  font-family: Georgia, serif; " +
                "  font-size: 18px; " +
                "  line-height: 1.6; " +
                "  padding: 20px; " +
                "  color: #333; " +
                "  background-color: #ffffff; " +
                "  max-width: 800px; " +
                "  margin: 0 auto; " +
                "}" +
                "p { margin-bottom: 1em; text-align: justify; }" +
                "h1, h2, h3 { margin-top: 1.5em; margin-bottom: 0.5em; }" +
                "img { max-width: 100%; height: auto; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                htmlContent +
                "</body>" +
                "</html>";
    }

    private void displayChapter(int index) {
        if (index >= 0 && index < chapters.size()) {
            currentChapter = index;
            webView.loadDataWithBaseURL(null, chapters.get(index), "text/html", "UTF-8", null);
            pageIndicator.setText((currentChapter + 1) + " / " + chapters.size());
        }
    }

    private void previousChapter() {
        if (currentChapter > 0) {
            displayChapter(currentChapter - 1);
            updateNavigationButtons();
        }
    }

    private void nextChapter() {
        if (currentChapter < chapters.size() - 1) {
            displayChapter(currentChapter + 1);
            updateNavigationButtons();
        }
    }

    private void updateNavigationButtons() {
        prevButton.setEnabled(currentChapter > 0);
        nextButton.setEnabled(currentChapter < chapters.size() - 1);

        prevButton.setAlpha(currentChapter > 0 ? 1.0f : 0.5f);
        nextButton.setAlpha(currentChapter < chapters.size() - 1 ? 1.0f : 0.5f);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
        }
    }
}
