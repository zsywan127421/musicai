package com.example.musicai;

import android.os.Bundle;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicai.util.ThemeManager;

public abstract class BaseActivity extends AppCompatActivity {
    
    protected ThemeManager themeManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeManager = ThemeManager.getInstance(this);
        super.onCreate(savedInstanceState);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        applyTheme();
    }
    
    protected void applyTheme() {
        Window window = getWindow();
        if (window != null) {
            themeManager.applyTheme(window);
        }
    }
    
    public ThemeManager getThemeManager() {
        return themeManager;
    }
    
    public void setThemeMode(int mode) {
        themeManager.setThemeMode(mode);
        recreate();
    }
}
