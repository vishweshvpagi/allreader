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

import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PowerPointReaderActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private Button btnPrev, btnNext;
    private TextView slideNumber;
    private Uri pptUri;
    private List<String> slides = new ArrayList<>();
    private int currentSlide = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_powerpoint_reader);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        slideNumber = findViewById(R.id.slideNumber);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);

        Intent intent = getIntent();
        String fileName;

        if (intent.getData() != null) {
            pptUri = intent.getData();
            fileName = getFileNameFromUri(pptUri);
        } else {
            String filePath = intent.getStringExtra(Constants.EXTRA_FILE_PATH);
            fileName = intent.getStringExtra(Constants.EXTRA_FILE_NAME);
            if (filePath != null) pptUri = Uri.parse(filePath);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(fileName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        btnPrev.setOnClickListener(v -> {
            if (currentSlide > 0) showSlide(currentSlide - 1);
        });

        btnNext.setOnClickListener(v -> {
            if (currentSlide < slides.size() - 1) showSlide(currentSlide + 1);
        });

        loadFile(fileName);
    }

    private String getFileNameFromUri(Uri uri) {
        try {
            Cursor c = getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx != -1) return c.getString(idx);
            }
        } catch (Exception e) {}
        return "Presentation";
    }

    private void loadFile(String fileName) {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(pptUri);
                if (fileName.endsWith(".pptx")) {
                    XMLSlideShow ppt = new XMLSlideShow(is);
                    for (XSLFSlide slide : ppt.getSlides()) {
                        StringBuilder sb = new StringBuilder();
                        for (XSLFShape shape : slide.getShapes()) {
                            if (shape instanceof XSLFTextShape) {
                                for (XSLFTextParagraph p : ((XSLFTextShape) shape).getTextParagraphs()) {
                                    for (XSLFTextRun r : p.getTextRuns()) {
                                        String txt = r.getRawText();
                                        if (txt != null && !txt.trim().isEmpty()) {
                                            String styled = txt;
                                            if (r.isBold()) styled = "<b>" + styled + "</b>";
                                            if (r.isItalic()) styled = "<i>" + styled + "</i>";
                                            sb.append(styled);
                                        }
                                    }
                                    if (sb.length() > 0) {
                                        int lvl = p.getIndentLevel();
                                        slides.add(makeHtml(sb.toString(), lvl == 0));
                                        sb.setLength(0);
                                    }
                                }
                            }
                        }
                        if (slides.isEmpty()) slides.add(makeHtml("Empty slide", false));
                    }
                    ppt.close();
                } else {
                    HSLFSlideShow ppt = new HSLFSlideShow(is);
                    for (HSLFSlide slide : ppt.getSlides()) {
                        StringBuilder sb = new StringBuilder();
                        for (List<HSLFTextParagraph> paras : slide.getTextParagraphs()) {
                            String txt = HSLFTextParagraph.getText(paras);
                            if (txt != null && !txt.trim().isEmpty()) sb.append(txt).append("\n");
                        }
                        slides.add(makeHtml(sb.length() > 0 ? sb.toString() : "Empty slide", false));
                    }
                    ppt.close();
                }
                is.close();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (slides.isEmpty()) {
                        Toast.makeText(this, "No content found", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        showSlide(0);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }

    private String makeHtml(String content, boolean isTitle) {
        String size = isTitle ? "28px" : "18px";
        String weight = isTitle ? "bold" : "normal";
        return "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>body{font-family:Arial;padding:40px;background:#fff;margin:0;line-height:1.8;font-size:" + size + ";font-weight:" + weight + ";color:#000;}</style>" +
                "</head><body>" + content.replace("\n", "<br>") + "</body></html>";
    }

    private void showSlide(int idx) {
        currentSlide = idx;
        webView.loadDataWithBaseURL(null, slides.get(idx), "text/html", "UTF-8", null);
        slideNumber.setText((idx + 1) + " / " + slides.size());
        btnPrev.setEnabled(idx > 0);
        btnNext.setEnabled(idx < slides.size() - 1);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
