package com.example.musicai;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.musicai.util.ConfirmDialog;
import com.example.musicai.util.TimeUtils;
import com.example.musicai.util.ToastHelper;

import java.util.ArrayList;
import java.util.List;

public class SongEditorActivity extends BaseActivity implements MusicPlayerService.PlaybackListener {

    private static final float[] SPEEDS = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
    private static final String[] SPEED_LABELS = {"0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x"};

    private TextView tvTitle;
    private Spinner spSongSelect;
    private LinearLayout editorSection;
    private TextView tvSongInfo;
    private EditText etName;
    private TextView tvDuration;
    private TextView tvSource;
    
    private CursorSeekBar playbackProgress;
    private TextView tvPlaybackTime;
    private Button btnPlay;
    private Button btnStop;
    private LinearLayout speedControlLayout;
    private TextView tvSpeed;
    private Button[] speedButtons = new Button[6];
    private int selectedSpeedIndex = 2;
    
    private LinearLayout detailBar;
    private TextView tvDetailTitle;
    private TextView tvDetailInfo;
    private Button btnSave;
    private Button btnDelete;
    private ImageButton btnExpandDetail;
    private LinearLayout detailExpanded;
    private TextView tvDetailExpanded;
    
    private MusicRepository repository;
    private List<SongEntry> songs = new ArrayList<>();
    private SongEntry currentSong;
    private boolean isEditing = false;
    
