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
import com.example.musicai.util.DataChangeObserver;
import com.example.musicai.util.NoteEditBottomSheet;
import com.example.musicai.util.TimeUtils;
import com.example.musicai.util.SelectItemBottomSheet;
import com.example.musicai.util.ToastHelper;
import com.example.musicai.util.ConfirmDialog;
import com.example.musicai.util.ToolbarHelper;
import com.example.musicai.view.UnifiedPlaybackButton;
import com.example.musicai.MusicPlayerService.PlaybackListener;

import java.util.ArrayList;
import java.util.List;

public class LibraryDetailActivity extends BaseActivity implements PlaybackListener, DataChangeObserver.OnDataChangeListener {
    
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_ID = "id";
    public static final int TYPE_MELODY = 0;
    public static final int TYPE_CHORD = 1;
    
    private TextView tvStyle, tvCreated, tvNotes, tvChords;
    private EditText etName;
    private UnifiedPlaybackButton btnPlay;
    private Button btnStop;
    private Button btnEdit, btnDelete, btnSave;
    private Button btnPianoRoll;
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
    
    private static final int MENU_RENAME = 1;
    private static final int MENU_DELETE = 2;
    private static final int MENU_EXPORT = 3;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_detail);
        
        itemType = getIntent().getIntExtra(EXTRA_TYPE, TYPE_MELODY);
        itemId = getIntent().getStringExtra(EXTRA_ID);
        
        String title = itemType == TYPE_MELODY ? "旋律详情" : "和弦详情";
        initToolbar(R.id.toolbar, title);
        setBackVisible(true);
        setMenuVisible(true);
        
        List<ToolbarHelper.MenuItemData> menuItems = new ArrayList<>();
        menuItems.add(new ToolbarHelper.MenuItemData(MENU_RENAME, "改名"));
        menuItems.add(new ToolbarHelper.MenuItemData(MENU_DELETE, "删除", 0, true));
        menuItems.add(new ToolbarHelper.MenuItemData(MENU_EXPORT, "导出"));
        toolbarHelper.setMenuItems(menuItems, this::onMenuItemClick);
        
        repository = MusicRepository.getInstance(this);
        
        Intent serviceIntent = new Intent(this, MusicPlayerService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
        
        initViews();
        loadData();
        setupListeners();
        DataChangeObserver.getInstance().registerListener(this);
    }
    
    private void initViews() {
        tvStyle = findViewById(R.id.tv_style);
        tvCreated = findViewById(R.id.tv_created);
        tvNotes = findViewById(R.id.tv_notes);
        tvChords = findViewById(R.id.tv_chords);
        etName = findViewById(R.id.et_name);
        btnPlay = findViewById(R.id.btn_play);
        btnEdit = findViewById(R.id.btn_edit);
        btnDelete = findViewById(R.id.btn_delete);
        btnSave = findViewById(R.id.btn_save);
        btnPianoRoll = findViewById(R.id.btn_piano_roll);
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
            btnPianoRoll.setVisibility(View.VISIBLE);
        } else {
            notesSection.setVisibility(View.GONE);
            chordsSection.setVisibility(View.VISIBLE);
            playbackSection.setVisibility(View.GONE);
            speedSection.setVisibility(View.GONE);
            btnEdit.setVisibility(View.VISIBLE);
            btnPlay.setVisibility(View.GONE);
            btnPianoRoll.setVisibility(View.GONE);
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
                int durationMs = calculateMelodyDuration(melody);
                tvPlaybackTime.setText("0:00 / " + formatTime(durationMs));
                playbackProgress.setProgress(0);
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
    
    private int calculateMelodyDuration(MusicData.Melody mel) {
        if (mel == null || mel.notes == null || mel.notes.isEmpty()) {
            return 0;
        }
        int lastEnd = 0;
        for (MusicData.Note note : mel.notes) {
            int end = note.startTime + note.duration;
            if (end > lastEnd) lastEnd = end;
        }
        return lastEnd * 500;
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
        btnPlay.setOnPlaybackStateChangeListener(new UnifiedPlaybackButton.OnPlaybackStateChangeListener() {
            @Override
            public void onPlayClicked() {
                play();
            }

            @Override
            public void onPauseClicked() {
                pause();
            }

            @Override
            public void onResumeClicked() {
                resume();
            }
        });
        btnEdit.setOnClickListener(v -> showEditDialog());
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnSave.setOnClickListener(v -> save());
        btnPianoRoll.setOnClickListener(v -> openPianoRoll());
        
        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float speed = 0.25f + (progress / 50.0f * 3.75f);
                speed = Math.round(speed * 100) / 100.0f;
                speed = Math.max(0.25f, Math.min(4.0f, speed));
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
    
    private void onMenuItemClick(int itemId) {
        switch (itemId) {
            case MENU_RENAME:
                etName.requestFocus();
                etName.setSelection(etName.getText().length());
                ToastHelper.showInfo(this, "请在下方修改名称后点击保存");
                break;
            case MENU_DELETE:
                confirmDelete();
                break;
            case MENU_EXPORT:
                exportItem();
                break;
        }
    }
    
    private void exportItem() {
        String name = "";
        String exportData = "";
        
        if (itemType == TYPE_MELODY && melodyEntry != null) {
            name = melodyEntry.name;
            exportData = exportMelodyData();
        } else if (itemType == TYPE_CHORD && chordEntry != null) {
            name = chordEntry.name;
            exportData = exportChordData();
        }
        
        if (exportData.isEmpty()) {
            ToastHelper.showError(this, "无数据可导出");
            return;
        }
        
        try {
            String fileName = name.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_") + ".json";
            java.io.File exportDir = new java.io.File(getExternalFilesDir(null), "exports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            java.io.File exportFile = new java.io.File(exportDir, fileName);
            
            java.io.FileWriter writer = new java.io.FileWriter(exportFile);
            writer.write(exportData);
            writer.close();
            
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("application/json");
            shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri.fromFile(exportFile));
            shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(android.content.Intent.createChooser(shareIntent, "导出" + name));
            
        } catch (Exception e) {
            ToastHelper.showError(this, "导出失败: " + e.getMessage());
        }
    }
    
    private String exportMelodyData() {
        if (melodyEntry == null) return "";
        try {
            org.json.JSONObject obj = new org.json.JSONObject();
            obj.put("type", "melody");
            obj.put("name", melodyEntry.name);
            obj.put("style", melodyEntry.style);
            obj.put("createdAt", melodyEntry.createdAt);
            
            org.json.JSONArray notesArray = new org.json.JSONArray();
            for (MusicRepository.NoteData note : melodyEntry.notes) {
                org.json.JSONObject noteObj = new org.json.JSONObject();
                noteObj.put("pitch", note.pitch);
                noteObj.put("octave", note.octave);
                noteObj.put("duration", note.duration);
                noteObj.put("startTime", note.startTime);
                notesArray.put(noteObj);
            }
            obj.put("notes", notesArray);
            
            return obj.toString(2);
        } catch (Exception e) {
            return "";
        }
    }
    
    private String exportChordData() {
        if (chordEntry == null) return "";
        try {
            org.json.JSONObject obj = new org.json.JSONObject();
            obj.put("type", "chord");
            obj.put("name", chordEntry.name);
            obj.put("style", chordEntry.style);
            obj.put("keySignature", chordEntry.keySignature);
            obj.put("mood", chordEntry.mood);
            obj.put("createdAt", chordEntry.createdAt);
            
            org.json.JSONArray chordsArray = new org.json.JSONArray();
            for (MusicRepository.ChordData chord : chordEntry.chords) {
                org.json.JSONObject chordObj = new org.json.JSONObject();
                chordObj.put("name", chord.name);
                chordObj.put("type", chord.type);
                chordObj.put("duration", chord.duration);
                chordObj.put("startTime", chord.startTime);
                chordsArray.put(chordObj);
            }
            obj.put("chords", chordsArray);
            
            return obj.toString(2);
        } catch (Exception e) {
            return "";
        }
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
            pause();
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
            btnPlay.setState(UnifiedPlaybackButton.State.PAUSE);
            startProgressUpdater();
        }
    }
    
    private void pause() {
        if (isBound && playerService != null) {
            playerService.pause();
            isPaused = true;
            isPlaying = false;
            btnPlay.setState(UnifiedPlaybackButton.State.RESUME);
            stopProgressUpdater();
        }
    }
    
    private void resume() {
        if (isBound && playerService != null) {
            playerService.resume();
            isPlaying = true;
            isPaused = false;
            btnPlay.setState(UnifiedPlaybackButton.State.PAUSE);
            startProgressUpdater();
        }
    }
    
    private void stop() {
        if (isBound && playerService != null) {
            playerService.stopPlayback();
        }
        isPlaying = false;
        isPaused = false;
        btnPlay.setState(UnifiedPlaybackButton.State.PLAY);
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
        
        SelectItemBottomSheet.show(this, "选择音符", items, this::showNoteEditBottomSheet);
    }
    
    private void showNoteEditBottomSheet(int index) {
        if (melodyEntry == null || index >= melodyEntry.notes.size()) return;
        final int originalIndex = index;
        
        MusicRepository.NoteData note = melodyEntry.notes.get(index);
        final int[] currentIndex = {index};
        final int total = melodyEntry.notes.size();
        
        NoteEditBottomSheet.show(this, currentIndex[0], total, note, new NoteEditBottomSheet.OnNoteUpdateListener() {
            @Override
            public void onUpdated(MusicRepository.NoteData updatedNote, int newIndex) {
                if (originalIndex >= 0 && originalIndex < melodyEntry.notes.size()) {
                    melodyEntry.notes.remove(originalIndex);
                    int insertIndex = Math.min(newIndex, melodyEntry.notes.size());
                    melodyEntry.notes.add(insertIndex, updatedNote);
                    melody = melodyEntry.toMelody();
                    updateNotesDisplay();
                    repository.saveMelodiesToPrefs();
                    ToastHelper.showSuccess(LibraryDetailActivity.this, "音符已更新");
                }
            }
            
            @Override
            public void onPositionChanged(int newIndex) {
                if (currentIndex[0] != newIndex && newIndex >= 0 && newIndex < melodyEntry.notes.size()) {
                    MusicRepository.NoteData currentNote = melodyEntry.notes.get(currentIndex[0]);
                    melodyEntry.notes.remove(currentIndex[0]);
                    melodyEntry.notes.add(newIndex, currentNote);
                    currentIndex[0] = newIndex;
                    melody = melodyEntry.toMelody();
                    updateNotesDisplay();
                    repository.saveMelodiesToPrefs();
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
        
        SelectItemBottomSheet.show(this, "选择和弦", items, this::showChordEditBottomSheet);
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
    
    private void openPianoRoll() {
        if (melodyEntry == null || itemId == null) {
            ToastHelper.showError(this, "无法打开钢琴卷帘");
            return;
        }
        
        Intent intent = new Intent(this, PianoRollActivity.class);
        intent.putExtra(PianoRollActivity.EXTRA_MELODY_ID, itemId);
        startActivityForResult(intent, 100);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            melodyEntry = repository.getMelodyById(itemId);
            if (melodyEntry != null) {
                melody = melodyEntry.toMelody();
                updateNotesDisplay();
                int durationMs = calculateMelodyDuration(melody);
                tvPlaybackTime.setText("0:00 / " + formatTime(durationMs));
            }
        }
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
                btnPlay.setState(UnifiedPlaybackButton.State.PAUSE);
            } else {
                btnPlay.setState(UnifiedPlaybackButton.State.RESUME);
            }
        });
    }
    
    @Override
    public void onPlaybackCompleted() {
        runOnUiThread(() -> {
            isPlaying = false;
            isPaused = false;
            btnPlay.setState(UnifiedPlaybackButton.State.PLAY);
            playbackProgress.setProgress(0);
            int durationMs = calculateMelodyDuration(melody);
            tvPlaybackTime.setText("0:00 / " + formatTime(durationMs));
            stopProgressUpdater();
            if (isBound && playerService != null) {
                playerService.stopPlayback();
            }
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
        DataChangeObserver.getInstance().unregisterListener(this);
    }

    @Override
    public void onMelodyChanged() {
        refreshData();
    }

    @Override
    public void onChordChanged() {
        refreshData();
    }

    @Override
    public void onSongChanged() {
    }

    @Override
    public void onAllChanged() {
        refreshData();
    }

    private void refreshData() {
        if (itemId != null) {
            runOnUiThread(() -> {
                loadData();
            });
        }
    }
}
