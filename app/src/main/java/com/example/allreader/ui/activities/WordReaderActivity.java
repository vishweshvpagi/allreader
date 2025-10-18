package com.example.allreader.ui.activities;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.allreader.R;
import com.example.allreader.utils.Constants;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.InputStream;

public class WordReaderActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout searchBar;
    private EditText searchInput;
    private Button btnSearch, btnZoomIn, btnZoomOut, btnWordCount;
    private Button btnPrevSearch, btnNextSearch, btnCloseSearch;

    private Uri wordUri;
    private String documentText = "";
    private int currentTextSize = 16;
    private int wordCount = 0;
    private int characterCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_reader);

        initializeViews();
        setupWebView();
        setupButtons();

        // Handle both internal and external intents
        Intent intent = getIntent();
        String fileName;

        if (intent.getData() != null) {
            wordUri = intent.getData();
            fileName = getFileNameFromUri(wordUri);
        } else {
            String filePath = intent.getStringExtra(Constants.EXTRA_FILE_PATH);
            fileName = intent.getStringExtra(Constants.EXTRA_FILE_NAME);

            if (filePath != null) {
                wordUri = Uri.parse(filePath);
            } else {
                Toast.makeText(this, "No Word file to open", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(fileName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        loadWordDocument(fileName);
    }

    private void initializeViews() {
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        searchBar = findViewById(R.id.searchBar);
        searchInput = findViewById(R.id.searchInput);

        btnSearch = findViewById(R.id.btnSearch);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        btnWordCount = findViewById(R.id.btnWordCount);
        btnPrevSearch = findViewById(R.id.btnPrevSearch);
        btnNextSearch = findViewById(R.id.btnNextSearch);
        btnCloseSearch = findViewById(R.id.btnCloseSearch);
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
    }

    private void setupButtons() {
        btnSearch.setOnClickListener(v -> toggleSearchBar());

        btnZoomIn.setOnClickListener(v -> {
            currentTextSize += 2;
            updateFontSize();
        });

        btnZoomOut.setOnClickListener(v -> {
            if (currentTextSize > 10) {
                currentTextSize -= 2;
                updateFontSize();
            }
        });

        btnWordCount.setOnClickListener(v -> showDocumentInfo());

        btnCloseSearch.setOnClickListener(v -> {
            searchBar.setVisibility(View.GONE);
            webView.clearMatches();
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    webView.findAllAsync(s.toString());
                } else {
                    webView.clearMatches();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnNextSearch.setOnClickListener(v -> webView.findNext(true));
        btnPrevSearch.setOnClickListener(v -> webView.findNext(false));
    }

    private void toggleSearchBar() {
        if (searchBar.getVisibility() == View.GONE) {
            searchBar.setVisibility(View.VISIBLE);
            searchInput.requestFocus();
        } else {
            searchBar.setVisibility(View.GONE);
            webView.clearMatches();
        }
    }

    private void updateFontSize() {
        String js = "document.body.style.fontSize='" + currentTextSize + "px';";
        webView.evaluateJavascript(js, null);
    }

    private void showDocumentInfo() {
        String info = "📄 Document Statistics\n\n" +
                "Words: " + wordCount + "\n" +
                "Characters: " + characterCount + "\n" +
                "Characters (no spaces): " + (characterCount - countSpaces(documentText));

        new AlertDialog.Builder(this)
                .setTitle("Document Info")
                .setMessage(info)
                .setPositiveButton("OK", null)
                .show();
    }

    private int countSpaces(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) count++;
        }
        return count;
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = "Document.docx";
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

    private void loadWordDocument(String fileName) {
        if (wordUri == null) {
            Toast.makeText(this, "Invalid Word document", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(wordUri);
                if (inputStream == null) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Cannot open Word file", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                String htmlContent;

                if (fileName.endsWith(".docx")) {
                    htmlContent = readDocxToHtml(inputStream);
                } else {
                    htmlContent = readDocToHtml(inputStream);
                }

                inputStream.close();

                // Calculate statistics
                documentText = htmlContent.replaceAll("<[^>]*>", ""); // Strip HTML tags
                wordCount = documentText.trim().split("\\s+").length;
                characterCount = documentText.length();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                    webView.loadDataWithBaseURL(null, wrapHtml(htmlContent), "text/html", "UTF-8", null);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error reading Word document: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private String readDocxToHtml(InputStream inputStream) throws Exception {
        XWPFDocument document = new XWPFDocument(inputStream);
        StringBuilder htmlBuilder = new StringBuilder();

        for (IBodyElement element : document.getBodyElements()) {
            if (element instanceof XWPFParagraph) {
                XWPFParagraph paragraph = (XWPFParagraph) element;
                htmlBuilder.append(paragraphToHtml(paragraph));
            } else if (element instanceof XWPFTable) {
                XWPFTable table = (XWPFTable) element;
                htmlBuilder.append(tableToHtml(table));
            }
        }

        document.close();
        return htmlBuilder.toString();
    }

    private String paragraphToHtml(XWPFParagraph paragraph) {
        StringBuilder html = new StringBuilder("<p style='margin: 8px 0; text-align: ");

        // Alignment
        switch (paragraph.getAlignment()) {
            case CENTER:
                html.append("center");
                break;
            case RIGHT:
                html.append("right");
                break;
            case BOTH:
                html.append("justify");
                break;
            default:
                html.append("left");
        }
        html.append(";'>");

        for (XWPFRun run : paragraph.getRuns()) {
            String text = run.getText(0);
            if (text != null && !text.isEmpty()) {
                StringBuilder runHtml = new StringBuilder();

                // Apply formatting
                if (run.isBold()) runHtml.append("<strong>");
                if (run.isItalic()) runHtml.append("<em>");
                if (run.getUnderline() != org.apache.poi.xwpf.usermodel.UnderlinePatterns.NONE) {
                    runHtml.append("<u>");
                }
                if (run.isStrikeThrough()) runHtml.append("<s>");

                // Font color
                String color = run.getColor();
                if (color != null && !color.equals("auto")) {
                    runHtml.append("<span style='color: #").append(color).append(";'>");
                }

                // Font size
                int fontSize = run.getFontSize();
                if (fontSize > 0) {
                    runHtml.append("<span style='font-size: ").append(fontSize).append("pt;'>");
                }

                runHtml.append(text.replace("\n", "<br>"));

                // Close tags
                if (fontSize > 0) runHtml.append("</span>");
                if (color != null && !color.equals("auto")) runHtml.append("</span>");
                if (run.isStrikeThrough()) runHtml.append("</s>");
                if (run.getUnderline() != org.apache.poi.xwpf.usermodel.UnderlinePatterns.NONE) {
                    runHtml.append("</u>");
                }
                if (run.isItalic()) runHtml.append("</em>");
                if (run.isBold()) runHtml.append("</strong>");

                html.append(runHtml);
            }
        }

        html.append("</p>");
        return html.toString();
    }

    private String tableToHtml(XWPFTable table) {
        StringBuilder html = new StringBuilder("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse: collapse; width: 100%; margin: 16px 0;'>");

        for (XWPFTableRow row : table.getRows()) {
            html.append("<tr>");
            for (XWPFTableCell cell : row.getTableCells()) {
                html.append("<td style='border: 1px solid #ddd; padding: 8px;'>");
                html.append(cell.getText());
                html.append("</td>");
            }
            html.append("</tr>");
        }

        html.append("</table>");
        return html.toString();
    }

    private String readDocToHtml(InputStream inputStream) throws Exception {
        HWPFDocument document = new HWPFDocument(inputStream);
        WordExtractor extractor = new WordExtractor(document);

        String text = extractor.getText();
        extractor.close();
        document.close();

        // Convert plain text to HTML paragraphs
        String[] paragraphs = text.split("\n");
        StringBuilder html = new StringBuilder();

        for (String para : paragraphs) {
            if (!para.trim().isEmpty()) {
                html.append("<p>").append(para).append("</p>");
            }
        }

        return html.toString();
    }

    private String wrapHtml(String content) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0, user-scalable=yes'>" +
                "<style>" +
                "body { " +
                "  font-family: 'Segoe UI', Arial, sans-serif; " +
                "  font-size: " + currentTextSize + "px; " +
                "  line-height: 1.6; " +
                "  padding: 20px; " +
                "  color: #333; " +
                "  background-color: #ffffff; " +
                "  max-width: 800px; " +
                "  margin: 0 auto; " +
                "}" +
                "p { margin: 8px 0; }" +
                "table { margin: 16px 0; border-collapse: collapse; width: 100%; }" +
                "td, th { border: 1px solid #ddd; padding: 8px; }" +
                "strong { font-weight: bold; }" +
                "em { font-style: italic; }" +
                "u { text-decoration: underline; }" +
                "s { text-decoration: line-through; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                content +
                "</body>" +
                "</html>";
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
