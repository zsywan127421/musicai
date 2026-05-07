package com.example.musicai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicai.adapter.LibraryAdapter;

public class LibraryActivity extends AppCompatActivity {
    
    private RadioGroup rgType;
    private RadioButton rbMelodies, rbChords;
    private ListView lvLibrary;
    private TextView tvEmpty;
    private ProgressBar progressBar;
    
    private MusicRepository repository;
    private LibraryAdapter adapter;
    private boolean isShowingMelodies = true;
    
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
        tvEmpty = findViewById(R.id.tv_empty);
        progressBar = findViewById(R.id.progress_bar);
    }
    
    private void setupListeners() {
        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            isShowingMelodies = (checkedId == R.id.rb_melodies);
            loadData();
        });
        
        lvLibrary.setOnItemClickListener((parent, view, position, id) -> {
            if (adapter == null) return;
            
            String itemId = adapter.getItemId(position);
            if (itemId == null) return;
            
            int type = isShowingMelodies ? LibraryDetailActivity.TYPE_MELODY : LibraryDetailActivity.TYPE_CHORD;
            
            Intent intent = new Intent(this, LibraryDetailActivity.class);
            intent.putExtra(LibraryDetailActivity.EXTRA_TYPE, type);
            intent.putExtra(LibraryDetailActivity.EXTRA_ID, itemId);
            startActivity(intent);
        });
    }
    
    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        lvLibrary.setVisibility(View.GONE);
        
        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            runOnUiThread(() -> {
                if (isShowingMelodies) {
                    adapter = new LibraryAdapter(this, repository.getMelodyLibrary(), true);
                } else {
                    adapter = new LibraryAdapter(this, repository.getChordLibrary(), false);
                }
                
                lvLibrary.setAdapter(adapter);
                progressBar.setVisibility(View.GONE);
                
                if (adapter.getCount() == 0) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(isShowingMelodies ? 
                        "暂无旋律\n请在生成器中创建旋律" : "暂无和弦\n请在生成器中创建和弦");
                } else {
                    lvLibrary.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }
}
