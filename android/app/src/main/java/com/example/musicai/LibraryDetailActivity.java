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

import com.example.musicai.util.ChordEditBottomSheet;
import com.example.musicai.util.NoteEditBottomSheet;
import com.example.musicai.util.TimeUtils;
import com.example.musicai.util.ToastHelper;
import com.example.musicai.util.ConfirmDialog;
import com.example.musicai.MusicPlayerService.PlaybackListener;

public class LibraryDetailActivity extends AppCompatActivity implements PlaybackListener {
    
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_ID = "id";
    public static final int TYPE_MELODY = 0;
    public static final int TYPE_CHORD = 1;
    
    private TextView tvStyle, tvCreated, tvNotes, tvChords;
    private EditText etName;
    private Button btnPlay, btnStop, btnEdit, btnDelete, btnSave;
    private TextView tvSpeed, tvPlaybackTime;
    private CursorSeekBar playbackProgress;
    private ProgressBar progressBar;
    private SeekBar seekBarSpeed;
    private View notesSection, chordsSection, playbackSection, speedSection;
    
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
    private boolean isPaused = false;
    private float playbackSpeed = 1.0f;
    
    private static final String[] PITCHES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final String[] OCTAVES = {"2", "3", "4", "5", "6", "7"};
    private static final String[] DURATIONS = {"1", "2", "4", "8", "16"};
    private static final String[] CHORD_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final String[] CHORD_TYPES = {"major", "minor", "seventh", "diminished", "augmented", "sus2", "sus4"};
    
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
        playbackSection = findViewById(R.id.playback_section);
        speedSection = findViewById(R.id.speed_section);
        seekBarSpeed = findViewById(R.id.seekbar_speed);
        
        if (itemType == TYPE_MELODY) {
            notesSection.setVisibility(View.VISIBLE);
            chordsSection.setVisibility(View.GONE);
            btnEdit.setVisibility(View.VISIBLE);
            playbackSection.setVisibility(View.VISIBLE);
            speedSection.setVisibility(View.VISIBLE);
            btnPlay.setVisibility(View.VISIBLE);
            btnStop.setVisibility(View.VISIBLE);
        } else {
            notesSection.setVisibility(View.GONE);
            chordsSection.setVisibility(View.VISIBLE);
            playbackSection.setVisibility(View.GONE);
            speedSection.setVisibility(View.GONE);
            btnEdit.setVisibility(View.VISIBLE);
            btnPlay.setVisibility(View.GONE);
            btnStop.setVisibility(View.GONE);
        }
        
