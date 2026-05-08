package com.example.musicai;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.musicai.util.ConfirmDialog;
import com.example.musicai.util.ExportBottomSheet;
import com.example.musicai.util.MetronomeBottomSheet;
import com.example.musicai.util.TimeUtils;
import com.example.musicai.util.ToastHelper;

import java.util.ArrayList;
import java.util.List;

public class SongEditorActivity extends BaseActivity implements MusicPlayerService.PlaybackListener {

    private static final String[] TIME_SIGNATURES = {"4/4", "3/4", "6/8", "2/4", "5/4", "7/8", "12/8"};

    private TextView tvTitle;
    private ImageButton btnBack;
    private Spinner spSongSelect;
    private Spinner spTimeSignature;
    private LinearLayout editorSection;
    private LinearLayout songSelectorSection;
    private TextView tvEmptyHint;
    private TextView tvStyle;
    private EditText etName;
    private TextView tvDuration;
    private TextView tvSourceMelody;
    private TextView tvSourceChord;
    private TextView tvBpm;
    private Button btnBpmDecrease;
    private Button btnBpmIncrease;
    private Button btnPianoRoll;
    private Button btnTrackManager;
    private Button btnMetronome;
    private CursorSeekBar playbackProgress;
    private TextView tvPlaybackTime;
    private Button btnPlay;
    private Button btnStop;
    private Button btnRegenerate;
    private Button btnExport;
    private LinearLayout bottomBar;
    private Button btnSave;
    private Button btnSaveAs;
    private View[] beatIndicators;

    private MusicRepository repository;
    private List<SongEntry> songs = new ArrayList<>();
    private SongEntry currentSong;
    private boolean hasChanges = false;

    private MusicPlayerService playerService;
    private boolean isBound = false;
    private boolean isPlaying = false;
    private boolean isMetronomeEnabled = false;

