package com.example.musicai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.musicai.util.ConfirmDialog;
import com.example.musicai.util.ToolbarHelper;

import java.util.Arrays;

public class MainActivity extends BaseActivity {
    
    private static final int MENU_SETTINGS = 1;
    private static final int MENU_MODEL_CONFIG = 2;
    private static final int MENU_ABOUT = 3;
    
    private ModelConfig modelConfig;
    private ImageView ivStatusIcon;
    private TextView tvModelStatus;
    private TextView tvModelName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        modelConfig = new ModelConfig(this);
        
        initToolbar("MusicAI");
        setupMenu();
        initViews();
        updateModelStatus();
    }
    
    private void initToolbar(String title) {
        View toolbar = findViewById(R.id.toolbar);
        initToolbar(R.id.toolbar, title);
        setBackVisible(false);
        setMenuVisible(true);
    }
    
    private void setupMenu() {
        java.util.List<ToolbarHelper.MenuItemData> menuItems = Arrays.asList(
            new ToolbarHelper.MenuItemData(MENU_SETTINGS, "设置", R.drawable.ic_settings),
            new ToolbarHelper.MenuItemData(MENU_MODEL_CONFIG, "大模型配置", R.drawable.ic_settings),
            new ToolbarHelper.MenuItemData(MENU_ABOUT, "关于", R.drawable.ic_info)
        );
        
        toolbarHelper.setMenuItems(menuItems, itemId -> {
            if (itemId == MENU_SETTINGS) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            } else if (itemId == MENU_MODEL_CONFIG) {
                startActivity(new Intent(MainActivity.this, ModelConfigActivity.class));
            } else if (itemId == MENU_ABOUT) {
                showAboutDialog();
            }
        });
    }
    
    private void initViews() {
        ivStatusIcon = findViewById(R.id.iv_status_icon);
        tvModelStatus = findViewById(R.id.tv_model_status);
        tvModelName = findViewById(R.id.tv_model_name);
        
        Button btnSongGenerator = findViewById(R.id.btn_new_song_generator);
        Button btnLibrary = findViewById(R.id.btn_library);
        Button btnSongEditor = findViewById(R.id.btn_song_editor);
        Button btnSettings = findViewById(R.id.btn_settings);
        Button btnModelConfig = findViewById(R.id.btn_model_config);
        
        btnSongGenerator.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NewSongGeneratorActivity.class));
        });
        
        btnLibrary.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LibraryTabsActivity.class));
        });
        
        btnSongEditor.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SongEditorActivity.class));
        });
        
        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });
        
        btnModelConfig.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ModelConfigActivity.class));
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateModelStatus();
    }
    
    private void updateModelStatus() {
        if (ivStatusIcon == null || tvModelStatus == null) return;
        
        String apiKey = modelConfig.getApiKey();
        String modelName = modelConfig.getModelName();
        
        if (apiKey.isEmpty()) {
            ivStatusIcon.setImageResource(R.drawable.ic_warning_circle);
            ivStatusIcon.setColorFilter(getResources().getColor(R.color.apple_warning));
            tvModelStatus.setText("大模型未配置");
            tvModelStatus.setTextColor(getResources().getColor(R.color.apple_warning));
            tvModelName.setText("请在设置中配置");
        } else {
            ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
            ivStatusIcon.setColorFilter(getResources().getColor(R.color.apple_success));
            tvModelStatus.setText("大模型已配置");
            tvModelStatus.setTextColor(getResources().getColor(R.color.apple_text));
            tvModelName.setText(modelName);
        }
    }
    
    private void showAboutDialog() {
        String message = "MusicAI - AI音乐创作助手\n\n版本: 1.0.0\n\n基于大语言模型驱动的智能音乐生成应用，支持旋律、和弦、歌曲的AI创作。\n\n© 2024 MusicAI";
        new android.app.AlertDialog.Builder(this)
            .setTitle("关于")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show();
    }
}
