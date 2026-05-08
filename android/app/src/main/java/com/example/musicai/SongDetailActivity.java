package com.example.musicai;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.musicai.util.ConfirmDialog;
import com.example.musicai.util.TimeUtils;
import com.example.musicai.util.ToastHelper;
import com.example.musicai.util.ToolbarHelper;
import com.example.musicai.view.UnifiedPlaybackButton;
import com.example.musicai.MusicPlayerService.PlaybackListener;

import java.util.Arrays;
import java.util.List;

public class SongDetailActivity extends BaseActivity implements PlaybackListener {

    public static final String EXTRA_SONG_ID = "song_id";

    private static final int MENU_EDIT = 1;
    private static final int MENU_RENAME = 2;
    private static final int MENU_DELETE = 3;
    private static final int MENU_EXPORT_MIDI = 4;
    private static final int MENU_EXPORT_WAV = 5;

    private TextView tvTitle, tvStyle, tvCreated, tvSource, tvSegments;
    private EditText etName;
    private UnifiedPlaybackButton btnPlay;
    private Button btnDelete, btnSave;
    private TextView tvSpeed, tvPlaybackTime;
    private CursorSeekBar playbackProgress;
    private ProgressBar progressBar;
    private SeekBar seekBarSpeed;
    private View playbackSection, speedSection;

    private MusicRepository repository;
    private SongEntry songEntry;
    private MusicPlayerService playerService;
    private ToolbarHelper toolbarHelper;
    private boolean isBound = false;
    private boolean isPlaying = false;
    private float currentSpeed = 1.0f;

    private Handler handler = new Handler(Looper.getMainLooper());

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(android.content.ComponentName name, IBinder service) {
            MusicPlayerService.LocalBinder binder = (MusicPlayerService.LocalBinder) service;
            playerService = binder.getService();
            playerService.setPlaybackListener(SongDetailActivity.this);
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(android.content.ComponentName name) {
            if (playerService != null) {
                playerService.removePlaybackListener();
            }
            isBound = false;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_detail);

        String songId = getIntent().getStringExtra(EXTRA_SONG_ID);
        if (songId == null || songId.isEmpty()) {
            ToastHelper.showError(this, "无效的歌曲ID");
            finish();
            return;
        }

        repository = MusicRepository.getInstance(this);

        initToolbar();
        initViews();
        setupToolbarMenu();
        loadSongData(songId);
        updatePlaybackTimeDisplay();
    }

    private void initToolbar() {
        initToolbar(R.id.toolbar, "歌曲详情");
        setBackVisible(true);
        setMenuVisible(true);
    }