    private Handler autoSaveHandler;
    private Runnable autoSaveRunnable;
    private static final long AUTO_SAVE_INTERVAL = 30000;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.LocalBinder binder = (MusicPlayerService.LocalBinder) service;
            playerService = binder.getService();
            playerService.setPlaybackListener(SongEditorActivity.this);
            playerService.setMetronomeListener(new MusicPlayerService.MetronomeListener() {
                @Override
                public void onMetronomeBeat(int beat, boolean isDownbeat) {
                    runOnUiThread(() -> updateBeatIndicator(beat, isDownbeat));
                }
            });
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (playerService != null) {
                playerService.removePlaybackListener();
            }
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_editor);

        repository = MusicRepository.getInstance(this);

        initViews();
        setupTimeSignatureSpinner();
        setupListeners();
        loadSongs();
        startAutoSave();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        btnBack = findViewById(R.id.btn_back);
        spSongSelect = findViewById(R.id.sp_song_select);
        spTimeSignature = findViewById(R.id.sp_time_signature);
        editorSection = findViewById(R.id.editor_section);
        songSelectorSection = findViewById(R.id.song_selector_section);
        tvEmptyHint = findViewById(R.id.tv_empty_hint);
        tvStyle = findViewById(R.id.tv_style);
        etName = findViewById(R.id.et_name);
        tvDuration = findViewById(R.id.tv_duration);
        tvSourceMelody = findViewById(R.id.tv_source_melody);
        tvSourceChord = findViewById(R.id.tv_source_chord);
        tvBpm = findViewById(R.id.tv_bpm);
        btnBpmDecrease = findViewById(R.id.btn_bpm_decrease);
        btnBpmIncrease = findViewById(R.id.btn_bpm_increase);
        btnPianoRoll = findViewById(R.id.btn_piano_roll);
        btnTrackManager = findViewById(R.id.btn_track_manager);
        btnMetronome = findViewById(R.id.btn_metronome);
        playbackProgress = findViewById(R.id.playback_progress);
        tvPlaybackTime = findViewById(R.id.tv_playback_time);
        btnPlay = findViewById(R.id.btn_play);
        btnStop = findViewById(R.id.btn_stop);
        btnRegenerate = findViewById(R.id.btn_regenerate);
        btnExport = findViewById(R.id.btn_export);
        bottomBar = findViewById(R.id.bottom_bar);
        btnSave = findViewById(R.id.btn_save);
        btnSaveAs = findViewById(R.id.btn_save_as);

        beatIndicators = new View[]{
            findViewById(R.id.beat_1),
            findViewById(R.id.beat_2),
            findViewById(R.id.beat_3),
            findViewById(R.id.beat_4)
        };

        editorSection.setVisibility(View.GONE);
        bottomBar.setVisibility(View.GONE);
    }

    private void setupTimeSignatureSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            R.layout.spinner_item, TIME_SIGNATURES);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spTimeSignature.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        spSongSelect.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && position <= songs.size()) {
                    selectSong(position - 1);
                } else {
                    editorSection.setVisibility(View.GONE);
                    bottomBar.setVisibility(View.GONE);
                    tvEmptyHint.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spTimeSignature.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (currentSong != null) {
                    currentSong.keySignature = TIME_SIGNATURES[position];
                    hasChanges = true;
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnBpmDecrease.setOnClickListener(v -> adjustBpm(-5));
        btnBpmIncrease.setOnClickListener(v -> adjustBpm(5));

        btnPianoRoll.setOnClickListener(v -> openPianoRoll());
        btnTrackManager.setOnClickListener(v -> openTrackManager());
        btnMetronome.setOnClickListener(v -> toggleMetronome());

        btnPlay.setOnClickListener(v -> playSong());
        btnStop.setOnClickListener(v -> stopSong());

        playbackProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (isBound && playerService != null && isPlaying) {
                    int posMs = (int) (playerService.getDuration() * (long) seekBar.getProgress() / 100L);
                    playerService.seekTo(posMs);
                }
            }
        });

        btnRegenerate.setOnClickListener(v -> regenerateSong());
        btnExport.setOnClickListener(v -> exportSong());
        btnSave.setOnClickListener(v -> saveSong());
        btnSaveAs.setOnClickListener(v -> saveSongAs());
    }

    private void loadSongs() {
        songs = repository.getSongLibrary();
        List<String> songNames = new ArrayList<>();
        songNames.add("（选择歌曲）");

        for (SongEntry entry : songs) {
            songNames.add(entry.name);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            R.layout.spinner_item, songNames);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spSongSelect.setAdapter(adapter);
    }

    private void selectSong(int index) {
        if (index < 0 || index >= songs.size()) return;

        currentSong = songs.get(index);

        editorSection.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);
        tvEmptyHint.setVisibility(View.GONE);

        etName.setText(currentSong.name);
        tvStyle.setText(currentSong.style != null ? currentSong.style : "流行");
        tvDuration.setText(formatDuration(currentSong.totalDurationMs));
        tvSourceMelody.setText(currentSong.sourceMelodyName != null ? currentSong.sourceMelodyName : "未知");
        tvSourceChord.setText(currentSong.sourceChordName != null ? currentSong.sourceChordName : "未知");

        tvBpm.setText(String.valueOf(currentSong.bpm));
        int tsIndex = java.util.Arrays.asList(TIME_SIGNATURES).indexOf(currentSong.keySignature);
        if (tsIndex >= 0) {
            spTimeSignature.setSelection(tsIndex);
        }

        tvPlaybackTime.setText("0:00 / " + formatDuration(currentSong.totalDurationMs));
        playbackProgress.setProgress(0);

        btnPlay.setEnabled(true);
        btnStop.setEnabled(false);
        hasChanges = false;
    }

    private void adjustBpm(int delta) {
        if (currentSong == null) return;

        int newBpm = currentSong.bpm + delta;
        if (newBpm >= 20 && newBpm <= 300) {
            currentSong.bpm = newBpm;
            tvBpm.setText(String.valueOf(newBpm));
            hasChanges = true;

            if (isBound && playerService != null) {
                playerService.setMetronomeBpm(newBpm);
            }
        }
    }

    private void openPianoRoll() {
        if (currentSong == null) {
            ToastHelper.showError(this, "请先选择歌曲");
            return;
        }

        Intent intent = new Intent(this, PianoRollActivity.class);
        intent.putExtra(PianoRollActivity.EXTRA_MELODY_ID, currentSong.sourceMelodyId);
        startActivityForResult(intent, 100);
    }

    private void openTrackManager() {
        if (currentSong == null) {
            ToastHelper.showError(this, "请先选择歌曲");
            return;
        }

        Intent intent = new Intent(this, TrackManagerActivity.class);
        intent.putExtra(TrackManagerActivity.EXTRA_SONG_ID, currentSong.id);
        startActivity(intent);
    }

    private void toggleMetronome() {
        isMetronomeEnabled = !isMetronomeEnabled;

        if (isMetronomeEnabled) {
            btnMetronome.setText("关闭节拍器");
            btnMetronome.setBackgroundResource(R.drawable.apple_button_primary_bg);
        } else {
            btnMetronome.setText("开启节拍器");
            btnMetronome.setBackgroundResource(R.drawable.apple_button_bg);
            resetBeatIndicators();
        }

        if (isBound && playerService != null) {
            playerService.setMetronomeEnabled(isMetronomeEnabled);
        }
    }

    private void updateBeatIndicator(int beat, boolean isDownbeat) {
        resetBeatIndicators();

        if (beat >= 0 && beat < beatIndicators.length) {
            beatIndicators[beat].setBackgroundResource(
                isDownbeat ? R.drawable.beat_indicator_active : R.drawable.beat_indicator_secondary);
        }
    }

    private void resetBeatIndicators() {
        for (View indicator : beatIndicators) {
            if (indicator != null) {
                indicator.setBackgroundResource(R.drawable.beat_indicator_bg);
            }
        }
    }

    private void playSong() {
        if (currentSong == null) return;

        if (!isBound || playerService == null) {
            ToastHelper.showError(this, "播放器服务未连接");
            return;
        }

        if (isPlaying) {
            playerService.pause();
            isPlaying = false;
            btnPlay.setText("继续");
            btnStop.setEnabled(false);
            if (isMetronomeEnabled) {
                playerService.stopMetronome();
            }
        } else {
            MusicData.Song song = currentSong.toMusicDataSong();
            if (song != null && song.melody != null && !song.melody.notes.isEmpty()) {
                playerService.playSong(song);
                isPlaying = true;
                btnPlay.setText("暂停");
                btnStop.setEnabled(true);

                if (isMetronomeEnabled) {
                    playerService.setMetronomeBpm(currentSong.bpm);
                    playerService.setMetronomeBeatsPerMeasure(4);
                    playerService.startMetronome();
                }
            } else {
                ToastHelper.showError(this, "无法播放此歌曲");
            }
        }
    }

    private void stopSong() {
        if (isBound && playerService != null) {
            playerService.stopPlayback();
            playerService.stopMetronome();
        }
        isPlaying = false;
        btnPlay.setText("播放");
        playbackProgress.setProgress(0);
        tvPlaybackTime.setText("0:00 / " + formatDuration(currentSong != null ? currentSong.totalDurationMs : 0));
        btnStop.setEnabled(false);
        resetBeatIndicators();
    }

    private void regenerateSong() {
        if (currentSong == null) {
            ToastHelper.showError(this, "请先选择歌曲");
            return;
        }

        ConfirmDialog.show(this, "重新生成", "是否基于当前歌曲重新生成新版本？", () -> {
            ToastHelper.showInfo(this, "正在调用AI重新生成...");
        });
    }

    private void exportSong() {
        if (currentSong == null) {
            ToastHelper.showError(this, "请先选择歌曲");
            return;
        }

        ExportBottomSheet exportSheet = new ExportBottomSheet(this, currentSong);
        exportSheet.show();
    }

    private void saveSong() {
        if (currentSong == null) return;

        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            ToastHelper.showError(this, "名称不能为空");
            return;
        }

        if (!name.equals(currentSong.name) && repository.songNameExists(name)) {
            ConfirmDialog.showSave(this, name, () -> doSave(name));
        } else {
            doSave(name);
        }
    }

    private void saveSongAs() {
        if (currentSong == null) return;

        ConfirmDialog.show(this, "另存为", "是否将当前歌曲另存为新条目？", () -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                name = "新歌曲";
            }
            name = name + " (副本)";

            SongEntry newSong = new SongEntry(name, currentSong.style);
            newSong.bpm = currentSong.bpm;
            newSong.keySignature = currentSong.keySignature;
            newSong.segments = new ArrayList<>(currentSong.segments);
            newSong.sourceMelodyId = currentSong.sourceMelodyId;
            newSong.sourceMelodyName = currentSong.sourceMelodyName;
            newSong.sourceChordId = currentSong.sourceChordId;
            newSong.sourceChordName = currentSong.sourceChordName;
            newSong.totalDurationMs = currentSong.totalDurationMs;
            newSong.tracks = new ArrayList<>(currentSong.tracks);

            repository.addSong(newSong);
            ToastHelper.showSuccess(this, "已另存为: " + name);
            hasChanges = false;
            loadSongs();
        });
    }

    private void doSave(String name) {
        currentSong.name = name;
        currentSong.updatedAt = System.currentTimeMillis();
        repository.updateSong(currentSong.id, name);
        ToastHelper.showSuccess(this, "保存成功");
        hasChanges = false;
        loadSongs();
    }

    private void startAutoSave() {
        autoSaveHandler = new Handler(Looper.getMainLooper());
        autoSaveRunnable = new Runnable() {
            @Override
            public void run() {
                if (hasChanges && currentSong != null) {
                    doSave(currentSong.name);
                }
                autoSaveHandler.postDelayed(this, AUTO_SAVE_INTERVAL);
            }
        };
        autoSaveHandler.postDelayed(autoSaveRunnable, AUTO_SAVE_INTERVAL);
    }

    private String formatDuration(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            hasChanges = true;
        }
    }

    @Override
    public void onBackPressed() {
        if (hasChanges) {
            ConfirmDialog.show(this, "有未保存的更改", "是否放弃更改？", () -> {
                super.onBackPressed();
            });
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onPlaybackProgress(int positionMs, int totalMs, int currentNoteIndex) {
        runOnUiThread(() -> {
            if (totalMs > 0) {
                int progress = (positionMs * 100) / totalMs;
                playbackProgress.setProgress(progress);
                tvPlaybackTime.setText(formatTime(positionMs) + " / " + formatTime(totalMs));
            }
        });
    }

    @Override
    public void onPlaybackStateChanged(boolean playing) {
        isPlaying = playing;
        runOnUiThread(() -> {
            btnPlay.setText(playing ? "暂停" : "继续");
            btnStop.setEnabled(playing);
        });
    }

    @Override
    public void onPlaybackCompleted() {
        runOnUiThread(() -> {
            isPlaying = false;
            btnPlay.setText("播放");
            playbackProgress.setProgress(0);
            tvPlaybackTime.setText("0:00 / " + formatDuration(currentSong != null ? currentSong.totalDurationMs : 0));
            btnStop.setEnabled(false);
            if (isBound && playerService != null) {
                playerService.stopPlayback();
                playerService.stopMetronome();
            }
            resetBeatIndicators();
        });
    }

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format("%d:%02d", minutes, seconds);
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
        stopSong();
        if (isBound && playerService != null) {
            playerService.setPlaybackListener(null);
        }
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoSaveHandler != null && autoSaveRunnable != null) {
            autoSaveHandler.removeCallbacks(autoSaveRunnable);
        }
    }
}
