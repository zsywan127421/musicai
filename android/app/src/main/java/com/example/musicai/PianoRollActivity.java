package com.example.musicai;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicai.util.ConfirmDialog;
import com.example.musicai.view.PianoRollView;

import java.util.ArrayList;
import java.util.List;

public class PianoRollActivity extends BaseActivity {
    
    public static final String EXTRA_MELODY_ID = "melody_id";
    
    private PianoRollView pianoRollView;
    private TextView tvTitle;
    private TextView tvInfo;
    private Button btnBack;
    private Button btnSave;
    private Button btnAddNote;
    private Button btnDeleteNote;
    
    private MusicRepository repository;
    private MusicData.Melody melody;
    private String melodyId;
    private boolean hasChanges = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_piano_roll);
        
        repository = MusicRepository.getInstance(this);
        
        melodyId = getIntent().getStringExtra(EXTRA_MELODY_ID);
        
        initViews();
        loadMelody();
        setupListeners();
    }
    
    private void initViews() {
        pianoRollView = findViewById(R.id.piano_roll_view);
        tvTitle = findViewById(R.id.tv_title);
        tvInfo = findViewById(R.id.tv_info);
        btnBack = findViewById(R.id.btn_back);
        btnSave = findViewById(R.id.btn_save);
        btnAddNote = findViewById(R.id.btn_add_note);
        btnDeleteNote = findViewById(R.id.btn_delete_note);
        
        pianoRollView.setStartOctave(3);
    }
    
    private void loadMelody() {
        if (melodyId != null) {
            MusicRepository.MelodyEntry entry = repository.getMelodyById(melodyId);
            if (entry != null) {
                melody = entry.toMelody();
                tvTitle.setText(entry.name);
                updateInfo();
                pianoRollView.setNotes(melody.notes);
                return;
            }
        }
        
        melody = new MusicData.Melody();
        melody.notes = new ArrayList<>();
        melody.name = "新旋律";
        tvTitle.setText("新旋律");
        updateInfo();
        pianoRollView.setNotes(melody.notes);
    }
    
    private void updateInfo() {
        if (melody != null && melody.notes != null) {
            tvInfo.setText("音符数: " + melody.notes.size() + "  |  点击音符可拖拽编辑");
        }
    }
    
    private void setupListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());
        
        btnSave.setOnClickListener(v -> saveMelody());
        
        btnAddNote.setOnClickListener(v -> addRandomNote());
        
        btnDeleteNote.setOnClickListener(v -> deleteSelectedNote());
        
        pianoRollView.setOnNoteChangedListener(new PianoRollView.OnNoteChangedListener() {
            @Override
            public void onNoteChanged(int index, String pitch, int octave, int duration, int startTime) {
                if (index >= 0 && index < melody.notes.size()) {
                    MusicData.Note note = melody.notes.get(index);
                    note.pitch = pitch;
                    note.octave = octave;
                    note.duration = duration;
                    note.startTime = startTime;
                    hasChanges = true;
                    updateInfo();
                }
            }
            
            @Override
            public void onNoteSelected(int index) {
                btnDeleteNote.setEnabled(index >= 0);
                if (index >= 0) {
                    MusicData.Note note = melody.notes.get(index);
                    Toast.makeText(PianoRollActivity.this, 
                        "已选择: " + note.pitch + note.octave + " 时值:" + note.duration, Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        btnDeleteNote.setEnabled(false);
    }
    
    private void addRandomNote() {
        String[] pitches = MusicData.PITCHES;
        String pitch = pitches[(int) (Math.random() * pitches.length)];
        int octave = 3 + (int) (Math.random() * 3);
        int duration = 4;
        int startTime = 0;
        
        if (!melody.notes.isEmpty()) {
            MusicData.Note lastNote = melody.notes.get(melody.notes.size() - 1);
            startTime = lastNote.startTime + lastNote.duration;
        }
        
        MusicData.Note note = new MusicData.Note(pitch, octave, duration, startTime);
        melody.notes.add(note);
        pianoRollView.setNotes(melody.notes);
        hasChanges = true;
        updateInfo();
        Toast.makeText(this, "已添加音符: " + pitch + octave, Toast.LENGTH_SHORT).show();
    }
    
    private void deleteSelectedNote() {
        int selectedIndex = pianoRollView.getSelectedNoteIndex();
        if (selectedIndex >= 0 && selectedIndex < melody.notes.size()) {
            MusicData.Note note = melody.notes.get(selectedIndex);
            ConfirmDialog.showDelete(this, note.pitch + note.octave, () -> {
                melody.notes.remove(selectedIndex);
                pianoRollView.setNotes(melody.notes);
                hasChanges = true;
                updateInfo();
                btnDeleteNote.setEnabled(false);
                Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    private void saveMelody() {
        if (melody.notes.isEmpty()) {
            Toast.makeText(this, "请添加至少一个音符", Toast.LENGTH_SHORT).show();
            return;
        }
        
        MusicRepository.MelodyEntry entry = new MusicRepository.MelodyEntry();
        entry.name = melody.name;
        entry.style = "自定义";
        for (MusicData.Note note : melody.notes) {
            entry.notes.add(new MusicRepository.NoteData(note));
        }
        
        if (melodyId != null) {
            repository.deleteMelody(melodyId);
        }
        
        repository.addMelody(entry);
        hasChanges = false;
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    @Override
    public void onBackPressed() {
        if (hasChanges) {
            ConfirmDialog.show(this, "有未保存的更改", "是否放弃更改？", () -> {
                super.onBackPressed();
            });
        } else {
            super.onBackPressed();
        }
    }
}
