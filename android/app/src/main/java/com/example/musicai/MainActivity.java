package com.example.musicai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
        Button btnSettings = findViewById(R.id.btn_settings);
        TextView tvModelStatus = findViewById(R.id.tv_model_status);
        
        updateModelStatus(tvModelStatus);
        
        btnMelodyEditor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, MelodyEditorActivity.class));
            }
        });
        
        btnChordEditor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ChordEditorActivity.class));
            }
        });
        
        btnSongGenerator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SongGeneratorActivity.class));
            }
        });
        
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        TextView tvModelStatus = findViewById(R.id.tv_model_status);
        updateModelStatus(tvModelStatus);
    }
    
    private void updateModelStatus(TextView tv) {
        String apiKey = modelConfig.getApiKey();
        String modelName = modelConfig.getModelName();
        
        if (apiKey.isEmpty()) {
            tv.setText("大模型: 未配置");
            tv.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        } else {
            tv.setText("大模型: " + modelName);
            tv.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        }
    }
}
