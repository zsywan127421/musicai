package com.example.musicai;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    
    private ModelConfig modelConfig;
    
    private EditText etApiUrl;
    private EditText etApiKey;
    private EditText etModelName;
    private SeekBar sbTemperature;
    private SeekBar sbMaxTokens;
    private TextView tvTemperature;
    private TextView tvMaxTokens;
    private Button btnSave;
    private Button btnReset;
    private Button btnTestConnection;
    private ProgressBar progressBar;
    private TextView tvConnectionStatus;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        modelConfig = new ModelConfig(this);
        
        etApiUrl = findViewById(R.id.et_api_url);
        etApiKey = findViewById(R.id.et_api_key);
        etModelName = findViewById(R.id.et_model_name);
        sbTemperature = findViewById(R.id.seek_temperature);
        sbMaxTokens = findViewById(R.id.seek_max_tokens);
        tvTemperature = findViewById(R.id.tv_temperature_value);
        tvMaxTokens = findViewById(R.id.tv_max_tokens_value);
        
        btnSave = findViewById(R.id.btn_save);
        btnReset = findViewById(R.id.btn_reset);
        btnTestConnection = findViewById(R.id.btn_test_connection);
        progressBar = findViewById(R.id.progress_bar);
        tvConnectionStatus = findViewById(R.id.tv_test_result);
        
        loadSettings();
        
        sbTemperature.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double temp = progress / 100.0;
                tvTemperature.setText(String.format("温度: %.2f", temp));
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        sbMaxTokens.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int tokens = progress + 100;
                tvMaxTokens.setText("最大Token: " + tokens);
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
                Toast.makeText(SettingsActivity.this, "设置已保存", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modelConfig.resetToDefaults();
                loadSettings();
                Toast.makeText(SettingsActivity.this, "已重置为默认值", Toast.LENGTH_SHORT).show();
            }
        });
        
        btnTestConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testConnection();
            }
        });
    }
    
    private void loadSettings() {
        etApiUrl.setText(modelConfig.getApiUrl());
        etApiKey.setText(modelConfig.getApiKey());
        etModelName.setText(modelConfig.getModelName());
        
        sbTemperature.setProgress((int) (modelConfig.getTemperature() * 100));
        tvTemperature.setText(String.format("温度: %.2f", modelConfig.getTemperature()));
        
        sbMaxTokens.setProgress(modelConfig.getMaxTokens() - 100);
        tvMaxTokens.setText("最大Token: " + modelConfig.getMaxTokens());
    }
    
    private void saveSettings() {
        modelConfig.setApiUrl(etApiUrl.getText().toString().trim());
        modelConfig.setApiKey(etApiKey.getText().toString().trim());
        modelConfig.setModelName(etModelName.getText().toString().trim());
        modelConfig.setTemperature(sbTemperature.getProgress() / 100.0);
        modelConfig.setMaxTokens(sbMaxTokens.getProgress() + 100);
    }
    
    private void testConnection() {
        String apiUrl = etApiUrl.getText().toString().trim();
        String apiKey = etApiKey.getText().toString().trim();
        String modelName = etModelName.getText().toString().trim();
        
        if (apiKey.isEmpty()) {
            showConnectionResult(false, "API Key 不能为空");
            return;
        }
        
        if (apiUrl.isEmpty()) {
            showConnectionResult(false, "API 地址不能为空");
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        btnTestConnection.setEnabled(false);
        tvConnectionStatus.setVisibility(View.VISIBLE);
        tvConnectionStatus.setText("正在测试连接...");
        tvConnectionStatus.setTextColor(getResources().getColor(R.color.apple_text_secondary, null));
        
        new Thread(() -> {
            try {
                ModelConfig testConfig = new ModelConfig(this);
                testConfig.setApiUrl(apiUrl);
                testConfig.setApiKey(apiKey);
                testConfig.setModelName(modelName.isEmpty() ? "deepseek-chat" : modelName);
                
                String response = testConfig.testConnection();
                
                runOnUiThread(() -> {
                    if (response.startsWith("SUCCESS")) {
                        showConnectionResult(true, response.substring(8));
                    } else {
                        showConnectionResult(false, response.substring(7));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showConnectionResult(false, "连接失败: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void showConnectionResult(boolean success, String message) {
        progressBar.setVisibility(View.GONE);
        btnTestConnection.setEnabled(true);
        tvConnectionStatus.setVisibility(View.VISIBLE);
        
        if (success) {
            tvConnectionStatus.setText("✓ " + message);
            tvConnectionStatus.setTextColor(getResources().getColor(R.color.apple_green, null));
        } else {
            tvConnectionStatus.setText("✗ " + message);
            tvConnectionStatus.setTextColor(getResources().getColor(R.color.apple_red, null));
        }
    }
}
