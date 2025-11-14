package com.example.allreader.ui.activities;

import android.app.AlertDialog;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
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
import com.example.allreader.utils.PreferencesHelper;
import com.example.allreader.utils.RecentFilesManager;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfReaderActivity extends AppCompatActivity {

    private RecyclerView pdfRecyclerView;
    private TextView currentPageText, totalPageText;
    private ProgressBar progressBar;
    private ImageButton btnScrollDirection;

    private Uri pdfUri;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private PdfPagesAdapter adapter;
    private LinearLayoutManager layoutManager;

    private final ExecutorService renderExecutor = Executors.newSingleThreadExecutor();
    private int targetWidth;
    private boolean isHorizontalScroll = false;
    private boolean isNightMode = false;
    private boolean isFullscreen = false;
    private String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_reader);

        pdfRecyclerView = findViewById(R.id.pdfRecyclerView);
        currentPageText = findViewById(R.id.currentPageText);
        totalPageText = findViewById(R.id.totalPageText);
        progressBar = findViewById(R.id.progressBar);
        btnScrollDirection = findViewById(R.id.btnScrollDirection);

        Intent intent = getIntent();

        if (intent.getData() != null) {
            pdfUri = intent.getData();
            fileName = getFileNameFromUri(pdfUri);
        } else {
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

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        targetWidth = metrics.widthPixels;

        // Load preferences
        isNightMode = PreferencesHelper.isNightMode(this);
        isFullscreen = PreferencesHelper.isFullscreenMode(this);

        if (isFullscreen) {
            enterFullscreen();
        }

        if (isNightMode) {
            applyNightMode();
        }

        // Add to recent files
        if (pdfUri != null && fileName != null) {
            long fileSize = getFileSize(pdfUri);
            RecentFilesManager.addRecentFile(this, fileName, pdfUri.toString(), "PDF", fileSize);
        }

        btnScrollDirection.setOnClickListener(v -> toggleScrollDirection());

        setupRecyclerView();
        loadPdf();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pdf_reader, menu);

        MenuItem nightModeItem = menu.findItem(R.id.action_night_mode);
        nightModeItem.setTitle(isNightMode ? "Day Mode" : "Night Mode");

        MenuItem fullscreenItem = menu.findItem(R.id.action_fullscreen);
        fullscreenItem.setTitle(isFullscreen ? "Exit Fullscreen" : "Fullscreen");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_night_mode) {
            toggleNightMode();
            return true;
        } else if (id == R.id.action_fullscreen) {
            toggleFullscreen();
            return true;
        } else if (id == R.id.action_share) {
            shareDocument();
            return true;
        } else if (id == R.id.action_info) {
            showFileInfo();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void toggleNightMode() {
        isNightMode = !isNightMode;
        PreferencesHelper.setNightMode(this, isNightMode);
        applyNightMode();
        invalidateOptionsMenu();
    }

    private void applyNightMode() {
        if (isNightMode) {
            pdfRecyclerView.setBackgroundColor(0xFF1E1E1E);
            getWindow().getDecorView().setBackgroundColor(0xFF1E1E1E);
            Toast.makeText(this, "Night mode enabled", Toast.LENGTH_SHORT).show();
        } else {
            pdfRecyclerView.setBackgroundColor(0xFFEEEEEE);
            getWindow().getDecorView().setBackgroundColor(0xFFEEEEEE);
            Toast.makeText(this, "Day mode enabled", Toast.LENGTH_SHORT).show();
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

        btnScrollDirection.setVisibility(View.GONE);
        findViewById(R.id.pageCounterLayout).setVisibility(View.GONE);

        invalidateOptionsMenu();
        Toast.makeText(this, "Fullscreen mode", Toast.LENGTH_SHORT).show();
    }

    private void exitFullscreen() {
        isFullscreen = false;
        PreferencesHelper.setFullscreenMode(this, false);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getSupportActionBar() != null) {
            getSupportActionBar().show();
        }

        btnScrollDirection.setVisibility(View.VISIBLE);
        findViewById(R.id.pageCounterLayout).setVisibility(View.VISIBLE);

        invalidateOptionsMenu();
        Toast.makeText(this, "Fullscreen off", Toast.LENGTH_SHORT).show();
    }

    private void shareDocument() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share PDF"));
    }

    private void showFileInfo() {
        long fileSize = getFileSize(pdfUri);
        String sizeStr = formatFileSize(fileSize);

        String info = "📄 File Information\n\n" +
                "Name: " + fileName + "\n" +
                "Pages: " + (pdfRenderer != null ? pdfRenderer.getPageCount() : "N/A") + "\n" +
                "File Size: " + sizeStr + "\n" +
                "Format: PDF";

        new AlertDialog.Builder(this)
                .setTitle("Document Info")
                .setMessage(info)
                .setPositiveButton("OK", null)
                .show();
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

    private void toggleScrollDirection() {
        isHorizontalScroll = !isHorizontalScroll;

        int currentPosition = layoutManager.findFirstVisibleItemPosition();

        if (isHorizontalScroll) {
            layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
            btnScrollDirection.setImageResource(android.R.drawable.ic_menu_sort_by_size);
            Toast.makeText(this, "Horizontal scroll mode", Toast.LENGTH_SHORT).show();
        } else {
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            btnScrollDirection.setImageResource(android.R.drawable.ic_menu_view);
            Toast.makeText(this, "Vertical scroll mode", Toast.LENGTH_SHORT).show();
        }

        if (currentPosition >= 0) {
            pdfRecyclerView.scrollToPosition(currentPosition);
        }
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

    private void setupRecyclerView() {
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        pdfRecyclerView.setLayoutManager(layoutManager);
        pdfRecyclerView.setHasFixedSize(true);
        pdfRecyclerView.setItemViewCacheSize(3);

        pdfRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                int action = e.getAction();
                if (action == MotionEvent.ACTION_MOVE) {
                    rv.getParent().requestDisallowInterceptTouchEvent(true);
                }
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {}

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
        });

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

            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = parent.getHeight();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            view.setLayoutParams(params);

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

                        int renderWidth = targetWidth * 2;
                        int renderHeight = targetHeight * 2;

                        Bitmap bitmap = Bitmap.createBitmap(
                                renderWidth,
                                renderHeight,
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
            PhotoView imageView;

            public PageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.pdfPageImage);

                imageView.setMaximumScale(8.0f);
                imageView.setMediumScale(3.0f);
                imageView.setMinimumScale(1.0f);
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            }
        }
    }
}
