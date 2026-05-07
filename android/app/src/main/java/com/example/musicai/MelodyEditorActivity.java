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
    
    private MusicPlayerService playerService;
    private boolean isBound = false;
    
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
        notesList = new ArrayList<>();
        notesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notesList);
        lvNotes.setAdapter(notesAdapter);
        
        Spinner spStyle = findViewById(R.id.sp_style);
        ArrayAdapter<String> styleAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, MusicData.MUSIC_STYLES);
        styleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStyle.setAdapter(styleAdapter);
        
        Button btnGenerate = findViewById(R.id.btn_generate);
        Button btnAddNote = findViewById(R.id.btn_add_note);
        Button btnPlay = findViewById(R.id.btn_play);
        Button btnDelete = findViewById(R.id.btn_delete);
        
        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String style = (String) spStyle.getSelectedItem();
                generateMelody(style);
            }
        });
        
        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNote();
            }
        });
        
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playMelody();
            }
        });
        
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteNote();
            }
        });
        
        lvNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                editNote(position);
            }
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
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }
    
    private void generateMelody(String style) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    currentMelody = musicGenerator.generateMelody(style, 8);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateNotesList();
                            Toast.makeText(MelodyEditorActivity.this, "旋律生成完成", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MelodyEditorActivity.this, "生成失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
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
    }
    
    private void deleteNote() {
        int position = lvNotes.getCheckedItemPosition();
        if (position != ListView.INVALID_POSITION) {
            currentMelody.notes.remove(position);
            updateNotesList();
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
        }
    }
    
    private void playMelody() {
        if (currentMelody.notes.isEmpty()) {
            Toast.makeText(this, "没有可播放的音符", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (isBound && playerService != null) {
            playerService.playMelody(currentMelody);
        }
    }
    
    private void updateNotesList() {
        notesList.clear();
        for (MusicData.Note note : currentMelody.notes) {
            notesList.add(note.toString());
        }
        notesAdapter.notifyDataSetChanged();
    }
}
