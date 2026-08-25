package com.securityrecorder.app.ui.player;

import android.app.PictureInPictureParams;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Rational;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import com.securityrecorder.app.R;
import com.securityrecorder.app.data.model.VideoItem;
import com.securityrecorder.app.data.repository.VideoRepository;
import com.securityrecorder.app.databinding.ActivityVideoPlayerBinding;
import com.securityrecorder.app.ui.common.ConfirmationDialog;
import com.securityrecorder.app.ui.common.RenameDialog;
import com.securityrecorder.app.utils.DateTimeUtils;
import com.securityrecorder.app.utils.FileUtils;
import com.securityrecorder.app.utils.HapticUtils;
import java.io.File;

/**
 * Built-in Video Player built on Media3 ExoPlayer supporting Seek, Speed, PiP, Metadata, Share, Delete, and Rename.
 */
public class VideoPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_ITEM = "extra_video_item";

    private ActivityVideoPlayerBinding binding;
    private ExoPlayer player;
    private VideoItem videoItem;
    private VideoRepository repository;

    private boolean areControlsVisible = true;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Handler controlsHideHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    private float currentSpeed = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new VideoRepository(getApplication());
        videoItem = (VideoItem) getIntent().getSerializableExtra(EXTRA_VIDEO_ITEM);

        if (videoItem == null) {
            Toast.makeText(this, "Video not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupPlayer();
        setupListeners();
    }

    private void initViews() {
        binding.tvPlayerTitle.setText(videoItem.getTitle());
        updateFavoriteButtonUi();
    }

    private void setupPlayer() {
        player = new ExoPlayer.Builder(this).build();
        binding.playerView.setPlayer(player);

        Uri videoUri = Uri.fromFile(new File(videoItem.getFilePath()));
        MediaItem mediaItem = MediaItem.fromUri(videoUri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                binding.btnCenterPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
                if (isPlaying) {
                    scheduleControlsHide();
                } else {
                    controlsHideHandler.removeCallbacksAndMessages(null);
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    long duration = player.getDuration();
                    binding.tvTotalDuration.setText(DateTimeUtils.formatDuration(duration));
                    startProgressTracker();
                }
            }
        });
    }

    private void setupListeners() {
        binding.touchOverlay.setOnClickListener(v -> toggleControls());

        binding.btnPlayerBack.setOnClickListener(v -> finish());

        binding.btnCenterPlayPause.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            if (player != null) {
                if (player.isPlaying()) {
                    player.pause();
                } else {
                    player.play();
                }
            }
        });

        binding.playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    long newPos = (player.getDuration() * progress) / 1000;
                    binding.tvCurrentPosition.setText(DateTimeUtils.formatDuration(newPos));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                controlsHideHandler.removeCallbacksAndMessages(null);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (player != null) {
                    long newPos = (player.getDuration() * seekBar.getProgress()) / 1000;
                    player.seekTo(newPos);
                }
                scheduleControlsHide();
            }
        });

        binding.btnSpeed.setOnClickListener(this::showSpeedMenu);

        binding.btnPip.setOnClickListener(v -> enterPipMode());

        binding.btnMetadata.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            MetadataBottomSheetDialog.newInstance(videoItem)
                    .show(getSupportFragmentManager(), "metadata_dialog");
        });

        binding.btnShare.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            FileUtils.shareVideo(this, videoItem.getFilePath());
        });

        binding.btnDelete.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            ConfirmationDialog.show(this, getString(R.string.confirm_delete_title),
                    getString(R.string.confirm_delete_message), () -> {
                        repository.deleteVideo(videoItem, this::finish);
                    });
        });

        binding.btnRename.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            RenameDialog.show(this, videoItem.getTitle(), newName -> {
                repository.renameVideo(videoItem, newName, () -> {
                    binding.tvPlayerTitle.setText(videoItem.getTitle());
                });
            });
        });

        binding.btnPlayerFavorite.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(this);
            repository.toggleFavorite(videoItem);
            updateFavoriteButtonUi();
        });
    }

    private void updateFavoriteButtonUi() {
        binding.btnPlayerFavorite.setImageResource(
                videoItem.isFavorite() ? R.drawable.ic_star_filled : R.drawable.ic_star_outline
        );
    }

    private void showSpeedMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "0.5x");
        popup.getMenu().add(0, 2, 0, "1.0x (Normal)");
        popup.getMenu().add(0, 3, 0, "1.25x");
        popup.getMenu().add(0, 4, 0, "1.5x");
        popup.getMenu().add(0, 5, 0, "2.0x");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: currentSpeed = 0.5f; break;
                case 2: currentSpeed = 1.0f; break;
                case 3: currentSpeed = 1.25f; break;
                case 4: currentSpeed = 1.5f; break;
                case 5: currentSpeed = 2.0f; break;
            }
            if (player != null) {
                player.setPlaybackParameters(new PlaybackParameters(currentSpeed));
            }
            binding.btnSpeed.setText(String.format("%.2gx", currentSpeed));
            return true;
        });
        popup.show();
    }

    private void enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Rational rational = new Rational(16, 9);
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(rational)
                    .build();
            enterPictureInPictureMode(params);
        } else {
            Toast.makeText(this, "Picture-in-Picture requires Android 8.0+", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, @NonNull Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (isInPictureInPictureMode) {
            hideControls();
        } else {
            showControls();
        }
    }

    private void toggleControls() {
        if (areControlsVisible) {
            hideControls();
        } else {
            showControls();
            scheduleControlsHide();
        }
    }

    private void showControls() {
        binding.playerTopBar.setVisibility(View.VISIBLE);
        binding.playerBottomBar.setVisibility(View.VISIBLE);
        binding.btnCenterPlayPause.setVisibility(View.VISIBLE);
        areControlsVisible = true;
    }

    private void hideControls() {
        binding.playerTopBar.setVisibility(View.GONE);
        binding.playerBottomBar.setVisibility(View.GONE);
        binding.btnCenterPlayPause.setVisibility(View.GONE);
        areControlsVisible = false;
    }

    private void scheduleControlsHide() {
        controlsHideHandler.removeCallbacksAndMessages(null);
        controlsHideHandler.postDelayed(this::hideControls, 3500);
    }

    private void startProgressTracker() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (player != null && player.isPlaying()) {
                    long current = player.getCurrentPosition();
                    long total = player.getDuration();
                    if (total > 0) {
                        int progress = (int) ((current * 1000) / total);
                        binding.playerSeekBar.setProgress(progress);
                        binding.tvCurrentPosition.setText(DateTimeUtils.formatDuration(current));
                    }
                }
                progressHandler.postDelayed(this, 500);
            }
        };
        progressHandler.post(progressRunnable);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressHandler.removeCallbacksAndMessages(null);
        controlsHideHandler.removeCallbacksAndMessages(null);
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
