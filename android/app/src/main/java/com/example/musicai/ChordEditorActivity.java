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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicai.util.ConfirmDialog;

import java.util.ArrayList;
import java.util.List;

public class ChordEditorActivity extends BaseActivity {
    
    public static final String EXTRA_CHORD_ID = "chord_id";
    public static final int MODE_SELECT = 0;
    public static final int MODE_EDIT = 1;
    
    private int currentMode = MODE_SELECT;
    private String selectedEntryId = null;
    private boolean isOriginalEntry = false;
    private int selectedChordIndex = -1;
    
    private MusicRepository repository;
    private MusicData.ChordProgression currentChordProgression;
    private ListView lvChords;
    private ListView lvLibrary;
    private ArrayAdapter<String> chordsAdapter;
    private ArrayAdapter<String> libraryAdapter;
    private List<String> chordsList;
    private List<String> libraryList;
    
    private TextView tvTitle;
    private TextView tvEmpty;
    private LinearLayout selectLayout;
    private LinearLayout editLayout;
    private EditText etEntryName;
    private Button btnSave;
    private Button btnSaveAs;
    private Button btnBack;
    
    private Handler progressHandler = new Handler();
    
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
        }
        
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chord_editor);
        
        repository = MusicRepository.getInstance(this);
        currentChordProgression = new MusicData.ChordProgression();
        
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
        
        lvChords = findViewById(R.id.lv_notes);
        chordsList = new ArrayList<>();
        chordsAdapter = new ArrayAdapter<>(this, R.layout.list_item_note, chordsList);
        lvChords.setAdapter(chordsAdapter);
        lvChords.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        
        btnSave = findViewById(R.id.btn_save);
        btnSaveAs = findViewById(R.id.btn_save_as);
        btnBack = findViewById(R.id.btn_back);
        
        lvLibrary.setOnItemClickListener((parent, view, position, id) -> selectChord(position));
        lvChords.setOnItemClickListener((parent, view, position, id) -> selectChordForEdit(position));
        
        btnSave.setOnClickListener(v -> saveChord(false));
        btnSaveAs.setOnClickListener(v -> saveChord(true));
        btnBack.setOnClickListener(v -> backToSelectMode());
        
        Button btnAddChord = findViewById(R.id.btn_add_note);
        Button btnDelete = findViewById(R.id.btn_delete);
        Button btnTransposeUp = findViewById(R.id.btn_transpose_up);
        Button btnTransposeDown = findViewById(R.id.btn_transpose_down);
        
        if (btnAddChord != null) {
            btnAddChord.setOnClickListener(v -> addChord());
        }
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> deleteChord());
        }
        if (btnTransposeUp != null) {
            btnTransposeUp.setOnClickListener(v -> transpose(1));
        }
        if (btnTransposeDown != null) {
            btnTransposeDown.setOnClickListener(v -> transpose(-1));
        }
    }
    
    private void setupSpinners() {
        Spinner spPitch = findViewById(R.id.sp_pitch);
        if (spPitch != null) {
            String[] roots = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
            ArrayAdapter<String> rootAdapter = new ArrayAdapter<>(this, 
                R.layout.spinner_item, roots);
            rootAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spPitch.setAdapter(rootAdapter);
        }
        
        Spinner spOctave = findViewById(R.id.sp_octave);
        if (spOctave != null) {
            String[] octaves = {"2", "3", "4", "5"};
            ArrayAdapter<String> octaveAdapter = new ArrayAdapter<>(this, 
                R.layout.spinner_item, octaves);
            octaveAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spOctave.setAdapter(octaveAdapter);
            spOctave.setSelection(1);
        }
        
        Spinner spDuration = findViewById(R.id.sp_duration);
        if (spDuration != null) {
            String[] durations = {"1 (全)", "2 (半)", "4 (四分)", "8 (八分)"};
            ArrayAdapter<String> durationAdapter = new ArrayAdapter<>(this, 
                R.layout.spinner_item, durations);
            durationAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spDuration.setAdapter(durationAdapter);
            spDuration.setSelection(1);
        }
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
    
    private void selectChord(int position) {
        List<MusicRepository.ChordEntry> entries = repository.getChordLibrary();
        if (position >= entries.size()) return;
        
        MusicRepository.ChordEntry entry = entries.get(position);
        selectedEntryId = entry.id;
        isOriginalEntry = true;
        
        currentChordProgression = entry.toChordProgression();
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
        currentChordProgression = new MusicData.ChordProgression();
        etEntryName.setText("");
        chordsList.clear();
        chordsAdapter.notifyDataSetChanged();
        loadLibrary();
        updateUI();
    }
    
    private void updateUI() {
        if (currentMode == MODE_SELECT) {
            tvTitle.setText("选择和弦");
            selectLayout.setVisibility(View.VISIBLE);
            editLayout.setVisibility(View.GONE);
        } else {
            tvTitle.setText("编辑和弦");
            selectLayout.setVisibility(View.GONE);
            editLayout.setVisibility(View.VISIBLE);
        }
    }
    
    private void addChord() {
        Spinner spPitch = findViewById(R.id.sp_pitch);
        Spinner spDuration = findViewById(R.id.sp_duration);
        
        String root = (String) spPitch.getSelectedItem();
        String type = "major";
        
        int durationStr = 4;
        String durationItem = (String) spDuration.getSelectedItem();
        if (durationItem != null) {
            try {
                durationStr = Integer.parseInt(durationItem.split(" ")[0]);
            } catch (NumberFormatException e) {
                durationStr = 4;
            }
        }
        
        int startTime = 0;
        if (!currentChordProgression.chords.isEmpty()) {
            MusicData.Chord lastChord = currentChordProgression.chords.get(currentChordProgression.chords.size() - 1);
            startTime = lastChord.startTime + lastChord.duration;
        }
        
        MusicData.Chord chord = new MusicData.Chord(root, type, durationStr, startTime);
        currentChordProgression.chords.add(chord);
        updateChordsList();
        Toast.makeText(this, "已添加和弦", Toast.LENGTH_SHORT).show();
    }
    
    private void deleteChord() {
        int position = lvChords.getCheckedItemPosition();
        if (position != ListView.INVALID_POSITION) {
            currentChordProgression.chords.remove(position);
            lvChords.setItemChecked(position, false);
            updateChordsList();
            Toast.makeText(this, "已删除和弦", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "请先选择要删除的和弦", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void selectChordForEdit(int position) {
        if (position < 0 || position >= currentChordProgression.chords.size()) return;
        
        MusicData.Chord chord = currentChordProgression.chords.get(position);
        selectedChordIndex = position;
        
        Spinner spPitch = findViewById(R.id.sp_pitch);
        Spinner spDuration = findViewById(R.id.sp_duration);
        
        String[] roots = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        for (int i = 0; i < roots.length; i++) {
            if (roots[i].equals(chord.name)) {
                spPitch.setSelection(i);
                break;
            }
        }
        
        int durationIndex = 1;
        switch (chord.duration) {
            case 1: durationIndex = 0; break;
            case 2: durationIndex = 1; break;
            case 4: durationIndex = 2; break;
            case 8: durationIndex = 3; break;
        }
        spDuration.setSelection(durationIndex);
        
        lvChords.setItemChecked(position, true);
        Toast.makeText(this, "已选择和弦 " + (position + 1) + "，点击\"应用修改\"保存", Toast.LENGTH_SHORT).show();
    }
    
    private void transpose(int semitones) {
        String[] roots = MusicData.PITCHES;
        for (MusicData.Chord chord : currentChordProgression.chords) {
            int currentIndex = -1;
            for (int i = 0; i < roots.length; i++) {
                if (roots[i].equals(chord.name)) {
                    currentIndex = i;
                    break;
                }
            }
            
            if (currentIndex != -1) {
                int newIndex = currentIndex + semitones;
                
                if (newIndex >= roots.length) {
                    newIndex = newIndex - roots.length;
                } else if (newIndex < 0) {
                    newIndex = newIndex + roots.length;
                }
                
                chord.name = roots[newIndex];
            }
        }
        updateChordsList();
        Toast.makeText(this, semitones > 0 ? "已升调半音" : "已降调半音", Toast.LENGTH_SHORT).show();
    }
    
    private void saveChord(boolean saveAsNew) {
        String name = etEntryName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入名称", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentChordProgression.chords.isEmpty()) {
            Toast.makeText(this, "请添加至少一个和弦", Toast.LENGTH_SHORT).show();
            return;
        }
        
        boolean nameExists = !saveAsNew && isOriginalEntry && 
                             repository.chordNameExists(name) && 
                             !name.equals(getCurrentEntryName());
        
        if (nameExists) {
            ConfirmDialog.showSave(this, name, () -> doSaveChord(saveAsNew, name));
        } else {
            doSaveChord(saveAsNew, name);
        }
    }
    
    private String getCurrentEntryName() {
        if (selectedEntryId != null) {
            for (MusicRepository.ChordEntry entry : repository.getChordLibrary()) {
                if (entry.id.equals(selectedEntryId)) {
                    return entry.name;
                }
            }
        }
        return null;
    }
    
    private void doSaveChord(boolean saveAsNew, String name) {
        if (saveAsNew || !isOriginalEntry) {
            MusicRepository.ChordEntry newEntry = new MusicRepository.ChordEntry();
            newEntry.name = name;
            newEntry.style = "自定义";
            newEntry.keySignature = "";
            newEntry.mood = "";
            for (MusicData.Chord chord : currentChordProgression.chords) {
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
            updatedEntry.keySignature = "";
            updatedEntry.mood = "";
            for (MusicData.Chord chord : currentChordProgression.chords) {
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
        for (MusicData.Chord chord : currentChordProgression.chords) {
            chordsList.add(index++ + ". " + chord.toString());
        }
        chordsAdapter.notifyDataSetChanged();
    }
    
    @Override
    protected void onStart() {
        super.onStart();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
    }
}
