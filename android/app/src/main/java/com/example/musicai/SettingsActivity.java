package com.example.musicai;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.BuildCompat;

import com.example.musicai.util.ConfirmDialog;
import com.example.musicai.util.LanguageManager;
import com.example.musicai.util.SelectItemBottomSheet;
import com.example.musicai.util.ThemeManager;
import com.example.musicai.util.ToastHelper;

import java.io.File;

public class SettingsActivity extends BaseActivity {
    
    private static final String PREFS_NAME = "MusicAISettings";
    private static final String KEY_AUTO_SAVE = "auto_save";
    private static final String KEY_FONT_SIZE = "font_size";
    
    public static final int FONT_SIZE_SMALL = 0;
    public static final int FONT_SIZE_MEDIUM = 1;
    public static final int FONT_SIZE_LARGE = 2;
    
    private TextView tvThemeValue, tvFontSizeValue, tvCacheSize, tvVersion, tvLanguageValue;
    private View switchAutoSave;
    private SharedPreferences settingsPrefs;
    private LanguageManager languageManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        settingsPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        languageManager = LanguageManager.getInstance(this);
        
        initViews();
        updateUI();
        updateCacheSize();
    }
    
    private void initViews() {
        tvThemeValue = findViewById(R.id.tv_theme_value);
        tvFontSizeValue = findViewById(R.id.tv_font_size_value);
        tvCacheSize = findViewById(R.id.tv_cache_size);
        tvVersion = findViewById(R.id.tv_version);
        tvLanguageValue = findViewById(R.id.tv_language_value);
        switchAutoSave = findViewById(R.id.switch_auto_save);
        
        findViewById(R.id.item_theme).setOnClickListener(v -> showThemeSelector());
        findViewById(R.id.item_font_size).setOnClickListener(v -> showFontSizeSelector());
        findViewById(R.id.item_language).setOnClickListener(v -> showLanguageSelector());
        findViewById(R.id.item_clear_cache).setOnClickListener(v -> confirmClearCache());
        findViewById(R.id.item_license).setOnClickListener(v -> showLicense());
        
        if (switchAutoSave instanceof androidx.appcompat.widget.SwitchCompat) {
            ((androidx.appcompat.widget.SwitchCompat) switchAutoSave)
                .setChecked(settingsPrefs.getBoolean(KEY_AUTO_SAVE, true));
            ((androidx.appcompat.widget.SwitchCompat) switchAutoSave)
                .setOnCheckedChangeListener((button, isChecked) -> {
                    settingsPrefs.edit().putBoolean(KEY_AUTO_SAVE, isChecked).apply();
                });
        }
        
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText(version);
        } catch (Exception e) {
            tvVersion.setText("1.0.0");
        }
    }
    
    private void updateUI() {
        tvThemeValue.setText(themeManager.getThemeModeString());
        tvLanguageValue.setText(languageManager.getLanguageString());
        
        int fontSize = settingsPrefs.getInt(KEY_FONT_SIZE, FONT_SIZE_MEDIUM);
        switch (fontSize) {
            case FONT_SIZE_SMALL:
                tvFontSizeValue.setText("小");
                break;
            case FONT_SIZE_LARGE:
                tvFontSizeValue.setText("大");
                break;
            default:
                tvFontSizeValue.setText("中");
                break;
        }
    }
    
    private void updateCacheSize() {
        new Thread(() -> {
            long cacheSize = getCacheDirSize(getCacheDir());
            String sizeStr;
            if (cacheSize < 1024) {
                sizeStr = cacheSize + " B";
            } else if (cacheSize < 1024 * 1024) {
                sizeStr = (cacheSize / 1024) + " KB";
            } else {
                sizeStr = String.format("%.1f MB", cacheSize / (1024.0 * 1024.0));
            }
            runOnUiThread(() -> tvCacheSize.setText(sizeStr));
        }).start();
    }
    
    private long getCacheDirSize(File dir) {
        long size = 0;
        if (dir != null && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else {
                        size += getCacheDirSize(file);
                    }
                }
            }
        }
        return size;
    }
    
    private void showThemeSelector() {
        java.util.List<String> items = new java.util.ArrayList<>();
        items.add("跟随系统");
        items.add("浅色模式");
        items.add("深色模式");
        
        SelectItemBottomSheet.show(this, items, (index) -> {
            themeManager.setThemeMode(index);
            applyThemeMode(index);
            updateUI();
        });
    }
    
    private void applyThemeMode(int mode) {
        switch (mode) {
            case ThemeManager.THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case ThemeManager.THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
    
    private void showFontSizeSelector() {
        java.util.List<String> items = new java.util.ArrayList<>();
        items.add("小 (12sp)");
        items.add("中 (14sp)");
        items.add("大 (16sp)");
        
        int currentSize = settingsPrefs.getInt(KEY_FONT_SIZE, FONT_SIZE_MEDIUM);
        
        SelectItemBottomSheet.show(this, items, (index) -> {
            settingsPrefs.edit().putInt(KEY_FONT_SIZE, index).apply();
            updateUI();
            ToastHelper.showSuccess(this, "字体大小已更新");
        });
    }
    
    private void showLanguageSelector() {
        java.util.List<String> items = new java.util.ArrayList<>();
        items.add("跟随系统");
        items.add("简体中文");
        items.add("繁體中文");
        items.add("English");
        
        SelectItemBottomSheet.show(this, items, (index) -> {
            languageManager.setLanguage(index);
            languageManager.applyLanguage(this);
            updateUI();
            ToastHelper.showInfo(this, "语言切换将在重启应用后生效");
        });
    }
    
    private void confirmClearCache() {
        ConfirmDialog.showDanger(this, "确定清除所有生成缓存？", "此操作不可恢复", () -> {
            clearCache();
        });
    }
    
    private void clearCache() {
        new Thread(() -> {
            deleteDir(getCacheDir());
            runOnUiThread(() -> {
                updateCacheSize();
                ToastHelper.showSuccess(this, "缓存已清除");
            });
        }).start();
    }
    
    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    boolean success = deleteDir(child);
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return dir != null && dir.delete();
    }
    
    private void showLicense() {
        String license = "MusicAI - AI 音乐创作助手\n\n" +
            "开源许可声明\n\n" +
            "本应用基于以下开源项目构建：\n\n" +
            "• OkHttp - Apache 2.0 License\n" +
            "• Gson - Apache 2.0 License\n" +
            "• AndroidX - Apache 2.0 License\n\n" +
            "DeepSeek API 服务条款请参考 deepseek.com\n\n" +
            "MIT License\n\n" +
            "Copyright (c) 2024 MusicAI\n\n" +
            "Permission is hereby granted, free of charge, to any person obtaining a copy of this software...";
        
        new android.app.AlertDialog.Builder(this)
            .setTitle("开源许可")
            .setMessage(license)
            .setPositiveButton("确定", null)
            .show();
    }
    
    public static int getFontSize(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_FONT_SIZE, FONT_SIZE_MEDIUM);
    }
    
    public static boolean isAutoSaveEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_AUTO_SAVE, true);
    }
}
