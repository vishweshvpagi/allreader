package com.example.allreader.ui.activities;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.example.allreader.R;
import com.example.allreader.utils.Constants;
import com.example.allreader.utils.PreferencesHelper;
import com.example.allreader.utils.RecentFilesManager;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.card.MaterialCardView;

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
    private MaterialCardView searchCard;
    private EditText searchInput;
    private TextView searchResultText;
    private ImageButton btnSearch, btnZoomIn, btnZoomOut, btnWordCount;
    private ImageButton btnPrevSearch, btnNextSearch, btnCloseSearch;
    private ImageButton btnNightMode, btnFullscreen, btnShare;
    private BottomAppBar bottomAppBar;

    private Uri wordUri;
    private String fileName;
    private String documentText = "";
    private int currentTextSize = 16;
    private int wordCount = 0;
    private int characterCount = 0;
    private int currentSearchIndex = 0;
    private int totalSearchResults = 0;
    private boolean isNightMode = false;
    private boolean isFullscreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_reader);

        initializeViews();
        setupWebView();
        setupButtons();

        Intent intent = getIntent();

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

        // Load preferences
        isNightMode = PreferencesHelper.isNightMode(this);
        isFullscreen = PreferencesHelper.isFullscreenMode(this);
        currentTextSize = PreferencesHelper.getTextSize(this);

        if (isFullscreen) {
            enterFullscreen();
        }

        // Add to recent files
        if (wordUri != null && fileName != null) {
            long fileSize = getFileSize(wordUri);
            RecentFilesManager.addRecentFile(this, fileName, wordUri.toString(),
                    fileName.endsWith(".docx") ? "DOCX" : "DOC", fileSize);
        }

        loadWordDocument(fileName);
    }

    private void initializeViews() {
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        searchCard = findViewById(R.id.searchCard);
        searchInput = findViewById(R.id.searchInput);
        searchResultText = findViewById(R.id.searchResultText);
        bottomAppBar = findViewById(R.id.bottomAppBar);

        btnSearch = findViewById(R.id.btnSearch);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        btnWordCount = findViewById(R.id.btnWordCount);
        btnPrevSearch = findViewById(R.id.btnPrevSearch);
        btnNextSearch = findViewById(R.id.btnNextSearch);
        btnCloseSearch = findViewById(R.id.btnCloseSearch);
        btnNightMode = findViewById(R.id.btnNightMode);
        btnFullscreen = findViewById(R.id.btnFullscreen);
        btnShare = findViewById(R.id.btnShare);
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
    }

    private void setupButtons() {
        btnSearch.setOnClickListener(v -> toggleSearchBar());

        btnZoomIn.setOnClickListener(v -> {
            if (currentTextSize < 32) {
                currentTextSize += 2;
                updateFontSize();
                PreferencesHelper.setTextSize(this, currentTextSize);
                Toast.makeText(this, "Text size: " + currentTextSize + "px", Toast.LENGTH_SHORT).show();
            }
        });

        btnZoomOut.setOnClickListener(v -> {
            if (currentTextSize > 10) {
                currentTextSize -= 2;
                updateFontSize();
                PreferencesHelper.setTextSize(this, currentTextSize);
                Toast.makeText(this, "Text size: " + currentTextSize + "px", Toast.LENGTH_SHORT).show();
            }
        });

        btnWordCount.setOnClickListener(v -> showDocumentInfo());

        btnCloseSearch.setOnClickListener(v -> {
            searchCard.setVisibility(View.GONE);
            webView.clearMatches();
            searchInput.setText("");
            searchResultText.setVisibility(View.GONE);
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    performSearch(s.toString());
                } else {
                    webView.clearMatches();
                    searchResultText.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnNextSearch.setOnClickListener(v -> {
            webView.findNext(true);
            if (totalSearchResults > 0) {
                currentSearchIndex = (currentSearchIndex + 1) % totalSearchResults;
                updateSearchResults();
            }
        });

        btnPrevSearch.setOnClickListener(v -> {
            webView.findNext(false);
            if (totalSearchResults > 0) {
                currentSearchIndex = (currentSearchIndex - 1 + totalSearchResults) % totalSearchResults;
                updateSearchResults();
            }
        });

        btnNightMode.setOnClickListener(v -> toggleNightMode());
        btnFullscreen.setOnClickListener(v -> toggleFullscreen());
        btnShare.setOnClickListener(v -> shareDocument());
    }

    private void performSearch(String query) {
        webView.findAllAsync(query);

        String lowerText = documentText.toLowerCase();
        String lowerQuery = query.toLowerCase();
        totalSearchResults = 0;
        currentSearchIndex = 0;

        int index = 0;
        while ((index = lowerText.indexOf(lowerQuery, index)) != -1) {
            totalSearchResults++;
            index += lowerQuery.length();
        }

        updateSearchResults();
    }

    private void updateSearchResults() {
        if (totalSearchResults > 0) {
            searchResultText.setText((currentSearchIndex + 1) + " of " + totalSearchResults);
            searchResultText.setVisibility(View.VISIBLE);
        } else {
            searchResultText.setText("No results");
            searchResultText.setVisibility(View.VISIBLE);
        }
    }

    private void toggleSearchBar() {
        if (searchCard.getVisibility() == View.GONE) {
            searchCard.setVisibility(View.VISIBLE);
            searchInput.requestFocus();
        } else {
            searchCard.setVisibility(View.GONE);
            webView.clearMatches();
            searchInput.setText("");
        }
    }

    private void updateFontSize() {
        String js = "document.body.style.fontSize='" + currentTextSize + "px';";
        webView.evaluateJavascript(js, null);
    }

    private void toggleNightMode() {
        isNightMode = !isNightMode;
        PreferencesHelper.setNightMode(this, isNightMode);
        applyNightMode();
    }

    private void applyNightMode() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            if (isNightMode) {
                WebSettingsCompat.setForceDark(webView.getSettings(),
                        WebSettingsCompat.FORCE_DARK_ON);
                getWindow().getDecorView().setBackgroundColor(0xFF1E1E1E);
                Toast.makeText(this, "Night mode ON", Toast.LENGTH_SHORT).show();
            } else {
                WebSettingsCompat.setForceDark(webView.getSettings(),
                        WebSettingsCompat.FORCE_DARK_OFF);
                getWindow().getDecorView().setBackgroundColor(0xFFFAFAFA);
                Toast.makeText(this, "Night mode OFF", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void toggleFullscreen() {
        if (isFullscreen) {
            exitFullscreen();
        } else {
            enterFullscreen();
        }
    }

    private void enterFullscreen() {
        isFullscreen = true;
        PreferencesHelper.setFullscreenMode(this, true);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        bottomAppBar.setVisibility(View.GONE);

        Toast.makeText(this, "Fullscreen ON", Toast.LENGTH_SHORT).show();
    }

    private void exitFullscreen() {
        isFullscreen = false;
        PreferencesHelper.setFullscreenMode(this, false);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getSupportActionBar() != null) {
            getSupportActionBar().show();
        }
        bottomAppBar.setVisibility(View.VISIBLE);

        Toast.makeText(this, "Fullscreen OFF", Toast.LENGTH_SHORT).show();
    }

    private void shareDocument() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        shareIntent.putExtra(Intent.EXTRA_STREAM, wordUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share Document"));
    }

    private void showDocumentInfo() {
        long fileSize = getFileSize(wordUri);
        String sizeStr = formatFileSize(fileSize);

        String info = "📄 Document Statistics\n\n" +
                "Words: " + String.format("%,d", wordCount) + "\n" +
                "Characters: " + String.format("%,d", characterCount) + "\n" +
                "Characters (no spaces): " + String.format("%,d",
                (characterCount - countSpaces(documentText))) + "\n" +
                "Paragraphs: " + countParagraphs(documentText) + "\n" +
                "File Size: " + sizeStr;

        new AlertDialog.Builder(this)
                .setTitle("Document Information")
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

    private int countParagraphs(String text) {
        return text.split("\n\n").length;
    }

    private long getFileSize(Uri uri) {
        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                long size = cursor.getLong(sizeIndex);
                cursor.close();
                return size;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
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

                documentText = htmlContent.replaceAll("<[^>]*>", "");
                String[] words = documentText.trim().split("\\s+");
                wordCount = words.length;
                characterCount = documentText.length();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                    webView.loadDataWithBaseURL(null, wrapHtml(htmlContent), "text/html", "UTF-8", null);

                    if (isNightMode) {
                        applyNightMode();
                    }
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
        StringBuilder html = new StringBuilder("<p style='margin: 12px 0; line-height: 1.8; text-align: ");

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

                if (run.isBold()) runHtml.append("<strong>");
                if (run.isItalic()) runHtml.append("<em>");
                if (run.getUnderline() != org.apache.poi.xwpf.usermodel.UnderlinePatterns.NONE) {
                    runHtml.append("<u>");
                }
                if (run.isStrikeThrough()) runHtml.append("<s>");

                String color = run.getColor();
                if (color != null && !color.equals("auto")) {
                    runHtml.append("<span style='color: #").append(color).append(";'>");
                }

                int fontSize = run.getFontSize();
                if (fontSize > 0) {
                    runHtml.append("<span style='font-size: ").append(fontSize).append("pt;'>");
                }

                runHtml.append(text.replace("\n", "<br>"));

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
        StringBuilder html = new StringBuilder("<table style='border-collapse: collapse; width: 100%; margin: 20px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>");

        for (int i = 0; i < table.getRows().size(); i++) {
            XWPFTableRow row = table.getRows().get(i);
            html.append("<tr>");
            for (XWPFTableCell cell : row.getTableCells()) {
                String bgColor = i == 0 ? "#F5F5F5" : "#FFFFFF";
                String fontWeight = i == 0 ? "bold" : "normal";
                html.append("<td style='border: 1px solid #E0E0E0; padding: 12px; background-color: ")
                        .append(bgColor)
                        .append("; font-weight: ")
                        .append(fontWeight)
                        .append(";'>");
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

        String[] paragraphs = text.split("\n");
        StringBuilder html = new StringBuilder();

        for (String para : paragraphs) {
            if (!para.trim().isEmpty()) {
                html.append("<p style='margin: 12px 0; line-height: 1.8;'>").append(para).append("</p>");
            }
        }

        return html.toString();
    }

    private String wrapHtml(String content) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes'>" +
                "<style>" +
                "* { -webkit-tap-highlight-color: transparent; }" +
                "body { " +
                "  font-family: 'Segoe UI', 'Roboto', Arial, sans-serif; " +
                "  font-size: " + currentTextSize + "px; " +
                "  line-height: 1.8; " +
                "  padding: 24px 20px; " +
                "  color: #212121; " +
                "  background-color: #FFFFFF; " +
                "  max-width: 800px; " +
                "  margin: 0 auto; " +
                "  -webkit-font-smoothing: antialiased; " +
                "  -moz-osx-font-smoothing: grayscale; " +
                "}" +
                "p { margin: 12px 0; word-wrap: break-word; }" +
                "table { margin: 20px 0; border-collapse: collapse; width: 100%; }" +
                "td, th { border: 1px solid #E0E0E0; padding: 12px; }" +
                "strong { font-weight: 600; }" +
                "em { font-style: italic; }" +
                "u { text-decoration: underline; }" +
                "s { text-decoration: line-through; }" +
                "::selection { background-color: #B3E5FC; }" +
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
