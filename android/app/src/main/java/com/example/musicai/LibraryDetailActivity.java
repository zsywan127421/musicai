package com.example.musicai;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicai.util.ToastHelper;
import com.example.musicai.util.ConfirmDialog;

public class LibraryDetailActivity extends AppCompatActivity {
    
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_ID = "id";
    public static final int TYPE_MELODY = 0;
    public static final int TYPE_CHORD = 1;
    
    private TextView tvStyle, tvCreated, tvNotes, tvChords;
    private EditText etName;
    private Button btnPlay, btnStop, btnEdit, btnDelete, btnSave;
    private Button btnSpeed05, btnSpeed075, btnSpeed1, btnSpeed125, btnSpeed15, btnSpeed2;
    private TextView tvSpeed, tvPlaybackTime;
    private CursorSeekBar playbackProgress;
    private ProgressBar progressBar;
    private View notesSection, chordsSection;
    
    private MusicRepository repository;
    private MusicData.Melody melody;
    private MusicData.ChordProgression chordProgression;
    private MusicRepository.MelodyEntry melodyEntry;
    private MusicRepository.ChordEntry chordEntry;
    private MusicPlayerService playerService;
    private boolean isBound = false;
    private int itemType;
    private String itemId;
    
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable progressUpdater;
    private boolean isPlaying = false;
    private float playbackSpeed = 1.0f;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_detail);
        
        repository = MusicRepository.getInstance(this);
        
        Intent serviceIntent = new Intent(this, MusicPlayerService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
        
        itemType = getIntent().getIntExtra(EXTRA_TYPE, TYPE_MELODY);
        itemId = getIntent().getStringExtra(EXTRA_ID);
        
        initViews();
        loadData();
        setupListeners();
    }
    
    private void initViews() {
        tvStyle = findViewById(R.id.tv_style);
        tvCreated = findViewById(R.id.tv_created);
        tvNotes = findViewById(R.id.tv_notes);
        tvChords = findViewById(R.id.tv_chords);
        etName = findViewById(R.id.et_name);
        btnPlay = findViewById(R.id.btn_play);
        btnStop = findViewById(R.id.btn_stop);
        btnEdit = findViewById(R.id.btn_edit);
        btnDelete = findViewById(R.id.btn_delete);
        btnSave = findViewById(R.id.btn_save);
        tvSpeed = findViewById(R.id.tv_speed);
        tvPlaybackTime = findViewById(R.id.tv_playback_time);
        playbackProgress = findViewById(R.id.playback_progress);
        progressBar = findViewById(R.id.progress_bar);
        notesSection = findViewById(R.id.notes_section);
        chordsSection = findViewById(R.id.chords_section);
        
        btnSpeed05 = findViewById(R.id.btn_speed_05);
        btnSpeed075 = findViewById(R.id.btn_speed_075);
        btnSpeed1 = findViewById(R.id.btn_speed_1);
        btnSpeed125 = findViewById(R.id.btn_speed_125);
        btnSpeed15 = findViewById(R.id.btn_speed_15);
        btnSpeed2 = findViewById(R.id.btn_speed_2);
        
        if (itemType == TYPE_MELODY) {
            notesSection.setVisibility(View.VISIBLE);
            chordsSection.setVisibility(View.GONE);
        } else {
            notesSection.setVisibility(View.GONE);
            chordsSection.setVisibility(View.VISIBLE);
        }
        
        btnEdit.setVisibility(View.GONE);
    }
    
    private void loadData() {
        if (itemType == TYPE_MELODY) {
            melodyEntry = repository.getMelodyById(itemId);
            melody = melodyEntry != null ? melodyEntry.toMelody() : null;
            if (melody != null) {
                etName.setText(melody.name);
                tvStyle.setText(melody.style);
                tvCreated.setText(String.valueOf(melody.createdAt));
                
                StringBuilder notesStr = new StringBuilder();
                for (int i = 0; i < melody.notes.size(); i++) {
                    MusicData.Note note = melody.notes.get(i);
                    notesStr.append(String.format("%d. %s%d | 时值:%d | 位置:%d\n",
                        i + 1, note.pitch, note.octave, note.duration, note.startTime));
                }
                tvNotes.setText(notesStr.toString());
            }
        } else {
            chordEntry = repository.getChordById(itemId);
            chordProgression = chordEntry != null ? chordEntry.toChordProgression() : null;
            if (chordProgression != null) {
                etName.setText(chordProgression.name);
                tvStyle.setText(chordProgression.style);
                tvCreated.setText(String.valueOf(chordProgression.createdAt));
                
                StringBuilder chordsStr = new StringBuilder();
                for (int i = 0; i < chordProgression.chords.size(); i++) {
                    MusicData.Chord chord = chordProgression.chords.get(i);
                    chordsStr.append(String.format("%d. %s %s | 时值:%d | 位置:%d\n",
                        i + 1, chord.name, chord.type, chord.duration, chord.startTime));
                }
                tvChords.setText(chordsStr.toString());
            }
        }
        
        if (melody == null && chordProgression == null) {
            ToastHelper.showError(this, "数据加载失败");
            finish();
        }
    }
    
    private void setupListeners() {
        btnPlay.setOnClickListener(v -> play());
        btnStop.setOnClickListener(v -> stop());
        btnEdit.setOnClickListener(v -> edit());
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnSave.setOnClickListener(v -> save());
        
        playbackProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (isBound && playerService != null && isPlaying) {
                    int currentProgress = playbackProgress.getProgress();
                    int posMs = (int) (playerService.getDuration() * (long) currentProgress / 100L);
                    playerService.seekTo(posMs);
                }
            }
        });
        
        setupSpeedButtons();
    }
    
    private void setupSpeedButtons() {
        btnSpeed05.setOnClickListener(v -> setSpeed(0.5f));
        btnSpeed075.setOnClickListener(v -> setSpeed(0.75f));
        btnSpeed1.setOnClickListener(v -> setSpeed(1.0f));
        btnSpeed125.setOnClickListener(v -> setSpeed(1.25f));
        btnSpeed15.setOnClickListener(v -> setSpeed(1.5f));
        btnSpeed2.setOnClickListener(v -> setSpeed(2.0f));
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
    
    private void play() {
        if (itemType == TYPE_MELODY && (melody == null || melody.notes.isEmpty())) {
            ToastHelper.showError(this, "暂无可播放内容");
            return;
        }
        if (itemType == TYPE_CHORD && (chordProgression == null || chordProgression.chords.isEmpty())) {
            ToastHelper.showError(this, "暂无可播放内容");
            return;
        }
        
        if (!isBound || playerService == null) {
            ToastHelper.showError(this, "播放器服务未连接");
            return;
        }
        
        if (isPlaying) {
            playerService.pause();
            isPlaying = false;
            btnPlay.setText("播放");
            stopProgressUpdater();
        } else {
            if (itemType == TYPE_MELODY && melody != null) {
                playerService.playMelody(melody);
            } else {
                ToastHelper.showError(this, "无法播放此内容");
                return;
            }
            playerService.setSpeed(playbackSpeed);
            isPlaying = true;
            btnPlay.setText("暂停");
            startProgressUpdater();
        }
    }
    
    private void stop() {
        if (isBound && playerService != null) {
            playerService.stopPlayback();
        }
        isPlaying = false;
        btnPlay.setText("播放");
        playbackProgress.setProgress(0);
        tvPlaybackTime.setText("0:00 / 0:00");
        stopProgressUpdater();
    }
    
    private void edit() {
        ToastHelper.showInfo(this, "编辑功能开发中");
    }
    
    private void confirmDelete() {
        ConfirmDialog.show(this, "确定删除此条目？", "删除后不可恢复", () -> {
            delete();
        });
    }
    
    private void delete() {
        if (itemType == TYPE_MELODY) {
            repository.deleteMelody(itemId);
        } else {
            repository.deleteChord(itemId);
        }
        ToastHelper.showSuccess(this, "删除成功");
        finish();
    }
    
    private void save() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            ToastHelper.showError(this, "名称不能为空");
            return;
        }
        
        if (itemType == TYPE_MELODY && melodyEntry != null) {
            melodyEntry.name = name;
            // save via entry fields
            ToastHelper.showSuccess(this, "保存成功");
        } else if (itemType == TYPE_CHORD && chordEntry != null) {
            chordEntry.name = name;
            // save via entry fields
            ToastHelper.showSuccess(this, "保存成功");
        } else {
            ToastHelper.showError(this, "保存失败");
        }
    }
    
    private void startProgressUpdater() {
        stopProgressUpdater();
        progressUpdater = new Runnable() {
            @Override
            public void run() {
                if (isPlaying && isBound && playerService != null) {
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
    
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.LocalBinder binder = (MusicPlayerService.LocalBinder) service;
            playerService = binder.getService();
            isBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            playerService = null;
            isBound = false;
        }
    };
    
    private String formatTime(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stop();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}
