package com.example.allreader.ui.activities;

import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.allreader.R;
import com.example.allreader.utils.Constants;

public class VideoPlayerActivity extends AppCompatActivity {

    private VideoView videoView;
    private ProgressBar progressBar;
    private TextView loadingText;
    private SeekBar seekBar;
    private TextView currentTime, totalTime;
    private ImageButton btnPlayPause, btnRewind, btnFastForward, btnMute, btnFullscreen;
    private View controlsLayout;

    private Uri videoUri;
    private Handler handler = new Handler();
    private Handler hideHandler = new Handler();
    private boolean isPlaying = false;
    private boolean isMuted = false;
    private boolean isFullscreen = false;
    private int currentPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        initializeViews();

        Intent intent = getIntent();
        String fileName;

        if (intent.getData() != null) {
            videoUri = intent.getData();
            fileName = getFileNameFromUri(videoUri);
        } else {
            String filePath = intent.getStringExtra(Constants.EXTRA_FILE_PATH);
            fileName = intent.getStringExtra(Constants.EXTRA_FILE_NAME);
            if (filePath != null) videoUri = Uri.parse(filePath);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(fileName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupVideoPlayer();
        setupControls();
    }

    private void initializeViews() {
        videoView = findViewById(R.id.videoView);
        progressBar = findViewById(R.id.progressBar);
        loadingText = findViewById(R.id.loadingText);
        seekBar = findViewById(R.id.seekBar);
        currentTime = findViewById(R.id.currentTime);
        totalTime = findViewById(R.id.totalTime);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnRewind = findViewById(R.id.btnRewind);
        btnFastForward = findViewById(R.id.btnFastForward);
        btnMute = findViewById(R.id.btnMute);
        btnFullscreen = findViewById(R.id.btnFullscreen);
        controlsLayout = findViewById(R.id.controlsLayout);
    }

    private void setupVideoPlayer() {
        videoView.setVideoURI(videoUri);

        videoView.setOnPreparedListener(mp -> {
            progressBar.setVisibility(View.GONE);
            loadingText.setVisibility(View.GONE);

            int duration = videoView.getDuration();
            seekBar.setMax(duration);
            totalTime.setText(formatTime(duration));

            if (currentPosition > 0) {
                videoView.seekTo(currentPosition);
            }

            videoView.start();
            isPlaying = true;
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            updateSeekBar();
            hideControlsDelayed();
        });

        videoView.setOnCompletionListener(mp -> {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            isPlaying = false;
            controlsLayout.setVisibility(View.VISIBLE);
        });

        videoView.setOnErrorListener((mp, what, extra) -> {
            progressBar.setVisibility(View.GONE);
            loadingText.setVisibility(View.GONE);
            Toast.makeText(this, "Error playing video", Toast.LENGTH_SHORT).show();
            return true;
        });

        // Toggle controls on video tap
        videoView.setOnClickListener(v -> toggleControls());
    }

    private void setupControls() {
        btnPlayPause.setOnClickListener(v -> {
            if (isPlaying) {
                videoView.pause();
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                hideHandler.removeCallbacksAndMessages(null);
            } else {
                videoView.start();
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                hideControlsDelayed();
            }
            isPlaying = !isPlaying;
        });

        btnRewind.setOnClickListener(v -> {
            int pos = videoView.getCurrentPosition() - 10000;
            videoView.seekTo(Math.max(0, pos));
            Toast.makeText(this, "-10s", Toast.LENGTH_SHORT).show();
        });

        btnFastForward.setOnClickListener(v -> {
            int pos = videoView.getCurrentPosition() + 10000;
            videoView.seekTo(Math.min(videoView.getDuration(), pos));
            Toast.makeText(this, "+10s", Toast.LENGTH_SHORT).show();
        });

        btnMute.setOnClickListener(v -> {
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (isMuted) {
                am.setStreamVolume(AudioManager.STREAM_MUSIC,
                        am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2, 0);
                btnMute.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
            } else {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                btnMute.setImageResource(android.R.drawable.ic_lock_silent_mode);
            }
            isMuted = !isMuted;
        });

        btnFullscreen.setOnClickListener(v -> toggleFullscreen());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    videoView.seekTo(progress);
                    currentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                hideHandler.removeCallbacksAndMessages(null);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (isPlaying) {
                    hideControlsDelayed();
                }
            }
        });
    }

    private void toggleControls() {
        if (controlsLayout.getVisibility() == View.VISIBLE) {
            controlsLayout.setVisibility(View.GONE);
            hideHandler.removeCallbacksAndMessages(null);
        } else {
            controlsLayout.setVisibility(View.VISIBLE);
            if (isPlaying) {
                hideControlsDelayed();
            }
        }
    }

    private void updateSeekBar() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (videoView != null && isPlaying) {
                    int current = videoView.getCurrentPosition();
                    seekBar.setProgress(current);
                    currentTime.setText(formatTime(current));
                }
                handler.postDelayed(this, 500);
            }
        }, 0);
    }

    private void hideControlsDelayed() {
        hideHandler.removeCallbacksAndMessages(null);
        hideHandler.postDelayed(() -> {
            if (isPlaying && controlsLayout.getVisibility() == View.VISIBLE) {
                controlsLayout.setVisibility(View.GONE);
            }
        }, 4000);
    }

    private void toggleFullscreen() {
        if (isFullscreen) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (getSupportActionBar() != null) getSupportActionBar().show();
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (getSupportActionBar() != null) getSupportActionBar().hide();
        }
        isFullscreen = !isFullscreen;
    }

    private String formatTime(int millis) {
        int seconds = (millis / 1000) % 60;
        int minutes = (millis / (1000 * 60)) % 60;
        int hours = (millis / (1000 * 60 * 60));

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    private String getFileNameFromUri(Uri uri) {
        try {
            Cursor c = getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx != -1) return c.getString(idx);
            }
        } catch (Exception e) {}
        return "Video";
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView.isPlaying()) {
            currentPosition = videoView.getCurrentPosition();
            videoView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentPosition > 0) {
            videoView.seekTo(currentPosition);
            if (isPlaying) {
                videoView.start();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        hideHandler.removeCallbacksAndMessages(null);
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }
}
