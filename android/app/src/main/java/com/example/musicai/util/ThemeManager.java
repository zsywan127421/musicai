package com.example.musicai.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.view.View;
import android.view.Window;

public class ThemeManager {
    
    public static final String PREFS_NAME = "ThemePrefs";
    public static final String KEY_THEME_MODE = "theme_mode";
    
    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;
    
    private static ThemeManager instance;
    private SharedPreferences prefs;
    private int currentThemeMode = THEME_SYSTEM;
    
    private ThemeManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currentThemeMode = prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM);
    }
    
    public static synchronized ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context);
        }
        return instance;
    }
    
    public int getThemeMode() {
        return currentThemeMode;
    }
    
    public void setThemeMode(int mode) {
        currentThemeMode = mode;
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
    }
    
    public boolean isDarkMode() {
        switch (currentThemeMode) {
            case THEME_LIGHT:
                return false;
            case THEME_DARK:
                return true;
            default:
                return isSystemDarkMode();
        }
    }
    
    private boolean isSystemDarkMode() {
        Context context = prefs.getContext();
        int nightModeFlags = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
    
    public String getThemeModeString() {
        switch (currentThemeMode) {
            case THEME_LIGHT:
                return "浅色模式";
            case THEME_DARK:
                return "深色模式";
            default:
                return "跟随系统";
        }
    }
    
    public void applyTheme(Window window) {
        if (window == null) return;
        
        switch (currentThemeMode) {
            case THEME_LIGHT:
                window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
                break;
            case THEME_DARK:
                window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                break;
            default:
                if (isSystemDarkMode()) {
                    window.getDecorView().setSystemUiVisibility(0);
                } else {
                    window.getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
                }
                break;
        }
    }
    
    public static boolean isNightMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
}
