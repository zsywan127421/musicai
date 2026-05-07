package com.example.musicai;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ChordEditorActivity extends AppCompatActivity {
    
    private MusicGenerator musicGenerator;
    private MusicData.ChordProgression currentChords;
    private MusicData.ChordProgression customChordProgression;
    private ListView lvChords;
    private ArrayAdapter<String> chordsAdapter;
    private List<String> chordsList;
    private ProgressBar progressBar;
    private TextView tvCustomChords;
    
    private boolean isGenerating = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chord_editor);
        
        musicGenerator = new MusicGenerator(this);
        currentChords = new MusicData.ChordProgression();
        customChordProgression = new MusicData.ChordProgression();
        
        lvChords = findViewById(R.id.lv_chords);
        progressBar = findViewById(R.id.progress_bar);
        tvCustomChords = findViewById(R.id.tv_custom_chords);
        chordsList = new ArrayList<>();
        chordsAdapter = new ArrayAdapter<>(this, R.layout.list_item_note, chordsList);
        lvChords.setAdapter(chordsAdapter);
        
        setupSpinners();
        
        Button btnGenerate = findViewById(R.id.btn_generate);
        Button btnAddChord = findViewById(R.id.btn_add_chord);
        Button btnDelete = findViewById(R.id.btn_delete);
        Button btnAddCustomChord = findViewById(R.id.btn_add_custom_chord);
        Button btnClearCustom = findViewById(R.id.btn_clear_custom_chords);
        
        btnGenerate.setOnClickListener(v -> {
            if (isGenerating) return;
            String style = (String) ((Spinner) findViewById(R.id.sp_style)).getSelectedItem();
            generateChords(style);
        });
        
        btnAddChord.setOnClickListener(v -> addChord());
        
        btnDelete.setOnClickListener(v -> deleteChord());
        
        btnAddCustomChord.setOnClickListener(v -> addCustomChord());
        
        btnClearCustom.setOnClickListener(v -> clearCustomProgression());
        
        lvChords.setOnItemClickListener((parent, view, position, id) -> editChord(position));
        lvChords.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        
        updateCustomProgressionDisplay();
    }
    
    private void setupSpinners() {
        Spinner spStyle = findViewById(R.id.sp_style);
        ArrayAdapter<String> styleAdapter = new ArrayAdapter<>(this, 
            R.layout.spinner_item, MusicData.MUSIC_STYLES);
        styleAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spStyle.setAdapter(styleAdapter);
        
        Spinner spChordRoot = findViewById(R.id.sp_chord_root);
        String[] roots = {"C", "D", "E", "F", "G", "A", "B"};
        ArrayAdapter<String> rootAdapter = new ArrayAdapter<>(this, 
            R.layout.spinner_item, roots);
        rootAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spChordRoot.setAdapter(rootAdapter);
        
        Spinner spChordType = findViewById(R.id.sp_chord_type);
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, 
            R.layout.spinner_item, MusicData.CHORD_TYPES);
        typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spChordType.setAdapter(typeAdapter);
        
        Spinner spChordDuration = findViewById(R.id.sp_chord_duration);
        String[] durations = {"1 (全)", "2 (半)", "4 (四分)", "8 (八分)", "16 (十六分)"};
        ArrayAdapter<String> durationAdapter = new ArrayAdapter<>(this, 
            R.layout.spinner_item, durations);
        durationAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spChordDuration.setAdapter(durationAdapter);
        spChordDuration.setSelection(2);
    }
    
    private void generateChords(String style) {
        isGenerating = true;
        progressBar.setVisibility(View.VISIBLE);
        Button btnGenerate = findViewById(R.id.btn_generate);
        btnGenerate.setEnabled(false);
        
        new Thread(() -> {
            try {
                MusicData.ChordProgression progressionToUse = customChordProgression.chords.isEmpty() ? null : customChordProgression;
                currentChords = musicGenerator.generateChords(style, 4, progressionToUse);
                runOnUiThread(() -> {
                    updateChordsList();
                    Toast.makeText(ChordEditorActivity.this, "和弦进行生成完成！", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(ChordEditorActivity.this, "生成失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } finally {
                runOnUiThread(() -> {
                    isGenerating = false;
                    progressBar.setVisibility(View.GONE);
                    btnGenerate.setEnabled(true);
                });
            }
        }).start();
    }
    
    private void addChord() {
        String[] chordNames = {"C", "D", "E", "F", "G", "A", "B"};
        String name = chordNames[(int) (Math.random() * chordNames.length)];
        String type = MusicData.CHORD_TYPES[(int) (Math.random() * MusicData.CHORD_TYPES.length)];
        int duration = 4;
        
        int startTime = 0;
        if (!currentChords.chords.isEmpty()) {
            MusicData.Chord lastChord = currentChords.chords.get(currentChords.chords.size() - 1);
            startTime = lastChord.startTime + lastChord.duration;
        }
        
        MusicData.Chord chord = new MusicData.Chord(name, type, duration, startTime);
        currentChords.chords.add(chord);
        updateChordsList();
        Toast.makeText(this, "已添加和弦", Toast.LENGTH_SHORT).show();
    }
    
    private void deleteChord() {
        int position = lvChords.getCheckedItemPosition();
        if (position != ListView.INVALID_POSITION) {
            currentChords.chords.remove(position);
            lvChords.setItemChecked(position, false);
            updateChordsList();
            Toast.makeText(this, "已删除和弦", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "请先选择要删除的和弦", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void editChord(int position) {
        if (position >= 0 && position < currentChords.chords.size()) {
            MusicData.Chord chord = currentChords.chords.get(position);
            int currentIndex = 0;
            for (int i = 0; i < MusicData.CHORD_TYPES.length; i++) {
                if (MusicData.CHORD_TYPES[i].equals(chord.type)) {
                    currentIndex = i;
                    break;
                }
            }
            
            int newIndex = (currentIndex + 1) % MusicData.CHORD_TYPES.length;
            chord.type = MusicData.CHORD_TYPES[newIndex];
            updateChordsList();
            lvChords.setItemChecked(position, true);
        }
    }
    
    private void addCustomChord() {
        Spinner spChordRoot = findViewById(R.id.sp_chord_root);
        Spinner spChordType = findViewById(R.id.sp_chord_type);
        Spinner spChordDuration = findViewById(R.id.sp_chord_duration);
        
        String name = (String) spChordRoot.getSelectedItem();
        String type = (String) spChordType.getSelectedItem();
        
        String durationStr = (String) spChordDuration.getSelectedItem();
        int duration = Integer.parseInt(durationStr.split(" ")[0]);
        
        int startTime = 0;
        if (!customChordProgression.chords.isEmpty()) {
            MusicData.Chord lastChord = customChordProgression.chords.get(customChordProgression.chords.size() - 1);
            startTime = lastChord.startTime + lastChord.duration;
        }
        
        MusicData.Chord chord = new MusicData.Chord(name, type, duration, startTime);
        customChordProgression.chords.add(chord);
        updateCustomProgressionDisplay();
        Toast.makeText(this, "已添加和弦到进行", Toast.LENGTH_SHORT).show();
    }
    
    private void clearCustomProgression() {
        customChordProgression.chords.clear();
        updateCustomProgressionDisplay();
        Toast.makeText(this, "和弦进行已清除", Toast.LENGTH_SHORT).show();
    }
    
    private void updateCustomProgressionDisplay() {
        if (customChordProgression.chords.isEmpty()) {
            tvCustomChords.setText("尚未添加和弦");
        } else {
            StringBuilder sb = new StringBuilder();
            for (MusicData.Chord chord : customChordProgression.chords) {
                if (sb.length() > 0) sb.append(" → ");
                sb.append(chord.toString());
            }
            tvCustomChords.setText(sb.toString());
        }
    }
    
    private void updateChordsList() {
        chordsList.clear();
        int index = 1;
        for (MusicData.Chord chord : currentChords.chords) {
            chordsList.add(index++ + ". " + chord.toString());
        }
        chordsAdapter.notifyDataSetChanged();
    }
}
