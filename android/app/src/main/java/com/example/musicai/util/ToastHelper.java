package com.example.musicai.util;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.musicai.R;

public class ToastHelper {
    
    private static Toast currentToast;
    
    public static void showSuccess(Context context, String message) {
        showToast(context, message, R.color.apple_success);
    }
    
    public static void showError(Context context, String message) {
        showToast(context, message, R.color.apple_error);
    }
    
    public static void showWarning(Context context, String message) {
        showToast(context, message, R.color.apple_warning);
    }
    
    public static void showInfo(Context context, String message) {
        showToast(context, message, R.color.apple_accent);
    }
    
    private static void showToast(Context context, String message, int colorRes) {
        if (currentToast != null) {
            currentToast.cancel();
        }
        
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.toast_custom, null);
        
        TextView text = layout.findViewById(R.id.tv_toast_message);
        View indicator = layout.findViewById(R.id.toast_indicator);
        
        text.setText(message);
        indicator.setBackgroundResource(colorRes == R.color.apple_success ? R.drawable.toast_success_bg :
                                        colorRes == R.color.apple_error ? R.drawable.toast_error_bg :
                                        colorRes == R.color.apple_warning ? R.drawable.toast_warning_bg :
                                        R.drawable.toast_info_bg);
        
        currentToast = new Toast(context);
        currentToast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 100);
        currentToast.setDuration(Toast.LENGTH_SHORT);
        currentToast.setView(layout);
        currentToast.show();
    }
    
    public static void cancel() {
        if (currentToast != null) {
            currentToast.cancel();
            currentToast = null;
        }
    }
}
