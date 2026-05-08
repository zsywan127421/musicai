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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.musicai.util.ConfirmDialog;
import com.example.musicai.util.TimeUtils;
import com.example.musicai.util.ToastHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewSongGeneratorActivity extends BaseActivity implements MusicPlayerService.PlaybackListener {
    
    private static final int MODE_MELODY = 0;
    private static final int MODE_CHORDS = 1;
    private static final int MODE_SONG = 2;
    
    private static final float[] SPEEDS = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
    private static final String[] SPEED_LABELS = {"0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x"};
    
    private MusicGenerator musicGenerator;
    private MusicRepository repository;
    private MusicData.Song currentSong;
    private int currentHighlightedNoteIndex = -1;
    
    private int currentMode = MODE_MELODY;
    
    private RadioGroup rgMode;
    private LinearLayout melodySection;
    private LinearLayout chordsSection;
    private LinearLayout songSection;
    
    private Spinner spStyle;
    private Spinner spMelodyLength;
    private EditText etMelodyName;
    private EditText etChordName;
    private EditText etDescription;
    private Button btnGenerateMelody;
    private ProgressBar progressBar;
    
    private Spinner spMelodySelect;
    private RadioGroup rgChordMode;
    private EditText etKeySignature;
    private EditText etMood;
    private EditText etChordDescription;
    private Button btnGenerateChords;
    
    private Spinner spSongMelody;
    private Spinner spSongChords;
    private Button btnPreviewMelody;
    private Button btnPreviewChords;
    private Button btnGenerateSong;
    
    private CursorSeekBar playbackProgress;
    private TextView tvPlaybackTime;
    private Button btnPlay;
    private Button btnStop;
    private LinearLayout speedControlLayout;
    private TextView tvSpeed;
    private Button[] speedButtons = new Button[6];
    private int selectedSpeedIndex = 2;
    
    private ListView lvResult;
    private HighlightedAdapter resultAdapter;
    private List<String> resultList;
    
    private LinearLayout detailBar;
    private TextView tvDetailTitle;
    private TextView tvDetailInfo;
    private Button btnSave;
    private Button btnDiscard;
    private ImageButton btnExpandDetail;
    private LinearLayout detailExpanded;
    private TextView tvDetailExpanded;
    private boolean isDetailExpanded = false;
    
    private MusicPlayerService playerService;
    private boolean isBound = false;
    private boolean isGenerating = false;
    
    private Handler progressHandler = new Handler();
    private Runnable progressUpdateRunnable;
    
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MusicPlayerService.LocalBinder binder = (MusicPlayerService.LocalBinder) service;
            playerService = binder.getService();
            playerService.setPlaybackListener(NewSongGeneratorActivity.this);
            isBound = true;
        }
        
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            if (playerService != null) {
                playerService.setPlaybackListener(null);
            }
            isBound = false;
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_song_generator);
        
        musicGenerator = new MusicGenerator(this);
        repository = MusicRepository.getInstance(this);
        currentSong = new MusicData.Song();
        
        initViews();
        setupSpinners();
        setupSpeedControls();
        setupListeners();
        updateUI();
    }
    
    private void initViews() {
        rgMode = findViewById(R.id.rg_mode);
        melodySection = findViewById(R.id.melody_section);
        chordsSection = findViewById(R.id.chords_section);
        songSection = findViewById(R.id.song_section);
        
        spStyle = findViewById(R.id.sp_style);
        spMelodyLength = findViewById(R.id.sp_melody_length);
        etMelodyName = findViewById(R.id.et_melody_name);
        etChordName = findViewById(R.id.et_chord_name);
        etDescription = findViewById(R.id.et_description);
        btnGenerateMelody = findViewById(R.id.btn_generate_melody);
        progressBar = findViewById(R.id.progress_bar);
        
        spMelodySelect = findViewById(R.id.sp_melody_select);
        rgChordMode = findViewById(R.id.rg_chord_mode);
        etKeySignature = findViewById(R.id.et_key_signature);
        etMood = findViewById(R.id.et_mood);
        etChordDescription = findViewById(R.id.et_chord_description);
        btnGenerateChords = findViewById(R.id.btn_generate_chords);
        
        spSongMelody = findViewById(R.id.sp_song_melody);
        spSongChords = findViewById(R.id.sp_song_chords);
        btnPreviewMelody = findViewById(R.id.btn_preview_melody);
        btnPreviewChords = findViewById(R.id.btn_preview_chords);
        btnGenerateSong = findViewById(R.id.btn_generate_song);
        
        playbackProgress = findViewById(R.id.playback_progress);
        tvPlaybackTime = findViewById(R.id.tv_playback_time);
        btnPlay = findViewById(R.id.btn_play);
        btnStop = findViewById(R.id.btn_stop);
        speedControlLayout = findViewById(R.id.speed_control_layout);
        tvSpeed = findViewById(R.id.tv_speed);
        
        speedButtons[0] = findViewById(R.id.btn_speed_05);
        speedButtons[1] = findViewById(R.id.btn_speed_075);
        speedButtons[2] = findViewById(R.id.btn_speed_1);
        speedButtons[3] = findViewById(R.id.btn_speed_125);
        speedButtons[4] = findViewById(R.id.btn_speed_15);
        speedButtons[5] = findViewById(R.id.btn_speed_2);
        
        lvResult = findViewById(R.id.lv_result);
        resultList = new ArrayList<>();
        resultAdapter = new HighlightedAdapter(this, resultList);
        lvResult.setAdapter(resultAdapter);
        
        detailBar = findViewById(R.id.detail_bar);
        tvDetailTitle = findViewById(R.id.tv_detail_title);
        tvDetailInfo = findViewById(R.id.tv_detail_info);
        btnSave = findViewById(R.id.btn_detail_save);
        btnDiscard = findViewById(R.id.btn_detail_discard);
        btnExpandDetail = findViewById(R.id.btn_expand_detail);
        detailExpanded = findViewById(R.id.detail_expanded);
        tvDetailExpanded = findViewById(R.id.tv_detail_expanded);
        
        detailBar.setVisibility(View.GONE);
    }
    
    private void setupSpinners() {
        ArrayAdapter<String> styleAdapter = new ArrayAdapter<>(this, 
            R.layout.spinner_item, MusicData.MUSIC_STYLES);
        styleAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spStyle.setAdapter(styleAdapter);
        
        String[] melodyLengths = new String[24];
        for (int i = 0; i < 24; i++) {
            melodyLengths[i] = String.valueOf(i + 1);
        }
        ArrayAdapter<String> lengthAdapter = new ArrayAdapter<>(this, 
            R.layout.spinner_item, melodyLengths);
        lengthAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spMelodyLength.setAdapter(lengthAdapter);
        spMelodyLength.setSelection(7);
    }
    
    private void setupSpeedControls() {
        for (int i = 0; i < speedButtons.length; i++) {
            final int index = i;
            speedButtons[i].setOnClickListener(v -> selectSpeed(index));
        }
        updateSpeedButtons();
    }
    
    private void selectSpeed(int index) {
        selectedSpeedIndex = index;
        float speed = SPEEDS[index];
        if (playerService != null) {
            playerService.setSpeed(speed);
            if (playerService.isPlaying()) {
                updateProgressDisplay();
            }
        }
        updateSpeedButtons();
        tvSpeed.setText("速度: " + SPEED_LABELS[index]);
    }
    
    private void updateSpeedButtons() {
        for (int i = 0; i < speedButtons.length; i++) {
            if (i == selectedSpeedIndex) {
                speedButtons[i].setBackgroundResource(R.drawable.apple_button_primary_bg);
            } else {
                speedButtons[i].setBackgroundResource(R.drawable.apple_button_bg);
            }
        }
    }
    
    private void setupListeners() {
        rgMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_melody) {
                currentMode = MODE_MELODY;
            } else if (checkedId == R.id.rb_chords) {
                currentMode = MODE_CHORDS;
            } else if (checkedId == R.id.rb_song) {
                currentMode = MODE_SONG;
            }
            updateUI();
        });
        
        btnGenerateMelody.setOnClickListener(v -> generateMelody());
        btnGenerateChords.setOnClickListener(v -> generateChords());
        btnGenerateSong.setOnClickListener(v -> generateSong());
        
        btnPlay.setOnClickListener(v -> playSong());
        btnStop.setOnClickListener(v -> stopSong());
        btnPreviewMelody.setOnClickListener(v -> previewSelectedMelody());
        btnPreviewChords.setOnClickListener(v -> previewSelectedChords());
        
        btnSave.setOnClickListener(v -> saveCurrentSong());
        btnDiscard.setOnClickListener(v -> discardCurrentSong());
        btnExpandDetail.setOnClickListener(v -> toggleDetailExpanded());
        
        rgChordMode.setOnCheckedChangeListener((group, checkedId) -> {
            updateChordModeUI();
        });
    }
    
    private void updateUI() {
        melodySection.setVisibility(currentMode == MODE_MELODY ? View.VISIBLE : View.GONE);
        chordsSection.setVisibility(currentMode == MODE_CHORDS ? View.VISIBLE : View.GONE);
        songSection.setVisibility(currentMode == MODE_SONG ? View.VISIBLE : View.GONE);
        
        if (currentMode == MODE_CHORDS) {
            updateChordModeUI();
            loadMelodySpinner();
        }
        
        if (currentMode == MODE_SONG) {
            loadSongSelectors();
        }
        
        btnGenerateMelody.setEnabled(!isGenerating);
        btnGenerateChords.setEnabled(!isGenerating);
        btnGenerateSong.setEnabled(!isGenerating);
        progressBar.setVisibility(isGenerating ? View.VISIBLE : View.GONE);
    }
    
    private void updateChordModeUI() {
        int checkedId = rgChordMode.getCheckedRadioButtonId();
        boolean useMelody = checkedId == R.id.rb_from_melody;
        
        spMelodySelect.setVisibility(useMelody ? View.VISIBLE : View.GONE);
        etKeySignature.setVisibility(useMelody ? View.GONE : View.VISIBLE);
        etMood.setVisibility(useMelody ? View.GONE : View.VISIBLE);
        etChordDescription.setVisibility(useMelody ? View.GONE : View.VISIBLE);
    }
    
    private void loadMelodySpinner() {
        List<MusicRepository.MelodyEntry> melodies = repository.getMelodyLibrary();
        List<String> melodyNames = new ArrayList<>();
        
        if (melodies.isEmpty()) {
            melodyNames.add("（旋律库为空）");
        } else {
            for (MusicRepository.MelodyEntry entry : melodies) {
                melodyNames.add(entry.name + " - " + entry.style);
            }
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            R.layout.spinner_item, melodyNames);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spMelodySelect.setAdapter(adapter);
    }
    
    private void loadSongSelectors() {
        List<MusicRepository.MelodyEntry> melodies = repository.getMelodyLibrary();
        List<String> melodyNames = new ArrayList<>();
        
        if (melodies.isEmpty()) {
            melodyNames.add("（旋律库为空）");
        } else {
            for (MusicRepository.MelodyEntry entry : melodies) {
                melodyNames.add(entry.name);
            }
        }
        
        ArrayAdapter<String> melodyAdapter = new ArrayAdapter<>(this, 
            R.layout.spinner_item, melodyNames);
        melodyAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spSongMelody.setAdapter(melodyAdapter);
        
        List<MusicRepository.ChordEntry> chords = repository.getChordLibrary();
        List<String> chordNames = new ArrayList<>();
        
        if (chords.isEmpty()) {
            chordNames.add("（和弦库为空）");
        } else {
            for (MusicRepository.ChordEntry entry : chords) {
                chordNames.add(entry.name);
            }
        }
        
        ArrayAdapter<String> chordAdapter = new ArrayAdapter<>(this, 
            R.layout.spinner_item, chordNames);
        chordAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spSongChords.setAdapter(chordAdapter);
    }
    
    private String getCustomOrDefaultName(String customName, String prefix) {
        if (customName != null && !customName.trim().isEmpty()) {
            return customName.trim();
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd_HHmmss", Locale.getDefault());
        return prefix + "_" + sdf.format(new Date());
    }
    
    private void generateMelody() {
        if (isGenerating) return;
        
        isGenerating = true;
        updateUI();
        
        String style = (String) spStyle.getSelectedItem();
        String description = etDescription.getText().toString().trim();
        final String customName = etMelodyName.getText().toString().trim();
        
        String lengthStr = (String) spMelodyLength.getSelectedItem();
        int length = 8;
        try {
            length = Integer.parseInt(lengthStr);
        } catch (NumberFormatException e) {
            length = 8;
        }
        if (length < 1) length = 1;
        if (length > 24) length = 24;
        
        final int finalLength = length;
        final String generatedName = getCustomOrDefaultName(customName, "旋律");
        
        if (repository.melodyNameExists(generatedName)) {
            isGenerating = false;
            updateUI();
            ConfirmDialog.showSave(this, generatedName, () -> {
                doSaveMelody(style, description, generatedName, finalLength);
            });
        } else {
            doSaveMelody(style, description, generatedName, finalLength);
        }
    }
    
    private void doSaveMelody(String style, String description, String name, int length) {
        isGenerating = true;
        updateUI();
        
        final String finalName = name;
        
        new Thread(() -> {
            try {
                MusicData.Melody melody = musicGenerator.generateMelodyWithDescription(style, length, null, description);
                
                if (melody == null || melody.notes == null || melody.notes.isEmpty()) {
                    throw new Exception("生成失败：AI返回了空内容，请重试");
                }
                
                MusicRepository.MelodyEntry entry = new MusicRepository.MelodyEntry();
                entry.name = finalName;
                entry.style = style;
                for (MusicData.Note note : melody.notes) {
                    entry.notes.add(new MusicRepository.NoteData(note));
                }
                repository.addMelody(entry);
                
                runOnUiThread(() -> {
                    ToastHelper.showSuccess(NewSongGeneratorActivity.this, "旋律已保存到库中！");
                    etMelodyName.setText("");
                    addResultItem("✓ 旋律已完成", entry.getPreviewText());
                    isGenerating = false;
                    updateUI();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    ToastHelper.showError(NewSongGeneratorActivity.this, "生成失败: " + e.getMessage());
                    isGenerating = false;
                    updateUI();
                });
            }
        }).start();
    }
    
    private void generateChords() {
        if (isGenerating) return;
        
        isGenerating = true;
        updateUI();
        
        String style = (String) spStyle.getSelectedItem();
        final String customName = etChordName.getText().toString().trim();
        int checkedId = rgChordMode.getCheckedRadioButtonId();
        
        final String generatedName = getCustomOrDefaultName(customName, "和弦");
        
        if (repository.chordNameExists(generatedName)) {
            isGenerating = false;
            updateUI();
            ConfirmDialog.showSave(this, generatedName, () -> {
                doSaveChord(style, generatedName, checkedId);
            });
        } else {
            doSaveChord(style, generatedName, checkedId);
        }
    }
    
    private void doSaveChord(String style, String name, int checkedId) {
        isGenerating = true;
        updateUI();
        
        final String finalName = name;
        final int finalCheckedId = checkedId;
        
        new Thread(() -> {
            try {
                MusicData.ChordProgression progression;
                
                if (finalCheckedId == R.id.rb_from_melody) {
                    List<MusicRepository.MelodyEntry> melodies = repository.getMelodyLibrary();
                    int selectedPos = spMelodySelect.getSelectedItemPosition();
                    if (melodies.isEmpty() || selectedPos >= melodies.size()) {
                        throw new Exception("请先选择一条旋律");
                    }
                    
                    MusicRepository.MelodyEntry melodyEntry = melodies.get(selectedPos);
                    progression = musicGenerator.generateChordsWithMelody(style, 4, null, melodyEntry.toMelody());
                } else {
                    String keySignature = etKeySignature.getText().toString().trim();
                    String mood = etMood.getText().toString().trim();
                    String description = etChordDescription.getText().toString().trim();
                    progression = musicGenerator.generateCustomChords(style, 4, keySignature, mood, description);
                }
                
                if (progression == null || progression.chords == null || progression.chords.isEmpty()) {
                    throw new Exception("生成失败：AI返回了空内容，请重试");
                }
                
                MusicRepository.ChordEntry entry = new MusicRepository.ChordEntry();
                entry.name = finalName;
                entry.style = style;
                entry.keySignature = etKeySignature.getText().toString().trim();
                entry.mood = etMood.getText().toString().trim();
                for (MusicData.Chord chord : progression.chords) {
                    entry.chords.add(new MusicRepository.ChordData(chord));
                }
                repository.addChord(entry);
                
                runOnUiThread(() -> {
                    ToastHelper.showSuccess(NewSongGeneratorActivity.this, "和弦已保存到库中！");
                    etChordName.setText("");
                    addResultItem("✓ 和弦已完成", entry.getPreviewText());
                    isGenerating = false;
                    updateUI();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    ToastHelper.showError(NewSongGeneratorActivity.this, "生成失败: " + e.getMessage());
                    isGenerating = false;
                    updateUI();
                });
            }
        }).start();
    }
    
    private void generateSong() {
        if (isGenerating) return;
        
        List<MusicRepository.MelodyEntry> melodies = repository.getMelodyLibrary();
        List<MusicRepository.ChordEntry> chords = repository.getChordLibrary();
        
        if (melodies.isEmpty()) {
            Toast.makeText(this, "旋律库为空，请先生成旋律", Toast.LENGTH_SHORT).show();
            return;
        }
        if (chords.isEmpty()) {
            Toast.makeText(this, "和弦库为空，请先生成和弦", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int melodyPos = spSongMelody.getSelectedItemPosition();
        int chordPos = spSongChords.getSelectedItemPosition();
        
        if (melodyPos >= melodies.size() || chordPos >= chords.size()) {
            Toast.makeText(this, "请选择旋律和和弦", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isGenerating = true;
        updateUI();
        
        String style = (String) spStyle.getSelectedItem();
        final MusicRepository.MelodyEntry melodyEntry = melodies.get(melodyPos);
        final MusicRepository.ChordEntry chordEntry = chords.get(chordPos);
        
        new Thread(() -> {
            try {
                MusicData.Melody melody = melodyEntry.toMelody();
                MusicData.ChordProgression progression = chordEntry.toChordProgression();
                
                if (progression.chords.isEmpty()) {
                    throw new Exception("和弦数据为空");
                }
                
                runOnUiThread(() -> addResultItem("⏳ 开始生成歌曲...", "使用段落拼接模式"));
                
                currentSong = musicGenerator.generateSongWithSegments(style, melody, progression, (segmentIndex, totalSegments) -> {
                    runOnUiThread(() -> addResultItem(
                        "✓ 第" + (segmentIndex + 1) + "段生成完成",
                        "进度: " + (segmentIndex + 1) + "/" + totalSegments
                    ));
                });
                
                currentSong.melody = melody;
                currentSong.chords = progression;
                
                runOnUiThread(() -> {
                    addResultItem("✓ 曲子生成完成", 
                        "时长: " + formatDuration(currentSong.totalDurationMs) + 
                        " | 段落: " + currentSong.segments.size());
                    
                    showDetailBar(melodyEntry, chordEntry);
                    
                    btnPlay.setEnabled(true);
                    isGenerating = false;
                    updateUI();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(NewSongGeneratorActivity.this, 
                        "生成失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    isGenerating = false;
                    updateUI();
                });
            }
        }).start();
    }
    
    private void showDetailBar(MusicRepository.MelodyEntry melodyEntry, MusicRepository.ChordEntry chordEntry) {
        if (currentSong == null) return;
        
        detailBar.setVisibility(View.VISIBLE);
        tvDetailTitle.setText("生成成功");
        tvDetailInfo.setText("时长: " + formatDuration(currentSong.totalDurationMs) + 
            " | 段落: " + currentSong.segments.size() + 
            " | 来源: " + melodyEntry.name + " + " + chordEntry.name);
        detailExpanded.setVisibility(View.GONE);
        isDetailExpanded = false;
        btnExpandDetail.setRotation(0);
        
        updateDetailExpanded(melodyEntry, chordEntry);
    }
    
    private void updateDetailExpanded(MusicRepository.MelodyEntry melodyEntry, MusicRepository.ChordEntry chordEntry) {
        if (currentSong == null) return;
        
        StringBuilder sb = new StringBuilder();
        sb.append("标题: ").append(currentSong.title != null ? currentSong.title : "未命名").append("\n");
        sb.append("风格: ").append(currentSong.style).append("\n");
        sb.append("时长: ").append(formatDuration(currentSong.totalDurationMs)).append("\n");
        sb.append("段落数: ").append(currentSong.segments.size()).append("\n");
        sb.append("\n来源:\n");
        sb.append("旋律: ").append(melodyEntry.name).append("\n");
        sb.append("和弦: ").append(chordEntry.name).append("\n");
        sb.append("\n旋律长度: ").append(melodyEntry.notes.size()).append(" 音符\n");
        sb.append("和弦数量: ").append(chordEntry.chords.size()).append(" 和弦");
        
        tvDetailExpanded.setText(sb.toString());
    }
    
    private void toggleDetailExpanded() {
        if (detailExpanded.getVisibility() == View.VISIBLE) {
            detailExpanded.setVisibility(View.GONE);
            isDetailExpanded = false;
            btnExpandDetail.setRotation(0);
        } else {
            detailExpanded.setVisibility(View.VISIBLE);
            isDetailExpanded = true;
            btnExpandDetail.setRotation(180);
        }
    }
    
    private void saveCurrentSong() {
        if (currentSong == null) return;
        
        List<MusicRepository.MelodyEntry> melodies = repository.getMelodyLibrary();
        List<MusicRepository.ChordEntry> chords = repository.getChordLibrary();
        
        int melodyPos = spSongMelody.getSelectedItemPosition();
        int chordPos = spSongChords.getSelectedItemPosition();
        
        if (melodyPos >= melodies.size() || chordPos >= chords.size()) return;
        
        MusicRepository.MelodyEntry melodyEntry = melodies.get(melodyPos);
        MusicRepository.ChordEntry chordEntry = chords.get(chordPos);
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd_HHmmss", Locale.getDefault());
        String defaultName = "歌曲_" + sdf.format(new Date());
        
        saveSongToLibrary(defaultName, melodyEntry, chordEntry);
        
        ToastHelper.showSuccess(this, "已保存到资源库");
        
        detailBar.setVisibility(View.GONE);
        currentSong = null;
    }
    
    private void discardCurrentSong() {
        ConfirmDialog.show(this, "确定要丢弃此次生成结果吗？", "", "取消", "确认丢弃", () -> {
            currentSong = null;
            detailBar.setVisibility(View.GONE);
            resultList.clear();
            resultAdapter.notifyDataSetChanged();
            btnPlay.setEnabled(false);
            ToastHelper.showInfo(NewSongGeneratorActivity.this, "已丢弃");
        });
    }
    
    private void saveSongToLibrary(String name, MusicRepository.MelodyEntry melodyEntry, MusicRepository.ChordEntry chordEntry) {
        SongEntry songEntry = new SongEntry();
        songEntry.name = name;
        songEntry.style = currentSong.style;
        songEntry.sourceMelodyId = melodyEntry.id;
        songEntry.sourceMelodyName = melodyEntry.name;
        songEntry.sourceChordId = chordEntry.id;
        songEntry.sourceChordName = chordEntry.name;
        songEntry.bpm = 120;
        
        if (currentSong.segments != null) {
            int startTimeMs = 0;
            for (MusicData.Segment segment : currentSong.segments) {
                songEntry.addSegmentWithDuration(segment.melody, segment.chord, startTimeMs, segment.durationMs);
                startTimeMs += segment.durationMs;
            }
        }
        
        songEntry.totalDurationMs = currentSong.totalDurationMs;
        
        repository.addSong(songEntry);
    }
    
    private String formatDuration(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    private void updateSongResult() {
        resultList.clear();
        resultList.add("标题: " + currentSong.title);
        resultList.add("艺术家: " + currentSong.artist);
        resultList.add("风格: " + currentSong.style);
        resultList.add("");
        resultList.add("--- 旋律 ---");
        for (int i = 0; i < currentSong.melody.notes.size(); i++) {
            MusicData.Note note = currentSong.melody.notes.get(i);
            resultList.add((i + 1) + ". " + note.toString());
        }
        resultList.add("");
        resultList.add("--- 和弦 ---");
        for (int i = 0; i < currentSong.chords.chords.size(); i++) {
            MusicData.Chord chord = currentSong.chords.chords.get(i);
            resultList.add((i + 1) + ". " + chord.toString());
        }
        resultAdapter.setHighlightIndex(-1);
        resultAdapter.notifyDataSetChanged();
    }

    private void addResultItem(String title, String detail) {
        if (resultList.isEmpty()) {
            resultList.add("当前已生成：");
            resultList.add("");
        }
        int insertIndex = resultList.size();
        resultList.add(title);
        resultList.add("  " + detail);
        resultList.add("");
        resultAdapter.notifyDataSetChanged();
        lvResult.smoothScrollToPosition(resultList.size() - 1);
    }
    
    private void previewSelectedMelody() {
        List<MusicRepository.MelodyEntry> melodies = repository.getMelodyLibrary();
        if (melodies.isEmpty()) {
            Toast.makeText(this, "旋律库为空", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int pos = spSongMelody.getSelectedItemPosition();
        if (pos >= melodies.size()) return;
        
        if (isBound && playerService != null) {
            MusicData.Melody melody = melodies.get(pos).toMelody();
            if (!melody.notes.isEmpty()) {
                playerService.playMelody(melody);
                Toast.makeText(this, "正在播放旋律", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void previewSelectedChords() {
        Toast.makeText(this, "和弦预览功能开发中", Toast.LENGTH_SHORT).show();
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
            speedControlLayout.setVisibility(View.VISIBLE);
            btnPlay.setEnabled(false);
            btnStop.setEnabled(true);
        }
    }
    
    private void stopSong() {
        if (isBound && playerService != null) {
            playerService.stopPlayback();
            playbackProgress.setProgress(0);
            tvPlaybackTime.setText("0:00 / 0:00");
            currentHighlightedNoteIndex = -1;
            resultAdapter.setHighlightIndex(-1);
            resultAdapter.notifyDataSetChanged();
            btnPlay.setEnabled(true);
            btnStop.setEnabled(false);
        }
    }
    
    private void updateProgressDisplay() {
        if (playerService == null) return;
        
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
    
    @Override
    public void onPlaybackProgress(int positionMs, int totalMs, int currentNoteIndex) {
        runOnUiThread(() -> {
            if (totalMs > 0) {
                int progress = (positionMs * 100) / totalMs;
                playbackProgress.setProgress(progress);
                tvPlaybackTime.setText(formatTime(positionMs) + " / " + formatTime(totalMs));
                
                if (currentNoteIndex != currentHighlightedNoteIndex) {
                    currentHighlightedNoteIndex = currentNoteIndex;
                    resultAdapter.setHighlightIndex(currentNoteIndex + 4);
                    resultAdapter.notifyDataSetChanged();
                    if (currentNoteIndex >= 0) {
                        lvResult.smoothScrollToPosition(currentNoteIndex + 4);
                    }
                }
            }
        });
    }
    
    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        runOnUiThread(() -> {
            btnPlay.setEnabled(!isPlaying);
            btnPlay.setText(isPlaying ? "暂停" : "播放");
            btnStop.setEnabled(isPlaying);
        });
    }
    
    @Override
    public void onPlaybackCompleted() {
        runOnUiThread(() -> {
            playbackProgress.setProgress(0);
            tvPlaybackTime.setText("0:00 / 0:00");
            currentHighlightedNoteIndex = -1;
            resultAdapter.setHighlightIndex(-1);
            resultAdapter.notifyDataSetChanged();
            btnPlay.setEnabled(true);
            btnPlay.setText("播放");
            btnStop.setEnabled(false);
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
        if (isBound && playerService != null) {
            playerService.setPlaybackListener(null);
        }
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }
}
