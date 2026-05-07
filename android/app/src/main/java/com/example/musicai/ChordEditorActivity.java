package com.example.musicai;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ChordEditorActivity extends AppCompatActivity {
    
    private MusicGenerator musicGenerator;
    private MusicData.ChordProgression currentChords;
    private ListView lvChords;
    private ArrayAdapter<String> chordsAdapter;
    private List<String> chordsList;
    private ProgressBar progressBar;
    private boolean isGenerating = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chord_editor);
        
        musicGenerator = new MusicGenerator(this);
        currentChords = new MusicData.ChordProgression();
        
        lvChords = findViewById(R.id.lv_chords);
        progressBar = findViewById(R.id.progress_bar);
        chordsList = new ArrayList<>();
        chordsAdapter = new ArrayAdapter<>(this, R.layout.list_item_note, chordsList);
        lvChords.setAdapter(chordsAdapter);
        
        Spinner spStyle = findViewById(R.id.sp_style);
        ArrayAdapter<String> styleAdapter = new ArrayAdapter<>(this, 
            R.layout.spinner_item, MusicData.MUSIC_STYLES);
        styleAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spStyle.setAdapter(styleAdapter);
        
        Button btnGenerate = findViewById(R.id.btn_generate);
        Button btnAddChord = findViewById(R.id.btn_add_chord);
        Button btnDelete = findViewById(R.id.btn_delete);
        
        btnGenerate.setOnClickListener(v -> {
            if (isGenerating) return;
            String style = (String) spStyle.getSelectedItem();
            generateChords(style);
        });
        
        btnAddChord.setOnClickListener(v -> addChord());
        
        btnDelete.setOnClickListener(v -> deleteChord());
        
        lvChords.setOnItemClickListener((parent, view, position, id) -> editChord(position));
        lvChords.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }
    
    private void generateChords(String style) {
        isGenerating = true;
        progressBar.setVisibility(View.VISIBLE);
        Button btnGenerate = findViewById(R.id.btn_generate);
        btnGenerate.setEnabled(false);
        
        new Thread(() -> {
            try {
                currentChords = musicGenerator.generateChords(style, 4);
                runOnUiThread(() -> {
                    updateChordsList();
                    Toast.makeText(ChordEditorActivity.this, "和弦生成完成", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(ChordEditorActivity.this, "生成失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        String[] names = {"C", "D", "E", "F", "G", "A", "B"};
        String[] types = MusicData.CHORD_TYPES;
        
        int nameIndex = (int) (Math.random() * names.length);
        int typeIndex = (int) (Math.random() * types.length);
        
        int startTime = 0;
        if (!currentChords.chords.isEmpty()) {
            MusicData.Chord lastChord = currentChords.chords.get(currentChords.chords.size() - 1);
            startTime = lastChord.startTime + lastChord.duration;
        }
        
        MusicData.Chord chord = new MusicData.Chord(names[nameIndex], types[typeIndex], 4, startTime);
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
            String[] types = MusicData.CHORD_TYPES;
            int currentIndex = 0;
            for (int i = 0; i < types.length; i++) {
                if (types[i].equals(chord.type)) {
                    currentIndex = i;
                    break;
                }
            }
            
            int newIndex = (currentIndex + 1) % types.length;
            chord.type = types[newIndex];
            updateChordsList();
            lvChords.setItemChecked(position, true);
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