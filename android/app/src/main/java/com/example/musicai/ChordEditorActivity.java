package com.example.musicai;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
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
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chord_editor);
        
        musicGenerator = new MusicGenerator(this);
        currentChords = new MusicData.ChordProgression();
        
        lvChords = findViewById(R.id.lv_chords);
        chordsList = new ArrayList<>();
        chordsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, chordsList);
        lvChords.setAdapter(chordsAdapter);
        
        Spinner spStyle = findViewById(R.id.sp_style);
        ArrayAdapter<String> styleAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, MusicData.MUSIC_STYLES);
        styleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStyle.setAdapter(styleAdapter);
        
        Button btnGenerate = findViewById(R.id.btn_generate);
        Button btnAddChord = findViewById(R.id.btn_add_chord);
        Button btnDelete = findViewById(R.id.btn_delete);
        
        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String style = (String) spStyle.getSelectedItem();
                generateChords(style);
            }
        });
        
        btnAddChord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addChord();
            }
        });
        
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteChord();
            }
        });
        
        lvChords.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                editChord(position);
            }
        });
    }
    
    private void generateChords(String style) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    currentChords = musicGenerator.generateChords(style, 4);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateChordsList();
                            Toast.makeText(ChordEditorActivity.this, "和弦生成完成", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ChordEditorActivity.this, "生成失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
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
    }
    
    private void deleteChord() {
        int position = lvChords.getCheckedItemPosition();
        if (position != ListView.INVALID_POSITION) {
            currentChords.chords.remove(position);
            updateChordsList();
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
        }
    }
    
    private void updateChordsList() {
        chordsList.clear();
        for (MusicData.Chord chord : currentChords.chords) {
            chordsList.add(chord.toString());
        }
        chordsAdapter.notifyDataSetChanged();
    }
}
