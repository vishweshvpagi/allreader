package com.example.allreader.ui.activities;



import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.allreader.R;
import com.example.allreader.utils.Constants;

public class ImageViewerActivity extends AppCompatActivity {

    private ImageView imageView;
    private String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_reader);

        imageView = findViewById(R.id.imageView);

        filePath = getIntent().getStringExtra(Constants.EXTRA_FILE_PATH);
        String fileName = getIntent().getStringExtra(Constants.EXTRA_FILE_NAME);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(fileName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        loadImage();
    }

    private void loadImage() {
        try {
            Uri uri = Uri.parse(filePath);
            imageView.setImageURI(uri);
        } catch (Exception e) {
            Toast.makeText(this, "Error loading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
