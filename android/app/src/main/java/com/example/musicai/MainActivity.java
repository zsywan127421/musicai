package com.example.musicai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends BaseActivity {
    
    private ModelConfig modelConfig;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        modelConfig = new ModelConfig(this);
        
        Button btnSongGenerator = findViewById(R.id.btn_song_generator);
        Button btnNewSongGenerator = findViewById(R.id.btn_new_song_generator);
        Button btnLibrary = findViewById(R.id.btn_library);
        Button btnSettings = findViewById(R.id.btn_settings);
        Button btnModelConfig = findViewById(R.id.btn_model_config);
        Button btnProjectEditor = findViewById(R.id.btn_project_editor);
        
        updateModelStatus();
        
        btnNewSongGenerator.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NewSongGeneratorActivity.class));
        });
        
        btnSongGenerator.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SongGeneratorActivity.class));
        });
        
        btnLibrary.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LibraryActivity.class));
        });
        
        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });
        
        btnModelConfig.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ModelConfigActivity.class));
        });
        
        btnProjectEditor.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProjectEditorActivity.class));
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateModelStatus();
    }
    
    private void updateModelStatus() {
        ImageView ivStatusIcon = findViewById(R.id.iv_status_icon);
        TextView tvModelStatus = findViewById(R.id.tv_model_status);
        TextView tvModelName = findViewById(R.id.tv_model_name);
        
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
