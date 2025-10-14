package com.example.allreader.ui.activities;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.allreader.R;
import com.example.allreader.utils.Constants;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class PdfReaderActivity extends AppCompatActivity {

    private RecyclerView pdfRecyclerView;
    private TextView currentPageText, totalPageText;
    private ProgressBar progressBar;
    private String filePath;
    private PdfRenderer pdfRenderer;
    private PdfPagesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_reader);

        pdfRecyclerView = findViewById(R.id.pdfRecyclerView);
        currentPageText = findViewById(R.id.currentPageText);
        totalPageText = findViewById(R.id.totalPageText);
        progressBar = findViewById(R.id.progressBar);

        filePath = getIntent().getStringExtra(Constants.EXTRA_FILE_PATH);
        String fileName = getIntent().getStringExtra(Constants.EXTRA_FILE_NAME);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(fileName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupRecyclerView();
        loadPdf();
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        pdfRecyclerView.setLayoutManager(layoutManager);

        pdfRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int position = layoutManager.findFirstVisibleItemPosition();
                if (position >= 0) {
                    currentPageText.setText(String.valueOf(position + 1));
                }
            }
        });
    }

    private void loadPdf() {
        new Thread(() -> {
            try {
                Uri uri = Uri.parse(filePath);

                // Copy PDF to cache
                File cacheFile = new File(getCacheDir(), "temp.pdf");
                InputStream inputStream = getContentResolver().openInputStream(uri);
                FileOutputStream outputStream = new FileOutputStream(cacheFile);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                inputStream.close();
                outputStream.close();

                // Open PDF
                ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(
                        cacheFile,
                        ParcelFileDescriptor.MODE_READ_ONLY
                );

                pdfRenderer = new PdfRenderer(fileDescriptor);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    totalPageText.setText(String.valueOf(pdfRenderer.getPageCount()));
                    currentPageText.setText("1");

                    adapter = new PdfPagesAdapter(pdfRenderer);
                    pdfRecyclerView.setAdapter(adapter);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pdfRenderer != null) {
            pdfRenderer.close();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // RecyclerView Adapter
    private static class PdfPagesAdapter extends RecyclerView.Adapter<PdfPagesAdapter.PageViewHolder> {

        private final PdfRenderer pdfRenderer;

        public PdfPagesAdapter(PdfRenderer pdfRenderer) {
            this.pdfRenderer = pdfRenderer;
        }

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pdf_page, parent, false);
            return new PageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            PdfRenderer.Page page = pdfRenderer.openPage(position);

            // Create bitmap with higher resolution
            int width = page.getWidth() * 2;
            int height = page.getHeight() * 2;
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            // FIXED: Fill with white background
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            canvas.drawColor(android.graphics.Color.WHITE);
            canvas.drawBitmap(bitmap, 0, 0, null);

            // Render page
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            holder.photoView.setImageBitmap(bitmap);

            page.close();
        }


        @Override
        public int getItemCount() {
            return pdfRenderer.getPageCount();
        }

        static class PageViewHolder extends RecyclerView.ViewHolder {
            PhotoView photoView;

            public PageViewHolder(@NonNull View itemView) {
                super(itemView);
                photoView = itemView.findViewById(R.id.pdfPageImage);
            }
        }
    }
}
