package com.example.musicai;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.example.musicai.util.ConfirmDialog;
import com.example.musicai.util.SelectItemAdapter;
import com.example.musicai.util.ToastHelper;
import com.example.musicai.util.ToolbarHelper;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LibraryTabsActivity extends BaseActivity {

    private static final int MENU_SORT = 1;
    private static final int MENU_FILTER = 2;
    private static final int MENU_IMPORT = 3;

    public static final String EXTRA_TYPE = "type";
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
    private int currentSortOrder = 0;

    private List<MusicRepository.MelodyEntry> melodies = new ArrayList<>();
    private List<MusicRepository.ChordEntry> chords = new ArrayList<>();
    private List<SongEntry> songs = new ArrayList<>();

    private LibraryItemAdapter melodyAdapter, chordAdapter, songAdapter;

    private ActivityResultLauncher<String[]> importFileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_tabs);

        initToolbar(R.id.toolbar, "资源库");
        setBackVisible(true);
        setMenuVisible(true);
        toolbarHelper.setMenuItems(createMenuItems(), this::onMenuItemClick);

        repository = MusicRepository.getInstance(this);

        importFileLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    handleImportFile(uri);
                }
            }
        );

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
            
            applySorting();

            runOnUiThread(() -> {
                updateAdapters();
                updateVisibility();
                updateEmptyState();
                progressBar.setVisibility(View.GONE);
            });
        }).start();
    }
    
    private void applySorting() {
        switch (currentSortOrder) {
            case 0:
                Collections.sort(melodies, (a, b) -> Long.compare(b.createdAt, a.createdAt));
                Collections.sort(chords, (a, b) -> Long.compare(b.createdAt, a.createdAt));
                Collections.sort(songs, (a, b) -> Long.compare(b.createdAt, a.createdAt));
                break;
            case 1:
                Collections.sort(melodies, (a, b) -> a.name.compareToIgnoreCase(b.name));
                Collections.sort(chords, (a, b) -> a.name.compareToIgnoreCase(b.name));
                Collections.sort(songs, (a, b) -> a.name.compareToIgnoreCase(b.name));
                break;
            case 2:
                Collections.sort(melodies, (a, b) -> a.style.compareToIgnoreCase(b.style));
                Collections.sort(chords, (a, b) -> a.style.compareToIgnoreCase(b.style));
                Collections.sort(songs, (a, b) -> (a.style != null ? a.style : "").compareToIgnoreCase(b.style != null ? b.style : ""));
                break;
        }
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
                rvChords.setVisibility(View.VISIBLE);
                break;
            case 2:
                rvSongs.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void updateEmptyState() {
        int itemCount = 0;
        String emptyMessage = "暂无内容";

        switch (currentTab) {
            case 0:
                itemCount = melodies.size();
                emptyMessage = "暂无旋律，去AI生成页面创建吧";
                break;
            case 1:
                itemCount = chords.size();
                emptyMessage = "暂无和弦，去AI生成页面创建吧";
                break;
            case 2:
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

    private List<ToolbarHelper.MenuItemData> createMenuItems() {
        List<ToolbarHelper.MenuItemData> items = new ArrayList<>();
        items.add(new ToolbarHelper.MenuItemData(MENU_SORT, "排序方式"));
        items.add(new ToolbarHelper.MenuItemData(MENU_FILTER, "筛选"));
        items.add(new ToolbarHelper.MenuItemData(MENU_IMPORT, "导入"));
        return items;
    }

    private void onMenuItemClick(int itemId) {
        switch (itemId) {
            case MENU_SORT:
                showSortDialog();
                break;
            case MENU_FILTER:
                showFilterDialog();
                break;
            case MENU_IMPORT:
                importFileLauncher.launch(new String[]{"*/*"});
                break;
        }
    }
    
    private void showSortDialog() {
        String[] sortOptions = {"按创建时间（最新）", "按名称（A-Z）", "按风格"};
        
        new android.app.AlertDialog.Builder(this)
            .setTitle("排序方式")
            .setSingleChoiceItems(sortOptions, currentSortOrder, (dialog, which) -> {
                currentSortOrder = which;
                applySorting();
                updateAdapters();
                dialog.dismiss();
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void showFilterDialog() {
        List<String> allStyles = new ArrayList<>();
        allStyles.add("全部");
        
        for (MusicRepository.MelodyEntry m : melodies) {
            if (!allStyles.contains(m.style)) {
                allStyles.add(m.style);
            }
        }
        for (MusicRepository.ChordEntry c : chords) {
            if (!allStyles.contains(c.style)) {
                allStyles.add(c.style);
            }
        }
        
        if (allStyles.size() <= 1) {
            ToastHelper.showInfo(this, "暂无筛选条件");
            return;
        }
        
        String[] filterOptions = allStyles.toArray(new String[0]);
        
        new android.app.AlertDialog.Builder(this)
            .setTitle("按风格筛选")
            .setItems(filterOptions, (dialog, which) -> {
                String selectedStyle = filterOptions[which];
                ToastHelper.showInfo(this, "已筛选: " + selectedStyle);
                dialog.dismiss();
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void handleImportFile(Uri uri) {
        try {
            String mimeType = getContentResolver().getType(uri);
            String fileName = "导入文件";
            
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }
            
            ToastHelper.showSuccess(this, "已选择导入文件: " + fileName);
            
        } catch (Exception e) {
            ToastHelper.showError(this, "导入失败: " + e.getMessage());
        }
    }
}
