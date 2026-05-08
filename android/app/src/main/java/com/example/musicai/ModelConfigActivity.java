package com.example.musicai;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.musicai.util.ToastHelper;
import com.example.musicai.util.ToolbarHelper;

import java.util.ArrayList;
import java.util.List;

public class ModelConfigActivity extends BaseActivity {

    private static final int MENU_RESET = 1;
    private static final int MENU_TEST = 2;
    private static final int MENU_HELP = 3;

    private EditText etApiUrl, etApiKey, etModelName;
    private SeekBar seekbarTemperature, seekbarMaxTokens;
    private TextView tvTemperatureValue, tvMaxTokensValue, tvTestResult;
    private ImageView ivTestStatus;
    private Button btnSave, btnReset;

    private ModelConfig modelConfig;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_config);

        initToolbar(R.id.toolbar, "大模型配置");
        setBackVisible(true);
        setMenuVisible(true);

        List<ToolbarHelper.MenuItemData> menuItems = new ArrayList<>();
        menuItems.add(new ToolbarHelper.MenuItemData(MENU_RESET, "恢复默认配置"));
        menuItems.add(new ToolbarHelper.MenuItemData(MENU_TEST, "测试连接"));
        menuItems.add(new ToolbarHelper.MenuItemData(MENU_HELP, "帮助"));
        toolbarHelper.setMenuItems(menuItems, this::onMenuItemClick);

        modelConfig = new ModelConfig(this);
        handler = new Handler(Looper.getMainLooper());

        initViews();
        loadConfig();
        setupListeners();
    }

    private void initViews() {
        etApiUrl = findViewById(R.id.et_api_url);
        etApiKey = findViewById(R.id.et_api_key);
        etModelName = findViewById(R.id.et_model_name);
        seekbarTemperature = findViewById(R.id.seekbar_temperature);
        seekbarMaxTokens = findViewById(R.id.seekbar_max_tokens);
        tvTemperatureValue = findViewById(R.id.tv_temperature_value);
        tvMaxTokensValue = findViewById(R.id.tv_max_tokens_value);
        tvTestResult = findViewById(R.id.tv_test_result);
        ivTestStatus = findViewById(R.id.iv_test_status);
        btnSave = findViewById(R.id.btn_save);
        btnReset = findViewById(R.id.btn_reset);
    }

    private void loadConfig() {
        etApiUrl.setText(modelConfig.getApiUrl());
        etApiKey.setText(modelConfig.getApiKey());
        etModelName.setText(modelConfig.getModelName());

        double temperature = modelConfig.getTemperature();
        seekbarTemperature.setProgress((int) (temperature * 10));
        tvTemperatureValue.setText(String.format("%.1f", temperature));

        int maxTokens = modelConfig.getMaxTokens();
        maxTokens = Math.max(1000, Math.min(2500, maxTokens));
        seekbarMaxTokens.setProgress(maxTokens);
        tvMaxTokensValue.setText(String.valueOf(maxTokens));
    }

    private void setupListeners() {
        seekbarTemperature.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double value = progress / 10.0;
                tvTemperatureValue.setText(String.format("%.1f", value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekbarMaxTokens.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = (progress / 100) * 100;
                if (progress % 100 >= 50) {
                    value += 100;
                }
                value = Math.max(1000, Math.min(2500, value));
                tvMaxTokensValue.setText(String.valueOf(value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        findViewById(R.id.btn_test_connection).setOnClickListener(v -> testConnection());
        btnSave.setOnClickListener(v -> saveConfig());
        btnReset.setOnClickListener(v -> resetConfig());
    }

    private void onMenuItemClick(int itemId) {
        if (itemId == MENU_RESET) {
            ToastHelper.showInfo(this, "配置已恢复默认");
        } else if (itemId == MENU_TEST) {
            ToastHelper.showInfo(this, "连接测试功能开发中");
        } else if (itemId == MENU_HELP) {
            ToastHelper.showInfo(this, "帮助功能开发中");
        }
    }

    private void testConnection() {
        String apiUrl = etApiUrl.getText().toString().trim();
        String apiKey = etApiKey.getText().toString().trim();
        String modelName = etModelName.getText().toString().trim();

        if (apiKey.isEmpty()) {
            tvTestResult.setText("请先输入 API Key");
            tvTestResult.setTextColor(getResources().getColor(R.color.apple_warning));
            return;
        }

        tvTestResult.setText("正在测试连接...");
        tvTestResult.setTextColor(getResources().getColor(R.color.apple_text_secondary));
        ivTestStatus.setVisibility(View.GONE);

        new Thread(() -> {
            modelConfig.setApiUrl(apiUrl);
            modelConfig.setApiKey(apiKey);
            modelConfig.setModelName(modelName);

            String result = modelConfig.testConnection();

            handler.post(() -> {
                if (result.startsWith("SUCCESS:")) {
                    tvTestResult.setText(result.substring(8));
                    tvTestResult.setTextColor(getResources().getColor(R.color.apple_success));
                    ivTestStatus.setImageResource(R.drawable.ic_check_circle);
                    ivTestStatus.setColorFilter(getResources().getColor(R.color.apple_success));
                    ivTestStatus.setVisibility(View.VISIBLE);
                } else {
                    tvTestResult.setText(result.substring(7));
                    tvTestResult.setTextColor(getResources().getColor(R.color.apple_danger));
                    ivTestStatus.setImageResource(R.drawable.ic_warning_circle);
                    ivTestStatus.setColorFilter(getResources().getColor(R.color.apple_danger));
                    ivTestStatus.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }

    private void saveConfig() {
        String apiUrl = etApiUrl.getText().toString().trim();
        String apiKey = etApiKey.getText().toString().trim();
        String modelName = etModelName.getText().toString().trim();
        double temperature = seekbarTemperature.getProgress() / 10.0;
        
        int progress = seekbarMaxTokens.getProgress();
        int maxTokens = (progress / 100) * 100;
        if (progress % 100 >= 50) {
            maxTokens += 100;
        }
        maxTokens = Math.max(1000, Math.min(2500, maxTokens));

        if (apiKey.isEmpty()) {
            ToastHelper.showWarning(this, "API Key 不能为空");
            return;
        }

        modelConfig.setApiUrl(apiUrl);
        modelConfig.setApiKey(apiKey);
        modelConfig.setModelName(modelName);
        modelConfig.setTemperature(temperature);
        modelConfig.setMaxTokens(maxTokens);

        ToastHelper.showSuccess(this, "配置已保存");
        finish();
    }

    private void resetConfig() {
        modelConfig.resetToDefaults();
        loadConfig();
        ToastHelper.showInfo(this, "已恢复默认设置");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}
