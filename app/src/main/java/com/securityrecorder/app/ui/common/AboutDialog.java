package com.securityrecorder.app.ui.common;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Window;
import androidx.annotation.NonNull;
import com.securityrecorder.app.databinding.DialogAboutBinding;
import com.securityrecorder.app.utils.FileUtils;
import com.securityrecorder.app.utils.HapticUtils;

/**
 * About Dialog displaying app credits, company branding, and APK sharing.
 */
public class AboutDialog extends Dialog {

    private DialogAboutBinding binding;

    public AboutDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogAboutBinding.inflate(LayoutInflater.from(getContext()));
        setContentView(binding.getRoot());

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(
                    (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.90),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        binding.btnDialogClose.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(getContext());
            dismiss();
        });

        binding.btnDialogShareApp.setOnClickListener(v -> {
            HapticUtils.performClickFeedback(getContext());
            FileUtils.shareApp(getContext());
        });
    }

    public static void show(Context context) {
        new AboutDialog(context).show();
    }
}
