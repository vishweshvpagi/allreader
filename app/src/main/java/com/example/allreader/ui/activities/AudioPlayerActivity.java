package com.example.allreader.ui.activities;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.ContentDataSource;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import com.example.allreader.R;
import java.io.File;
import java.util.Formatter;
import java.util.Locale;

public class AudioPlayerActivity extends AppCompatActivity {
    private static final String TAG = "AudioPlayerActivity";
    private ExoPlayer exoPlayer;
    private ImageButton playPauseBtn, stopBtn, rewindBtn, forwardBtn;
    private SeekBar seekBar;
    private TextView currentTimeText, totalTimeText, songNameText;
    private boolean isPlaying = false;
    private Uri audioUri;
    private Runnable updateSeekBarRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        try {
            // Get URI from intent
            audioUri = getIntent().getData();
            Log.d(TAG, "URI from getData: " + audioUri);

            if (audioUri == null) {
                String uriString = getIntent().getStringExtra("AUDIO_URI");
                Log.d(TAG, "URI from extra: " + uriString);
                if (uriString != null) {
                    audioUri = Uri.parse(uriString);
                }
            }

            if (audioUri == null) {
                String audioPath = getIntent().getStringExtra("AUDIO_PATH");
                Log.d(TAG, "Path from extra: " + audioPath);
                if (audioPath != null) {
                    File file = new File(audioPath);
                    if (file.exists()) {
                        audioUri = Uri.fromFile(file);
                    }
                }
            }

            if (audioUri == null) {
                Log.e(TAG, "No audio URI found!");
                Toast.makeText(this, "No audio file provided", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Log.d(TAG, "Final URI: " + audioUri);

            initializeViews();
            setupExoPlayer();
            setupControls();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            finish();
        }
    }

    private void initializeViews() {
        playPauseBtn = findViewById(R.id.playPauseBtn);
        stopBtn = findViewById(R.id.stopBtn);
        rewindBtn = findViewById(R.id.rewindBtn);
        forwardBtn = findViewById(R.id.forwardBtn);
        seekBar = findViewById(R.id.seekBar);
        currentTimeText = findViewById(R.id.currentTimeText);
        totalTimeText = findViewById(R.id.totalTimeText);
        songNameText = findViewById(R.id.songNameText);

        String songName = getSongName();
        songNameText.setText(songName);
        Log.d(TAG, "Song name: " + songName);
    }

    private String getSongName() {
        try {
            String[] projection = {OpenableColumns.DISPLAY_NAME};
            Cursor cursor = getContentResolver().query(audioUri, projection, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
                String name = cursor.getString(nameIndex);
                cursor.close();
                return name;
            }
            if (cursor != null) cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "Error getting song name: " + e.getMessage());
        }

        String path = audioUri.getLastPathSegment();
        return path != null ? path : "Unknown";
    }

    @OptIn(markerClass = UnstableApi.class) private void setupExoPlayer() {
        try {
            Log.d(TAG, "Setting up ExoPlayer...");

            // Create ExoPlayer with ContentDataSource for handling content:// URIs
            DataSource.Factory dataSourceFactory = () -> new ContentDataSource(this);

            exoPlayer = new ExoPlayer.Builder(this)
                    .setMediaSourceFactory(
                            new ProgressiveMediaSource.Factory(dataSourceFactory)
                    )
                    .build();

            MediaItem mediaItem = MediaItem.fromUri(audioUri);
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();

            Log.d(TAG, "ExoPlayer prepared");

            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    Log.d(TAG, "Playback state changed: " + state);
                    if (state == Player.STATE_READY) {
                        long duration = exoPlayer.getDuration();
                        Log.d(TAG, "Duration: " + duration);
                        if (duration > 0) {
                            seekBar.setMax((int) duration);
                            totalTimeText.setText(formatTime(duration));
                            Toast.makeText(AudioPlayerActivity.this, "Ready to play", Toast.LENGTH_SHORT).show();
                        }
                    } else if (state == Player.STATE_ENDED) {
                        isPlaying = false;
                        playPauseBtn.setImageResource(android.R.drawable.ic_media_play);
                        seekBar.setProgress(0);
                        currentTimeText.setText("00:00");
                    } else if (state == Player.STATE_BUFFERING) {
                        Toast.makeText(AudioPlayerActivity.this, "Buffering...", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onIsPlayingChanged(boolean playing) {
                    Log.d(TAG, "Is playing changed: " + playing);
                    isPlaying = playing;
                    playPauseBtn.setImageResource(playing ?
                            android.R.drawable.ic_media_pause :
                            android.R.drawable.ic_media_play);
                    if (playing) {
                        updateSeekBar();
                    }
                }

                @Override
                public void onPlayerError(PlaybackException error) {
                    Log.e(TAG, "Playback error: " + error.getMessage(), error);
                    Toast.makeText(AudioPlayerActivity.this,
                            "Playback error: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                    error.printStackTrace();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error setting up ExoPlayer: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing player: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupControls() {
        playPauseBtn.setOnClickListener(v -> {
            if (exoPlayer != null) {
                if (isPlaying) {
                    exoPlayer.pause();
                    Log.d(TAG, "Paused");
                } else {
                    exoPlayer.play();
                    Log.d(TAG, "Playing");
                }
            }
        });

        stopBtn.setOnClickListener(v -> {
            if (exoPlayer != null) {
                exoPlayer.stop();
                exoPlayer.seekTo(0);
                seekBar.setProgress(0);
                currentTimeText.setText("00:00");
                isPlaying = false;
                playPauseBtn.setImageResource(android.R.drawable.ic_media_play);
                Log.d(TAG, "Stopped");
            }
        });

        rewindBtn.setOnClickListener(v -> {
            if (exoPlayer != null) {
                long currentPos = exoPlayer.getCurrentPosition();
                long newPos = Math.max(0, currentPos - 10000);
                exoPlayer.seekTo(newPos);
            }
        });

        forwardBtn.setOnClickListener(v -> {
            if (exoPlayer != null) {
                long currentPos = exoPlayer.getCurrentPosition();
                long duration = exoPlayer.getDuration();
                long newPos = Math.min(duration, currentPos + 10000);
                exoPlayer.seekTo(newPos);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && exoPlayer != null) {
                    exoPlayer.seekTo(progress);
                    currentTimeText.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateSeekBar() {
        if (exoPlayer != null && isPlaying) {
            long currentPos = exoPlayer.getCurrentPosition();
            seekBar.setProgress((int) currentPos);
            currentTimeText.setText(formatTime(currentPos));

            if (updateSeekBarRunnable == null) {
                updateSeekBarRunnable = this::updateSeekBar;
            }
            seekBar.postDelayed(updateSeekBarRunnable, 100);
        }
    }

    private String formatTime(long millis) {
        StringBuilder formatBuilder = new StringBuilder();
        Formatter formatter = new Formatter(formatBuilder, Locale.getDefault());
        int totalSeconds = (int) (millis / 1000);
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        formatBuilder.setLength(0);
        if (hours > 0) {
            return formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return formatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        if (updateSeekBarRunnable != null) {
            seekBar.removeCallbacks(updateSeekBarRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (exoPlayer != null && isPlaying) {
            exoPlayer.pause();
        }
    }
}