    private void setupToolbarMenu() {
        toolbarHelper = getToolbarHelper();
        if (toolbarHelper == null) return;

        List<ToolbarHelper.MenuItemData> menuItems = Arrays.asList(
            new ToolbarHelper.MenuItemData(MENU_EDIT, "编辑"),
            new ToolbarHelper.MenuItemData(MENU_RENAME, "改名"),
            new ToolbarHelper.MenuItemData(MENU_DELETE, "删除", 0, true),
            new ToolbarHelper.MenuItemData(MENU_EXPORT_MIDI, "导出MIDI"),
            new ToolbarHelper.MenuItemData(MENU_EXPORT_WAV, "导出WAV")
        );

        toolbarHelper.setMenuItems(menuItems, this::onMenuItemClick);
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        tvStyle = findViewById(R.id.tv_style);
        tvCreated = findViewById(R.id.tv_created);
        tvSource = findViewById(R.id.tv_source);
        tvSegments = findViewById(R.id.tv_segments);
        etName = findViewById(R.id.et_name);
        btnPlay = findViewById(R.id.btn_play);
        btnDelete = findViewById(R.id.btn_delete);
        btnSave = findViewById(R.id.btn_save);
        tvSpeed = findViewById(R.id.tv_speed);
        tvPlaybackTime = findViewById(R.id.tv_playback_time);
        playbackProgress = findViewById(R.id.playback_progress);
        progressBar = findViewById(R.id.progress_bar);
        seekBarSpeed = findViewById(R.id.seekbar_speed);
        playbackSection = findViewById(R.id.playback_section);
        speedSection = findViewById(R.id.speed_section);

        btnPlay.setOnPlaybackStateChangeListener(new UnifiedPlaybackButton.OnPlaybackStateChangeListener() {
            @Override
            public void onPlayClicked() {
                startPlayback();
            }

            @Override
            public void onPauseClicked() {
                pausePlayback();
            }

            @Override
            public void onResumeClicked() {
                resumePlayback();
            }
        });

        btnSave.setOnClickListener(v -> saveChanges());
        btnDelete.setOnClickListener(v -> confirmDelete());

        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentSpeed = 0.5f + (progress / 50f) * 2.5f;
                tvSpeed.setText(String.format("速度: %.1fx", currentSpeed));
                if (isBound && playerService != null) {
                    playerService.setSpeed(currentSpeed);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        playbackProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (isBound && playerService != null) {
                    int durationMs = songEntry != null ? songEntry.totalDurationMs : 0;
                    int seekPosMs = (int) ((long) durationMs * progress / 100L);
                    playerService.seekTo(seekPosMs);
                }
            }
        });
    }

    private void loadSongData(String songId) {
        songEntry = repository.getSongById(songId);
        if (songEntry != null) {
            etName.setText(songEntry.name);
            tvTitle.setText(songEntry.name);
            tvStyle.setText(songEntry.style != null ? songEntry.style : "流行");
            tvCreated.setText(TimeUtils.formatRelativeTime(songEntry.createdAt));

            String melodyName = "未知";
            String chordName = "未知";
            if (songEntry.sourceMelodyId != null && !songEntry.sourceMelodyId.isEmpty()) {
                MusicRepository.MelodyEntry melodyEntry = repository.getMelodyById(songEntry.sourceMelodyId);
                if (melodyEntry != null) {
                    melodyName = melodyEntry.name;
                }
            }
            if (songEntry.sourceChordId != null && !songEntry.sourceChordId.isEmpty()) {
                MusicRepository.ChordEntry chordEntry = repository.getChordById(songEntry.sourceChordId);
                if (chordEntry != null) {
                    chordName = chordEntry.name;
                }
            }
            String sourceText = "来源旋律: " + melodyName + "\n来源和弦: " + chordName;
            tvSource.setText(sourceText);

            String segmentsText = "段落数: " + songEntry.segments.size() + " | 时长: " + formatDuration(songEntry.totalDurationMs);
            tvSegments.setText(segmentsText);
        } else {
            ToastHelper.showError(this, "歌曲加载失败");
            finish();
        }
    }

    private void updatePlaybackTimeDisplay() {
        if (songEntry != null) {
            tvPlaybackTime.setText("0:00 / " + formatDuration(songEntry.totalDurationMs));
            playbackProgress.setProgress(0);
        }
    }

    private void startPlayback() {
        if (songEntry == null) {
            ToastHelper.showError(this, "无歌曲可播放");
            return;
        }

        if (!isBound || playerService == null) {
            ToastHelper.showError(this, "播放器服务未就绪");
            return;
        }

        MusicData.Song song = songEntry.toMusicDataSong();
        if (song == null || song.melody == null || song.melody.notes.isEmpty()) {
            ToastHelper.showError(this, "无法播放此歌曲");
            return;
        }

        playerService.setSpeed(currentSpeed);
        playerService.playSong(song);
        isPlaying = true;
        btnPlay.setState(UnifiedPlaybackButton.State.PAUSE);
    }

    private void pausePlayback() {
        if (isBound && playerService != null) {
            playerService.pause();
            isPlaying = false;
            btnPlay.setState(UnifiedPlaybackButton.State.RESUME);
        }
    }

    private void resumePlayback() {
        if (isBound && playerService != null) {
            playerService.resume();
            isPlaying = true;
            btnPlay.setState(UnifiedPlaybackButton.State.PAUSE);
        }
    }

    private void stopPlayback() {
        if (isBound && playerService != null) {
            playerService.stopPlayback();
        }
        isPlaying = false;
        btnPlay.setState(UnifiedPlaybackButton.State.PLAY);
        if (songEntry != null) {
            tvPlaybackTime.setText("0:00 / " + formatDuration(songEntry.totalDurationMs));
            playbackProgress.setProgress(0);
        }
    }

    private void saveChanges() {
        if (songEntry == null) return;

        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            ToastHelper.showError(this, "名称不能为空");
            return;
        }

        songEntry.name = name;
        songEntry.updatedAt = System.currentTimeMillis();
        repository.updateSong(songEntry.id, name);
        ToastHelper.showSuccess(this, "保存成功");
    }

    private void confirmDelete() {
        if (songEntry == null) return;

        new android.app.AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除歌曲 \"" + songEntry.name + "\" 吗？此操作不可撤销。")
            .setPositiveButton("删除", (dialog, which) -> {
                repository.deleteSong(songEntry.id);
                ToastHelper.showSuccess(this, "已删除");
                finish();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void onMenuItemClick(int itemId) {
        switch (itemId) {
            case MENU_EDIT:
                if (songEntry != null) {
                    Intent intent = new Intent(this, SongEditorActivity.class);
                    intent.putExtra("song_id", songEntry.id);
                    startActivity(intent);
                }
                break;
            case MENU_RENAME:
                etName.requestFocus();
                etName.setSelection(etName.getText().length());
                ToastHelper.showInfo(this, "请修改名称后点击保存");
                break;
            case MENU_DELETE:
                confirmDelete();
                break;
            case MENU_EXPORT_MIDI:
                ToastHelper.showInfo(this, "导出MIDI功能开发中");
                break;
            case MENU_EXPORT_WAV:
                ToastHelper.showInfo(this, "导出WAV功能开发中");
                break;
        }
    }

    private String formatDuration(int millis) {
        if (millis <= 0) return "0:00";
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    public void onPlaybackProgress(int positionMs, int totalMs, int currentNoteIndex) {
        runOnUiThread(() -> {
            if (totalMs > 0) {
                int progress = (int) ((long) positionMs * 100 / totalMs);
                playbackProgress.setProgress(progress);
                int adjustedTotal = (int) (totalMs / currentSpeed);
                tvPlaybackTime.setText(formatTime(positionMs) + " / " + formatTime(adjustedTotal));
            }
        });
    }

    @Override
    public void onPlaybackStateChanged(boolean playing) {
        isPlaying = playing;
        runOnUiThread(() -> {
            if (playing) {
                btnPlay.setState(UnifiedPlaybackButton.State.PAUSE);
            } else {
                btnPlay.setState(UnifiedPlaybackButton.State.RESUME);
            }
        });
    }

    @Override
    public void onPlaybackCompleted() {
        runOnUiThread(() -> {
            isPlaying = false;
            btnPlay.setState(UnifiedPlaybackButton.State.PLAY);
            playbackProgress.setProgress(0);
            if (songEntry != null) {
                tvPlaybackTime.setText("0:00 / " + formatDuration(songEntry.totalDurationMs));
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
            if (playerService != null) {
                playerService.stopPlayback();
                playerService.setPlaybackListener(null);
            }
            unbindService(connection);
            isBound = false;
        }
        isPlaying = false;
    }
}
