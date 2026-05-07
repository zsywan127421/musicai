package com.example.musicai;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicai.adapter.LibraryAdapter;
import com.example.musicai.util.ToastHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LibraryActivity extends AppCompatActivity {
    
    private RadioGroup rgType;
    private RadioButton rbMelodies, rbChords;
    private ListView lvLibrary;
    private LinearLayout emptyStateLayout;
    private TextView tvEmptyTitle, tvEmptyMessage, tvNoResults;
    private Button btnGoCreate;
    private EditText etSearch;
    private ProgressBar progressBar;
    
    private MusicRepository repository;
    private LibraryAdapter adapter;
    private boolean isShowingMelodies = true;
    private List<Object> allMelodies = new ArrayList<>();
    private List<Object> allChords = new ArrayList<>();
    private String currentSearchQuery = "";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);
        
        repository = MusicRepository.getInstance(this);
        
        initViews();
        setupListeners();
        loadData();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }
    
    private void initViews() {
        rgType = findViewById(R.id.rg_type);
        rbMelodies = findViewById(R.id.rb_melodies);
        rbChords = findViewById(R.id.rb_chords);
        lvLibrary = findViewById(R.id.lv_library);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        tvEmptyTitle = findViewById(R.id.tv_empty_title);
        tvEmptyMessage = findViewById(R.id.tv_empty_message);
        tvNoResults = findViewById(R.id.tv_no_results);
        btnGoCreate = findViewById(R.id.btn_go_create);
        etSearch = findViewById(R.id.et_search);
        progressBar = findViewById(R.id.progress_bar);
    }
    
    private void setupListeners() {
        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            isShowingMelodies = (checkedId == R.id.rb_melodies);
            etSearch.setText("");
            currentSearchQuery = "";
            loadData();
        });
        
        lvLibrary.setOnItemClickListener((parent, view, position, id) -> {
            if (adapter == null) return;
            
            String itemId = adapter.getItemStringId(position);
            if (itemId == null) return;
            
            int type = isShowingMelodies ? LibraryDetailActivity.TYPE_MELODY : LibraryDetailActivity.TYPE_CHORD;
            
            Intent intent = new Intent(this, LibraryDetailActivity.class);
            intent.putExtra(LibraryDetailActivity.EXTRA_TYPE, type);
            intent.putExtra(LibraryDetailActivity.EXTRA_ID, itemId);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                currentSearchQuery = s.toString().trim().toLowerCase();
                filterAndDisplayData();
            }
        });
        
        btnGoCreate.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }
    
    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
        tvNoResults.setVisibility(View.GONE);
        lvLibrary.setVisibility(View.GONE);
        
        new Thread(() -> {
            allMelodies = new ArrayList<>(repository.getMelodyLibrary());
            allChords = new ArrayList<>(repository.getChordLibrary());
            
            sortByCreatedTimeDescending(allMelodies);
            sortByCreatedTimeDescending(allChords);
            
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                filterAndDisplayData();
            });
        }).start();
    }
    
    private void sortByCreatedTimeDescending(List<Object> items) {
        Collections.sort(items, (a, b) -> {
            long timeA = 0, timeB = 0;
            if (a instanceof MusicRepository.MelodyEntry && b instanceof MusicRepository.MelodyEntry) {
                timeA = ((MusicRepository.MelodyEntry) a).createdAt;
                timeB = ((MusicRepository.MelodyEntry) b).createdAt;
            } else if (a instanceof MusicRepository.ChordEntry && b instanceof MusicRepository.ChordEntry) {
                timeA = ((MusicRepository.ChordEntry) a).createdAt;
                timeB = ((MusicRepository.ChordEntry) b).createdAt;
            }
            return Long.compare(timeB, timeA);
        });
    }
    
    private void filterAndDisplayData() {
        List<Object> sourceList = isShowingMelodies ? allMelodies : allChords;
        List<Object> filteredList = new ArrayList<>();
        
        if (currentSearchQuery.isEmpty()) {
            filteredList.addAll(sourceList);
        } else {
            for (Object item : sourceList) {
                String name = "";
                if (item instanceof MusicRepository.MelodyEntry) {
                    name = ((MusicRepository.MelodyEntry) item).name.toLowerCase();
                } else if (item instanceof MusicRepository.ChordEntry) {
                    name = ((MusicRepository.ChordEntry) item).name.toLowerCase();
                }
                if (name.contains(currentSearchQuery)) {
                    filteredList.add(item);
                }
            }
        }
        
        if (sourceList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            tvNoResults.setVisibility(View.GONE);
            lvLibrary.setVisibility(View.GONE);
            
            if (isShowingMelodies) {
                tvEmptyTitle.setText("暂无旋律");
                tvEmptyMessage.setText("还没有生成过旋律\n快去创作吧");
            } else {
                tvEmptyTitle.setText("暂无和弦");
                tvEmptyMessage.setText("还没有生成过和弦\n快去创作吧");
            }
        } else if (filteredList.isEmpty()) {
            emptyStateLayout.setVisibility(View.GONE);
            tvNoResults.setVisibility(View.VISIBLE);
            lvLibrary.setVisibility(View.GONE);
            tvNoResults.setText("未找到 \"" + currentSearchQuery + "\" 相关结果");
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            tvNoResults.setVisibility(View.GONE);
            lvLibrary.setVisibility(View.VISIBLE);
            
            adapter = new LibraryAdapter(this, filteredList, isShowingMelodies);
            lvLibrary.setAdapter(adapter);
        }
    }
}
