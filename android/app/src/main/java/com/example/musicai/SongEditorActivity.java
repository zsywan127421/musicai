package com.example.musicai;

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
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.musicai.util.ConfirmDialog;
import com.example.musicai.util.ExportBottomSheet;
import com.example.musicai.util.ToastHelper;
import com.example.musicai.view.PianoRollView;
import com.example.musicai.view.UnifiedPlaybackButton;

import java.util.ArrayList;
import java.util.List;

public class SongEditorActivity extends BaseActivity implements MusicPlayerService.PlaybackListener {

    private static final String[] TIME_SIGNATURES = {"4/4", "3/4", "6/8", "2/4", "5/4", "7/8", "12/8"};
    private static final float[] SPEEDS = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f, 3.0f};
    private static final String KEY_SONG_INDEX = "song_index";
    private static final String KEY_HAS_CHANGES = "has_changes";
    private static final String KEY_SPEED_INDEX = "speed_index";
    private static final String KEY_METRONOME = "metronome_enabled";
    private static final String KEY_SONG_NAME = "song_name";

    private TextView tvTitle;
    private ImageButton btnBack;
    private ImageButton btnMenu;
    private Spinner spSongSelect;
    private Spinner spTimeSignature;
    private Spinner spSpeed;
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
    private Button btnMetronome;
    private CursorSeekBar playbackProgress;
    private TextView tvPlaybackTime;
    private UnifiedPlaybackButton btnPlay;
    private View[] beatIndicators;
    
    private PianoRollView pianoRollView;
    private HorizontalScrollView pianoScrollView;
    private Button btnAddNote;
    private Button btnDeleteNote;
    private Button btnZoomIn;
    private Button btnZoomOut;
    private LinearLayout tracksContainer;
    private Button btnAddTrack;
    private View noteInfoPanel;

    private MusicRepository repository;
    private List<SongEntry> songs = new ArrayList<>();
    private SongEntry currentSong;
    private MusicData.Melody currentMelody;
    private boolean hasChanges = false;
    private int savedSongIndex = -1;
    private int savedSpeedIndex = 2;
    private boolean savedMetronomeEnabled = false;
    private String savedSongName = "";
    private float currentScaleFactor = 1.0f;

    private MusicPlayerService playerService;
    private boolean isBound = false;
    private boolean isPlaying = false;
    private boolean isMetronomeEnabled = false;
    private float currentSpeed = 1.0f;
    private int playbackPositionMs = 0;

    private Handler autoSaveHandler;
    private Runnable autoSaveRunnable;
    private static final long AUTO_SAVE_INTERVAL = 30000;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(android.content.ComponentName name, IBinder service) {
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
            if (savedSpeedIndex >= 0 && savedSpeedIndex < SPEEDS.length) {
                playerService.setSpeed(SPEEDS[savedSpeedIndex]);
            }
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
        setContentView(R.layout.activity_song_editor_v2);

        repository = MusicRepository.getInstance(this);

        initViews();
        setupSpinners();
        setupListeners();
        loadSongs();

        if (savedInstanceState != null) {
            savedSongIndex = savedInstanceState.getInt(KEY_SONG_INDEX, -1);
            hasChanges = savedInstanceState.getBoolean(KEY_HAS_CHANGES, false);
            savedSpeedIndex = savedInstanceState.getInt(KEY_SPEED_INDEX, 2);
            savedMetronomeEnabled = savedInstanceState.getBoolean(KEY_METRONOME, false);
            savedSongName = savedInstanceState.getString(KEY_SONG_NAME, "");
        }

        startAutoSave();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        btnBack = findViewById(R.id.btn_back);
        btnMenu = findViewById(R.id.btn_menu);
        spSongSelect = findViewById(R.id.sp_song_select);
        spTimeSignature = findViewById(R.id.sp_time_signature);
        spSpeed = findViewById(R.id.sp_speed);
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
        btnMetronome = findViewById(R.id.btn_metronome);
        playbackProgress = findViewById(R.id.playback_progress);
        tvPlaybackTime = findViewById(R.id.tv_playback_time);
        btnPlay = findViewById(R.id.btn_play);

        pianoRollView = findViewById(R.id.piano_roll_view);
        pianoScrollView = findViewById(R.id.piano_scroll_view);
        btnAddNote = findViewById(R.id.btn_add_note);
        btnDeleteNote = findViewById(R.id.btn_delete_note);
        btnZoomIn = findViewById(R.id.btn_zoom_in);
        btnZoomOut = findViewById(R.id.btn_zoom_out);
        tracksContainer = findViewById(R.id.tracks_container);
        btnAddTrack = findViewById(R.id.btn_add_track);

        beatIndicators = new View[]{
            findViewById(R.id.beat_1),
            findViewById(R.id.beat_2),
            findViewById(R.id.beat_3),
            findViewById(R.id.beat_4)
        };

        editorSection.setVisibility(View.GONE);
        tvEmptyHint.setVisibility(View.VISIBLE);
        
        pianoRollView.setOnPlayheadChangedListener(position -> {
            playbackPositionMs = position;
        });
    }

    private void setupSpinners() {
        ArrayAdapter<String> tsAdapter = new ArrayAdapter<>(this,
            R.layout.spinner_item, TIME_SIGNATURES);
        tsAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spTimeSignature.setAdapter(tsAdapter);

        String[] speedLabels = new String[SPEEDS.length];
        for (int i = 0; i < SPEEDS.length; i++) {
            speedLabels[i] = SPEEDS[i] + "x";
        }
        ArrayAdapter<String> speedAdapter = new ArrayAdapter<>(this,
            R.layout.spinner_item, speedLabels);
        speedAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spSpeed.setAdapter(speedAdapter);
        spSpeed.setSelection(savedSpeedIndex);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());
        btnMenu.setOnClickListener(v -> showPopupMenu(v));

        spSongSelect.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && position <= songs.size()) {
                    selectSong(position - 1);
                } else {
                    editorSection.setVisibility(View.GONE);
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

        spSpeed.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < SPEEDS.length) {
                    currentSpeed = SPEEDS[position];
                    savedSpeedIndex = position;
                    if (isBound && playerService != null) {
                        playerService.setSpeed(currentSpeed);
                    }
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnBpmDecrease.setOnClickListener(v -> adjustBpm(-5));
        btnBpmIncrease.setOnClickListener(v -> adjustBpm(5));

        btnMetronome.setOnClickListener(v -> toggleMetronome());

        btnPlay.setOnPlaybackStateChangeListener(new UnifiedPlaybackButton.OnPlaybackStateChangeListener() {
            @Override
            public void onPlayClicked() {
                playSong();
            }

            @Override
            public void onPauseClicked() {
                pauseSong();
            }

            @Override
            public void onResumeClicked() {
                resumeSong();
            }
        });

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
                    playerService.setPlaybackListener(SongEditorActivity.this);
                }
            }
        });
        
        btnAddNote.setOnClickListener(v -> addRandomNote());
        btnDeleteNote.setOnClickListener(v -> deleteSelectedNote());
        btnZoomIn.setOnClickListener(v -> zoomIn());
        btnZoomOut.setOnClickListener(v -> zoomOut());
        btnAddTrack.setOnClickListener(v -> ToastHelper.showInfo(this, "添加轨道功能开发中"));
        
        btnDeleteNote.setEnabled(false);
        
        pianoRollView.setOnNoteChangedListener(new PianoRollView.OnNoteChangedListener() {
            @Override
            public void onNoteChanged(int index, String pitch, int octave, int duration, int startTime) {
                if (currentMelody != null && index >= 0 && index < currentMelody.notes.size()) {
                    MusicData.Note note = currentMelody.notes.get(index);
                    note.pitch = pitch;
                    note.octave = octave;
                    note.duration = duration;
                    note.startTime = startTime;
                    hasChanges = true;
                    updateSongFromMelody();
                }
            }
            
            @Override
            public void onNoteSelected(int index) {
                btnDeleteNote.setEnabled(index >= 0);
                if (index >= 0 && currentMelody != null && index < currentMelody.notes.size()) {
                    MusicData.Note note = currentMelody.notes.get(index);
                    Toast.makeText(SongEditorActivity.this, 
                        "已选择: " + note.pitch + note.octave + " 时值:" + note.duration, Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        pianoRollView.setOnPlayheadChangedListener(new PianoRollView.OnPlayheadChangedListener() {
            @Override
            public void onPlayheadChanged(int position) {
                playbackPositionMs = position;
            }
        });
    }
    
    private void zoomIn() {
        currentScaleFactor = Math.min(3.0f, currentScaleFactor + 0.2f);
        pianoRollView.setScaleFactor(currentScaleFactor);
    }
    
    private void zoomOut() {
        currentScaleFactor = Math.max(0.5f, currentScaleFactor - 0.2f);
        pianoRollView.setScaleFactor(currentScaleFactor);
    }
    
    private void addRandomNote() {
        if (currentMelody == null) {
            currentMelody = new MusicData.Melody();
            currentMelody.notes = new ArrayList<>();
        }
        
        String[] pitches = MusicData.PITCHES;
        String pitch = pitches[(int) (Math.random() * pitches.length)];
        int octave = 3 + (int) (Math.random() * 3);
        int duration = 4;
        int startTime = 0;
        
        if (!currentMelody.notes.isEmpty()) {
            MusicData.Note lastNote = currentMelody.notes.get(currentMelody.notes.size() - 1);
            startTime = lastNote.startTime + lastNote.duration;
        }
        
        MusicData.Note note = new MusicData.Note(pitch, octave, duration, startTime);
        currentMelody.notes.add(note);
        pianoRollView.setNotes(currentMelody.notes);
        hasChanges = true;
        updateSongFromMelody();
        Toast.makeText(this, "已添加: " + pitch + octave, Toast.LENGTH_SHORT).show();
    }
    
    private void deleteSelectedNote() {
        int selectedIndex = pianoRollView.getSelectedNoteIndex();
        if (selectedIndex >= 0 && currentMelody != null && selectedIndex < currentMelody.notes.size()) {
            ConfirmDialog.showDelete(this, "该音符", () -> {
                currentMelody.notes.remove(selectedIndex);
                pianoRollView.setNotes(currentMelody.notes);
                hasChanges = true;
                updateSongFromMelody();
                btnDeleteNote.setEnabled(false);
                Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void showPopupMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_song_editor, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_regenerate) {
                regenerateSong();
                return true;
            } else if (id == R.id.menu_export_midi) {
                exportMidi();
                return true;
            } else if (id == R.id.menu_export_wav) {
                exportWav();
                return true;
            } else if (id == R.id.menu_save) {
                saveSong();
                return true;
            } else if (id == R.id.menu_save_as) {
                saveSongAs();
                return true;
            }
            return false;
        });

        popup.show();
    }

    @Override
    protected void onSaveInstanceState(@androidx.annotation.NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SONG_INDEX, savedSongIndex);
        outState.putBoolean(KEY_HAS_CHANGES, hasChanges);
        outState.putInt(KEY_SPEED_INDEX, savedSpeedIndex);
        outState.putBoolean(KEY_METRONOME, isMetronomeEnabled);
        outState.putString(KEY_SONG_NAME, etName.getText().toString());
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

        if (savedSongIndex >= 0 && savedSongIndex < songs.size()) {
            spSongSelect.setSelection(savedSongIndex + 1);
        }
    }

    private void selectSong(int index) {
        if (index < 0 || index >= songs.size()) return;

        savedSongIndex = index;
        currentSong = songs.get(index);

        editorSection.setVisibility(View.VISIBLE);
        tvEmptyHint.setVisibility(View.GONE);

        etName.setText(currentSong.name);
        savedSongName = currentSong.name;
        tvStyle.setText(currentSong.style != null ? currentSong.style : "流行");
        tvDuration.setText(formatDuration(currentSong.totalDurationMs));
        
        String melodyName = "未知";
        String chordName = "未知";
        if (currentSong.sourceMelodyId != null && !currentSong.sourceMelodyId.isEmpty()) {
            MusicRepository.MelodyEntry melodyEntry = repository.getMelodyById(currentSong.sourceMelodyId);
            if (melodyEntry != null) {
                melodyName = melodyEntry.name;
                currentMelody = melodyEntry.toMelody();
            }
        }
        if (currentSong.sourceChordId != null && !currentSong.sourceChordId.isEmpty()) {
            MusicRepository.ChordEntry chordEntry = repository.getChordById(currentSong.sourceChordId);
            if (chordEntry != null) {
                chordName = chordEntry.name;
            }
        }
        tvSourceMelody.setText(melodyName);
        tvSourceChord.setText(chordName);

        tvBpm.setText(String.valueOf(currentSong.bpm));
        int tsIndex = java.util.Arrays.asList(TIME_SIGNATURES).indexOf(currentSong.keySignature);
        if (tsIndex >= 0) {
            spTimeSignature.setSelection(tsIndex);
        }

        tvPlaybackTime.setText("0:00 / " + formatDuration(currentSong.totalDurationMs));
        playbackProgress.setProgress(0);
        
        if (currentMelody != null && currentMelody.notes != null) {
            pianoRollView.setNotes(currentMelody.notes);
        } else {
            MusicData.Melody defaultMelody = currentSong.toMusicDataSong().melody;
            if (defaultMelody != null) {
                currentMelody = defaultMelody;
                pianoRollView.setNotes(currentMelody.notes);
            }
        }
        
        updateTracksList();

        btnPlay.setEnabled(true);
        hasChanges = false;
    }
    
    private void updateSongFromMelody() {
        if (currentSong == null || currentMelody == null) return;
        
        if (currentSong.segments == null) {
            currentSong.segments = new ArrayList<>();
        }
        
        if (!currentSong.segments.isEmpty()) {
            SongEntry.SegmentData segment = currentSong.segments.get(0);
            segment.melodyJson = melodyToJson(currentMelody);
        } else {
            SongEntry.SegmentData segment = new SongEntry.SegmentData();
            segment.melodyJson = melodyToJson(currentMelody);
            segment.chordJson = "{}";
            segment.startTimeMs = 0;
            segment.durationMs = calculateMelodyDuration(currentMelody);
            currentSong.segments.add(segment);
        }
        
        currentSong.totalDurationMs = calculateMelodyDuration(currentMelody);
        tvDuration.setText(formatDuration(currentSong.totalDurationMs));
    }
    
    private String melodyToJson(MusicData.Melody melody) {
        org.json.JSONArray array = new org.json.JSONArray();
        if (melody != null && melody.notes != null) {
            for (MusicData.Note note : melody.notes) {
                try {
                    array.put(note.toJson());
                } catch (org.json.JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return array.toString();
    }
    
    private int calculateMelodyDuration(MusicData.Melody melody) {
        if (melody == null || melody.notes == null || melody.notes.isEmpty()) {
            return 0;
        }
        int lastNoteEnd = 0;
        for (MusicData.Note note : melody.notes) {
            int noteEnd = note.startTime + note.duration;
            if (noteEnd > lastNoteEnd) {
                lastNoteEnd = noteEnd;
            }
        }
        int bpm = currentSong != null && currentSong.bpm > 0 ? currentSong.bpm : 120;
        return lastNoteEnd * (60000 / bpm);
    }
    
    private void updateTracksList() {
        tracksContainer.removeAllViews();
        
        if (currentSong == null) return;
        
        String trackName = "主旋律";
        if (tvSourceMelody.getText() != null && !tvSourceMelody.getText().toString().equals("未知")) {
            trackName = tvSourceMelody.getText().toString();
        }
        
        addTrackView(trackName, true, false, "Piano");
    }
    
    private void addTrackView(String name, boolean muted, boolean solo, String instrument) {
        View trackView = getLayoutInflater().inflate(R.layout.item_track_editor, tracksContainer, false);
        
        TextView tvTrackName = trackView.findViewById(R.id.tv_track_name);
        Button btnMute = trackView.findViewById(R.id.btn_mute);
        Button btnSolo = trackView.findViewById(R.id.btn_solo);
        Button btnInstrument = trackView.findViewById(R.id.btn_instrument);
        
        tvTrackName.setText(name);
        
        btnMute.setText(muted ? "静音" : "取消静音");
        btnMute.setBackgroundResource(muted ? R.drawable.apple_button_danger_bg : R.drawable.apple_button_bg);
        
        btnSolo.setText(solo ? "独奏" : "取消独奏");
        btnSolo.setBackgroundResource(solo ? R.drawable.apple_button_primary_bg : R.drawable.apple_button_bg);
        
        btnInstrument.setText(instrument);
        
        btnMute.setOnClickListener(v -> {
            ToastHelper.showInfo(this, "静音切换功能开发中");
        });
        
        btnSolo.setOnClickListener(v -> {
            ToastHelper.showInfo(this, "独奏切换功能开发中");
        });
        
        btnInstrument.setOnClickListener(v -> {
            ToastHelper.showInfo(this, "音色选择功能开发中");
        });
        
        tracksContainer.addView(trackView);
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
        if (currentSong == null) {
            ToastHelper.showError(this, "请先选择歌曲");
            return;
        }

        if (!isBound || playerService == null) {
            ToastHelper.showError(this, "播放器服务未连接");
            return;
        }

        MusicData.Song song = currentSong.toMusicDataSong();
        if (song != null && song.melody != null && !song.melody.notes.isEmpty()) {
            playerService.setSpeed(currentSpeed);
            playerService.playSong(song);
            isPlaying = true;
            btnPlay.setState(UnifiedPlaybackButton.State.PAUSE);

            if (isMetronomeEnabled) {
                playerService.setMetronomeBpm(currentSong.bpm);
                playerService.setMetronomeBeatsPerMeasure(4);
                playerService.startMetronome();
            }
        } else {
            ToastHelper.showError(this, "无法播放此歌曲");
        }
    }

    private void pauseSong() {
        if (isBound && playerService != null) {
            playerService.pause();
            isPlaying = false;
            btnPlay.setState(UnifiedPlaybackButton.State.RESUME);
            if (isMetronomeEnabled) {
                playerService.stopMetronome();
            }
        }
    }

    private void resumeSong() {
        if (isBound && playerService != null) {
            playerService.resume();
            isPlaying = true;
            btnPlay.setState(UnifiedPlaybackButton.State.PAUSE);
        }
    }

    private void stopSong() {
        if (isBound && playerService != null) {
            playerService.stopPlayback();
            playerService.stopMetronome();
        }
        isPlaying = false;
        btnPlay.setState(UnifiedPlaybackButton.State.PLAY);
        playbackProgress.setProgress(0);
        tvPlaybackTime.setText("0:00 / " + formatDuration(currentSong != null ? currentSong.totalDurationMs : 0));
        pianoRollView.setPlayheadPosition(0);
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

    private void exportMidi() {
        if (currentSong == null) {
            ToastHelper.showError(this, "请先选择歌曲");
            return;
        }

        ExportBottomSheet exportSheet = new ExportBottomSheet(this, currentSong);
        exportSheet.show();
    }

    private void exportWav() {
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

        updateSongFromMelody();
        
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
        updateSongFromMelody();
        currentSong.name = name;
        currentSong.updatedAt = System.currentTimeMillis();
        repository.updateSong(currentSong.id, name);
        ToastHelper.showSuccess(this, "保存成功");
        hasChanges = false;
        savedSongName = name;
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
        if (millis <= 0) return "0:00";
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
            if (savedSongIndex >= 0 && savedSongIndex < songs.size()) {
                selectSong(savedSongIndex);
            }
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
                int progress = (int) ((long) positionMs * 100 / totalMs);
                playbackProgress.setProgress(progress);
                int adjustedTotal = (int) (totalMs / currentSpeed);
                tvPlaybackTime.setText(formatTime(positionMs) + " / " + formatTime(adjustedTotal));
                
                pianoRollView.setPlayheadPosition(positionMs / 500);
            }
        });
    }

    @Override
    public void onPlaybackStateChanged(boolean playing) {
        isPlaying = playing;
        runOnUiThread(() -> {
            btnPlay.setState(playing ? UnifiedPlaybackButton.State.PAUSE : UnifiedPlaybackButton.State.RESUME);
        });
    }

    @Override
    public void onPlaybackCompleted() {
        runOnUiThread(() -> {
            isPlaying = false;
            btnPlay.setState(UnifiedPlaybackButton.State.PLAY);
            playbackProgress.setProgress(0);
            tvPlaybackTime.setText("0:00 / " + formatTime((int) (currentSong != null ? currentSong.totalDurationMs / currentSpeed : 0)));
            pianoRollView.setPlayheadPosition(0);
            if (isBound && playerService != null) {
                playerService.stopPlayback();
                playerService.stopMetronome();
            }
            resetBeatIndicators();
        });
    }

    private String formatTime(int milliseconds) {
        if (milliseconds <= 0) return "0:00";
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
