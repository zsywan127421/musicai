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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicai.util.ToastHelper;

import java.util.ArrayList;
import java.util.List;

public class SongGeneratorActivity extends AppCompatActivity {
    
    private MusicRepository repository;
    private MusicGenerator musicGenerator;
    
    private Spinner spStyle, spMelody, spChords;
    private ListView lvResult;
    private ArrayAdapter<String> resultAdapter;
    private List<String> resultList;
    
    private Button btnGenerate, btnPlay, btnStop;
    private TextView tvPlaybackTime;
    private CursorSeekBar playbackProgress;
    private ProgressBar progressBar;
    private View speedControlLayout;
    private TextView tvSpeed;
    
    private MusicData.Melody selectedMelody;
    private MusicData.ChordProgression selectedChords;
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
        
        repository = MusicRepository.getInstance(this);
        musicGenerator = new MusicGenerator(this);
        
        currentSong = new MusicData.Song();
        
        initViews();
        setupSpinners();
        setupListeners();
        loadLibraryData();
    }
    
    private void initViews() {
        spStyle = findViewById(R.id.sp_style);
        spMelody = findViewById(R.id.sp_melody);
        spChords = findViewById(R.id.sp_chords);
        lvResult = findViewById(R.id.lv_result);
        btnGenerate = findViewById(R.id.btn_generate);
        btnPlay = findViewById(R.id.btn_play);
        btnStop = findViewById(R.id.btn_stop);
        tvPlaybackTime = findViewById(R.id.tv_playback_time);
        playbackProgress = findViewById(R.id.playback_progress);
        progressBar = findViewById(R.id.progress_bar);
        speedControlLayout = findViewById(R.id.speed_control_layout);
        tvSpeed = findViewById(R.id.tv_speed);
        
        resultList = new ArrayList<>();
        resultAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, resultList);
        lvResult.setAdapter(resultAdapter);
        
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
        
        ArrayAdapter<String> melodyAdapter = new ArrayAdapter<>(this,
            R.layout.spinner_item, new String[]{"从资源库选择旋律"});
        melodyAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spMelody.setAdapter(melodyAdapter);
        
        ArrayAdapter<String> chordAdapter = new ArrayAdapter<>(this,
            R.layout.spinner_item, new String[]{"从资源库选择和弦"});
        chordAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spChords.setAdapter(chordAdapter);
    }
    
    private void setupListeners() {
        btnGenerate.setOnClickListener(v -> generate());
        btnPlay.setOnClickListener(v -> play());
        btnStop.setOnClickListener(v -> stop());
        
        Button btnSpeed05 = findViewById(R.id.btn_speed_05);
        Button btnSpeed075 = findViewById(R.id.btn_speed_075);
        Button btnSpeed1 = findViewById(R.id.btn_speed_1);
        Button btnSpeed125 = findViewById(R.id.btn_speed_125);
        Button btnSpeed15 = findViewById(R.id.btn_speed_15);
        Button btnSpeed2 = findViewById(R.id.btn_speed_2);
        
        btnSpeed05.setOnClickListener(v -> setSpeed(0.5f));
        btnSpeed075.setOnClickListener(v -> setSpeed(0.75f));
        btnSpeed1.setOnClickListener(v -> setSpeed(1.0f));
        btnSpeed125.setOnClickListener(v -> setSpeed(1.25f));
        btnSpeed15.setOnClickListener(v -> setSpeed(1.5f));
        btnSpeed2.setOnClickListener(v -> setSpeed(2.0f));
    }
    
    private void loadLibraryData() {
        List<MusicRepository.MelodyEntry> melodies = repository.getMelodyLibrary();
        List<String> melodyNames = new ArrayList<>();
        melodyNames.add("从资源库选择旋律");
        for (MusicRepository.MelodyEntry entry : melodies) {
            melodyNames.add(entry.name);
        }
        
        ArrayAdapter<String> melodyAdapter = new ArrayAdapter<>(this,
            R.layout.spinner_item, melodyNames);
        melodyAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spMelody.setAdapter(melodyAdapter);
        
        List<MusicRepository.ChordEntry> chords = repository.getChordLibrary();
        List<String> chordNames = new ArrayList<>();
        chordNames.add("从资源库选择和弦");
        for (MusicRepository.ChordEntry entry : chords) {
            chordNames.add(entry.name);
        }
        
        ArrayAdapter<String> chordAdapter = new ArrayAdapter<>(this,
            R.layout.spinner_item, chordNames);
        chordAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spChords.setAdapter(chordAdapter);
    }
    
    private void generate() {
        if (isGenerating) return;
        
        int melodyPos = spMelody.getSelectedItemPosition();
        int chordPos = spChords.getSelectedItemPosition();
        
        if (melodyPos <= 0) {
            ToastHelper.showWarning(this, "请选择一条旋律");
            return;
        }
        
        if (chordPos <= 0) {
            ToastHelper.showWarning(this, "请选择一组和弦");
            return;
        }
        
        List<MusicRepository.MelodyEntry> melodies = repository.getMelodyLibrary();
        List<MusicRepository.ChordEntry> chords = repository.getChordLibrary();
        
        int melodyIndex = melodyPos - 1;
        int chordIndex = chordPos - 1;
        
        if (melodyIndex >= melodies.size() || chordIndex >= chords.size()) {
            ToastHelper.showError(this, "选择的数据不存在，请重新选择");
            loadLibraryData();
            return;
        }
        
        selectedMelody = melodies.get(melodyIndex).toMelody();
        selectedChords = chords.get(chordIndex).toChordProgression();
        
        if (selectedMelody == null || selectedMelody.notes.isEmpty()) {
            ToastHelper.showError(this, "旋律数据为空");
            return;
        }
        
        if (selectedChords == null || selectedChords.chords.isEmpty()) {
            ToastHelper.showError(this, "和弦数据为空");
            return;
        }
        
        String style = (String) spStyle.getSelectedItem();
        
        isGenerating = true;
        btnGenerate.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        
        new Thread(() -> {
            try {
                MusicData.Song song = musicGenerator.generateCompleteSong(style, selectedMelody, selectedChords);
                
                runOnUiThread(() -> {
                    currentSong = song;
                    updateResultList();
                    ToastHelper.showSuccess(SongGeneratorActivity.this, "曲子生成成功！");
                    
                    btnPlay.setEnabled(true);
                    tvPlaybackTime.setVisibility(View.VISIBLE);
                    playbackProgress.setVisibility(View.VISIBLE);
                    speedControlLayout.setVisibility(View.VISIBLE);
                    
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
    
    private void updateResultList() {
        resultList.clear();
        
        if (currentSong != null) {
            if (currentSong.title != null && !currentSong.title.isEmpty()) {
                resultList.add("标题: " + currentSong.title);
            }
            if (currentSong.artist != null && !currentSong.artist.isEmpty()) {
                resultList.add("艺术家: " + currentSong.artist);
            }
            resultList.add("风格: " + (currentSong.style != null ? currentSong.style : "未知"));
            resultList.add("");
        }
        
        if (selectedMelody != null && !selectedMelody.notes.isEmpty()) {
            resultList.add("--- 旋律 (" + selectedMelody.notes.size() + "个音符) ---");
            int index = 1;
            for (MusicData.Note note : selectedMelody.notes) {
                resultList.add("  " + (index++) + ". " + note.toString());
            }
            resultList.add("");
        }
        
        if (selectedChords != null && !selectedChords.chords.isEmpty()) {
            resultList.add("--- 和弦 (" + selectedChords.chords.size() + "个) ---");
            int index = 1;
            for (MusicData.Chord chord : selectedChords.chords) {
                resultList.add("  " + (index++) + ". " + chord.toString());
            }
        }
        
        resultAdapter.notifyDataSetChanged();
    }
    
    private void play() {
        if (currentSong == null || currentSong.melody == null || currentSong.melody.notes.isEmpty()) {
            ToastHelper.showWarning(this, "暂无可播放内容");
            return;
        }
        
        if (!isBound || playerService == null) {
            ToastHelper.showError(this, "播放器服务未连接");
            return;
        }
        
        playerService.playSong(currentSong);
        playerService.setSpeed(playbackSpeed);
        
        btnPlay.setEnabled(false);
        btnStop.setEnabled(true);
        startProgressUpdater();
    }
    
    private void stop() {
        if (isBound && playerService != null) {
            playerService.stopPlayback();
        }
        
        btnPlay.setEnabled(true);
        btnStop.setEnabled(false);
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
    protected void onResume() {
        super.onResume();
        loadLibraryData();
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
