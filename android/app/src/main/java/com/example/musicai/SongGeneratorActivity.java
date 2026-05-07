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

import com.example.musicai.util.NetworkUtils;
import com.example.musicai.util.ToastHelper;
import com.example.musicai.MusicPlayerService.PlaybackListener;

public class SongGeneratorActivity extends BaseActivity implements PlaybackListener {
    
    private Spinner spStyle;
    private EditText etName;
    private TextView tvResult;
    private View resultSection;
    
    private Button btnGenerate, btnPlay, btnStop;
    private TextView tvSpeed, tvPlaybackTime, tvGeneratingTip;
    private CursorSeekBar playbackProgress;
    private ProgressBar progressBar;
    private View speedControlLayout;
    private SeekBar seekBarSpeed;
    
    private MusicGenerator musicGenerator;
    private MusicData.Song currentSong;
    
    private MusicPlayerService playerService;
    private boolean isBound = false;
    private boolean isGenerating = false;
    private boolean isPaused = false;
    private boolean isPlayingSong = false;
    private float playbackSpeed = 1.0f;
    
    private Handler handler = new Handler();
    private Runnable progressUpdater;
    
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MusicPlayerService.LocalBinder binder = (MusicPlayerService.LocalBinder) service;
            playerService = binder.getService();
            isBound = true;
            playerService.setPlaybackListener(SongGeneratorActivity.this);
        }
        
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            if (playerService != null) {
                playerService.removePlaybackListener();
            }
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
        tvGeneratingTip = findViewById(R.id.tv_generating_tip);
        seekBarSpeed = findViewById(R.id.seekbar_speed);
        
        tvPlaybackTime.setVisibility(View.GONE);
        playbackProgress.setVisibility(View.GONE);
        speedControlLayout.setVisibility(View.GONE);
        tvGeneratingTip.setVisibility(View.GONE);
        
        btnPlay.setEnabled(false);
        btnStop.setEnabled(false);
        
        seekBarSpeed.setMax(50);
        seekBarSpeed.setProgress(10);
        tvSpeed.setText("速度: 1.0x");
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
        
        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float speed = 0.5f + (progress / 20.0f);
                speed = Math.round(speed * 100) / 100.0f;
                tvSpeed.setText(String.format("速度: %.2fx", speed));
                if (fromUser) {
                    setSpeed(speed);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
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
        
        if (!NetworkUtils.isNetworkAvailable(this)) {
            ToastHelper.showError(this, "当前无网络连接，请检查网络后重试");
            return;
        }
        
        String style = (String) spStyle.getSelectedItem();
        
        isGenerating = true;
        btnGenerate.setEnabled(false);
        btnGenerate.setText("生成中...");
        progressBar.setVisibility(View.VISIBLE);
        tvGeneratingTip.setVisibility(View.VISIBLE);
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
                    btnGenerate.setText("一键生成完整曲子");
                    progressBar.setVisibility(View.GONE);
                    tvGeneratingTip.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                final String errorMsg = e.getMessage();
                runOnUiThread(() -> {
                    if (errorMsg != null && errorMsg.contains("timeout")) {
                        ToastHelper.showError(SongGeneratorActivity.this, "网络超时（超过30秒），请稍后重试");
                    } else {
                        ToastHelper.showError(SongGeneratorActivity.this, "生成失败: " + errorMsg);
                    }
                    isGenerating = false;
                    btnGenerate.setEnabled(true);
                    btnGenerate.setText("一键生成完整曲子");
                    progressBar.setVisibility(View.GONE);
                    tvGeneratingTip.setVisibility(View.GONE);
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
            isPaused = true;
            btnPlay.setText("继续");
        } else {
            playerService.playSong(currentSong);
            playerService.setSpeed(playbackSpeed);
            isPaused = false;
            btnPlay.setText("暂停");
            startProgressUpdater();
        }
    }
    
    private void stop() {
        if (isBound && playerService != null) {
            playerService.stopPlayback();
        }
        isPaused = false;
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
    public void onPlaybackProgress(int positionMs, int totalMs, int currentNoteIndex) {}
    
    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        isPlayingSong = isPlaying;
        runOnUiThread(() -> {
            if (isPlaying) {
                btnPlay.setText("暂停");
            } else if (!isPaused) {
                btnPlay.setText("继续");
            }
        });
    }
    
    @Override
    public void onPlaybackCompleted() {
        runOnUiThread(() -> {
            isPlayingSong = false;
            isPaused = false;
            btnPlay.setText("播放");
            playbackProgress.setProgress(0);
            tvPlaybackTime.setText("0:00 / 0:00");
            stopProgressUpdater();
        });
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
