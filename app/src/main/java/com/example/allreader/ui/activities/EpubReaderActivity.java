package com.example.allreader.ui.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
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

    private WebView webView, webViewNext;
    private ProgressBar progressBar;
    private Button prevButton, nextButton;
    private TextView pageIndicator;

    private Uri epubUri;
    private List<String> chapters = new ArrayList<>();
    private int currentChapter = 0;

    private GestureDetector gestureDetector;
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    private boolean isAnimating = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_epub_reader);

        webView = findViewById(R.id.webView);
        webViewNext = findViewById(R.id.webViewNext);
        progressBar = findViewById(R.id.progressBar);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);
        pageIndicator = findViewById(R.id.pageIndicator);

        // Configure both WebViews
        configureWebView(webView);
        configureWebView(webViewNext);

        // Hide the second WebView initially
        webViewNext.setVisibility(View.GONE);

        // Initialize gesture detector for swipe navigation
        setupGestureDetector();

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

    private void configureWebView(WebView webView) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
    }

    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null || isAnimating) return false;

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                // Check if horizontal swipe
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
                        }
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });

        // Set touch listener on both WebViews
        View.OnTouchListener touchListener = (v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        };

        webView.setOnTouchListener(touchListener);
        webViewNext.setOnTouchListener(touchListener);
    }

    private void onSwipeRight() {
        // Swipe right = Previous chapter
        if (currentChapter > 0) {
            animateToChapter(currentChapter - 1, true);
        } else {
            Toast.makeText(this, "Already at first chapter", Toast.LENGTH_SHORT).show();
        }
    }

    private void onSwipeLeft() {
        // Swipe left = Next chapter
        if (currentChapter < chapters.size() - 1) {
            animateToChapter(currentChapter + 1, false);
        } else {
            Toast.makeText(this, "Already at last chapter", Toast.LENGTH_SHORT).show();
        }
    }

    private void animateToChapter(int targetChapter, boolean slideRight) {
        if (isAnimating || targetChapter < 0 || targetChapter >= chapters.size()) {
            return;
        }

        isAnimating = true;

        // Load content into the next WebView
        webViewNext.loadDataWithBaseURL(null, chapters.get(targetChapter), "text/html", "UTF-8", null);

        // Position the next WebView off-screen
        int screenWidth = webView.getWidth();
        if (slideRight) {
            // Coming from left
            webViewNext.setTranslationX(-screenWidth);
        } else {
            // Coming from right
            webViewNext.setTranslationX(screenWidth);
        }

        webViewNext.setVisibility(View.VISIBLE);
        webViewNext.setAlpha(1.0f);

        // Animate both WebViews
        long animationDuration = 300;

        // Slide current page out
        webView.animate()
                .translationX(slideRight ? screenWidth : -screenWidth)
                .alpha(0.7f)
                .setDuration(animationDuration)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(null);

        // Slide next page in
        webViewNext.animate()
                .translationX(0)
                .alpha(1.0f)
                .setDuration(animationDuration)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // Swap the WebViews
                        WebView temp = webView;
                        webView = webViewNext;
                        webViewNext = temp;

                        // Reset positions
                        webView.setTranslationX(0);
                        webView.setAlpha(1.0f);

                        webViewNext.setVisibility(View.GONE);
                        webViewNext.setTranslationX(0);
                        webViewNext.setAlpha(1.0f);

                        // Update state
                        currentChapter = targetChapter;
                        pageIndicator.setText((currentChapter + 1) + " / " + chapters.size());
                        updateNavigationButtons();

                        isAnimating = false;
                    }
                });
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
            animateToChapter(currentChapter - 1, true);
        } else {
            Toast.makeText(this, "Already at first chapter", Toast.LENGTH_SHORT).show();
        }
    }

    private void nextChapter() {
        if (currentChapter < chapters.size() - 1) {
            animateToChapter(currentChapter + 1, false);
        } else {
            Toast.makeText(this, "Already at last chapter", Toast.LENGTH_SHORT).show();
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
        if (webViewNext != null) {
            webViewNext.destroy();
        }
    }
}