    private MusicPlayerService playerService;
    private boolean isBound = false;
    private boolean isPlaying = false;
    private float playbackSpeed = 1.0f;
    
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.LocalBinder binder = (MusicPlayerService.LocalBinder) service;
            playerService = binder.getService();
            playerService.setPlaybackListener(SongEditorActivity.this);
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
        setupSpeedControls();
        setupListeners();
        loadSongs();
    }
    
    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        spSongSelect = findViewById(R.id.sp_song_select);
        editorSection = findViewById(R.id.editor_section);
        tvSongInfo = findViewById(R.id.tv_song_info);
        etName = findViewById(R.id.et_name);
        tvDuration = findViewById(R.id.tv_duration);
        tvSource = findViewById(R.id.tv_source);
        
        playbackProgress = findViewById(R.id.playback_progress);
        tvPlaybackTime = findViewById(R.id.tv_playback_time);
        btnPlay = findViewById(R.id.btn_play);
        btnStop = findViewById(R.id.btn_stop);
        speedControlLayout = findViewById(R.id.speed_control_layout);
        tvSpeed = findViewById(R.id.tv_speed);
        
        speedButtons[0] = findViewById(R.id.btn_speed_05);
        speedButtons[1] = findViewById(R.id.btn_speed_075);
        speedButtons[2] = findViewById(R.id.btn_speed_1);
        speedButtons[3] = findViewById(R.id.btn_speed_125);
        speedButtons[4] = findViewById(R.id.btn_speed_15);
        speedButtons[5] = findViewById(R.id.btn_speed_2);
        
        detailBar = findViewById(R.id.detail_bar);
        tvDetailTitle = findViewById(R.id.tv_detail_title);
        tvDetailInfo = findViewById(R.id.tv_detail_info);
        btnSave = findViewById(R.id.btn_detail_save);
        btnDelete = findViewById(R.id.btn_detail_delete);
        btnExpandDetail = findViewById(R.id.btn_expand_detail);
        detailExpanded = findViewById(R.id.detail_expanded);
        tvDetailExpanded = findViewById(R.id.tv_detail_expanded);
        
        detailBar.setVisibility(View.GONE);
        editorSection.setVisibility(View.GONE);
    }
    
    private void setupSpeedControls() {
        for (int i = 0; i < speedButtons.length; i++) {
            final int index = i;
            speedButtons[i].setOnClickListener(v -> selectSpeed(index));
        }
        updateSpeedButtons();
    }
    
    private void selectSpeed(int index) {
        selectedSpeedIndex = index;
        playbackSpeed = SPEEDS[index];
        if (playerService != null) {
            playerService.setSpeed(playbackSpeed);
        }
        updateSpeedButtons();
        tvSpeed.setText("速度: " + SPEED_LABELS[index]);
    }
    
    private void updateSpeedButtons() {
        for (int i = 0; i < speedButtons.length; i++) {
            if (i == selectedSpeedIndex) {
                speedButtons[i].setBackgroundResource(R.drawable.apple_button_primary_bg);
            } else {
                speedButtons[i].setBackgroundResource(R.drawable.apple_button_bg);
            }
        }
    }
    
    private void setupListeners() {
        spSongSelect.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && position <= songs.size()) {
                    selectSong(position - 1);
                } else {
                    editorSection.setVisibility(View.GONE);
                    detailBar.setVisibility(View.GONE);
                }
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
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
                    int posMs = (int) (playerService.getDuration() * (long) progress / 100L);
                    playerService.seekTo(posMs);
                }
            }
        });
        
        btnSave.setOnClickListener(v -> saveSong());
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnExpandDetail.setOnClickListener(v -> toggleDetailExpanded());
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
        isEditing = true;
        
        editorSection.setVisibility(View.VISIBLE);
        
        etName.setText(currentSong.name);
        tvDuration.setText("时长: " + formatDuration(currentSong.totalDurationMs));
        tvSource.setText("来源: " + 
            (currentSong.sourceMelodyName != null ? currentSong.sourceMelodyName : "未知") + " + " +
            (currentSong.sourceChordName != null ? currentSong.sourceChordName : "未知"));
        
        String info = "风格: " + currentSong.style + " | 段落: " + currentSong.segments.size();
        tvSongInfo.setText(info);
        
        btnPlay.setEnabled(true);
        btnStop.setEnabled(false);
        
        playbackProgress.setProgress(0);
        tvPlaybackTime.setText("0:00 / " + formatDuration(currentSong.totalDurationMs));
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
        } else {
            MusicData.Song song = currentSong.toMusicDataSong();
            if (song != null && song.melody != null && !song.melody.notes.isEmpty()) {
                playerService.playSong(song);
                playerService.setSpeed(playbackSpeed);
                isPlaying = true;
                btnPlay.setText("暂停");
                btnStop.setEnabled(true);
            } else {
                ToastHelper.showError(this, "无法播放此歌曲");
            }
        }
    }
    
    private void stopSong() {
        if (isBound && playerService != null) {
            playerService.stopPlayback();
        }
        isPlaying = false;
        btnPlay.setText("播放");
        playbackProgress.setProgress(0);
        tvPlaybackTime.setText("0:00 / " + formatDuration(currentSong != null ? currentSong.totalDurationMs : 0));
        btnStop.setEnabled(false);
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
    
    private void doSave(String name) {
        currentSong.name = name;
        repository.updateSong(currentSong.id, name);
        ToastHelper.showSuccess(this, "保存成功");
        loadSongs();
    }
    
    private void confirmDelete() {
        if (currentSong != null) {
            ConfirmDialog.showDelete(this, currentSong.name, () -> deleteSong());
        }
    }
    
    private void deleteSong() {
        if (currentSong != null) {
            repository.deleteSong(currentSong.id);
            ToastHelper.showSuccess(this, "已删除");
            currentSong = null;
            isEditing = false;
            editorSection.setVisibility(View.GONE);
            detailBar.setVisibility(View.GONE);
            loadSongs();
        }
    }
    
    private void toggleDetailExpanded() {
        if (detailExpanded.getVisibility() == View.VISIBLE) {
            detailExpanded.setVisibility(View.GONE);
            btnExpandDetail.setRotation(0);
        } else {
            detailExpanded.setVisibility(View.VISIBLE);
            btnExpandDetail.setRotation(180);
            updateDetailExpanded();
        }
    }
    
    private void updateDetailExpanded() {
        if (currentSong == null) return;
        
        StringBuilder sb = new StringBuilder();
        sb.append("标题: ").append(currentSong.name).append("\n");
        sb.append("风格: ").append(currentSong.style).append("\n");
        sb.append("时长: ").append(formatDuration(currentSong.totalDurationMs)).append("\n");
        sb.append("BPM: ").append(currentSong.bpm).append("\n");
        sb.append("段落数: ").append(currentSong.segments.size()).append("\n");
        sb.append("\n来源:\n");
        sb.append("旋律: ").append(currentSong.sourceMelodyName != null ? currentSong.sourceMelodyName : "未知").append("\n");
        sb.append("和弦: ").append(currentSong.sourceChordName != null ? currentSong.sourceChordName : "未知").append("\n");
        sb.append("\n创建时间: ").append(TimeUtils.formatRelativeTime(currentSong.createdAt));
        
        tvDetailExpanded.setText(sb.toString());
    }
    
    private void showDetailBar() {
        if (currentSong == null) return;
        
        detailBar.setVisibility(View.VISIBLE);
        tvDetailTitle.setText(currentSong.name);
        tvDetailInfo.setText("时长: " + formatDuration(currentSong.totalDurationMs) + 
            " | 段落: " + currentSong.segments.size());
        detailExpanded.setVisibility(View.GONE);
        btnExpandDetail.setRotation(0);
    }
    
    private String formatDuration(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
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
}
