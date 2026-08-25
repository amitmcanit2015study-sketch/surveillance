package com.securityrecorder.app.ui.main;

import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.securityrecorder.app.R;
import com.securityrecorder.app.data.model.VideoItem;
import com.securityrecorder.app.databinding.ItemVideoGridBinding;
import com.securityrecorder.app.databinding.ItemVideoListBinding;
import com.securityrecorder.app.utils.DateTimeUtils;
import com.securityrecorder.app.utils.FileUtils;
import java.io.File;

/**
 * ViewHolder supporting both Grid and List presentation for videos, audios, and photos with multi-select checkboxes.
 */
public abstract class VideoViewHolder extends RecyclerView.ViewHolder {

    public VideoViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public abstract void bind(VideoItem item, boolean isSelectionMode, VideoAdapter.OnVideoItemClickListener listener);

    /**
     * Grid Layout ViewHolder
     */
    public static class GridViewHolder extends VideoViewHolder {
        private final ItemVideoGridBinding binding;

        public GridViewHolder(ItemVideoGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @Override
        public void bind(VideoItem item, boolean isSelectionMode, VideoAdapter.OnVideoItemClickListener listener) {
            binding.tvTitle.setText(item.getTitle());
            binding.tvDate.setText(DateTimeUtils.formatCardDate(item.getTimestamp()));
            binding.tvSize.setText(FileUtils.formatFileSize(item.getSizeBytes()));

            binding.btnFavorite.setImageResource(
                    item.isFavorite() ? R.drawable.ic_star_filled : R.drawable.ic_star_outline
            );

            // Selection Checkbox
            binding.cbSelect.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
            binding.cbSelect.setOnCheckedChangeListener(null);
            binding.cbSelect.setChecked(item.isSelected());
            binding.cbSelect.setOnCheckedChangeListener((btn, isChecked) -> {
                item.setSelected(isChecked);
                if (listener != null) listener.onSelectionChanged(item, isChecked);
            });

            if (item.isAudio()) {
                binding.tvDuration.setVisibility(View.VISIBLE);
                binding.tvDuration.setText(DateTimeUtils.formatDuration(item.getDurationMs()));
                Glide.with(itemView.getContext()).clear(binding.ivThumbnail);
                binding.ivThumbnail.setImageResource(R.drawable.ic_audio_file);
                binding.ivThumbnail.setScaleType(ImageView.ScaleType.FIT_CENTER);
                binding.ivThumbnail.setPadding(24, 24, 24, 24);
            } else if (item.isImage()) {
                binding.tvDuration.setVisibility(View.VISIBLE);
                binding.tvDuration.setText("PHOTO");
                binding.ivThumbnail.setPadding(0, 0, 0, 0);
                binding.ivThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Glide.with(itemView.getContext())
                        .load(new File(item.getFilePath()))
                        .placeholder(R.drawable.ic_image_file)
                        .centerCrop()
                        .into(binding.ivThumbnail);
            } else {
                // Video
                binding.tvDuration.setVisibility(View.VISIBLE);
                binding.tvDuration.setText(DateTimeUtils.formatDuration(item.getDurationMs()));
                binding.ivThumbnail.setPadding(0, 0, 0, 0);
                binding.ivThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Glide.with(itemView.getContext())
                        .asBitmap()
                        .load(item.getFilePath())
                        .placeholder(R.drawable.ic_security_shield)
                        .centerCrop()
                        .into(binding.ivThumbnail);
            }

            binding.cardVideo.setOnClickListener(v -> {
                if (isSelectionMode) {
                    boolean newState = !item.isSelected();
                    item.setSelected(newState);
                    binding.cbSelect.setChecked(newState);
                    if (listener != null) listener.onSelectionChanged(item, newState);
                } else {
                    if (listener != null) listener.onVideoClick(item);
                }
            });

            binding.cardVideo.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onVideoLongClick(item);
                }
                return true;
            });

            binding.btnFavorite.setOnClickListener(v -> {
                if (listener != null) listener.onFavoriteToggle(item);
            });
        }
    }

    /**
     * List Layout ViewHolder
     */
    public static class ListViewHolder extends VideoViewHolder {
        private final ItemVideoListBinding binding;

        public ListViewHolder(ItemVideoListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @Override
        public void bind(VideoItem item, boolean isSelectionMode, VideoAdapter.OnVideoItemClickListener listener) {
            binding.tvTitle.setText(item.getTitle());
            binding.tvDate.setText(DateTimeUtils.formatDisplayDate(item.getTimestamp()));
            binding.tvSize.setText(FileUtils.formatFileSize(item.getSizeBytes()));

            binding.btnFavorite.setImageResource(
                    item.isFavorite() ? R.drawable.ic_star_filled : R.drawable.ic_star_outline
            );

            // Selection Checkbox
            binding.cbSelect.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
            binding.cbSelect.setOnCheckedChangeListener(null);
            binding.cbSelect.setChecked(item.isSelected());
            binding.cbSelect.setOnCheckedChangeListener((btn, isChecked) -> {
                item.setSelected(isChecked);
                if (listener != null) listener.onSelectionChanged(item, isChecked);
            });

            if (item.isAudio()) {
                binding.tvDuration.setVisibility(View.VISIBLE);
                binding.tvDuration.setText(DateTimeUtils.formatDuration(item.getDurationMs()));
                binding.tvResolution.setText("Audio");
                Glide.with(itemView.getContext()).clear(binding.ivThumbnail);
                binding.ivThumbnail.setImageResource(R.drawable.ic_audio_file);
                binding.ivThumbnail.setScaleType(ImageView.ScaleType.FIT_CENTER);
                binding.ivThumbnail.setPadding(16, 16, 16, 16);
            } else if (item.isImage()) {
                binding.tvDuration.setVisibility(View.GONE);
                binding.tvResolution.setText("Photo");
                binding.ivThumbnail.setPadding(0, 0, 0, 0);
                binding.ivThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Glide.with(itemView.getContext())
                        .load(new File(item.getFilePath()))
                        .placeholder(R.drawable.ic_image_file)
                        .centerCrop()
                        .into(binding.ivThumbnail);
            } else {
                // Video
                binding.tvDuration.setVisibility(View.VISIBLE);
                binding.tvDuration.setText(DateTimeUtils.formatDuration(item.getDurationMs()));
                binding.tvResolution.setText(item.getResolution() != null ? item.getResolution() : "1080p");
                binding.ivThumbnail.setPadding(0, 0, 0, 0);
                binding.ivThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Glide.with(itemView.getContext())
                        .asBitmap()
                        .load(item.getFilePath())
                        .placeholder(R.drawable.ic_security_shield)
                        .centerCrop()
                        .into(binding.ivThumbnail);
            }

            binding.cardVideoList.setOnClickListener(v -> {
                if (isSelectionMode) {
                    boolean newState = !item.isSelected();
                    item.setSelected(newState);
                    binding.cbSelect.setChecked(newState);
                    if (listener != null) listener.onSelectionChanged(item, newState);
                } else {
                    if (listener != null) listener.onVideoClick(item);
                }
            });

            binding.cardVideoList.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onVideoLongClick(item);
                }
                return true;
            });

            binding.btnFavorite.setOnClickListener(v -> {
                if (listener != null) listener.onFavoriteToggle(item);
            });
        }
    }
}
