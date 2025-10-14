package com.example.allreader.ui.activities;
//
//import android.net.Uri;
//import android.os.Bundle;
//import android.webkit.WebView;
//import android.webkit.WebSettings;
//import android.widget.Toast;
//import androidx.appcompat.app.AppCompatActivity;
//import com.example.allreader.R;
//import com.example.allreader.utils.Constants;
//import java.io.InputStream;
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import nl.siegmann.epublib.domain.Book;
//import nl.siegmann.epublib.epub.EpubReader;

public class EpubReaderActivity  {

//    private WebView webView;
//    private String filePath;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_epub_reader);
//
//        webView = findViewById(R.id.webView);
//
//        // Configure WebView settings
//        WebSettings webSettings = webView.getSettings();
//        webSettings.setJavaScriptEnabled(true);
//        webSettings.setDomStorageEnabled(true);
//        webSettings.setAllowFileAccess(true);
//        webSettings.setBuiltInZoomControls(true);
//        webSettings.setDisplayZoomControls(false);
//        webSettings.setLoadWithOverviewMode(true);
//        webSettings.setUseWideViewPort(true);
//
//        filePath = getIntent().getStringExtra(Constants.EXTRA_FILE_PATH);
//        String fileName = getIntent().getStringExtra(Constants.EXTRA_FILE_NAME);
//
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setTitle(fileName);
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        }
//
//        loadEpub();
//    }
//
//    private void loadEpub() {
//        try {
//            Uri uri = Uri.parse(filePath);
//            InputStream epubInputStream = getContentResolver().openInputStream(uri);
//
//            EpubReader epubReader = new EpubReader();
//            Book book = epubReader.readEpub(epubInputStream);
//
//            // Get the first spine reference (actual content)
//            if (book.getSpine().getSpineReferences().size() > 0) {
//                InputStream is = book.getSpine().getSpineReferences().get(0)
//                        .getResource().getInputStream();
//
//                // Read the content
//                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//                StringBuilder htmlContent = new StringBuilder();
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    htmlContent.append(line).append("\n");
//                }
//                reader.close();
//
//                // Load with proper base URL for resource loading
//                webView.loadDataWithBaseURL("file:///android_asset/",
//                        htmlContent.toString(),
//                        "text/html",
//                        "UTF-8",
//                        null);
//            } else {
//                Toast.makeText(this, "EPUB has no readable content", Toast.LENGTH_SHORT).show();
//                finish();
//            }
//
//        } catch (Exception e) {
//            Toast.makeText(this, "Error loading EPUB: " + e.getMessage(), Toast.LENGTH_LONG).show();
//            e.printStackTrace();
//            finish();
//        }
//    }
//
//    @Override
//    public boolean onSupportNavigateUp() {
//        onBackPressed();
//        return true;
//    }
}
