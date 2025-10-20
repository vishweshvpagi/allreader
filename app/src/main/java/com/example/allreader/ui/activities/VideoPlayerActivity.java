package com.example.allreader.ui.activities;

import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.allreader.R;
import com.example.allreader.utils.Constants;

public class VideoPlayerActivity extends AppCompatActivity {

    private VideoView videoView;
    private ProgressBar progressBar, bufferingProgress;
    private TextView loadingText, speedIndicator, brightnessIndicator, volumeIndicator;
    private SeekBar seekBar;
    private TextView currentTime, totalTime;
    private ImageButton btnPlayPause, btnRewind, btnFastForward, btnMute, btnFullscreen,
            btnSpeed, btnAspectRatio, btnRotate, btnSubtitles, btnLoop;
    private View controlsLayout;

    private Uri videoUri;
    private Handler handler = new Handler();
    private Handler hideHandler = new Handler();
    private boolean isPlaying = false;
    private boolean isMuted = false;
    private boolean isFullscreen = false;
    private boolean isLooping = false;
    private int currentPosition = 0;
    private float playbackSpeed = 1.0f;
    private int aspectRatioIndex = 0;
    private int rotationAngle = 0;

    private AudioManager audioManager;
    private GestureDetector gestureDetector;
    private float brightness = -1;
    private int maxVolume, currentVolume;

