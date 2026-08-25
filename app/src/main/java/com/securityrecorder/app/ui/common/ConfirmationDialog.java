package com.securityrecorder.app.ui.common;

import android.content.Context;
import android.view.LayoutInflater;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.securityrecorder.app.databinding.DialogConfirmationBinding;

/**
 * Reusable Material 3 confirmation dialog.
 */
public class ConfirmationDialog {

    public interface ConfirmationCallback {
        void onConfirmed();
    }

    public static void show(Context context, String title, String message, ConfirmationCallback callback) {
        DialogConfirmationBinding binding = DialogConfirmationBinding.inflate(LayoutInflater.from(context));
        binding.tvConfirmTitle.setText(title);
        binding.tvConfirmMessage.setText(message);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(binding.getRoot())
                .create();

        binding.btnCancelAction.setOnClickListener(v -> dialog.dismiss());
        binding.btnConfirmAction.setOnClickListener(v -> {
            dialog.dismiss();
            if (callback != null) callback.onConfirmed();
        });

        dialog.show();
    }
}
