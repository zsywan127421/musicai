package com.example.musicai.util;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import com.example.musicai.R;
import com.example.musicai.SongEntry;

import java.io.File;

public class ExportBottomSheet {

    private Dialog dialog;
    private AudioExporter exporter;
    private SongEntry songEntry;
    private TextView tvTitle;
    private TextView tvProgress;
    private ProgressBar progressBar;
    private Button btnExportMidi;
    private Button btnExportWav;
    private Button btnCancel;
    private View exportOptions;
    private View exportProgress;

    public interface OnExportCompleteListener {
        void onExportComplete(File file);
        void onExportCancelled();
    }

    private OnExportCompleteListener listener;

    public ExportBottomSheet(Context context, SongEntry songEntry) {
        this.songEntry = songEntry;
        this.exporter = new AudioExporter(context);

        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_sheet_export);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setWindowAnimations(R.style.BottomSheetAnimation);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.4f);
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        tvTitle = dialog.findViewById(R.id.tv_export_title);
        tvProgress = dialog.findViewById(R.id.tv_progress);
        progressBar = dialog.findViewById(R.id.progress_bar);
        btnExportMidi = dialog.findViewById(R.id.btn_export_midi);
        btnExportWav = dialog.findViewById(R.id.btn_export_wav);
        btnCancel = dialog.findViewById(R.id.btn_cancel);
        exportOptions = dialog.findViewById(R.id.export_options);
        exportProgress = dialog.findViewById(R.id.export_progress);

        tvTitle.setText("导出 " + songEntry.name);

        exportOptions.setVisibility(View.VISIBLE);
        exportProgress.setVisibility(View.GONE);
    }

    private void setupListeners() {
        ImageButton btnClose = dialog.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> dismiss());

        btnExportMidi.setOnClickListener(v -> exportMidi());
        btnExportWav.setOnClickListener(v -> exportWav());
        btnCancel.setOnClickListener(v -> cancelExport());
    }

    private void exportMidi() {
        showProgress("正在导出MIDI...");

        exporter.setListener(new AudioExporter.ExportListener() {
            @Override
            public void onProgress(int progress, String message) {
                updateProgress(progress, message);
            }

            @Override
            public void onSuccess(File file) {
                showSuccess(file, "MIDI");
            }

            @Override
            public void onError(String error) {
                showError(error);
            }
        });

        new Thread(() -> {
            try {
                exporter.exportToMidi(songEntry);
            } catch (Exception e) {
                showError(e.getMessage());
            }
        }).start();
    }

    private void exportWav() {
        showProgress("正在合成WAV音频...");

        exporter.setListener(new AudioExporter.ExportListener() {
            @Override
            public void onProgress(int progress, String message) {
                updateProgress(progress, message);
            }

            @Override
            public void onSuccess(File file) {
                showSuccess(file, "WAV");
            }

            @Override
            public void onError(String error) {
                showError(error);
            }
        });

        new Thread(() -> {
            try {
                exporter.exportToWav(songEntry);
            } catch (Exception e) {
                showError(e.getMessage());
            }
        }).start();
    }

    private void showProgress(String message) {
        exportOptions.setVisibility(View.GONE);
        exportProgress.setVisibility(View.VISIBLE);
        tvProgress.setText(message);
        progressBar.setProgress(0);
        btnCancel.setText("取消导出");
        btnCancel.setVisibility(View.VISIBLE);
    }

    private void updateProgress(int progress, String message) {
        if (dialog.isShowing()) {
            progressBar.setProgress(progress);
            tvProgress.setText(message);
        }
    }

    private void showSuccess(File file, String format) {
        dialog.runOnUiThread(() -> {
            exportOptions.setVisibility(View.VISIBLE);
            exportProgress.setVisibility(View.GONE);

            ToastHelper.showSuccess(dialog.getContext(), format + " 导出成功！");

            if (listener != null) {
                listener.onExportComplete(file);
            }

            if (dialog.isShowing()) {
                btnCancel.setText("关闭");
                btnCancel.setOnClickListener(v -> {
                    shareFile(file);
                    dismiss();
                });
            }
        });
    }

    private void showError(String error) {
        dialog.runOnUiThread(() -> {
            exportOptions.setVisibility(View.VISIBLE);
            exportProgress.setVisibility(View.GONE);

            ToastHelper.showError(dialog.getContext(), "导出失败: " + error);

            btnCancel.setText("关闭");
            btnCancel.setOnClickListener(v -> dismiss());
        });
    }

    private void cancelExport() {
        if (exportProgress.getVisibility() == View.VISIBLE) {
            exporter.cancel();
            ToastHelper.showWarning(dialog.getContext(), "已取消导出");
        }
        dismiss();
    }

    private void shareFile(File file) {
        if (file == null || !file.exists()) return;

        Context context = dialog.getContext();
        Intent shareIntent = new Intent(Intent.ACTION_SEND);

        if (file.getName().endsWith(".mid")) {
            shareIntent.setType("audio/midi");
        } else if (file.getName().endsWith(".wav")) {
            shareIntent.setType("audio/wav");
        } else {
            shareIntent.setType("audio/*");
        }

        Uri fileUri;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fileUri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", file);
            } else {
                fileUri = Uri.fromFile(file);
            }
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(shareIntent, "分享" + file.getName()));
        } catch (Exception e) {
            ToastHelper.showError(context, "分享失败: " + e.getMessage());
        }
    }

    public void setOnExportCompleteListener(OnExportCompleteListener listener) {
        this.listener = listener;
    }

    public void show() {
        dialog.show();
    }

    public void dismiss() {
        exporter.cancel();
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
        if (listener != null) {
            listener.onExportCancelled();
        }
    }
}
