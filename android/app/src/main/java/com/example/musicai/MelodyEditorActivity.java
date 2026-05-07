package com.example.musicai;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MelodyEditorActivity extends AppCompatActivity {
    
    private MusicGenerator musicGenerator;
    private MusicData.Melody currentMelody;
    private ListView lvNotes;
    private ArrayAdapter<String> notesAdapter;
    private List<String> notesList;
    private ProgressBar progressBar;
    
    private MusicPlayerService playerService;
    private boolean isBound = false;
    private boolean isGenerating = false;
    
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
        setContentView(R.layout.activity_melody_editor);
        
        musicGenerator = new MusicGenerator(this);
        currentMelody = new MusicData.Melody();
        
        lvNotes = findViewById(R.id.lv_notes);
        progressBar = findViewById(R.id.progress_bar);
        notesList = new ArrayList<>();
        notesAdapter = new ArrayAdapter<>(this, R.layout.list_item_note, notesList);
        lvNotes.setAdapter(notesAdapter);
        
        Spinner spStyle = findViewById(R.id.sp_style);
        ArrayAdapter<String> styleAdapter = new ArrayAdapter<>(this, 
            R.layout.spinner_item, MusicData.MUSIC_STYLES);
        styleAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spStyle.setAdapter(styleAdapter);
        
        Button btnGenerate = findViewById(R.id.btn_generate);
        Button btnAddNote = findViewById(R.id.btn_add_note);
        Button btnPlay = findViewById(R.id.btn_play);
        Button btnDelete = findViewById(R.id.btn_delete);
        
        btnGenerate.setOnClickListener(v -> {
            if (isGenerating) return;
            String style = (String) spStyle.getSelectedItem();
            generateMelody(style);
        });
        
        btnAddNote.setOnClickListener(v -> addNote());
        
        btnPlay.setOnClickListener(v -> playMelody());
        
        btnDelete.setOnClickListener(v -> deleteNote());
        
        lvNotes.setOnItemClickListener((parent, view, position, id) -> editNote(position));
        
        lvNotes.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
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
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }
    
    private void generateMelody(String style) {
        isGenerating = true;
        progressBar.setVisibility(View.VISIBLE);
        Button btnGenerate = findViewById(R.id.btn_generate);
        btnGenerate.setEnabled(false);
        
        new Thread(() -> {
            try {
                currentMelody = musicGenerator.generateMelody(style, 8);
                runOnUiThread(() -> {
                    updateNotesList();
                    Toast.makeText(MelodyEditorActivity.this, "旋律生成完成", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(MelodyEditorActivity.this, "生成失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
    
    private void addNote() {
        String[] pitches = MusicData.PITCHES;
        int pitchIndex = (int) (Math.random() * pitches.length);
        int octave = 4;
        int duration = 4;
        
        int startTime = 0;
        if (!currentMelody.notes.isEmpty()) {
            MusicData.Note lastNote = currentMelody.notes.get(currentMelody.notes.size() - 1);
            startTime = lastNote.startTime + lastNote.duration;
        }
        
        MusicData.Note note = new MusicData.Note(pitches[pitchIndex], octave, duration, startTime);
        currentMelody.notes.add(note);
        updateNotesList();
        Toast.makeText(this, "已添加音符", Toast.LENGTH_SHORT).show();
    }
    
    private void deleteNote() {
        int position = lvNotes.getCheckedItemPosition();
        if (position != ListView.INVALID_POSITION) {
            currentMelody.notes.remove(position);
            lvNotes.setItemChecked(position, false);
            updateNotesList();
            Toast.makeText(this, "已删除音符", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "请先选择要删除的音符", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void editNote(int position) {
        if (position >= 0 && position < currentMelody.notes.size()) {
            MusicData.Note note = currentMelody.notes.get(position);
            String[] pitches = MusicData.PITCHES;
            int currentIndex = 0;
            for (int i = 0; i < pitches.length; i++) {
                if (pitches[i].equals(note.pitch)) {
                    currentIndex = i;
                    break;
                }
            }
            
            int newIndex = (currentIndex + 1) % pitches.length;
            note.pitch = pitches[newIndex];
            updateNotesList();
            lvNotes.setItemChecked(position, true);
        }
    }
    
    private void playMelody() {
        if (currentMelody.notes.isEmpty()) {
            Toast.makeText(this, "没有可播放的音符", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (isBound && playerService != null) {
            playerService.playMelody(currentMelody);
            Toast.makeText(this, "开始播放", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateNotesList() {
        notesList.clear();
        int index = 1;
        for (MusicData.Note note : currentMelody.notes) {
            notesList.add(index++ + ". " + note.toString());
        }
        notesAdapter.notifyDataSetChanged();
    }
}