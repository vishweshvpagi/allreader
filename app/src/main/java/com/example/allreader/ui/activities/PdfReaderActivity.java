package com.example.allreader.ui.activities;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.allreader.R;
import com.example.allreader.utils.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfReaderActivity extends AppCompatActivity {

    private RecyclerView pdfRecyclerView;
    private TextView currentPageText, totalPageText;
    private ProgressBar progressBar;

    private Uri pdfUri;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private PdfPagesAdapter adapter;

    private final ExecutorService renderExecutor = Executors.newSingleThreadExecutor();
    private int targetWidth;

    // Zoom variables
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 5.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_reader);

        pdfRecyclerView = findViewById(R.id.pdfRecyclerView);
        currentPageText = findViewById(R.id.currentPageText);
        totalPageText = findViewById(R.id.totalPageText);
        progressBar = findViewById(R.id.progressBar);

        // Handle both internal and external intents
        Intent intent = getIntent();
        String fileName;

        if (intent.getData() != null) {
            // Opened from external app (file manager, browser, etc.)
            pdfUri = intent.getData();
            fileName = getFileNameFromUri(pdfUri);
        } else {
            // Opened from within your app
            String filePath = intent.getStringExtra(Constants.EXTRA_FILE_PATH);
            fileName = intent.getStringExtra(Constants.EXTRA_FILE_NAME);

            if (filePath != null) {
                pdfUri = Uri.parse(filePath);
            } else {
                Toast.makeText(this, "No PDF to display", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(fileName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Calculate target width based on screen
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        targetWidth = metrics.widthPixels;

        // Initialize scale gesture detector
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        setupRecyclerView();
        loadPdf();
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = "Document.pdf";
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
            String path = uri.getPath();
            if (path != null && path.contains("/")) {
                fileName = path.substring(path.lastIndexOf("/") + 1);
            }
        }
        return fileName;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(MIN_ZOOM, Math.min(scaleFactor, MAX_ZOOM));

            pdfRecyclerView.setScaleX(scaleFactor);
            pdfRecyclerView.setScaleY(scaleFactor);

            return true;
        }
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        pdfRecyclerView.setLayoutManager(layoutManager);
        pdfRecyclerView.setHasFixedSize(true);
        pdfRecyclerView.setItemViewCacheSize(3);

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
        if (pdfUri == null) {
            Toast.makeText(this, "Invalid PDF URI", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        renderExecutor.execute(() -> {
            try {
                // Copy PDF to cache file using ContentResolver
                File cacheFile = new File(getCacheDir(), "temp_pdf_" + System.currentTimeMillis() + ".pdf");

                InputStream inputStream = getContentResolver().openInputStream(pdfUri);
                if (inputStream == null) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Cannot open PDF file", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                try (FileOutputStream outputStream = new FileOutputStream(cacheFile)) {
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                }
                inputStream.close();

                // Open PDF with PdfRenderer
                parcelFileDescriptor = ParcelFileDescriptor.open(
                        cacheFile,
                        ParcelFileDescriptor.MODE_READ_ONLY
                );

                pdfRenderer = new PdfRenderer(parcelFileDescriptor);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    totalPageText.setText(String.valueOf(pdfRenderer.getPageCount()));
                    currentPageText.setText("1");

                    adapter = new PdfPagesAdapter(pdfRenderer, renderExecutor, targetWidth);
                    pdfRecyclerView.setAdapter(adapter);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        renderExecutor.shutdownNow();

        if (pdfRenderer != null) {
            try {
                pdfRenderer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (parcelFileDescriptor != null) {
            try {
                parcelFileDescriptor.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // RecyclerView Adapter with Bitmap Caching
    private static class PdfPagesAdapter extends RecyclerView.Adapter<PdfPagesAdapter.PageViewHolder> {

        private final PdfRenderer pdfRenderer;
        private final ExecutorService executor;
        private final int targetWidth;
        private final LruCache<Integer, Bitmap> bitmapCache;

        public PdfPagesAdapter(PdfRenderer pdfRenderer, ExecutorService executor, int targetWidth) {
            this.pdfRenderer = pdfRenderer;
            this.executor = executor;
            this.targetWidth = targetWidth;

            int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
            int cacheSize = maxMemory / 4;

            bitmapCache = new LruCache<Integer, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(Integer key, Bitmap bitmap) {
                    return bitmap.getByteCount() / 1024;
                }
            };

            setHasStableIds(true);
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
            Bitmap cachedBitmap = bitmapCache.get(position);

            if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
                holder.imageView.setImageBitmap(cachedBitmap);
                return;
            }

            holder.imageView.setImageDrawable(null);

            executor.execute(() -> {
                try {
                    synchronized (pdfRenderer) {
                        PdfRenderer.Page page = pdfRenderer.openPage(position);

                        float aspectRatio = (float) page.getHeight() / (float) page.getWidth();
                        int targetHeight = (int) (targetWidth * aspectRatio);

                        Bitmap bitmap = Bitmap.createBitmap(
                                targetWidth,
                                targetHeight,
                                Bitmap.Config.ARGB_8888
                        );

                        Canvas canvas = new Canvas(bitmap);
                        canvas.drawColor(Color.WHITE);

                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        page.close();

                        bitmapCache.put(position, bitmap);

                        holder.itemView.post(() -> {
                            if (holder.getBindingAdapterPosition() == position) {
                                holder.imageView.setImageBitmap(bitmap);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        @Override
        public int getItemCount() {
            return pdfRenderer.getPageCount();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        static class PageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            public PageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.pdfPageImage);
            }
        }
    }
}
