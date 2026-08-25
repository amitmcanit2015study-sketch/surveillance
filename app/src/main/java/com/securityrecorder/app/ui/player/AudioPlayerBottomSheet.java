package com.securityrecorder.app.ui.player;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.securityrecorder.app.R;
import com.securityrecorder.app.data.model.VideoItem;
import com.securityrecorder.app.databinding.BottomSheetAudioPlayerBinding;
import com.securityrecorder.app.utils.DateTimeUtils;
import com.securityrecorder.app.utils.FileUtils;
import com.securityrecorder.app.utils.HapticUtils;
import java.io.File;

/**
 * BottomSheetDialogFragment providing audio playback with play/pause, seekbar tracking, vaulting, and sharing.
 */
public class AudioPlayerBottomSheet extends BottomSheetDialogFragment {

    public interface OnAudioActionListener {
        void onVaultToggled(VideoItem item);
        void onDeleted(VideoItem item);
    }

    private static final String ARG_AUDIO_ITEM = "arg_audio_item";

    private VideoItem audioItem;
    private OnAudioActionListener actionListener;
    private BottomSheetAudioPlayerBinding binding;
    private MediaPlayer mediaPlayer;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private boolean isUserSeeking = false;

    public static AudioPlayerBottomSheet newInstance(VideoItem item, OnAudioActionListener listener) {
        AudioPlayerBottomSheet fragment = new AudioPlayerBottomSheet();
        fragment.audioItem = item;
        fragment.actionListener = listener;
        Bundle args = new Bundle();
        args.putSerializable(ARG_AUDIO_ITEM, item);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetAudioPlayerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (audioItem == null && getArguments() != null) {
            audioItem = (VideoItem) getArguments().getSerializable(ARG_AUDIO_ITEM);
        }

        if (audioItem == null) {
            dismiss();
            return;
        }

        binding.tvAudioTitle.setText(audioItem.getTitle());
        String subtitle = DateTimeUtils.formatDisplayDate(audioItem.getTimestamp()) + " · "
                + FileUtils.formatFileSize(audioItem.getSizeBytes());
        binding.tvAudioSubtitle.setText(subtitle);

        binding.btnAudioVault.setImageResource(audioItem.isVault() ? R.drawable.ic_vault_unlocked : R.drawable.ic_vault);

        initMediaPlayer();
        setupControls();
    }

    private void initMediaPlayer() {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(audioItem.getFilePath());
            mediaPlayer.prepare();
            int duration = mediaPlayer.getDuration();
            binding.seekBarAudio.setMax(duration);
            binding.tvAudioTotalDuration.setText(DateTimeUtils.formatDuration(duration));
            binding.tvAudioCurrentPosition.setText("00:00");

            mediaPlayer.setOnCompletionListener(mp -> {
                binding.fabPlayPauseAudio.setImageResource(R.drawable.ic_play);
                binding.seekBarAudio.setProgress(0);
                binding.tvAudioCurrentPosition.setText("00:00");
                progressHandler.removeCallbacks(updateProgressRunnable);
            });

            // Start playback automatically on open
            playAudio();

        } catch (Exception e) {
            Toast.makeText(getContext(), "Unable to play audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            dismiss();
        }
    }

    private void setupControls() {
        binding.fabPlayPauseAudio.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(getContext());
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                pauseAudio();
            } else {
                playAudio();
            }
        });

        binding.btnAudioRewind.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(getContext());
            if (mediaPlayer != null) {
                int pos = Math.max(0, mediaPlayer.getCurrentPosition() - 5000);
                mediaPlayer.seekTo(pos);
                binding.seekBarAudio.setProgress(pos);
            }
        });

        binding.btnAudioShare.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(getContext());
            FileUtils.shareFile(getContext(), audioItem.getFilePath());
        });

        binding.btnAudioVault.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(getContext());
            dismiss();
            if (actionListener != null) {
                actionListener.onVaultToggled(audioItem);
            }
        });

        binding.btnAudioDelete.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(getContext());
            dismiss();
            if (actionListener != null) {
                actionListener.onDeleted(audioItem);
            }
        });

        binding.seekBarAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    binding.tvAudioCurrentPosition.setText(DateTimeUtils.formatDuration(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(seekBar.getProgress());
                }
            }
        });
    }

    private void playAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            binding.fabPlayPauseAudio.setImageResource(R.drawable.ic_pause);
            progressHandler.post(updateProgressRunnable);
        }
    }

    private void pauseAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            binding.fabPlayPauseAudio.setImageResource(R.drawable.ic_play);
            progressHandler.removeCallbacks(updateProgressRunnable);
        }
    }

    private final Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying() && !isUserSeeking) {
                int pos = mediaPlayer.getCurrentPosition();
                binding.seekBarAudio.setProgress(pos);
                binding.tvAudioCurrentPosition.setText(DateTimeUtils.formatDuration(pos));
                progressHandler.postDelayed(this, 250);
            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        progressHandler.removeCallbacks(updateProgressRunnable);
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
