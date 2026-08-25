package com.securityrecorder.app.ui.common;

import android.content.Context;
import android.view.LayoutInflater;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.securityrecorder.app.databinding.DialogRenameBinding;

/**
 * Reusable Material 3 rename dialog.
 */
public class RenameDialog {

    public interface RenameCallback {
        void onRenamed(String newName);
    }

    public static void show(Context context, String currentName, RenameCallback callback) {
        DialogRenameBinding binding = DialogRenameBinding.inflate(LayoutInflater.from(context));
        if (currentName != null) {
            String nameWithoutExt = currentName.endsWith(".mp4")
                    ? currentName.substring(0, currentName.length() - 4)
                    : currentName;
            binding.etNewFilename.setText(nameWithoutExt);
            binding.etNewFilename.selectAll();
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(binding.getRoot())
                .create();

        binding.btnCancelRename.setOnClickListener(v -> dialog.dismiss());
        binding.btnSaveRename.setOnClickListener(v -> {
            String newName = binding.etNewFilename.getText() != null
                    ? binding.etNewFilename.getText().toString().trim()
                    : "";
            if (!newName.isEmpty()) {
                dialog.dismiss();
                if (callback != null) callback.onRenamed(newName);
            }
        });

        dialog.show();
    }
}
