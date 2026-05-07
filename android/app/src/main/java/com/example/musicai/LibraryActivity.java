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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class LibraryActivity extends AppCompatActivity {
    
    public static final int TAB_MELODY = 0;
    public static final int TAB_CHORD = 1;
    
    private int currentTab = TAB_MELODY;
    
    private MusicRepository repository;
    private MusicPlayerService playerService;
    private boolean isBound = false;
    
    private ListView lvLibrary;
    private TextView tvEmpty;
    private TextView tvTabMelody;
    private TextView tvTabChord;
    private TextView tvTitle;
    private Button btnPreview;
    private Button btnDelete;
    private Button btnClearAll;
    
    private List<String> libraryList;
    private ArrayAdapter<String> adapter;
    
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
        setContentView(R.layout.activity_library);
        
        repository = MusicRepository.getInstance(this);
        
        initViews();
        setupListeners();
        updateUI();
    }
    
    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        tvTabMelody = findViewById(R.id.tv_tab_melody);
        tvTabChord = findViewById(R.id.tv_tab_chord);
        lvLibrary = findViewById(R.id.lv_library);
        tvEmpty = findViewById(R.id.tv_empty);
        btnPreview = findViewById(R.id.btn_preview);
        btnDelete = findViewById(R.id.btn_delete);
        btnClearAll = findViewById(R.id.btn_clear_all);
        
        libraryList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, R.layout.list_item_note, libraryList);
        lvLibrary.setAdapter(adapter);
        lvLibrary.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }
    
    private void setupListeners() {
        tvTabMelody.setOnClickListener(v -> switchTab(TAB_MELODY));
        tvTabChord.setOnClickListener(v -> switchTab(TAB_CHORD));
        
        btnPreview.setOnClickListener(v -> previewSelected());
        btnDelete.setOnClickListener(v -> deleteSelected());
        btnClearAll.setOnClickListener(v -> clearAll());
        
        lvLibrary.setOnItemClickListener((parent, view, position, id) -> {
            lvLibrary.setItemChecked(position, true);
            updateButtonStates();
        });
    }
    
    private void switchTab(int tab) {
        currentTab = tab;
        updateUI();
    }
    
    private void updateUI() {
        tvTabMelody.setBackgroundResource(currentTab == TAB_MELODY ? 
            R.drawable.tab_selected_bg : R.drawable.tab_bg);
        tvTabChord.setBackgroundResource(currentTab == TAB_CHORD ? 
            R.drawable.tab_selected_bg : R.drawable.tab_bg);
        
        tvTabMelody.setTextColor(getResources().getColor(
            currentTab == TAB_MELODY ? R.color.apple_text : R.color.apple_text_secondary, null));
        tvTabChord.setTextColor(getResources().getColor(
            currentTab == TAB_CHORD ? R.color.apple_text : R.color.apple_text_secondary, null));
        
        tvTitle.setText(currentTab == TAB_MELODY ? "我的旋律库" : "我的和弦库");
        
        loadLibrary();
        updateButtonStates();
    }
    
    private void loadLibrary() {
        libraryList.clear();
        
        if (currentTab == TAB_MELODY) {
            List<MusicRepository.MelodyEntry> melodies = repository.getMelodyLibrary();
            if (melodies.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("旋律库为空\n请先在歌曲生成器中创建旋律");
            } else {
                tvEmpty.setVisibility(View.GONE);
                for (MusicRepository.MelodyEntry entry : melodies) {
                    String display = entry.name + "\n" + 
                        entry.getPreviewText() + "\n" +
                        "风格: " + entry.style + " | " + entry.notes.size() + "个音符";
                    libraryList.add(display);
                }
            }
        } else {
            List<MusicRepository.ChordEntry> chords = repository.getChordLibrary();
            if (chords.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("和弦库为空\n请先在歌曲生成器中创建和弦");
            } else {
                tvEmpty.setVisibility(View.GONE);
                for (MusicRepository.ChordEntry entry : chords) {
                    String display = entry.name + "\n" + 
                        entry.getPreviewText() + "\n" +
                        "风格: " + entry.style + " | " + entry.chords.size() + "个和弦";
                    libraryList.add(display);
                }
            }
        }
        
        adapter.notifyDataSetChanged();
        lvLibrary.setItemChecked(-1, true);
    }
    
    private void updateButtonStates() {
        int position = lvLibrary.getCheckedItemPosition();
        boolean hasSelection = position != ListView.INVALID_POSITION;
        btnPreview.setEnabled(hasSelection);
        btnDelete.setEnabled(hasSelection);
    }
    
    private void previewSelected() {
        int position = lvLibrary.getCheckedItemPosition();
        if (position == ListView.INVALID_POSITION) {
            Toast.makeText(this, "请先选择要预览的条目", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!isBound) {
            Toast.makeText(this, "播放器服务未连接", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentTab == TAB_MELODY) {
            List<MusicRepository.MelodyEntry> melodies = repository.getMelodyLibrary();
            if (position < melodies.size()) {
                MusicRepository.MelodyEntry entry = melodies.get(position);
                MusicData.Melody melody = entry.toMelody();
                if (!melody.notes.isEmpty()) {
                    playerService.playMelody(melody);
                    Toast.makeText(this, "正在播放: " + entry.name, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "旋律为空，无法播放", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            List<MusicRepository.ChordEntry> chords = repository.getChordLibrary();
            if (position < chords.size()) {
                MusicRepository.ChordEntry entry = chords.get(position);
                MusicData.ChordProgression progression = entry.toChordProgression();
                if (!progression.chords.isEmpty()) {
                    Toast.makeText(this, "正在播放: " + entry.name, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "和弦为空，无法播放", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    private void deleteSelected() {
        int position = lvLibrary.getCheckedItemPosition();
        if (position == ListView.INVALID_POSITION) {
            Toast.makeText(this, "请先选择要删除的条目", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentTab == TAB_MELODY) {
            List<MusicRepository.MelodyEntry> melodies = repository.getMelodyLibrary();
            if (position < melodies.size()) {
                String name = melodies.get(position).name;
                repository.deleteMelody(melodies.get(position).id);
                Toast.makeText(this, "已删除: " + name, Toast.LENGTH_SHORT).show();
            }
        } else {
            List<MusicRepository.ChordEntry> chords = repository.getChordLibrary();
            if (position < chords.size()) {
                String name = chords.get(position).name;
                repository.deleteChord(chords.get(position).id);
                Toast.makeText(this, "已删除: " + name, Toast.LENGTH_SHORT).show();
            }
        }
        
        loadLibrary();
    }
    
    private void clearAll() {
        if (currentTab == TAB_MELODY) {
            if (repository.getMelodyCount() == 0) {
                Toast.makeText(this, "旋律库已经是空的", Toast.LENGTH_SHORT).show();
                return;
            }
            repository.clearMelodyLibrary();
            Toast.makeText(this, "已清空旋律库", Toast.LENGTH_SHORT).show();
        } else {
            if (repository.getChordCount() == 0) {
                Toast.makeText(this, "和弦库已经是空的", Toast.LENGTH_SHORT).show();
                return;
            }
            repository.clearChordLibrary();
            Toast.makeText(this, "已清空和弦库", Toast.LENGTH_SHORT).show();
        }
        
        loadLibrary();
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
}
