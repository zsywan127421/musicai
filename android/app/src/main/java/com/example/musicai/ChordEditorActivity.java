package com.example.musicai;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ChordEditorActivity extends AppCompatActivity {
    
    public static final String EXTRA_CHORD_ID = "extra_chord_id";
    public static final int MODE_SELECT = 0;
    public static final int MODE_EDIT = 1;
    
    private int currentMode = MODE_SELECT;
    private String selectedEntryId = null;
    private boolean isOriginalEntry = false;
    
    private MusicRepository repository;
    private MusicData.ChordProgression currentChords;
    private ListView lvChords;
    private ListView lvLibrary;
    private ArrayAdapter<String> chordsAdapter;
    private ArrayAdapter<String> libraryAdapter;
    private List<String> chordsList;
    private List<String> libraryList;
    
    private TextView tvTitle;
    private TextView tvEmpty;
    private EditText etEntryName;
    private Button btnSave;
    private Button btnSaveAs;
    private Button btnBack;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chord_editor);
        
        repository = MusicRepository.getInstance(this);
        currentChords = new MusicData.ChordProgression();
        
        initViews();
        setupSpinners();
        loadLibrary();
        updateUI();
    }
    
    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        tvEmpty = findViewById(R.id.tv_empty);
        etEntryName = findViewById(R.id.et_entry_name);
        
        lvLibrary = findViewById(R.id.lv_library);
        libraryList = new ArrayList<>();
        libraryAdapter = new ArrayAdapter<>(this, R.layout.list_item_note, libraryList);
        lvLibrary.setAdapter(libraryAdapter);
        lvLibrary.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        
        lvChords = findViewById(R.id.lv_chords);
        chordsList = new ArrayList<>();
        chordsAdapter = new ArrayAdapter<>(this, R.layout.list_item_note, chordsList);
        lvChords.setAdapter(chordsAdapter);
        lvChords.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        
        btnSave = findViewById(R.id.btn_save);
        btnSaveAs = findViewById(R.id.btn_save_as);
        btnBack = findViewById(R.id.btn_back);
        
        lvLibrary.setOnItemClickListener((parent, view, position, id) -> selectChords(position));
        lvChords.setOnItemClickListener((parent, view, position, id) -> editChord(position));
        
        btnSave.setOnClickListener(v -> saveChords(false));
        btnSaveAs.setOnClickListener(v -> saveChords(true));
        btnBack.setOnClickListener(v -> backToSelectMode());
    }
    
    private void setupSpinners() {
        Spinner spChordRoot = findViewById(R.id.sp_chord_root);
        String[] roots = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
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
        
        Button btnAddChord = findViewById(R.id.btn_add_chord);
        Button btnDelete = findViewById(R.id.btn_delete);
        Button btnAddCustomChord = findViewById(R.id.btn_add_custom_chord);
        Button btnShiftLeft = findViewById(R.id.btn_shift_left);
        Button btnShiftRight = findViewById(R.id.btn_shift_right);
        
        btnAddChord.setOnClickListener(v -> addRandomChord());
        btnDelete.setOnClickListener(v -> deleteChord());
        btnAddCustomChord.setOnClickListener(v -> addCustomChord());
        btnShiftLeft.setOnClickListener(v -> shiftChords(-1));
        btnShiftRight.setOnClickListener(v -> shiftChords(1));
    }
    
    private void loadLibrary() {
        libraryList.clear();
        List<MusicRepository.ChordEntry> entries = repository.getChordLibrary();
        
        if (entries.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("和弦库为空\n请先在高级生成器中创建和弦");
        } else {
            tvEmpty.setVisibility(View.GONE);
            for (MusicRepository.ChordEntry entry : entries) {
                String display = entry.name + "\n" + 
                    entry.getPreviewText() + "\n" +
                    "风格: " + entry.style;
                libraryList.add(display);
            }
        }
        libraryAdapter.notifyDataSetChanged();
    }
    
    private void selectChords(int position) {
        List<MusicRepository.ChordEntry> entries = repository.getChordLibrary();
        if (position >= entries.size()) return;
        
        MusicRepository.ChordEntry entry = entries.get(position);
        selectedEntryId = entry.id;
        isOriginalEntry = true;
        
        currentChords = entry.toChordProgression();
        etEntryName.setText(entry.name);
        
        updateChordsList();
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
        currentChords = new MusicData.ChordProgression();
        etEntryName.setText("");
        chordsList.clear();
        chordsAdapter.notifyDataSetChanged();
        loadLibrary();
        updateUI();
    }
    
    private void updateUI() {
        if (currentMode == MODE_SELECT) {
            tvTitle.setText("选择和弦");
            findViewById(R.id.select_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.edit_layout).setVisibility(View.GONE);
        } else {
            tvTitle.setText("编辑和弦");
            findViewById(R.id.select_layout).setVisibility(View.GONE);
            findViewById(R.id.edit_layout).setVisibility(View.VISIBLE);
        }
    }
    
    private void addRandomChord() {
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
    
    private void addCustomChord() {
        Spinner spChordRoot = findViewById(R.id.sp_chord_root);
        Spinner spChordType = findViewById(R.id.sp_chord_type);
        Spinner spChordDuration = findViewById(R.id.sp_chord_duration);
        
        String name = (String) spChordRoot.getSelectedItem();
        String type = (String) spChordType.getSelectedItem();
        
        String durationStr = (String) spChordDuration.getSelectedItem();
        int duration = Integer.parseInt(durationStr.split(" ")[0]);
        
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
            
            Spinner spChordRoot = findViewById(R.id.sp_chord_root);
            Spinner spChordType = findViewById(R.id.sp_chord_type);
            Spinner spChordDuration = findViewById(R.id.sp_chord_duration);
            
            chord.name = (String) spChordRoot.getSelectedItem();
            chord.type = (String) spChordType.getSelectedItem();
            
            String durationStr = (String) spChordDuration.getSelectedItem();
            chord.duration = Integer.parseInt(durationStr.split(" ")[0]);
            
            updateChordsList();
            lvChords.setItemChecked(position, true);
            Toast.makeText(this, "已更新和弦", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void shiftChords(int direction) {
        String[] notes = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        
        for (MusicData.Chord chord : currentChords.chords) {
            int currentIndex = -1;
            for (int i = 0; i < notes.length; i++) {
                if (notes[i].equals(chord.name)) {
                    currentIndex = i;
                    break;
                }
            }
            
            if (currentIndex != -1) {
                int newIndex = (currentIndex + direction + notes.length) % notes.length;
                chord.name = notes[newIndex];
            }
        }
        
        updateChordsList();
        Toast.makeText(this, direction > 0 ? "已升半音" : "已降半音", Toast.LENGTH_SHORT).show();
    }
    
    private void saveChords(boolean saveAsNew) {
        String name = etEntryName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入名称", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentChords.chords.isEmpty()) {
            Toast.makeText(this, "请添加至少一个和弦", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (saveAsNew || !isOriginalEntry) {
            MusicRepository.ChordEntry newEntry = new MusicRepository.ChordEntry();
            newEntry.name = name;
            newEntry.style = "自定义";
            for (MusicData.Chord chord : currentChords.chords) {
                newEntry.chords.add(new MusicRepository.ChordData(chord));
            }
            repository.addChord(newEntry);
            Toast.makeText(this, "已保存为新条目", Toast.LENGTH_SHORT).show();
        } else {
            repository.deleteChord(selectedEntryId);
            
            MusicRepository.ChordEntry updatedEntry = new MusicRepository.ChordEntry();
            updatedEntry.id = selectedEntryId;
            updatedEntry.name = name;
            updatedEntry.style = "自定义";
            for (MusicData.Chord chord : currentChords.chords) {
                updatedEntry.chords.add(new MusicRepository.ChordData(chord));
            }
            repository.addChord(updatedEntry);
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        }
        
        backToSelectMode();
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
