package com.example.musicai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.example.musicai.util.ConfirmDialog;
import com.example.musicai.util.SelectItemAdapter;
import com.example.musicai.util.ToastHelper;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener;

import java.util.ArrayList;
import java.util.List;

public class LibraryTabsActivity extends BaseActivity {

    public static final String EXTRA_TYPE = "type";
    public static final int TYPE_ALL = 0;
    public static final int TYPE_MELODY = 1;
    public static final int TYPE_CHORD = 2;
    public static final int TYPE_SONG = 3;

    private TabLayout tabLayout;
    private RecyclerView rvMelodies, rvChords, rvSongs;
    private LinearLayout emptyState;
    private TextView tvEmpty;
    private ProgressBar progressBar;
    private ImageButton btnClose;

    private MusicRepository repository;
    private int currentTab = 0;

    private List<MusicRepository.MelodyEntry> melodies = new ArrayList<>();
    private List<MusicRepository.ChordEntry> chords = new ArrayList<>();
    private List<SongEntry> songs = new ArrayList<>();

    private LibraryItemAdapter melodyAdapter, chordAdapter, songAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_tabs);

        repository = MusicRepository.getInstance(this);

        initViews();
        setupTabs();
        loadData();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tab_layout);
        rvMelodies = findViewById(R.id.rv_melodies);
        rvChords = findViewById(R.id.rv_chords);
        rvSongs = findViewById(R.id.rv_songs);
        emptyState = findViewById(R.id.empty_state);
        tvEmpty = findViewById(R.id.tv_empty);
        progressBar = findViewById(R.id.progress_bar);
        btnClose = findViewById(R.id.btn_close);

        btnClose.setOnClickListener(v -> finish());

        setupRecyclerView(rvMelodies, new ArrayList<>(), TYPE_MELODY);
        setupRecyclerView(rvChords, new ArrayList<>(), TYPE_CHORD);
        setupRecyclerView(rvSongs, new ArrayList<>(), TYPE_SONG);
    }

    private void setupRecyclerView(RecyclerView recyclerView, List<?> items, int type) {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setNestedScrollingEnabled(false);

        if (type == TYPE_MELODY) {
            melodyAdapter = new LibraryItemAdapter((List<MusicRepository.MelodyEntry>) items, TYPE_MELODY);
            melodyAdapter.setOnItemActionListener(new LibraryItemAdapter.OnItemActionListener() {
                @Override
                public void onItemClick(int position) {
                    MusicRepository.MelodyEntry entry = melodies.get(position);
                    Intent intent = new Intent(LibraryTabsActivity.this, LibraryDetailActivity.class);
                    intent.putExtra(LibraryDetailActivity.EXTRA_TYPE, LibraryDetailActivity.TYPE_MELODY);
                    intent.putExtra(LibraryDetailActivity.EXTRA_ID, entry.id);
                    startActivity(intent);
                }

                @Override
                public void onItemDelete(int position) {
                    MusicRepository.MelodyEntry entry = melodies.get(position);
                    ConfirmDialog.showDelete(LibraryTabsActivity.this, entry.name, () -> {
                        repository.deleteMelody(entry.id);
                        loadData();
                        ToastHelper.showSuccess(LibraryTabsActivity.this, "已删除");
                    });
                }

                @Override
                public void onItemPlay(int position) {
                    ToastHelper.showInfo(LibraryTabsActivity.this, "点击播放功能开发中");
                }
            });
            recyclerView.setAdapter(melodyAdapter);
        } else if (type == TYPE_CHORD) {
            chordAdapter = new LibraryItemAdapter((List<MusicRepository.ChordEntry>) items, TYPE_CHORD);
            chordAdapter.setOnItemActionListener(new LibraryItemAdapter.OnItemActionListener() {
                @Override
                public void onItemClick(int position) {
                    MusicRepository.ChordEntry entry = chords.get(position);
                    Intent intent = new Intent(LibraryTabsActivity.this, LibraryDetailActivity.class);
                    intent.putExtra(LibraryDetailActivity.EXTRA_TYPE, LibraryDetailActivity.TYPE_CHORD);
                    intent.putExtra(LibraryDetailActivity.EXTRA_ID, entry.id);
                    startActivity(intent);
                }

                @Override
                public void onItemDelete(int position) {
                    MusicRepository.ChordEntry entry = chords.get(position);
                    ConfirmDialog.showDelete(LibraryTabsActivity.this, entry.name, () -> {
                        repository.deleteChord(entry.id);
                        loadData();
                        ToastHelper.showSuccess(LibraryTabsActivity.this, "已删除");
                    });
                }

                @Override
                public void onItemPlay(int position) {
                    ToastHelper.showInfo(LibraryTabsActivity.this, "点击播放功能开发中");
                }
            });
            recyclerView.setAdapter(chordAdapter);
        } else if (type == TYPE_SONG) {
            songAdapter = new LibraryItemAdapter((List<SongEntry>) items, TYPE_SONG);
            songAdapter.setOnItemActionListener(new LibraryItemAdapter.OnItemActionListener() {
                @Override
                public void onItemClick(int position) {
                    SongEntry entry = songs.get(position);
                    Intent intent = new Intent(LibraryTabsActivity.this, SongDetailActivity.class);
                    intent.putExtra(SongDetailActivity.EXTRA_SONG_ID, entry.id);
                    startActivity(intent);
                }

                @Override
                public void onItemDelete(int position) {
                    SongEntry entry = songs.get(position);
                    ConfirmDialog.showDelete(LibraryTabsActivity.this, entry.name, () -> {
                        repository.deleteSong(entry.id);
                        loadData();
                        ToastHelper.showSuccess(LibraryTabsActivity.this, "已删除");
                    });
                }

                @Override
                public void onItemPlay(int position) {
                    SongEntry entry = songs.get(position);
                    ToastHelper.showInfo(LibraryTabsActivity.this, "播放: " + entry.name);
                }
            });
            recyclerView.setAdapter(songAdapter);
        }
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("全部"));
        tabLayout.addTab(tabLayout.newTab().setText("旋律"));
        tabLayout.addTab(tabLayout.newTab().setText("和弦"));
        tabLayout.addTab(tabLayout.newTab().setText("歌曲"));

        tabLayout.addOnTabSelectedListener(new OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                updateVisibility();
                updateEmptyState();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            melodies = repository.getMelodyLibrary();
            chords = repository.getChordLibrary();
            songs = repository.getSongLibrary();

            runOnUiThread(() -> {
                updateAdapters();
                updateVisibility();
                updateEmptyState();
                progressBar.setVisibility(View.GONE);
            });
        }).start();
    }

    private void updateAdapters() {
        melodyAdapter.updateData(melodies);
        chordAdapter.updateData(chords);
        songAdapter.updateData(songs);
    }

    private void updateVisibility() {
        rvMelodies.setVisibility(View.GONE);
        rvChords.setVisibility(View.GONE);
        rvSongs.setVisibility(View.GONE);

        switch (currentTab) {
            case 0:
                rvMelodies.setVisibility(View.VISIBLE);
                break;
            case 1:
                rvMelodies.setVisibility(View.VISIBLE);
                break;
            case 2:
                rvChords.setVisibility(View.VISIBLE);
                break;
            case 3:
                rvSongs.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void updateEmptyState() {
        int itemCount = 0;
        String emptyMessage = "暂无内容";

        switch (currentTab) {
            case 0:
            case 1:
                itemCount = melodies.size();
                emptyMessage = "暂无旋律，去AI生成页面创建吧";
                break;
            case 2:
                itemCount = chords.size();
                emptyMessage = "暂无和弦，去AI生成页面创建吧";
                break;
            case 3:
                itemCount = songs.size();
                emptyMessage = "暂无歌曲，去AI生成页面创建吧";
                break;
        }

        emptyState.setVisibility(itemCount == 0 ? View.VISIBLE : View.GONE);
        tvEmpty.setText(emptyMessage);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }
}
