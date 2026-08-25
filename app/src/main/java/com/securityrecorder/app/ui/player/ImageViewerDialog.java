package com.securityrecorder.app.ui.player;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.Window;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.securityrecorder.app.R;
import com.securityrecorder.app.data.model.VideoItem;
import com.securityrecorder.app.databinding.DialogImageViewerBinding;
import com.securityrecorder.app.utils.DateTimeUtils;
import com.securityrecorder.app.utils.FileUtils;
import com.securityrecorder.app.utils.HapticUtils;
import java.io.File;

/**
 * Fullscreen Image Viewer Dialog with Share, Favorite, Vault, and Delete actions.
 */
public class ImageViewerDialog extends Dialog {

    public interface OnImageActionListener {
        void onFavoriteToggled(VideoItem item);
        void onVaultToggled(VideoItem item);
        void onDeleted(VideoItem item);
    }

    private final VideoItem imageItem;
    private final OnImageActionListener actionListener;
    private DialogImageViewerBinding binding;

    public ImageViewerDialog(@NonNull Context context, VideoItem item, OnImageActionListener listener) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.imageItem = item;
        this.actionListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogImageViewerBinding.inflate(LayoutInflater.from(getContext()));
        setContentView(binding.getRoot());

        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }

        binding.tvImageTitle.setText(imageItem.getTitle());
        String info = DateTimeUtils.formatDisplayDate(imageItem.getTimestamp()) + " · "
                + FileUtils.formatFileSize(imageItem.getSizeBytes())
                + (imageItem.getResolution() != null ? " · " + imageItem.getResolution() : "");
        binding.tvImageDetails.setText(info);

        updateFavoriteIcon();
        updateVaultIcon();

        Glide.with(getContext())
                .load(new File(imageItem.getFilePath()))
                .fitCenter()
                .into(binding.ivFullImage);

        binding.btnBackImage.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(getContext());
            dismiss();
        });

        binding.btnImageShare.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(getContext());
            FileUtils.shareFile(getContext(), imageItem.getFilePath());
        });

        binding.btnImageFavorite.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(getContext());
            imageItem.setFavorite(!imageItem.isFavorite());
            updateFavoriteIcon();
            if (actionListener != null) {
                actionListener.onFavoriteToggled(imageItem);
            }
        });

        binding.btnImageVault.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(getContext());
            dismiss();
            if (actionListener != null) {
                actionListener.onVaultToggled(imageItem);
            }
        });

        binding.btnImageDelete.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(getContext());
            dismiss();
            if (actionListener != null) {
                actionListener.onDeleted(imageItem);
            }
        });
    }

    private void updateFavoriteIcon() {
        binding.btnImageFavorite.setImageResource(
                imageItem.isFavorite() ? R.drawable.ic_star_filled : R.drawable.ic_star_outline
        );
    }

    private void updateVaultIcon() {
        binding.btnImageVault.setImageResource(
                imageItem.isVault() ? R.drawable.ic_vault_unlocked : R.drawable.ic_vault
        );
    }

    public static void show(Context context, VideoItem item, OnImageActionListener listener) {
        new ImageViewerDialog(context, item, listener).show();
    }
}
