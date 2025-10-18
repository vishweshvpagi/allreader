package com.example.allreader.ui.activities;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.allreader.R;
import com.example.allreader.utils.Constants;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.InputStream;

public class ImageViewerActivity extends AppCompatActivity {

    private PhotoView photoView;
    private ProgressBar progressBar;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_reader);

        photoView = findViewById(R.id.imageView);
        progressBar = findViewById(R.id.progressBar);

        // Enable zoom features
        photoView.setMaximumScale(6.0f);
        photoView.setMediumScale(3.0f);
        photoView.setMinimumScale(1.0f);

        // Add tap listener for immersive mode
        photoView.setOnPhotoTapListener((view, x, y) -> {
            if (getSupportActionBar() != null) {
                if (getSupportActionBar().isShowing()) {
                    getSupportActionBar().hide();
                } else {
                    getSupportActionBar().show();
                }
            }
        });

        // Handle both internal and external intents
        Intent intent = getIntent();
        String fileName;

        if (intent.getData() != null) {
            // Opened from external app (file manager, browser, etc.)
            imageUri = intent.getData();
            fileName = getFileNameFromUri(imageUri);
        } else {
            // Opened from within your app
            String filePath = intent.getStringExtra(Constants.EXTRA_FILE_PATH);
            fileName = intent.getStringExtra(Constants.EXTRA_FILE_NAME);

            if (filePath != null) {
                imageUri = Uri.parse(filePath);
            } else {
                Toast.makeText(this, "No image to display", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(fileName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        loadImage();
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = "Image";
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
            // Fallback: try to get filename from path
            String path = uri.getPath();
            if (path != null && path.contains("/")) {
                fileName = path.substring(path.lastIndexOf("/") + 1);
            }
        }
        return fileName;
    }

    private void loadImage() {
        if (imageUri == null) {
            Toast.makeText(this, "Invalid image URI", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                // Load image efficiently using ContentResolver
                InputStream inputStream = getContentResolver().openInputStream(imageUri);

                if (inputStream == null) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Cannot open image", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                // First pass: get image dimensions
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();

                // Calculate inSampleSize for large images
                options.inSampleSize = calculateInSampleSize(options, 2048, 2048);
                options.inJustDecodeBounds = false;

                // Second pass: decode the actual bitmap
                inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);

                if (inputStream != null) {
                    inputStream.close();
                }

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (bitmap != null) {
                        photoView.setImageBitmap(bitmap);
                    } else {
                        Toast.makeText(this, "Failed to decode image", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up bitmap to free memory
        if (photoView.getDrawable() != null) {
            photoView.setImageDrawable(null);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