    private static final String[] SPEED_OPTIONS = {"0.25x", "0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "1.75x", "2.0x"};
    private static final float[] SPEED_VALUES = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
    private static final String[] ASPECT_RATIOS = {"Default", "16:9", "4:3", "Fill", "Zoom"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        initializeViews();
        setupAudioManager();
        setupGestureDetector();

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
        bufferingProgress = findViewById(R.id.bufferingProgress);
        loadingText = findViewById(R.id.loadingText);
        speedIndicator = findViewById(R.id.speedIndicator);
        brightnessIndicator = findViewById(R.id.brightnessIndicator);
        volumeIndicator = findViewById(R.id.volumeIndicator);
        seekBar = findViewById(R.id.seekBar);
        currentTime = findViewById(R.id.currentTime);
        totalTime = findViewById(R.id.totalTime);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnRewind = findViewById(R.id.btnRewind);
        btnFastForward = findViewById(R.id.btnFastForward);
        btnMute = findViewById(R.id.btnMute);
        btnFullscreen = findViewById(R.id.btnFullscreen);
        btnSpeed = findViewById(R.id.btnSpeed);
        btnAspectRatio = findViewById(R.id.btnAspectRatio);
        btnRotate = findViewById(R.id.btnRotate);
        btnSubtitles = findViewById(R.id.btnSubtitles);
        btnLoop = findViewById(R.id.btnLoop);
        controlsLayout = findViewById(R.id.controlsLayout);
    }

    private void setupAudioManager() {
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                float x = e.getX();
                int screenWidth = videoView.getWidth();

                if (x < screenWidth / 3) {
                    seekBackward(10000);
                } else if (x > 2 * screenWidth / 3) {
                    seekForward(10000);
                } else {
                    togglePlayPause();
                }
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                float x1 = e1.getX();
                float screenWidth = videoView.getWidth();
                float deltaY = e1.getY() - e2.getY();

                if (Math.abs(distanceY) > Math.abs(distanceX)) {
                    if (x1 < screenWidth / 2) {
                        adjustBrightness(deltaY);
                    } else {
                        adjustVolume(deltaY);
                    }
                }
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleControls();
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true; // MUST return true to receive further events
            }
        });

        videoView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true; // Consume the event
        });
    }

    private void setupVideoPlayer() {
        // HIDE CONTROLS INITIALLY
        controlsLayout.setVisibility(View.GONE);

        videoView.setVideoURI(videoUri);

        videoView.setOnPreparedListener(mp -> {
            progressBar.setVisibility(View.GONE);
            loadingText.setVisibility(View.GONE);

            // Set playback speed
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                mp.setPlaybackParams(mp.getPlaybackParams().setSpeed(playbackSpeed));
            }

            // Set looping
            mp.setLooping(isLooping);

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

            // DON'T show controls automatically - wait for user tap
        });

        videoView.setOnCompletionListener(mp -> {
            if (!isLooping) {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                isPlaying = false;
                controlsLayout.setVisibility(View.VISIBLE); // Show controls when video ends
                seekBar.setProgress(0);
            }
        });

        videoView.setOnInfoListener((mp, what, extra) -> {
            if (what == android.media.MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                bufferingProgress.setVisibility(View.VISIBLE);
            } else if (what == android.media.MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                bufferingProgress.setVisibility(View.GONE);
            }
            return false;
        });

        videoView.setOnErrorListener((mp, what, extra) -> {
            progressBar.setVisibility(View.GONE);
            loadingText.setVisibility(View.GONE);
            bufferingProgress.setVisibility(View.GONE);
            Toast.makeText(this, "Error playing video", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private void setupControls() {
        btnPlayPause.setOnClickListener(v -> togglePlayPause());

        btnRewind.setOnClickListener(v -> seekBackward(10000));

        btnFastForward.setOnClickListener(v -> seekForward(10000));

        btnMute.setOnClickListener(v -> toggleMute());

        btnFullscreen.setOnClickListener(v -> toggleFullscreen());

        btnSpeed.setOnClickListener(v -> showSpeedDialog());

        btnAspectRatio.setOnClickListener(v -> showAspectRatioMenu());

        btnRotate.setOnClickListener(v -> rotateVideo());

        btnSubtitles.setOnClickListener(v -> {
            Toast.makeText(this, "Subtitle support - Select subtitle file from storage", Toast.LENGTH_SHORT).show();
            // You can implement subtitle file picker here
        });

        btnLoop.setOnClickListener(v -> toggleLoop());

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

        // Long press for faster seeking
        btnRewind.setOnLongClickListener(v -> {
            seekBackward(30000);
            return true;
        });

        btnFastForward.setOnLongClickListener(v -> {
            seekForward(30000);
            return true;
        });
    }

    private void togglePlayPause() {
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
    }

    private void seekBackward(int millis) {
        int pos = videoView.getCurrentPosition() - millis;
        videoView.seekTo(Math.max(0, pos));
        showSeekIndicator("-" + (millis / 1000) + "s");
    }

    private void seekForward(int millis) {
        int pos = videoView.getCurrentPosition() + millis;
        videoView.seekTo(Math.min(videoView.getDuration(), pos));
        showSeekIndicator("+" + (millis / 1000) + "s");
    }

    private void showSeekIndicator(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void toggleMute() {
        if (isMuted) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
            btnMute.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
            Toast.makeText(this, "Unmuted", Toast.LENGTH_SHORT).show();
        } else {
            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            btnMute.setImageResource(android.R.drawable.ic_lock_silent_mode);
            Toast.makeText(this, "Muted", Toast.LENGTH_SHORT).show();
        }
        isMuted = !isMuted;
    }

    private void toggleLoop() {
        isLooping = !isLooping;
        if (isLooping) {
            btnLoop.setAlpha(1.0f);
            Toast.makeText(this, "Loop enabled", Toast.LENGTH_SHORT).show();
        } else {
            btnLoop.setAlpha(0.5f);
            Toast.makeText(this, "Loop disabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSpeedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Playback Speed");

        int currentSpeedIndex = 3; // 1.0x default
        for (int i = 0; i < SPEED_VALUES.length; i++) {
            if (SPEED_VALUES[i] == playbackSpeed) {
                currentSpeedIndex = i;
                break;
            }
        }

        builder.setSingleChoiceItems(SPEED_OPTIONS, currentSpeedIndex, (dialog, which) -> {
            playbackSpeed = SPEED_VALUES[which];
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (videoView.isPlaying()) {
                    videoView.pause();
                    int pos = videoView.getCurrentPosition();
                    videoView.setVideoURI(videoUri);
                    videoView.setOnPreparedListener(mp -> {
                        mp.setPlaybackParams(mp.getPlaybackParams().setSpeed(playbackSpeed));
                        videoView.seekTo(pos);
                        videoView.start();
                        showSpeedIndicator(SPEED_OPTIONS[which]);
                    });
                }
            } else {
                Toast.makeText(this, "Speed control requires Android 6.0+", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showSpeedIndicator(String speed) {
        speedIndicator.setText("Speed: " + speed);
        speedIndicator.setVisibility(View.VISIBLE);
        new Handler().postDelayed(() -> speedIndicator.setVisibility(View.GONE), 1500);
    }

    private void showAspectRatioMenu() {
        PopupMenu popup = new PopupMenu(this, btnAspectRatio);
        for (int i = 0; i < ASPECT_RATIOS.length; i++) {
            popup.getMenu().add(0, i, i, ASPECT_RATIOS[i]);
        }

        popup.setOnMenuItemClickListener(item -> {
            aspectRatioIndex = item.getItemId();
            applyAspectRatio(aspectRatioIndex);
            return true;
        });

        popup.show();
    }

    private void applyAspectRatio(int index) {
        // Note: VideoView has limited aspect ratio control
        // For full control, consider using ExoPlayer or custom SurfaceView
        Toast.makeText(this, "Aspect Ratio: " + ASPECT_RATIOS[index], Toast.LENGTH_SHORT).show();
    }

    private void rotateVideo() {
        rotationAngle = (rotationAngle + 90) % 360;
        videoView.setRotation(rotationAngle);
        Toast.makeText(this, "Rotated " + rotationAngle + "°", Toast.LENGTH_SHORT).show();
    }

    private void adjustBrightness(float delta) {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();

        if (brightness < 0) {
            brightness = layoutParams.screenBrightness;
        }

        brightness += delta / 500f;
        brightness = Math.max(0.01f, Math.min(brightness, 1.0f));

        layoutParams.screenBrightness = brightness;
        getWindow().setAttributes(layoutParams);

        int percent = (int) (brightness * 100);
        brightnessIndicator.setText("☀ " + percent + "%");
        brightnessIndicator.setVisibility(View.VISIBLE);

        hideHandler.removeCallbacksAndMessages(null);
        hideHandler.postDelayed(() -> brightnessIndicator.setVisibility(View.GONE), 1000);
    }

    private void adjustVolume(float delta) {
        int change = (int) (delta / 50f);
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        currentVolume = Math.max(0, Math.min(currentVolume + change, maxVolume));

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);

        int percent = (currentVolume * 100) / maxVolume;
        volumeIndicator.setText("🔊 " + percent + "%");
        volumeIndicator.setVisibility(View.VISIBLE);

        hideHandler.removeCallbacksAndMessages(null);
        hideHandler.postDelayed(() -> volumeIndicator.setVisibility(View.GONE), 1000);
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
                if (idx != -1) {
                    String name = c.getString(idx);
                    c.close();
                    return name;
                }
                c.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    @Override
    public void onBackPressed() {
        if (isFullscreen) {
            toggleFullscreen();
        } else {
            super.onBackPressed();
        }
    }
}
