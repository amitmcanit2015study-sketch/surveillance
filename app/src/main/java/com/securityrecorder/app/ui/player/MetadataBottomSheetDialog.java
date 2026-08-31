package com.securityrecorder.app.ui.player;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.securityrecorder.app.data.model.VideoItem;
import com.securityrecorder.app.databinding.BottomSheetMetadataBinding;
import com.securityrecorder.app.utils.DateTimeUtils;
import com.securityrecorder.app.utils.DeviceInfoHelper;
import com.securityrecorder.app.utils.FileUtils;

/**
 * Bottom sheet displaying full EXIF, location coordinates & address, device model, OS version,
 * device owner username, mobile SIM details, codec, and file size metadata.
 */
public class MetadataBottomSheetDialog extends BottomSheetDialogFragment {

    private static final String ARG_VIDEO_ITEM = "arg_video_item";
    private BottomSheetMetadataBinding binding;
    private VideoItem videoItem;

    public static MetadataBottomSheetDialog newInstance(VideoItem item) {
        MetadataBottomSheetDialog fragment = new MetadataBottomSheetDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_VIDEO_ITEM, item);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            videoItem = (VideoItem) getArguments().getSerializable(ARG_VIDEO_ITEM);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetMetadataBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            if (videoItem != null && getContext() != null) {
                binding.tvMetaFilename.setText(videoItem.getTitle() != null ? videoItem.getTitle() : "Recording");
                binding.tvMetaDate.setText(DateTimeUtils.formatDisplayDate(videoItem.getTimestamp()));

                String typeStr = "Video Recording";
                if (videoItem.isAudio()) {
                    typeStr = "Audio Recording (M4A)";
                } else if (videoItem.isImage()) {
                    typeStr = "Photo / Image (JPEG)";
                }
                binding.tvMetaType.setText(typeStr);
                binding.tvMetaVault.setText(videoItem.isVault() ? "🔒 Protected in Vault" : "Standard Archive");

                if (videoItem.isImage()) {
                    binding.tvMetaDuration.setText("N/A (Photo)");
                } else {
                    binding.tvMetaDuration.setText(DateTimeUtils.formatDuration(videoItem.getDurationMs()));
                }

                binding.tvMetaSize.setText(FileUtils.formatFileSize(videoItem.getSizeBytes()) + " (" + videoItem.getSizeBytes() + " bytes)");
                binding.tvMetaResolution.setText(videoItem.getResolution() != null ? videoItem.getResolution() : "1080p");
                binding.tvMetaCodec.setText(videoItem.getCodec() != null ? videoItem.getCodec() : (videoItem.isAudio() ? "audio/mp4" : "video/mp4"));
                binding.tvMetaLocation.setText(videoItem.getLocation() != null ? videoItem.getLocation() : "Not available");

                // Comprehensive Device, OS, User, and SIM info
                try {
                    binding.tvMetaDevice.setText(DeviceInfoHelper.getDeviceModel());
                    binding.tvMetaOsVersion.setText(DeviceInfoHelper.getOsVersion());
                    binding.tvMetaUserName.setText(DeviceInfoHelper.getDeviceUserName(getContext()));
                    binding.tvMetaSimInfo.setText(DeviceInfoHelper.getSimInfo(getContext()));
                } catch (Throwable ignored) {}

                binding.tvMetaPath.setText(videoItem.getFilePath() != null ? videoItem.getFilePath() : "");
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        binding.btnCloseMetadata.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
