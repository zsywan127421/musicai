package com.example.musicai;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicai.util.ThemeManager;
import com.example.musicai.util.ToolbarHelper;

public abstract class BaseActivity extends AppCompatActivity {
    
    protected ThemeManager themeManager;
    protected ToolbarHelper toolbarHelper;
    protected View toolbarContainer;
    protected ImageButton btnBack;
    protected ImageButton btnMenu;
    protected TextView tvTitle;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeManager = ThemeManager.getInstance(this);
        super.onCreate(savedInstanceState);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        applyTheme();
        updateToolbarColors();
    }
    
    protected void applyTheme() {
        Window window = getWindow();
        if (window != null) {
            themeManager.applyTheme(window);
        }
    }
    
    protected void updateToolbarColors() {
        if (toolbarContainer != null) {
            int bgColor = themeManager.isDarkMode() ? 
                getColor(R.color.dark_surface) : getColor(R.color.light_surface);
            toolbarContainer.setBackgroundColor(bgColor);
        }
    }
    
    protected void initToolbar(int containerId, String title) {
        toolbarContainer = findViewById(containerId);
        if (toolbarContainer == null) return;
        
        btnBack = toolbarContainer.findViewById(R.id.btn_back);
        btnMenu = toolbarContainer.findViewById(R.id.btn_menu);
        tvTitle = toolbarContainer.findViewById(R.id.tv_title);
        
        if (tvTitle != null) {
            tvTitle.setText(title);
        }
        
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }
        
        toolbarHelper = new ToolbarHelper(this, toolbarContainer);
        updateToolbarColors();
    }
    
    protected void setToolbarTitle(String title) {
        if (tvTitle != null) {
            tvTitle.setText(title);
        }
    }
    
    protected void setBackVisible(boolean visible) {
        if (btnBack != null) {
            btnBack.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
    
    protected void setMenuVisible(boolean visible) {
        if (btnMenu != null) {
            btnMenu.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
    
    public ThemeManager getThemeManager() {
        return themeManager;
    }
    
    public void setThemeMode(int mode) {
        themeManager.setThemeMode(mode);
        recreate();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (toolbarHelper != null) {
            toolbarHelper.destroy();
        }
    }
}
