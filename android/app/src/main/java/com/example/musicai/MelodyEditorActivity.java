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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MelodyEditorActivity extends AppCompatActivity {
    
    public static final String EXTRA_MELODY_ID = "melody_id";
    public static final int MODE_SELECT = 0;
    public static final int MODE_EDIT = 1;
    
    private int currentMode = MODE_SELECT;
    private String selectedEntryId = null;
    private boolean isOriginalEntry = false;
    
    private MusicRepository repository;
    private MusicData.Melody currentMelody;
    private ListView lvNotes;
    private ListView lvLibrary;
    private ArrayAdapter<String> notesAdapter;
    private ArrayAdapter<String> libraryAdapter;
    private List<String> notesList;
    private List<String> libraryList;
    
    private TextView tvTitle;
    private TextView tvEmpty;
    private LinearLayout selectLayout;
    private LinearLayout editLayout;
    private EditText etEntryName;
    private Button btnSave;
    private Button btnSaveAs;
    private Button btnBack;
    
    private MusicPlayerService playerService;
    private boolean isBound = false;
    
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
        setContentView(R.layout.activity_melody_editor);
        
        repository = MusicRepository.getInstance(this);
        currentMelody = new MusicData.Melody();
        
        initViews();
        setupSpinners();
        loadLibrary();
        updateUI();
    }
    
    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        tvEmpty = findViewById(R.id.tv_empty);
        selectLayout = findViewById(R.id.select_layout);
        editLayout = findViewById(R.id.edit_layout);
        etEntryName = findViewById(R.id.et_entry_name);
        
        lvLibrary = findViewById(R.id.lv_library);
        libraryList = new ArrayList<>();
        libraryAdapter = new ArrayAdapter<>(this, R.layout.list_item_note, libraryList);
        lvLibrary.setAdapter(libraryAdapter);
        lvLibrary.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        
        lvNotes = findViewById(R.id.lv_notes);
        notesList = new ArrayList<>();
        notesAdapter = new ArrayAdapter<>(this, R.layout.list_item_note, notesList);
        lvNotes.setAdapter(notesAdapter);
        lvNotes.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        
        btnSave = findViewById(R.id.btn_save);
        btnSaveAs = findViewById(R.id.btn_save_as);
        btnBack = findViewById(R.id.btn_back);
        
        lvLibrary.setOnItemClickListener((parent, view, position, id) -> selectMelody(position));
        lvNotes.setOnItemClickListener((parent, view, position, id) -> editNote(position));
        
        btnSave.setOnClickListener(v -> saveMelody(false));
        btnSaveAs.setOnClickListener(v -> saveMelody(true));
        btnBack.setOnClickListener(v -> backToSelectMode());
    }
    
    private void setupSpinners() {
        Spinner spPitch = findViewById(R.id.sp_pitch);
        ArrayAdapter<String> pitchAdapter = new ArrayAdapter<>(this, 
            R.layout.spinner_item, MusicData.PITCHES);
        pitchAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spPitch.setAdapter(pitchAdapter);
        
        Spinner spOctave = findViewById(R.id.sp_octave);
        String[] octaves = {"2", "3", "4", "5", "6"};
        ArrayAdapter<String> octaveAdapter = new ArrayAdapter<>(this, 
            R.layout.spinner_item, octaves);
        octaveAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spOctave.setAdapter(octaveAdapter);
        spOctave.setSelection(2);
        
        Spinner spDuration = findViewById(R.id.sp_duration);
        String[] durations = {"1 (全)", "2 (半)", "4 (四分)", "8 (八分)", "16 (十六分)"};
        ArrayAdapter<String> durationAdapter = new ArrayAdapter<>(this, 
            R.layout.spinner_item, durations);
        durationAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spDuration.setAdapter(durationAdapter);
        spDuration.setSelection(2);
        
        Button btnAddNote = findViewById(R.id.btn_add_note);
        Button btnDelete = findViewById(R.id.btn_delete);
        Button btnPlay = findViewById(R.id.btn_play);
        Button btnAddCustomNote = findViewById(R.id.btn_add_custom_note);
        Button btnTransposeUp = findViewById(R.id.btn_transpose_up);
        Button btnTransposeDown = findViewById(R.id.btn_transpose_down);
        
        btnAddNote.setOnClickListener(v -> addNote());
        btnDelete.setOnClickListener(v -> deleteNote());
        btnPlay.setOnClickListener(v -> playMelody());
        btnAddCustomNote.setOnClickListener(v -> addCustomNote());
        btnTransposeUp.setOnClickListener(v -> transpose(1));
        btnTransposeDown.setOnClickListener(v -> transpose(-1));
    }
    
    private void loadLibrary() {
        libraryList.clear();
        List<MusicRepository.MelodyEntry> entries = repository.getMelodyLibrary();
        
        if (entries.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("旋律库为空\n请先在高级生成器中创建旋律");
        } else {
            tvEmpty.setVisibility(View.GONE);
            for (MusicRepository.MelodyEntry entry : entries) {
                String display = entry.name + "\n" + 
                    entry.getPreviewText() + "\n" +
                    "风格: " + entry.style;
                libraryList.add(display);
            }
        }
        libraryAdapter.notifyDataSetChanged();
    }
    
    private void selectMelody(int position) {
        List<MusicRepository.MelodyEntry> entries = repository.getMelodyLibrary();
        if (position >= entries.size()) return;
        
        MusicRepository.MelodyEntry entry = entries.get(position);
        selectedEntryId = entry.id;
        isOriginalEntry = true;
        
        currentMelody = entry.toMelody();
        etEntryName.setText(entry.name);
        
        updateNotesList();
        switchToEditMode();
    }
    
    private void switchToEditMode() {
        currentMode = MODE_EDIT;
        updateUI();
    }
    
    private void backToSelectMode() {
        currentMode = MODE_SELECT;
        selectedEntryId = null;
        isOriginalEntry = false;
        currentMelody = new MusicData.Melody();
        etEntryName.setText("");
        notesList.clear();
        notesAdapter.notifyDataSetChanged();
        loadLibrary();
        updateUI();
    }
    
    private void updateUI() {
        if (currentMode == MODE_SELECT) {
            tvTitle.setText("选择旋律");
            selectLayout.setVisibility(View.VISIBLE);
            editLayout.setVisibility(View.GONE);
        } else {
            tvTitle.setText("编辑旋律");
            selectLayout.setVisibility(View.GONE);
            editLayout.setVisibility(View.VISIBLE);
        }
    }
    
    private void addNote() {
        Spinner spPitch = findViewById(R.id.sp_pitch);
        Spinner spOctave = findViewById(R.id.sp_octave);
        Spinner spDuration = findViewById(R.id.sp_duration);
        
        String pitch = (String) spPitch.getSelectedItem();
        int octave = Integer.parseInt((String) spOctave.getSelectedItem());
        
        String durationStr = (String) spDuration.getSelectedItem();
        int duration = Integer.parseInt(durationStr.split(" ")[0]);
        
        int startTime = 0;
        if (!currentMelody.notes.isEmpty()) {
            MusicData.Note lastNote = currentMelody.notes.get(currentMelody.notes.size() - 1);
            startTime = lastNote.startTime + lastNote.duration;
        }
        
        MusicData.Note note = new MusicData.Note(pitch, octave, duration, startTime);
        currentMelody.notes.add(note);
        updateNotesList();
        Toast.makeText(this, "已添加音符", Toast.LENGTH_SHORT).show();
    }
    
    private void addCustomNote() {
        addNote();
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
            
            Spinner spPitch = findViewById(R.id.sp_pitch);
            Spinner spOctave = findViewById(R.id.sp_octave);
            Spinner spDuration = findViewById(R.id.sp_duration);
            
            note.pitch = (String) spPitch.getSelectedItem();
            note.octave = Integer.parseInt((String) spOctave.getSelectedItem());
            
            String durationStr = (String) spDuration.getSelectedItem();
            note.duration = Integer.parseInt(durationStr.split(" ")[0]);
            
            updateNotesList();
            lvNotes.setItemChecked(position, true);
            Toast.makeText(this, "已更新音符", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void transpose(int semitones) {
        String[] pitches = MusicData.PITCHES;
        for (MusicData.Note note : currentMelody.notes) {
            int currentIndex = -1;
            for (int i = 0; i < pitches.length; i++) {
                if (pitches[i].equals(note.pitch)) {
                    currentIndex = i;
                    break;
                }
            }
            
            if (currentIndex != -1) {
                int newIndex = currentIndex + semitones;
                int newOctave = note.octave;
                
                if (newIndex >= pitches.length) {
                    newIndex = newIndex - pitches.length;
                    newOctave++;
                } else if (newIndex < 0) {
                    newIndex = newIndex + pitches.length;
                    newOctave--;
                }
                
                if (newOctave >= 2 && newOctave <= 6) {
                    note.pitch = pitches[newIndex];
                    note.octave = newOctave;
                }
            }
        }
        updateNotesList();
        Toast.makeText(this, semitones > 0 ? "已升调半音" : "已降调半音", Toast.LENGTH_SHORT).show();
    }
    
    private void saveMelody(boolean saveAsNew) {
        String name = etEntryName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入名称", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentMelody.notes.isEmpty()) {
            Toast.makeText(this, "请添加至少一个音符", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (saveAsNew || !isOriginalEntry) {
            MusicRepository.MelodyEntry newEntry = new MusicRepository.MelodyEntry();
            newEntry.name = name;
            newEntry.style = "自定义";
            for (MusicData.Note note : currentMelody.notes) {
                newEntry.notes.add(new MusicRepository.NoteData(note));
            }
            repository.addMelody(newEntry);
            Toast.makeText(this, "已保存为新条目", Toast.LENGTH_SHORT).show();
        } else {
            repository.deleteMelody(selectedEntryId);
            
            MusicRepository.MelodyEntry updatedEntry = new MusicRepository.MelodyEntry();
            updatedEntry.id = selectedEntryId;
            updatedEntry.name = name;
            updatedEntry.style = "自定义";
            for (MusicData.Note note : currentMelody.notes) {
                updatedEntry.notes.add(new MusicRepository.NoteData(note));
            }
            repository.addMelody(updatedEntry);
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        }
        
        backToSelectMode();
    }
    
    private void playMelody() {
        if (currentMelody.notes.isEmpty()) {
            Toast.makeText(this, "没有可播放的音符", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (isBound && playerService != null) {
            playerService.playMelody(currentMelody);
            startProgressUpdate();
            Toast.makeText(this, "开始播放", Toast.LENGTH_SHORT).show();
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
                }
            }
        };
        
        progressHandler.post(progressUpdateRunnable);
    }
    
    private void updateProgressDisplay() {
        int currentPosition = playerService.getCurrentPosition();
        
        int currentNoteIndex = findCurrentNoteIndex(currentPosition);
        if (currentNoteIndex >= 0 && currentNoteIndex < notesList.size()) {
            lvNotes.setItemChecked(currentNoteIndex, true);
            lvNotes.smoothScrollToPosition(currentNoteIndex);
        }
    }
    
    private int findCurrentNoteIndex(int currentPosition) {
        int positionMs = currentPosition;
        for (int i = 0; i < currentMelody.notes.size(); i++) {
            MusicData.Note note = currentMelody.notes.get(i);
            int noteStartMs = note.startTime * 250;
            int noteEndMs = (note.startTime + note.duration) * 250;
            if (positionMs >= noteStartMs && positionMs < noteEndMs) {
                return i;
            }
        }
        return -1;
    }
    
    private void updateNotesList() {
        notesList.clear();
        int index = 1;
        for (MusicData.Note note : currentMelody.notes) {
            notesList.add(index++ + ". " + note.toString());
        }
        notesAdapter.notifyDataSetChanged();
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
