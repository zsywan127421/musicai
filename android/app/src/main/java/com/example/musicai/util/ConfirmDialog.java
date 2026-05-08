package com.example.musicai.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.musicai.R;

public class ConfirmDialog {
    
    public interface OnConfirmListener {
        void onConfirm();
    }
    
    public static void show(Context context, String title, String message, OnConfirmListener listener) {
        show(context, title, message, false, listener);
    }
    
    public static void show(Context context, String title, String message, String cancelText, String confirmText, OnConfirmListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm);
        
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setWindowAnimations(android.R.style.Animation_Dialog);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.4f);
        }
        
        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = dialog.findViewById(R.id.tv_dialog_message);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnConfirm = dialog.findViewById(R.id.btn_confirm);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close);
        
        tvTitle.setText(title);
        tvMessage.setText(message);
        btnCancel.setText(cancelText);
        btnConfirm.setText(confirmText);
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onConfirm();
            }
        });
        
        dialog.show();
    }
    
    public static void showDanger(Context context, String title, String message, OnConfirmListener listener) {
        show(context, title, message, true, listener);
    }
    
    private static void show(Context context, String title, String message, boolean isDanger, OnConfirmListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm);
        
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setWindowAnimations(android.R.style.Animation_Dialog);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.4f);
        }
        
        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = dialog.findViewById(R.id.tv_dialog_message);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnConfirm = dialog.findViewById(R.id.btn_confirm);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close);
        
        tvTitle.setText(title);
        tvMessage.setText(message);
        
        if (isDanger) {
            btnConfirm.setBackgroundResource(R.drawable.apple_button_danger_bg);
        } else {
            btnConfirm.setBackgroundResource(R.drawable.apple_button_primary_bg);
        }
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onConfirm();
            }
        });
        
        dialog.show();
    }
    
    public static void showDelete(Context context, String itemName, OnConfirmListener listener) {
        showDanger(context, "确定删除《" + itemName + "》吗？", "此操作不可恢复", listener);
    }
    
    public static void showSave(Context context, String itemName, OnConfirmListener listener) {
        show(context, "已有同名条目", "是否覆盖《" + itemName + "》？", listener);
    }
    
    public static void showCustom(Context context, String title, View customView, OnConfirmListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm);
        
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setWindowAnimations(android.R.style.Animation_Dialog);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.4f);
        }
        
        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = dialog.findViewById(R.id.tv_dialog_message);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnConfirm = dialog.findViewById(R.id.btn_confirm);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close);
        
        tvTitle.setText(title);
        tvMessage.setVisibility(View.GONE);
        
        ViewGroup parent = (ViewGroup) tvMessage.getParent();
        int messageIndex = parent.indexOfChild(tvMessage);
        parent.addView(customView, messageIndex);
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onConfirm();
            }
        });
        
        dialog.show();
    }
}
