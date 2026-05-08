package com.example.musicai.util;

import android.content.Context;
import android.widget.Toast;

public class ToastHelper {

    public static void showSuccess(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void showError(Context context, String message) {
        Toast.makeText(context, "❌ " + message, Toast.LENGTH_SHORT).show();
    }

    public static void showInfo(Context context, String message) {
        Toast.makeText(context, "ℹ️ " + message, Toast.LENGTH_SHORT).show();
    }

    public static void showWarning(Context context, String message) {
        Toast.makeText(context, "⚠️ " + message, Toast.LENGTH_SHORT).show();
    }

    public static void showSuccessLong(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public static void showErrorLong(Context context, String message) {
        Toast.makeText(context, "❌ " + message, Toast.LENGTH_LONG).show();
    }
}
