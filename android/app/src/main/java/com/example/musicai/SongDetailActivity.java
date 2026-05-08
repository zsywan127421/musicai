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

import com.example.musicai.util.ConfirmDialog;
import com.example.musicai.util.TimeUtils;
import com.example.musicai.util.ToastHelper;
import com.example.musicai.util.ToolbarHelper;
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
    private Button btnPlay, btnStop, btnDelete, btnSave;
    private TextView tvSpeed, tvPlaybackTime;
    private CursorSeekBar playbackProgress;
    private ProgressBar progressBar;
    private SeekBar seekBarSpeed;
    private View playbackSection, speedSection;

    private MusicRepository repository;
    private SongEntry songEntry;
    private MusicPlayerService playerService;
    private boolean isBound = false;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable progressUpdater;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private float playbackSpeed = 1.0f;
    private String songId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_detail);

        initToolbar(R.id.toolbar, "歌曲详情");
        setBackVisible(true);
        setMenuVisible(true);
        setupToolbarMenu();

        repository = MusicRepository.getInstance(this);

        Intent serviceIntent = new Intent(this, MusicPlayerService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

        songId = getIntent().getStringExtra(EXTRA_SONG_ID);

        initViews();
        loadData();
        setupListeners();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        tvStyle = findViewById(R.id.tv_style);
        tvCreated = findViewById(R.id.tv_created);
        tvSource = findViewById(R.id.tv_source);
        tvSegments = findViewById(R.id.tv_segments);
        etName = findViewById(R.id.et_name);
        btnPlay = findViewById(R.id.btn_play);
        btnStop = findViewById(R.id.btn_stop);
        btnDelete = findViewById(R.id.btn_delete);
        btnSave = findViewById(R.id.btn_save);
        tvSpeed = findViewById(R.id.tv_speed);
        tvPlaybackTime = findViewById(R.id.tv_playback_time);
        playbackProgress = findViewById(R.id.playback_progress);
        progressBar = findViewById(R.id.progress_bar);
        playbackSection = findViewById(R.id.playback_section);
        speedSection = findViewById(R.id.speed_section);
        seekBarSpeed = findViewById(R.id.seekbar_speed);

        playbackSection.setVisibility(View.VISIBLE);
        speedSection.setVisibility(View.VISIBLE);
        btnPlay.setVisibility(View.VISIBLE);
        btnStop.setVisibility(View.VISIBLE);

        seekBarSpeed.setMax(50);
        seekBarSpeed.setProgress(10);
        tvSpeed.setText("速度: 1.0x");
    }

    private void loadData() {
        songEntry = repository.getSongById(songId);
        if (songEntry != null) {
            etName.setText(songEntry.name);
            tvTitle.setText(songEntry.name);
            tvStyle.setText("风格: " + songEntry.style);
            tvCreated.setText("创建: " + TimeUtils.formatRelativeTime(songEntry.createdAt));

            String sourceText = "来源旋律: " + (songEntry.sourceMelodyName != null ? songEntry.sourceMelodyName : "未知") + "\n" +
                    "来源和弦: " + (songEntry.sourceChordName != null ? songEntry.sourceChordName : "未知");
            tvSource.setText(sourceText);

            String segmentsText = "段落数: " + songEntry.segments.size() + " | " +
                    "时长: " + formatDuration(songEntry.totalDurationMs);
            tvSegments.setText(segmentsText);
            
            tvPlaybackTime.setText("0:00 / " + formatDuration(songEntry.totalDurationMs));
            playbackProgress.setProgress(0);
        } else {
            ToastHelper.showError(this, "歌曲加载失败");
            finish();
        }
    }

    private void setupListeners() {
        btnPlay.setOnClickListener(v -> play());
        btnStop.setOnClickListener(v -> stop());
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnSave.setOnClickListener(v -> save());

        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float speed = 0.25f + (progress / 50.0f * 3.75f);
                speed = Math.round(speed * 100) / 100.0f;
                speed = Math.max(0.25f, Math.min(4.0f, speed));
                tvSpeed.setText(String.format("速度: %.2fx", speed));
                if (fromUser) {
                    setSpeed(speed);
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
                if (isBound && playerService != null && isPlaying) {
                    int currentProgress = playbackProgress.getProgress();
                    int posMs = (int) (playerService.getDuration() * (long) currentProgress / 100L);
                    playerService.seekTo(posMs);
                }
            }
        });
    }

    private void setSpeed(float speed) {
        playbackSpeed = speed;
        if (isBound && playerService != null) {
            playerService.setSpeed(speed);
        }
    }

    private void play() {
        if (songEntry == null || songEntry.segments.isEmpty()) {
            ToastHelper.showError(this, "暂无可播放内容");
            return;
        }

        if (!isBound || playerService == null) {
            ToastHelper.showError(this, "播放器服务未连接");
            return;
        }

        if (isPlaying) {
            playerService.pause();
            isPaused = true;
            isPlaying = false;
            btnPlay.setText("继续");
            stopProgressUpdater();
        } else {
            MusicData.Song song = songEntry.toMusicDataSong();
            if (song != null && song.melody != null && !song.melody.notes.isEmpty()) {
                playerService.playMelody(song.melody);
            } else {
                ToastHelper.showError(this, "无法播放此内容");
                return;
            }
            playerService.setSpeed(playbackSpeed);
            isPlaying = true;
            isPaused = false;
            btnPlay.setText("暂停");
            startProgressUpdater();
        }
    }

    private void stop() {
        if (isBound && playerService != null) {
            playerService.stopPlayback();
        }
        isPlaying = false;
        isPaused = false;
        btnPlay.setText("播放");
        playbackProgress.setProgress(0);
        tvPlaybackTime.setText("0:00 / 0:00");
        stopProgressUpdater();
    }

    private void confirmDelete() {
        if (songEntry != null) {
            ConfirmDialog.showDelete(this, songEntry.name, () -> delete());
        }
    }

    private void delete() {
        if (songEntry != null) {
            repository.deleteSong(songEntry.id);
            ToastHelper.showSuccess(this, "删除成功");
            finish();
        }
    }

    private void save() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            ToastHelper.showError(this, "名称不能为空");
            return;
        }

        if (songEntry != null) {
            if (!name.equals(songEntry.name) && repository.songNameExists(name)) {
                ConfirmDialog.showSave(this, name, () -> doSave(name));
            } else {
                doSave(name);
            }
        }
    }

    private void doSave(String name) {
        if (songEntry != null) {
            repository.updateSong(songEntry.id, name);
            tvTitle.setText(name);
            ToastHelper.showSuccess(this, "保存成功");
        }
    }

    private void setupToolbarMenu() {
        List<ToolbarHelper.MenuItemData> menuItems = Arrays.asList(
            new ToolbarHelper.MenuItemData(MENU_EDIT, "编辑"),
            new ToolbarHelper.MenuItemData(MENU_RENAME, "改名"),
            new ToolbarHelper.MenuItemData(MENU_DELETE, "删除", 0, true),
            new ToolbarHelper.MenuItemData(MENU_EXPORT_MIDI, "导出MIDI"),
            new ToolbarHelper.MenuItemData(MENU_EXPORT_WAV, "导出WAV")
        );

        toolbarHelper.setMenuItems(menuItems, itemId -> {
            switch (itemId) {
                case MENU_EDIT:
                    ToastHelper.showInfo(SongDetailActivity.this, "打开编辑器...");
                    break;
                case MENU_RENAME:
                    ToastHelper.showInfo(SongDetailActivity.this, "改名功能开发中");
                    break;
                case MENU_DELETE:
                    confirmDelete();
                    break;
                case MENU_EXPORT_MIDI:
                    ToastHelper.showInfo(SongDetailActivity.this, "导出MIDI功能开发中");
                    break;
                case MENU_EXPORT_WAV:
                    ToastHelper.showInfo(SongDetailActivity.this, "导出WAV功能开发中");
                    break;
            }
        });
    }

    private void startProgressUpdater() {
        stopProgressUpdater();
        progressUpdater = new Runnable() {
            @Override
            public void run() {
                if (isPlaying && isBound && playerService != null) {
                    int current = playerService.getCurrentPosition();
                    int total = playerService.getDuration();
                    if (total > 0) {
                        int progress = (int) ((current / (float) total) * 100);
                        playbackProgress.setProgress(progress);
                        tvPlaybackTime.setText(formatTime(current) + " / " + formatTime(total));
                    }
                    handler.postDelayed(this, 100);
                }
            }
        };
        handler.post(progressUpdater);
    }

    private void stopProgressUpdater() {
        if (progressUpdater != null) {
            handler.removeCallbacks(progressUpdater);
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.LocalBinder binder = (MusicPlayerService.LocalBinder) service;
            playerService = binder.getService();
            isBound = true;
            playerService.setPlaybackListener(SongDetailActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (playerService != null) {
                playerService.removePlaybackListener();
            }
            playerService = null;
            isBound = false;
        }
    };

    @Override
    public void onPlaybackProgress(int positionMs, int totalMs, int currentNoteIndex) {}

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        this.isPlaying = isPlaying;
        runOnUiThread(() -> {
            if (isPlaying) {
                btnPlay.setText("暂停");
            } else {
                btnPlay.setText("继续");
            }
        });
    }

    @Override
    public void onPlaybackCompleted() {
        runOnUiThread(() -> {
            isPlaying = false;
            isPaused = false;
            btnPlay.setText("播放");
            playbackProgress.setProgress(0);
            tvPlaybackTime.setText("0:00 / " + formatTime(songEntry != null ? songEntry.totalDurationMs : 0));
            stopProgressUpdater();
            if (isBound && playerService != null) {
                playerService.stopPlayback();
            }
        });
    }

    private String formatTime(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private String formatDuration(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stop();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}