        seekBarSpeed.setMax(50);
        seekBarSpeed.setProgress(10);
        tvSpeed.setText("速度: 1.0x");
    }
    
    private void loadData() {
        if (itemType == TYPE_MELODY) {
            melodyEntry = repository.getMelodyById(itemId);
            melody = melodyEntry != null ? melodyEntry.toMelody() : null;
            if (melodyEntry != null) {
                etName.setText(melodyEntry.name);
                tvStyle.setText(melodyEntry.style);
                tvCreated.setText(TimeUtils.formatRelativeTime(melodyEntry.createdAt));
                updateNotesDisplay();
            }
        } else {
            chordEntry = repository.getChordById(itemId);
            chordProgression = chordEntry != null ? chordEntry.toChordProgression() : null;
            if (chordEntry != null) {
                etName.setText(chordEntry.name);
                tvStyle.setText(chordEntry.style);
                tvCreated.setText(TimeUtils.formatRelativeTime(chordEntry.createdAt));
                updateChordsDisplay();
            }
        }
        
        if (melodyEntry == null && chordEntry == null) {
            ToastHelper.showError(this, "数据加载失败");
            finish();
        }
    }
    
    private void updateNotesDisplay() {
        if (melodyEntry == null) return;
        StringBuilder notesStr = new StringBuilder();
        notesStr.append(String.format("%-4s %-8s %-6s %-6s\n", "序号", "音符", "时值", "位置"));
        notesStr.append(String.format("%-4s %-8s %-6s %-6s\n", "----", "------", "----", "----"));
        for (int i = 0; i < melodyEntry.notes.size(); i++) {
            MusicRepository.NoteData note = melodyEntry.notes.get(i);
            String noteName = note.pitch + note.octave;
            notesStr.append(String.format("%-4d %-8s %-6d %-6d\n",
                i + 1, noteName, note.duration, note.startTime));
        }
        tvNotes.setText(notesStr.toString());
        melody = melodyEntry.toMelody();
    }
    
    private void updateChordsDisplay() {
        if (chordEntry == null) return;
        StringBuilder chordsStr = new StringBuilder();
        chordsStr.append(String.format("%-4s %-8s %-10s %-6s %-6s\n", "序号", "根音", "类型", "时值", "位置"));
        chordsStr.append(String.format("%-4s %-8s %-10s %-6s %-6s\n", "----", "----", "----", "----", "----"));
        for (int i = 0; i < chordEntry.chords.size(); i++) {
            MusicRepository.ChordData chord = chordEntry.chords.get(i);
            chordsStr.append(String.format("%-4d %-8s %-10s %-6d %-6d\n",
                i + 1, chord.name, chord.type, chord.duration, chord.startTime));
        }
        tvChords.setText(chordsStr.toString());
        chordProgression = chordEntry.toChordProgression();
    }
    
    private void setupListeners() {
        btnPlay.setOnClickListener(v -> play());
        btnStop.setOnClickListener(v -> stop());
        btnEdit.setOnClickListener(v -> showEditDialog());
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnSave.setOnClickListener(v -> save());
        
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
                if (isBound && playerService != null && isPlaying) {
                    int currentProgress = playbackProgress.getProgress();
                    int posMs = (int) (playerService.getDuration() * (long) currentProgress / 100L);
                    playerService.seekTo(posMs);
                }
            }
        });
    }
    
    private void setSpeed(float speed) {
        playbackSpeed = speed;
        if (isBound && playerService != null) {
            playerService.setSpeed(speed);
        }
    }
    
    private void play() {
        if (itemType == TYPE_MELODY && (melody == null || melody.notes.isEmpty())) {
            ToastHelper.showError(this, "暂无可播放内容");
            return;
        }
        
        if (!isBound || playerService == null) {
            ToastHelper.showError(this, "播放器服务未连接");
            return;
        }
        
        if (isPlaying) {
            playerService.pause();
            isPaused = true;
            isPlaying = false;
            btnPlay.setText("继续");
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
            isPaused = false;
            btnPlay.setText("暂停");
            startProgressUpdater();
        }
    }
    
    private void stop() {
        if (isBound && playerService != null) {
            playerService.stopPlayback();
        }
        isPlaying = false;
        isPaused = false;
        btnPlay.setText("播放");
        playbackProgress.setProgress(0);
        tvPlaybackTime.setText("0:00 / 0:00");
        stopProgressUpdater();
    }
    
    private void showEditDialog() {
        if (itemType == TYPE_MELODY) {
            showNoteEditDialog();
        } else {
            showChordEditDialog();
        }
    }
    
    private void showNoteEditDialog() {
        if (melodyEntry == null || melodyEntry.notes.isEmpty()) {
            ToastHelper.showWarning(this, "暂无音符数据");
            return;
        }
        
        java.util.List<String> items = new java.util.ArrayList<>();
        for (int i = 0; i < melodyEntry.notes.size(); i++) {
            MusicRepository.NoteData note = melodyEntry.notes.get(i);
            items.add(String.format("%s%d | 时值:%d", note.pitch, note.octave, note.duration));
        }
        
        SelectItemBottomSheet.showNoteSelection(this, items, this::showNoteEditBottomSheet);
    }
    
    private void showNoteEditBottomSheet(int index) {
        if (melodyEntry == null || index >= melodyEntry.notes.size()) return;
        
        MusicRepository.NoteData note = melodyEntry.notes.get(index);
        
        NoteEditBottomSheet.show(this, index, melodyEntry.notes.size(), note, new NoteEditBottomSheet.OnNoteUpdateListener() {
            @Override
            public void onUpdated(MusicRepository.NoteData updatedNote, int newIndex) {
                melodyEntry.notes.remove(index);
                melodyEntry.notes.add(newIndex, updatedNote);
                melody = melodyEntry.toMelody();
                updateNotesDisplay();
                repository.saveMelodiesToPrefs();
                ToastHelper.showSuccess(LibraryDetailActivity.this, "音符已更新");
            }
            
            @Override
            public void onPositionChanged(int newIndex) {
                if (newIndex != index) {
                    MusicRepository.NoteData currentNote = melodyEntry.notes.get(index);
                    melodyEntry.notes.remove(index);
                    melodyEntry.notes.add(newIndex, currentNote);
                    index = newIndex;
                    melody = melodyEntry.toMelody();
                    updateNotesDisplay();
                }
            }
        });
    }
    
    private void showChordEditDialog() {
        if (chordEntry == null || chordEntry.chords.isEmpty()) {
            ToastHelper.showWarning(this, "暂无和弦数据");
            return;
        }
        
        java.util.List<String> items = new java.util.ArrayList<>();
        for (int i = 0; i < chordEntry.chords.size(); i++) {
            MusicRepository.ChordData chord = chordEntry.chords.get(i);
            items.add(String.format("%s %s | 时值:%d", chord.name, chord.type, chord.duration));
        }
        
        SelectItemBottomSheet.showChordSelection(this, items, this::showChordEditBottomSheet);
    }
    
    private void showChordEditBottomSheet(int index) {
        if (chordEntry == null || index >= chordEntry.chords.size()) return;
        
        MusicRepository.ChordData chord = chordEntry.chords.get(index);
        
        ChordEditBottomSheet.show(this, index, chord, (updatedChord) -> {
            if (updatedChord != null) {
                chordEntry.chords.set(index, updatedChord);
                updateChordsDisplay();
                repository.saveChordsToPrefs();
                ToastHelper.showSuccess(this, "和弦已更新");
            }
        });
    }
    
    private void confirmDelete() {
        String itemName = "";
        if (itemType == TYPE_MELODY && melodyEntry != null) {
            itemName = melodyEntry.name;
        } else if (itemType == TYPE_CHORD && chordEntry != null) {
            itemName = chordEntry.name;
        }
        
        ConfirmDialog.showDelete(this, itemName, () -> delete());
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
            if (!name.equals(melodyEntry.name) && repository.melodyNameExists(name)) {
                ConfirmDialog.showSave(this, name, () -> doSave(name));
            } else {
                doSave(name);
            }
        } else if (itemType == TYPE_CHORD && chordEntry != null) {
            if (!name.equals(chordEntry.name) && repository.chordNameExists(name)) {
                ConfirmDialog.showSave(this, name, () -> doSave(name));
            } else {
                doSave(name);
            }
        } else {
            ToastHelper.showError(this, "保存失败");
        }
    }
    
    private void doSave(String name) {
        if (itemType == TYPE_MELODY && melodyEntry != null) {
            melodyEntry.name = name;
            repository.saveMelodiesToPrefs();
            ToastHelper.showSuccess(this, "保存成功");
        } else if (itemType == TYPE_CHORD && chordEntry != null) {
            chordEntry.name = name;
            repository.saveChordsToPrefs();
            ToastHelper.showSuccess(this, "保存成功");
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
            playerService.setPlaybackListener(LibraryDetailActivity.this);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (playerService != null) {
                playerService.removePlaybackListener();
            }
            playerService = null;
            isBound = false;
        }
    };
    
    @Override
    public void onPlaybackProgress(int positionMs, int totalMs, int currentNoteIndex) {}
    
    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        this.isPlaying = isPlaying;
        runOnUiThread(() -> {
            if (isPlaying) {
                btnPlay.setText("暂停");
            } else {
                btnPlay.setText("继续");
            }
        });
    }
    
    @Override
    public void onPlaybackCompleted() {
        runOnUiThread(() -> {
            isPlaying = false;
            isPaused = false;
            btnPlay.setText("播放");
            playbackProgress.setProgress(0);
            tvPlaybackTime.setText("0:00 / 0:00");
            stopProgressUpdater();
        });
    }
    
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
