package com.example.allreader.ui.activities;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.allreader.R;
import com.example.allreader.utils.Constants;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TxtReaderActivity extends AppCompatActivity {

    private TextView txtContent, txtSize;
    private ProgressBar progressBar;
    private Button btnZoomIn, btnZoomOut, btnInfo;

    private Uri txtUri;
    private String content = "";
    private int currentTextSize = 16;
    private int lineCount = 0;
    private int wordCount = 0;
    private int charCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_txt_reader);

        txtContent = findViewById(R.id.txtContent);
        progressBar = findViewById(R.id.progressBar);
        txtSize = findViewById(R.id.txtSize);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        btnInfo = findViewById(R.id.btnInfo);

        // Handle both internal and external intents
        Intent intent = getIntent();
        String fileName;

        if (intent.getData() != null) {
            txtUri = intent.getData();
            fileName = getFileNameFromUri(txtUri);
        } else {
            String filePath = intent.getStringExtra(Constants.EXTRA_FILE_PATH);
            fileName = intent.getStringExtra(Constants.EXTRA_FILE_NAME);

            if (filePath != null) {
                txtUri = Uri.parse(filePath);
            } else {
                Toast.makeText(this, "No text file to open", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(fileName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupButtons();
        loadTextFile();
    }

    private void setupButtons() {
        btnZoomIn.setOnClickListener(v -> {
            if (currentTextSize < 32) {
                currentTextSize += 2;
                updateTextSize();
            }
        });

        btnZoomOut.setOnClickListener(v -> {
            if (currentTextSize > 10) {
                currentTextSize -= 2;
                updateTextSize();
            }
        });

        btnInfo.setOnClickListener(v -> showFileInfo());
    }

    private void updateTextSize() {
        txtContent.setTextSize(currentTextSize);
        txtSize.setText("Text Size: " + currentTextSize + "sp");
    }

    private void showFileInfo() {
        String info = "📄 Text File Statistics\n\n" +
                "Lines: " + lineCount + "\n" +
                "Words: " + wordCount + "\n" +
                "Characters: " + charCount + "\n" +
                "Characters (no spaces): " + (charCount - countSpaces());

        new AlertDialog.Builder(this)
                .setTitle("File Information")
                .setMessage(info)
                .setPositiveButton("OK", null)
                .show();
    }

    private int countSpaces() {
        int count = 0;
        for (char c : content.toCharArray()) {
            if (Character.isWhitespace(c)) count++;
        }
        return count;
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = "Document.txt";
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

    private void loadTextFile() {
        if (txtUri == null) {
            Toast.makeText(this, "Invalid text file", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        txtContent.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(txtUri);
                if (inputStream == null) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Cannot open text file", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder sb = new StringBuilder();
                String line;
                lineCount = 0;

                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                    lineCount++;
                }

                reader.close();
                inputStream.close();

                content = sb.toString();

                // Calculate statistics
                charCount = content.length();
                if (!content.trim().isEmpty()) {
                    wordCount = content.trim().split("\\s+").length;
                }

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    txtContent.setVisibility(View.VISIBLE);
                    txtContent.setText(content);
                    updateTextSize();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading text file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
