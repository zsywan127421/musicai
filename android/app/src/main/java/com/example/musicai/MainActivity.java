package com.example.musicai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    
    private ModelConfig modelConfig;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        modelConfig = new ModelConfig(this);
        
        Button btnMelodyEditor = findViewById(R.id.btn_melody_editor);
        Button btnChordEditor = findViewById(R.id.btn_chord_editor);
        Button btnSongGenerator = findViewById(R.id.btn_song_generator);
        Button btnNewSongGenerator = findViewById(R.id.btn_new_song_generator);
        Button btnLibrary = findViewById(R.id.btn_library);
        Button btnSettings = findViewById(R.id.btn_settings);
        Button btnGeneratorTest = findViewById(R.id.btn_generator_test);
        
        updateModelStatus();
        
        btnMelodyEditor.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, MelodyEditorActivity.class));
        });
        
        btnChordEditor.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ChordEditorActivity.class));
        });
        
        btnSongGenerator.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SongGeneratorActivity.class));
        });
        
        btnNewSongGenerator.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NewSongGeneratorActivity.class));
        });
        
        btnLibrary.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LibraryActivity.class));
        });
        
        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });
        
        btnGeneratorTest.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, GeneratorTestActivity.class));
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
