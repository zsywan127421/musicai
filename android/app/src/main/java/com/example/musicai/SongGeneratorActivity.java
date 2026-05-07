package com.example.musicai;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class SongGeneratorActivity extends AppCompatActivity {
    
    private MusicGenerator musicGenerator;
    private MusicData.Song currentSong;
    private ListView lvSong;
    private ArrayAdapter<String> songAdapter;
    private List<String> songList;
    private ProgressBar progressBar;
    
    private MusicPlayerService playerService;
    private boolean isBound = false;
    private boolean isGenerating = false;
    
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
        setContentView(R.layout.activity_song_generator);
        
        musicGenerator = new MusicGenerator(this);
        currentSong = new MusicData.Song();
        
        lvSong = findViewById(R.id.lv_song);
        progressBar = findViewById(R.id.progress_bar);
        songList = new ArrayList<>();
        songAdapter = new ArrayAdapter<>(this, R.layout.list_item_note, songList);
        lvSong.setAdapter(songAdapter);
        
        Spinner spStyle = findViewById(R.id.sp_style);
        ArrayAdapter<String> styleAdapter = new ArrayAdapter<>(this, 
            R.layout.spinner_item, MusicData.MUSIC_STYLES);
        styleAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spStyle.setAdapter(styleAdapter);
        
        Button btnGenerate = findViewById(R.id.btn_generate);
        Button btnPlay = findViewById(R.id.btn_play);
        
        btnGenerate.setOnClickListener(v -> {
            if (isGenerating) return;
            String style = (String) spStyle.getSelectedItem();
            generateSong(style);
        });
        
        btnPlay.setOnClickListener(v -> playSong());
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
    
    private void generateSong(String style) {
        isGenerating = true;
        progressBar.setVisibility(View.VISIBLE);
        Button btnGenerate = findViewById(R.id.btn_generate);
        btnGenerate.setEnabled(false);
        
        new Thread(() -> {
            try {
                MusicData.Melody melody = musicGenerator.generateMelody(style, 8);
                MusicData.ChordProgression chords = musicGenerator.generateChords(style, 4);
                currentSong = musicGenerator.generateSong(style, melody, chords);
                
                runOnUiThread(() -> {
                    updateSongList();
                    TextView tvTitle = findViewById(R.id.tv_title);
                    TextView tvArtist = findViewById(R.id.tv_artist);
                    tvTitle.setText("标题: " + currentSong.title);
                    tvArtist.setText("艺术家: " + currentSong.artist);
                    Toast.makeText(SongGeneratorActivity.this, "歌曲生成完成", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(SongGeneratorActivity.this, "生成失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } finally {
                runOnUiThread(() -> {
                    isGenerating = false;
                    progressBar.setVisibility(View.GONE);
                    btnGenerate.setEnabled(true);
                });
            }
        }).start();
    }
    
    private void playSong() {
        if (currentSong.melody == null || currentSong.melody.notes.isEmpty()) {
            Toast.makeText(this, "没有可播放的歌曲", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (isBound && playerService != null) {
            playerService.playSong(currentSong);
            Toast.makeText(this, "开始播放", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateSongList() {
        songList.clear();
        
        songList.add("标题: " + currentSong.title);
        songList.add("风格: " + currentSong.style);
        songList.add("--- 旋律 ---");
        int noteIndex = 1;
        for (MusicData.Note note : currentSong.melody.notes) {
            songList.add("  " + noteIndex++ + ". " + note.toString());
        }
        songList.add("--- 和弦 ---");
        int chordIndex = 1;
        for (MusicData.Chord chord : currentSong.chords.chords) {
            songList.add("  " + chordIndex++ + ". " + chord.toString());
        }
        
        songAdapter.notifyDataSetChanged();
    }
}