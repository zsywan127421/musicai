package com.example.musicai.util;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.musicai.R;

public class ConfirmDialog {
    
    public interface OnConfirmListener {
        void onConfirm();
    }
    
    public static void show(Context context, String title, String message, OnConfirmListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_confirm, null);
        builder.setView(dialogView);
        
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = dialogView.findViewById(R.id.tv_dialog_message);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        
        tvTitle.setText(title);
        tvMessage.setText(message);
        
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onConfirm();
            }
        });
        
        dialog.show();
    }
    
    public static void showDelete(Context context, OnConfirmListener listener) {
        show(context, "确认删除", "确定删除此条目？删除后不可恢复", listener);
    }
    
    public static void showSave(Context context, OnConfirmListener listener) {
        show(context, "确认保存", "是否保存当前修改？", listener);
    }
}
