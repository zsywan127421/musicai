package com.example.musicai;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class NewSongGeneratorActivity extends AppCompatActivity {
    
    private static final int MODE_MELODY = 0;
    private static final int MODE_CHORDS = 1;
    private static final int MODE_SONG = 2;
    
    private MusicGenerator musicGenerator;
    private MusicRepository repository;
    private MusicData.Song currentSong;
    
    private int currentMode = MODE_MELODY;
    
    private RadioGroup rgMode;
    private LinearLayout melodySection;
    private LinearLayout chordsSection;
    private LinearLayout songSection;
    
    private Spinner spStyle;
    private Spinner spMelodyLength;
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
    
    private ProgressBar playbackProgress;
    private TextView tvPlaybackTime;
    private Button btnPlay;
    private Button btnStop;
    
    private ListView lvResult;
    private ArrayAdapter<String> resultAdapter;
    private List<String> resultList;
    
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
        setContentView(R.layout.activity_new_song_generator);
        
        musicGenerator = new MusicGenerator(this);
        repository = MusicRepository.getInstance(this);
        currentSong = new MusicData.Song();
        
        initViews();
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
        
        lvResult = findViewById(R.id.lv_result);
        resultList = new ArrayList<>();
        resultAdapter = new ArrayAdapter<>(this, R.layout.list_item_note, resultList);
        lvResult.setAdapter(resultAdapter);
        
        ArrayAdapter<String> styleAdapter = new ArrayAdapter<>(this, 
            R.layout.spinner_item, MusicData.MUSIC_STYLES);
        styleAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spStyle.setAdapter(styleAdapter);
        
        String[] melodyLengths = {"4音符", "8音符", "16音符", "32音符"};
        ArrayAdapter<String> lengthAdapter = new ArrayAdapter<>(this, 
            R.layout.spinner_item, melodyLengths);
        lengthAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spMelodyLength.setAdapter(lengthAdapter);
        spMelodyLength.setSelection(1);
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
    
    private void generateMelody() {
        if (isGenerating) return;
        
        isGenerating = true;
        updateUI();
        
        String style = (String) spStyle.getSelectedItem();
        String description = etDescription.getText().toString().trim();
        
        String lengthStr = (String) spMelodyLength.getSelectedItem();
        int length = 8;
        if (lengthStr.contains("4")) length = 4;
        else if (lengthStr.contains("8")) length = 8;
        else if (lengthStr.contains("16")) length = 16;
        else if (lengthStr.contains("32")) length = 32;
        
        final int finalLength = length;
        new Thread(() -> {
            try {
                MusicData.Melody melody = musicGenerator.generateMelodyWithDescription(style, finalLength, null, description);
                
                MusicRepository.MelodyEntry entry = new MusicRepository.MelodyEntry();
                entry.name = "旋律_" + System.currentTimeMillis();
                entry.style = style;
                for (MusicData.Note note : melody.notes) {
                    entry.notes.add(new MusicRepository.NoteData(note));
                }
                repository.addMelody(entry);
                
                runOnUiThread(() -> {
                    Toast.makeText(NewSongGeneratorActivity.this, 
                        "旋律已保存到库中！", Toast.LENGTH_SHORT).show();
                    resultList.clear();
                    resultList.add("生成旋律：" + entry.getPreviewText());
                    resultAdapter.notifyDataSetChanged();
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
    
    private void generateChords() {
        if (isGenerating) return;
        
        isGenerating = true;
        updateUI();
        
        String style = (String) spStyle.getSelectedItem();
        int checkedId = rgChordMode.getCheckedRadioButtonId();
        
        new Thread(() -> {
            try {
                MusicData.ChordProgression progression;
                
                if (checkedId == R.id.rb_from_melody) {
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
                
                MusicRepository.ChordEntry entry = new MusicRepository.ChordEntry();
                entry.name = "和弦_" + System.currentTimeMillis();
                entry.style = style;
                entry.keySignature = etKeySignature.getText().toString().trim();
                entry.mood = etMood.getText().toString().trim();
                for (MusicData.Chord chord : progression.chords) {
                    entry.chords.add(new MusicRepository.ChordData(chord));
                }
                repository.addChord(entry);
                
                runOnUiThread(() -> {
                    Toast.makeText(NewSongGeneratorActivity.this, 
                        "和弦已保存到库中！", Toast.LENGTH_SHORT).show();
                    resultList.clear();
                    resultList.add("生成和弦：" + entry.getPreviewText());
                    resultAdapter.notifyDataSetChanged();
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
        MusicRepository.MelodyEntry melodyEntry = melodies.get(melodyPos);
        MusicRepository.ChordEntry chordEntry = chords.get(chordPos);
        
        new Thread(() -> {
            try {
                MusicData.Melody melody = melodyEntry.toMelody();
                MusicData.ChordProgression progression = chordEntry.toChordProgression();
                
                currentSong = musicGenerator.generateCompleteSong(style, melody, progression);
                currentSong.melody = melody;
                currentSong.chords = progression;
                
                runOnUiThread(() -> {
                    Toast.makeText(NewSongGeneratorActivity.this, 
                        "曲子生成完成！", Toast.LENGTH_SHORT).show();
                    updateSongResult();
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
    
    private void updateSongResult() {
        resultList.clear();
        resultList.add("标题: " + currentSong.title);
        resultList.add("艺术家: " + currentSong.artist);
        resultList.add("风格: " + currentSong.style);
        resultList.add("");
        resultList.add("--- 旋律 ---");
        for (MusicData.Note note : currentSong.melody.notes) {
            resultList.add("  " + note.toString());
        }
        resultList.add("");
        resultList.add("--- 和弦 ---");
        for (MusicData.Chord chord : currentSong.chords.chords) {
            resultList.add("  " + chord.toString());
        }
        resultAdapter.notifyDataSetChanged();
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
            startProgressUpdate();
            btnPlay.setEnabled(false);
            btnStop.setEnabled(true);
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
}
