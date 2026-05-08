package com.example.musicai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends BaseActivity {
    
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
        initViews();
        updateModelStatus();
    }
    
    private void initToolbar(String title) {
        initToolbar(R.id.toolbar, title);
        setBackVisible(false);
        setMenuVisible(false);
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
}
