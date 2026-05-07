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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class SongGeneratorActivity extends AppCompatActivity {
    
    private static final int STEP_NOT_STARTED = 0;
    private static final int STEP_ACTIVE = 1;
    private static final int STEP_COMPLETED = 2;
    
    private MusicGenerator musicGenerator;
    
    private MusicData.Melody melodyData;
    private MusicData.ChordProgression chordProgression;
    private MusicData.Song currentSong;
    
    private ListView lvSong;
    private ArrayAdapter<String> songAdapter;
    private List<String> songList;
    private ProgressBar progressBar;
    private ProgressBar playbackProgress;
    private TextView tvPlaybackTime;
    
    private View step1Indicator, step2Indicator, step3Indicator, step4Indicator;
    private TextView step1Status, step2Status, step3Status, step4Status;
    private TextView tvMelodyPreview, tvChordsPreview, tvSongTitle;
    private EditText etDescription;
    private Button btnGenerateMelody, btnGenerateChords, btnGenerateSong;
    private Button btnPlay, btnStop;
    private CheckBox cbAutoChain;
    
    private MusicPlayerService playerService;
    private boolean isBound = false;
    private boolean isGenerating = false;
    private boolean autoChainEnabled = false;
    
    private Handler progressHandler = new Handler();
    private Runnable progressUpdateRunnable;
    
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
        melodyData = new MusicData.Melody();
        chordProgression = new MusicData.ChordProgression();
        currentSong = new MusicData.Song();
        
        initViews();
        setupListeners();
        setupStyleSpinner();
        updateUI();
    }
    
    private void initViews() {
        step1Indicator = findViewById(R.id.step1_indicator);
        step2Indicator = findViewById(R.id.step2_indicator);
        step3Indicator = findViewById(R.id.step3_indicator);
        step4Indicator = findViewById(R.id.step4_indicator);
        
        step1Status = findViewById(R.id.step1_status);
        step2Status = findViewById(R.id.step2_status);
        step3Status = findViewById(R.id.step3_status);
        step4Status = findViewById(R.id.step4_status);
        
        tvMelodyPreview = findViewById(R.id.tv_melody_preview);
        tvChordsPreview = findViewById(R.id.tv_chords_preview);
        tvSongTitle = findViewById(R.id.tv_song_title);
        etDescription = findViewById(R.id.et_description);
        
        btnGenerateMelody = findViewById(R.id.btn_generate_melody);
        btnGenerateChords = findViewById(R.id.btn_generate_chords);
        btnGenerateSong = findViewById(R.id.btn_generate_song);
        btnPlay = findViewById(R.id.btn_play);
        btnStop = findViewById(R.id.btn_stop);
        
        cbAutoChain = findViewById(R.id.cb_auto_chain);
        
        lvSong = findViewById(R.id.lv_song);
        progressBar = findViewById(R.id.progress_bar);
        playbackProgress = findViewById(R.id.playback_progress);
        tvPlaybackTime = findViewById(R.id.tv_playback_time);
        
        songList = new ArrayList<>();
        songAdapter = new ArrayAdapter<>(this, R.layout.list_item_note, songList);
        lvSong.setAdapter(songAdapter);
    }
    
    private void setupListeners() {
        btnGenerateMelody.setOnClickListener(v -> generateMelody());
        btnGenerateChords.setOnClickListener(v -> generateChords());
        btnGenerateSong.setOnClickListener(v -> generateSong());
        
        btnPlay.setOnClickListener(v -> playSong());
        btnStop.setOnClickListener(v -> stopSong());
        
        cbAutoChain.setOnCheckedChangeListener((buttonView, isChecked) -> {
            autoChainEnabled = isChecked;
        });
    }
    
    private void setupStyleSpinner() {
        Spinner spStyle = findViewById(R.id.sp_style);
        ArrayAdapter<String> styleAdapter = new ArrayAdapter<>(this, 
            R.layout.spinner_item, MusicData.MUSIC_STYLES);
        styleAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spStyle.setAdapter(styleAdapter);
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
        if (progressUpdateRunnable != null) {
            progressHandler.removeCallbacks(progressUpdateRunnable);
        }
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }
    
    private void updateStepIndicator(View indicator, TextView status, int state, String statusText) {
        indicator.setBackgroundResource(
            state == STEP_COMPLETED ? R.drawable.step_indicator_completed :
            state == STEP_ACTIVE ? R.drawable.step_indicator_active :
            R.drawable.step_indicator_pending
        );
        status.setText(statusText);
    }
    
    private void updateUI() {
        boolean hasMelody = melodyData != null && !melodyData.notes.isEmpty();
        boolean hasChords = chordProgression != null && !chordProgression.chords.isEmpty();
        boolean hasSong = currentSong.melody != null && !currentSong.melody.notes.isEmpty();
        
        btnGenerateChords.setEnabled(hasMelody && !isGenerating);
        btnGenerateSong.setEnabled(hasMelody && hasChords && !isGenerating);
        btnPlay.setEnabled(hasSong && !isGenerating);
        btnStop.setEnabled(playerService != null && playerService.isPlaying());
        
        btnGenerateMelody.setEnabled(!isGenerating);
        btnGenerateMelody.setText(isGenerating ? "生成中..." : "生成旋律");
        btnGenerateChords.setText(isGenerating ? "生成中..." : "生成和弦");
        btnGenerateSong.setText(isGenerating ? "生成中..." : "生成完整曲子");
        
        progressBar.setVisibility(isGenerating ? View.VISIBLE : View.GONE);
        
        if (hasMelody) {
            updateMelodyPreview();
        }
        if (hasChords) {
            updateChordsPreview();
        }
        if (hasSong) {
            tvSongTitle.setText("标题: " + currentSong.title);
        }
    }
    
    private void updateMelodyPreview() {
        if (melodyData == null || melodyData.notes.isEmpty()) {
            tvMelodyPreview.setText("尚未生成旋律");
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        int count = Math.min(melodyData.notes.size(), 8);
        for (int i = 0; i < count; i++) {
            MusicData.Note note = melodyData.notes.get(i);
            if (sb.length() > 0) sb.append(" → ");
            sb.append(note.toString());
        }
        if (melodyData.notes.size() > 8) {
            sb.append(" ... (+").append(melodyData.notes.size() - 8).append(")");
        }
        tvMelodyPreview.setText(sb.toString());
    }
    
    private void updateChordsPreview() {
        if (chordProgression == null || chordProgression.chords.isEmpty()) {
            tvChordsPreview.setText("尚未生成和弦");
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        for (MusicData.Chord chord : chordProgression.chords) {
            if (sb.length() > 0) sb.append(" → ");
            sb.append(chord.toString());
        }
        tvChordsPreview.setText(sb.toString());
    }
    
    private void generateMelody() {
        if (isGenerating) return;
        
        isGenerating = true;
        updateUI();
        
        updateStepIndicator(step1Indicator, step1Status, STEP_COMPLETED, "已完成");
        updateStepIndicator(step2Indicator, step2Status, STEP_ACTIVE, "生成中...");
        
        String style = (String) ((Spinner) findViewById(R.id.sp_style)).getSelectedItem();
        String description = etDescription.getText().toString().trim();
        
        new Thread(() -> {
            try {
                melodyData = musicGenerator.generateMelodyWithDescription(style, 8, null, description);
                
                runOnUiThread(() -> {
                    updateStepIndicator(step2Indicator, step2Status, STEP_COMPLETED, "已完成");
                    updateMelodyPreview();
                    updateSongList();
                    Toast.makeText(SongGeneratorActivity.this, "旋律生成完成！", Toast.LENGTH_SHORT).show();
                    
                    isGenerating = false;
                    updateUI();
                    
                    if (autoChainEnabled && btnGenerateChords.isEnabled()) {
                        generateChords();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    updateStepIndicator(step2Indicator, step2Status, STEP_NOT_STARTED, "失败");
                    Toast.makeText(SongGeneratorActivity.this, "旋律生成失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    isGenerating = false;
                    updateUI();
                });
            }
        }).start();
    }
    
    private void generateChords() {
        if (isGenerating) return;
        if (melodyData == null || melodyData.notes.isEmpty()) {
            Toast.makeText(this, "请先生成旋律", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isGenerating = true;
        updateUI();
        
        updateStepIndicator(step3Indicator, step3Status, STEP_ACTIVE, "生成中...");
        
        String style = (String) ((Spinner) findViewById(R.id.sp_style)).getSelectedItem();
        
        new Thread(() -> {
            try {
                chordProgression = musicGenerator.generateChordsWithMelody(style, 4, null, melodyData);
                
                runOnUiThread(() -> {
                    updateStepIndicator(step3Indicator, step3Status, STEP_COMPLETED, "已完成");
                    updateChordsPreview();
                    updateSongList();
                    Toast.makeText(SongGeneratorActivity.this, "和弦生成完成！", Toast.LENGTH_SHORT).show();
                    
                    isGenerating = false;
                    updateUI();
                    
                    if (autoChainEnabled && btnGenerateSong.isEnabled()) {
                        generateSong();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    updateStepIndicator(step3Indicator, step3Status, STEP_NOT_STARTED, "失败");
                    Toast.makeText(SongGeneratorActivity.this, "和弦生成失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    isGenerating = false;
                    updateUI();
                });
            }
        }).start();
    }
    
    private void generateSong() {
        if (isGenerating) return;
        if (melodyData == null || melodyData.notes.isEmpty()) {
            Toast.makeText(this, "请先生成旋律", Toast.LENGTH_SHORT).show();
            return;
        }
        if (chordProgression == null || chordProgression.chords.isEmpty()) {
            Toast.makeText(this, "请先生成和弦", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isGenerating = true;
        updateUI();
        
        updateStepIndicator(step4Indicator, step4Status, STEP_ACTIVE, "生成中...");
        
        String style = (String) ((Spinner) findViewById(R.id.sp_style)).getSelectedItem();
        
        new Thread(() -> {
            try {
                currentSong = musicGenerator.generateCompleteSong(style, melodyData, chordProgression);
                
                runOnUiThread(() -> {
                    updateStepIndicator(step4Indicator, step4Status, STEP_COMPLETED, "已完成");
                    tvSongTitle.setText("标题: " + currentSong.title);
                    updateSongList();
                    Toast.makeText(SongGeneratorActivity.this, "曲子生成完成！", Toast.LENGTH_SHORT).show();
                    isGenerating = false;
                    updateUI();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    updateStepIndicator(step4Indicator, step4Status, STEP_NOT_STARTED, "失败");
                    Toast.makeText(SongGeneratorActivity.this, "曲子生成失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    isGenerating = false;
                    updateUI();
                });
            }
        }).start();
    }
    
    private void playSong() {
        if (currentSong.melody == null || currentSong.melody.notes.isEmpty()) {
            Toast.makeText(this, "没有可播放的歌曲", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (isBound && playerService != null) {
            playerService.playSong(currentSong);
            playbackProgress.setVisibility(View.VISIBLE);
            tvPlaybackTime.setVisibility(View.VISIBLE);
            startProgressUpdate();
            btnPlay.setEnabled(false);
            btnStop.setEnabled(true);
            Toast.makeText(this, "开始播放", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void stopSong() {
        if (isBound && playerService != null) {
            playerService.stopPlayback();
            playbackProgress.setVisibility(View.GONE);
            tvPlaybackTime.setVisibility(View.GONE);
            playbackProgress.setProgress(0);
            if (progressUpdateRunnable != null) {
                progressHandler.removeCallbacks(progressUpdateRunnable);
            }
            btnPlay.setEnabled(true);
            btnStop.setEnabled(false);
            Toast.makeText(this, "已停止", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void startProgressUpdate() {
        if (progressUpdateRunnable != null) {
            progressHandler.removeCallbacks(progressUpdateRunnable);
        }
        
        progressUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isBound && playerService != null && playerService.isPlaying()) {
                    updateProgressDisplay();
                    progressHandler.postDelayed(this, 100);
                } else {
                    playbackProgress.setProgress(0);
                    tvPlaybackTime.setText("0:00 / 0:00");
                    btnPlay.setEnabled(true);
                    btnStop.setEnabled(false);
                }
            }
        };
        
        progressHandler.post(progressUpdateRunnable);
    }
    
    private void updateProgressDisplay() {
        int currentPosition = playerService.getCurrentPosition();
        int totalDuration = playerService.getDuration();
        
        if (totalDuration > 0) {
            int progress = (currentPosition * 100) / totalDuration;
            playbackProgress.setProgress(progress);
            
            String currentTime = formatTime(currentPosition);
            String totalTime = formatTime(totalDuration);
            tvPlaybackTime.setText(currentTime + " / " + totalTime);
        }
    }
    
    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    private void updateSongList() {
        songList.clear();
        
        if (currentSong != null && currentSong.title != null && !currentSong.title.isEmpty()) {
            songList.add("标题: " + currentSong.title);
            songList.add("风格: " + currentSong.style);
            songList.add("");
        }
        
        if (melodyData != null && !melodyData.notes.isEmpty()) {
            songList.add("--- 旋律 (" + melodyData.notes.size() + "个音符) ---");
            int noteIndex = 1;
            for (MusicData.Note note : melodyData.notes) {
                songList.add("  " + noteIndex++ + ". " + note.toString());
            }
        }
        
        if (chordProgression != null && !chordProgression.chords.isEmpty()) {
            songList.add("");
            songList.add("--- 和弦 ---");
            int chordIndex = 1;
            for (MusicData.Chord chord : chordProgression.chords) {
                songList.add("  " + chordIndex++ + ". " + chord.toString());
            }
        }
        
        songAdapter.notifyDataSetChanged();
    }
}
