package com.example.allreader.ui.activities;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.PrecomputedTextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.allreader.R;
import com.example.allreader.utils.Constants;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TxtReaderActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView txtSize;
    private Button btnZoomIn, btnZoomOut, btnInfo;

    private Uri txtUri;
    private TextAdapter adapter;
    private List<String> paragraphs = new ArrayList<>();
    private int currentTextSize = 16;
    private int lineCount = 0;
    private int wordCount = 0;
    private int charCount = 0;
    private Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_txt_reader);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        txtSize = findViewById(R.id.txtSize);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        btnInfo = findViewById(R.id.btnInfo);

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

        setupRecyclerView();
        setupButtons();
        loadTextFile();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TextAdapter(paragraphs, currentTextSize);
        recyclerView.setAdapter(adapter);
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
        adapter.setTextSize(currentTextSize);
        adapter.notifyDataSetChanged();
        txtSize.setText("Text Size: " + currentTextSize + "sp");
    }

    private void showFileInfo() {
        String info = "📄 Text File Statistics\n\n" +
                "Paragraphs: " + paragraphs.size() + "\n" +
                "Lines: " + lineCount + "\n" +
                "Words: " + wordCount + "\n" +
                "Characters: " + charCount;

        new AlertDialog.Builder(this)
                .setTitle("File Information")
                .setMessage(info)
                .setPositiveButton("OK", null)
                .show();
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
        recyclerView.setVisibility(View.GONE);

        executor.execute(() -> {
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

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream), 8192);
                StringBuilder currentParagraph = new StringBuilder();
                String line;
                lineCount = 0;
                wordCount = 0;
                charCount = 0;

                int batchSize = 10; // Load paragraphs in batches
                List<String> batch = new ArrayList<>();

                while ((line = reader.readLine()) != null) {
                    lineCount++;
                    charCount += line.length() + 1; // +1 for newline

                    if (!line.trim().isEmpty()) {
                        String[] words = line.trim().split("\\s+");
                        wordCount += words.length;
                    }

                    // Create paragraphs by grouping lines
                    if (line.trim().isEmpty()) {
                        // Empty line indicates paragraph break
                        if (currentParagraph.length() > 0) {
                            String paragraph = currentParagraph.toString();
                            batch.add(paragraph);

                            // Add batch to main list and update UI
                            if (batch.size() >= batchSize) {
                                List<String> finalBatch = new ArrayList<>(batch);
                                runOnUiThread(() -> {
                                    int startPos = paragraphs.size();
                                    paragraphs.addAll(finalBatch);
                                    adapter.notifyItemRangeInserted(startPos, finalBatch.size());

                                    // Show RecyclerView after first batch
                                    if (startPos == 0) {
                                        progressBar.setVisibility(View.GONE);
                                        recyclerView.setVisibility(View.VISIBLE);
                                    }
                                });
                                batch.clear();
                            }

                            currentParagraph = new StringBuilder();
                        }
                    } else {
                        currentParagraph.append(line).append("\n");
                    }
                }

                // Add remaining paragraph
                if (currentParagraph.length() > 0) {
                    batch.add(currentParagraph.toString());
                }

                // Add final batch
                if (!batch.isEmpty()) {
                    List<String> finalBatch = new ArrayList<>(batch);
                    runOnUiThread(() -> {
                        int startPos = paragraphs.size();
                        paragraphs.addAll(finalBatch);
                        adapter.notifyItemRangeInserted(startPos, finalBatch.size());

                        if (startPos == 0) {
                            progressBar.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                    });
                }

                reader.close();
                inputStream.close();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    updateTextSize();

                    // If no paragraphs were added (single block of text), handle it
                    if (paragraphs.isEmpty()) {
                        Toast.makeText(this, "Empty file or unsupported format", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading text file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        paragraphs.clear();
    }

    // RecyclerView Adapter with PrecomputedText
    private class TextAdapter extends RecyclerView.Adapter<TextAdapter.TextViewHolder> {
        private List<String> items;
        private int textSize;
        private Executor precomputeExecutor = Executors.newSingleThreadExecutor();

        TextAdapter(List<String> items, int textSize) {
            this.items = items;
            this.textSize = textSize;
        }

        void setTextSize(int size) {
            this.textSize = size;
        }

        @NonNull
        @Override
        public TextViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_text_paragraph, parent, false);
            return new TextViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TextViewHolder holder, int position) {
            String text = items.get(position);
            holder.textView.setTextSize(textSize);

            // Use PrecomputedText for better performance
            PrecomputedTextCompat.Params params = TextViewCompat.getTextMetricsParams(holder.textView);

            precomputeExecutor.execute(() -> {
                PrecomputedTextCompat precomputedText = PrecomputedTextCompat.create(text, params);
                holder.textView.post(() -> {
                    TextViewCompat.setPrecomputedText(holder.textView, precomputedText);
                });
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class TextViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            TextViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.paragraphText);
            }
        }
    }
}
