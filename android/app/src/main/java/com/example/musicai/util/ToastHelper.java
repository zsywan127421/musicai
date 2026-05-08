package com.example.musicai.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.musicai.R;

public class ToastHelper {
    
    private static final int SUCCESS_COLOR = 0xFF34C759;
    private static final int ERROR_COLOR = 0xFFFF3B30;
    private static final int INFO_COLOR = 0xFF007AFF;
    private static final int WARNING_COLOR = 0xFFFF9500;
    
    private static Toast currentToast;
    
    public static void showSuccess(Context context, String message) {
        showCustomToast(context, message, SUCCESS_COLOR, R.drawable.ic_check_circle);
    }
    
    public static void showError(Context context, String message) {
        showCustomToast(context, message, ERROR_COLOR, R.drawable.ic_error);
    }
    
    public static void showInfo(Context context, String message) {
        showCustomToast(context, message, INFO_COLOR, R.drawable.ic_info);
    }
    
    public static void showWarning(Context context, String message) {
        showCustomToast(context, message, WARNING_COLOR, R.drawable.ic_warning_circle);
    }
    
    public static void showSuccessLong(Context context, String message) {
        showCustomToast(context, message, SUCCESS_COLOR, R.drawable.ic_check_circle, Toast.LENGTH_LONG);
    }
    
    public static void showErrorLong(Context context, String message) {
        showCustomToast(context, message, ERROR_COLOR, R.drawable.ic_error, Toast.LENGTH_LONG);
    }
    
    private static void showCustomToast(Context context, String message, int color, int iconRes) {
        showCustomToast(context, message, color, iconRes, Toast.LENGTH_SHORT);
    }
    
    private static void showCustomToast(Context context, String message, int color, int iconRes, int duration) {
        if (currentToast != null) {
            currentToast.cancel();
        }
        
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        layout.setPadding(dpToPx(context, 16), dpToPx(context, 12), dpToPx(context, 16), dpToPx(context, 12));
        
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dpToPx(context, 12));
        background.setColor(0xE6000000);
        layout.setBackground(background);
        
        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(color);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(context, 20), dpToPx(context, 20));
        iconParams.setMarginEnd(dpToPx(context, 10));
        icon.setLayoutParams(iconParams);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        
        TextView text = new TextView(context);
        text.setText(message);
        text.setTextColor(Color.WHITE);
        text.setTextSize(14);
        text.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        
        layout.addView(icon);
        layout.addView(text);
        
        currentToast = new Toast(context);
        currentToast.setView(layout);
        currentToast.setDuration(duration);
        currentToast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, dpToPx(context, 100));
        currentToast.show();
    }
    
    private static int dpToPx(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
