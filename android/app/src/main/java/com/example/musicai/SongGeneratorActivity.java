package com.example.musicai;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicai.util.ToastHelper;

public class SongGeneratorActivity extends AppCompatActivity {
    
    private Spinner spStyle;
    private EditText etName;
    private TextView tvResult;
    private View resultSection;
    
    private Button btnGenerate, btnPlay, btnStop;
    private Button btnSpeed05, btnSpeed075, btnSpeed1, btnSpeed125, btnSpeed15, btnSpeed2;
    private TextView tvSpeed, tvPlaybackTime;
    private CursorSeekBar playbackProgress;
    private ProgressBar progressBar;
    private View speedControlLayout;
    
    private MusicGenerator musicGenerator;
    private MusicData.Song currentSong;
    
    private MusicPlayerService playerService;
    private boolean isBound = false;
    private boolean isGenerating = false;
    private float playbackSpeed = 1.0f;
    
    private Handler handler = new Handler();
    private Runnable progressUpdater;
    
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MusicPlayerService.LocalBinder binder = (MusicPlayerService.LocalBinder) service;
            playerService = binder.getService();
            isBound = true;
        }
        
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_generator);
        
        musicGenerator = new MusicGenerator(this);
        currentSong = new MusicData.Song();
        
        initViews();
        setupSpinners();
        setupListeners();
    }
    
    private void initViews() {
        spStyle = findViewById(R.id.sp_style);
        etName = findViewById(R.id.et_name);
        tvResult = findViewById(R.id.tv_result);
        resultSection = findViewById(R.id.result_section);
        
        btnGenerate = findViewById(R.id.btn_generate);
        btnPlay = findViewById(R.id.btn_play);
        btnStop = findViewById(R.id.btn_stop);
        tvPlaybackTime = findViewById(R.id.tv_playback_time);
        playbackProgress = findViewById(R.id.playback_progress);
        progressBar = findViewById(R.id.progress_bar);
        speedControlLayout = findViewById(R.id.speed_control_layout);
        tvSpeed = findViewById(R.id.tv_speed);
        
        btnSpeed05 = findViewById(R.id.btn_speed_05);
        btnSpeed075 = findViewById(R.id.btn_speed_075);
        btnSpeed1 = findViewById(R.id.btn_speed_1);
        btnSpeed125 = findViewById(R.id.btn_speed_125);
        btnSpeed15 = findViewById(R.id.btn_speed_15);
        btnSpeed2 = findViewById(R.id.btn_speed_2);
        
        tvPlaybackTime.setVisibility(View.GONE);
        playbackProgress.setVisibility(View.GONE);
        speedControlLayout.setVisibility(View.GONE);
        
        btnPlay.setEnabled(false);
        btnStop.setEnabled(false);
    }
    
    private void setupSpinners() {
        ArrayAdapter<String> styleAdapter = new ArrayAdapter<>(this, 
            R.layout.spinner_item, MusicData.MUSIC_STYLES);
        styleAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spStyle.setAdapter(styleAdapter);
    }
    
    private void setupListeners() {
        btnGenerate.setOnClickListener(v -> generate());
        btnPlay.setOnClickListener(v -> play());
        btnStop.setOnClickListener(v -> stop());
        
        btnSpeed05.setOnClickListener(v -> setSpeed(0.5f));
        btnSpeed075.setOnClickListener(v -> setSpeed(0.75f));
        btnSpeed1.setOnClickListener(v -> setSpeed(1.0f));
        btnSpeed125.setOnClickListener(v -> setSpeed(1.25f));
        btnSpeed15.setOnClickListener(v -> setSpeed(1.5f));
        btnSpeed2.setOnClickListener(v -> setSpeed(2.0f));
        
        playbackProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (isBound && playerService != null && playerService.isPlaying()) {
                    int currentProgress = playbackProgress.getProgress();
                    int posMs = (int) (playerService.getDuration() * (long) currentProgress / 100L);
                    playerService.seekTo(posMs);
                }
            }
        });
    }
    
    private void generate() {
        if (isGenerating) return;
        
        String style = (String) spStyle.getSelectedItem();
        
        isGenerating = true;
        btnGenerate.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        resultSection.setVisibility(View.GONE);
        
        new Thread(() -> {
            try {
                MusicData.Melody melody = musicGenerator.generateMelodyWithDescription(style, 8, null, null);
                
                MusicData.ChordProgression chords = musicGenerator.generateChordsWithMelody(style, 4, null, melody);
                
                MusicData.Song song = musicGenerator.generateCompleteSong(style, melody, chords);
                
                String customName = etName.getText().toString().trim();
                if (!customName.isEmpty()) {
                    song.title = customName;
                }
                
                currentSong = song;
                currentSong.melody = melody;
                currentSong.chords = chords;
                
                runOnUiThread(() -> {
                    updateResultDisplay();
                    resultSection.setVisibility(View.VISIBLE);
                    
                    btnPlay.setEnabled(true);
                    tvPlaybackTime.setVisibility(View.VISIBLE);
                    playbackProgress.setVisibility(View.VISIBLE);
                    speedControlLayout.setVisibility(View.VISIBLE);
                    
                    ToastHelper.showSuccess(SongGeneratorActivity.this, "曲子生成成功！");
                    
                    isGenerating = false;
                    btnGenerate.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    ToastHelper.showError(SongGeneratorActivity.this, "生成失败: " + e.getMessage());
                    isGenerating = false;
                    btnGenerate.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                });
            }
        }).start();
    }
    
    private void updateResultDisplay() {
        StringBuilder sb = new StringBuilder();
        sb.append("标题: ").append(currentSong.title != null ? currentSong.title : "未命名").append("\n\n");
        sb.append("风格: ").append(currentSong.style).append("\n\n");
        
        if (currentSong.melody != null && !currentSong.melody.notes.isEmpty()) {
            sb.append("--- 旋律 (").append(currentSong.melody.notes.size()).append("个音符) ---\n");
            int count = Math.min(currentSong.melody.notes.size(), 8);
            for (int i = 0; i < count; i++) {
                MusicData.Note note = currentSong.melody.notes.get(i);
                sb.append(String.format("%d. %s%d | 时值:%d\n", i + 1, note.pitch, note.octave, note.duration));
            }
            if (currentSong.melody.notes.size() > 8) {
                sb.append("... (+").append(currentSong.melody.notes.size() - 8).append(")\n");
            }
            sb.append("\n");
        }
        
        if (currentSong.chords != null && !currentSong.chords.chords.isEmpty()) {
            sb.append("--- 和弦 (").append(currentSong.chords.chords.size()).append("个) ---\n");
            for (int i = 0; i < currentSong.chords.chords.size(); i++) {
                MusicData.Chord chord = currentSong.chords.chords.get(i);
                sb.append(String.format("%d. %s %s\n", i + 1, chord.name, chord.type));
            }
        }
        
        tvResult.setText(sb.toString());
    }
    
    private void play() {
        if (currentSong.melody == null || currentSong.melody.notes.isEmpty()) {
            ToastHelper.showWarning(this, "暂无可播放内容");
            return;
        }
        
        if (!isBound || playerService == null) {
            ToastHelper.showError(this, "播放器服务未连接");
            return;
        }
        
        if (playerService.isPlaying()) {
            playerService.pause();
            btnPlay.setText("播放");
        } else {
            playerService.playSong(currentSong);
            playerService.setSpeed(playbackSpeed);
            btnPlay.setText("暂停");
            startProgressUpdater();
        }
    }
    
    private void stop() {
        if (isBound && playerService != null) {
            playerService.stopPlayback();
        }
        btnPlay.setText("播放");
        playbackProgress.setProgress(0);
        tvPlaybackTime.setText("0:00 / 0:00");
        stopProgressUpdater();
    }
    
    private void setSpeed(float speed) {
        playbackSpeed = speed;
        if (isBound && playerService != null) {
            playerService.setSpeed(speed);
        }
        tvSpeed.setText(String.format("速度: %.2fx", speed));
        
        btnSpeed05.setBackgroundResource(R.drawable.apple_button_bg);
        btnSpeed05.setTextColor(getColor(R.color.apple_text));
        btnSpeed075.setBackgroundResource(R.drawable.apple_button_bg);
        btnSpeed075.setTextColor(getColor(R.color.apple_text));
        btnSpeed1.setBackgroundResource(R.drawable.apple_button_bg);
        btnSpeed1.setTextColor(getColor(R.color.apple_text));
        btnSpeed125.setBackgroundResource(R.drawable.apple_button_bg);
        btnSpeed125.setTextColor(getColor(R.color.apple_text));
        btnSpeed15.setBackgroundResource(R.drawable.apple_button_bg);
        btnSpeed15.setTextColor(getColor(R.color.apple_text));
        btnSpeed2.setBackgroundResource(R.drawable.apple_button_bg);
        btnSpeed2.setTextColor(getColor(R.color.apple_text));
        
        Button selectedBtn;
        switch ((int)(speed * 100)) {
            case 50: selectedBtn = btnSpeed05; break;
            case 75: selectedBtn = btnSpeed075; break;
            case 100: selectedBtn = btnSpeed1; break;
            case 125: selectedBtn = btnSpeed125; break;
            case 150: selectedBtn = btnSpeed15; break;
            case 200: selectedBtn = btnSpeed2; break;
            default: selectedBtn = btnSpeed1;
        }
        selectedBtn.setBackgroundResource(R.drawable.apple_button_primary_bg);
        selectedBtn.setTextColor(getColor(R.color.apple_white));
    }
    
    private void startProgressUpdater() {
        stopProgressUpdater();
        progressUpdater = new Runnable() {
            @Override
            public void run() {
                if (isBound && playerService != null && playerService.isPlaying()) {
                    int current = playerService.getCurrentPosition();
                    int total = playerService.getDuration();
                    if (total > 0) {
                        int progress = (int) ((current / (float) total) * 100);
                        playbackProgress.setProgress(progress);
                        tvPlaybackTime.setText(formatTime(current) + " / " + formatTime(total));
                    }
                    handler.postDelayed(this, 100);
                }
            }
        };
        handler.post(progressUpdater);
    }
    
    private void stopProgressUpdater() {
        if (progressUpdater != null) {
            handler.removeCallbacks(progressUpdater);
        }
    }
    
    private String formatTime(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MusicPlayerService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        stopProgressUpdater();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }
}
