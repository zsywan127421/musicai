package com.example.musicai;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicai.util.ConfirmDialog;
import com.example.musicai.util.EffectChainBottomSheet;
import com.example.musicai.util.ToastHelper;

import java.util.ArrayList;
import java.util.List;

public class TrackManagerActivity extends BaseActivity {
    
    public static final String EXTRA_SONG_ID = "song_id";
    
    private TextView tvTitle;
    private RecyclerView rvTracks;
    private Button btnAddTrack;
    private ImageButton btnBack;
    
    private MusicRepository repository;
    private Project currentProject;
    private String songId;
    private TrackAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_manager);
        
        repository = MusicRepository.getInstance(this);
        songId = getIntent().getStringExtra(EXTRA_SONG_ID);
        
        initViews();
        loadProject();
        setupRecyclerView();
    }
    
    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        rvTracks = findViewById(R.id.rv_tracks);
        btnAddTrack = findViewById(R.id.btn_add_track);
        btnBack = findViewById(R.id.btn_back);
        
        btnBack.setOnClickListener(v -> onBackPressed());
        btnAddTrack.setOnClickListener(v -> addNewTrack());
    }
    
    private void loadProject() {
        if (songId != null) {
            SongEntry song = repository.getSongById(songId);
            if (song != null) {
                currentProject = song.toProject();
                tvTitle.setText("轨道管理: " + song.name);
                return;
            }
        }
        currentProject = new Project("新项目");
        tvTitle.setText("轨道管理");
    }
    
    private void setupRecyclerView() {
        adapter = new TrackAdapter(this, currentProject.tracks);
        rvTracks.setLayoutManager(new LinearLayoutManager(this));
        rvTracks.setAdapter(adapter);
    }
    
    private void addNewTrack() {
        showAddTrackDialog();
    }
    
    private void showAddTrackDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_track, null);
        
        EditText etTrackName = dialogView.findViewById(R.id.et_track_name);
        Spinner spInstrument = dialogView.findViewById(R.id.sp_instrument);
        
        ArrayAdapter<String> instrumentAdapter = new ArrayAdapter<>(
            this, R.layout.spinner_item, Track.INSTRUMENTS);
        instrumentAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spInstrument.setAdapter(instrumentAdapter);
        
        etTrackName.setText("轨道 " + (currentProject.tracks.size() + 1));
        
        ConfirmDialog.showCustom(this, "添加新轨道", dialogView, () -> {
            String name = etTrackName.getText().toString().trim();
            if (name.isEmpty()) {
                name = "轨道 " + (currentProject.tracks.size() + 1);
            }
            String instrument = Track.INSTRUMENTS[spInstrument.getSelectedItemPosition()];
            
            Track newTrack = new Track(name, instrument);
            currentProject.tracks.add(newTrack);
            saveProject();
            adapter.notifyItemInserted(currentProject.tracks.size() - 1);
            ToastHelper.showSuccess(this, "已添加轨道: " + name);
        });
    }
    
    private void saveProject() {
        if (songId != null) {
            SongEntry song = repository.getSongById(songId);
            if (song != null) {
                song.tracks = currentProject.tracks;
                repository.saveSongsToPrefs();
            }
        }
    }
    
    @Override
    public void onBackPressed() {
        saveProject();
        super.onBackPressed();
    }
    
    private static class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {
        
        private Context context;
        private List<Track> tracks;
        
        TrackAdapter(Context context, List<Track> tracks) {
            this.context = context;
            this.tracks = tracks;
        }
        
        @Override
        public TrackViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_track, parent, false);
            return new TrackViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(TrackViewHolder holder, int position) {
            Track track = tracks.get(position);
            holder.bind(track, position);
        }
        
        @Override
        public int getItemCount() {
            return tracks.size();
        }
        
        class TrackViewHolder extends RecyclerView.ViewHolder {
            
            private TextView tvTrackName;
            private TextView tvInstrument;
            private Button btnMute;
            private Button btnSolo;
            private Button btnEffects;
            private ImageButton btnDelete;
            private LinearLayout trackControls;
            
            TrackViewHolder(View itemView) {
                super(itemView);
                tvTrackName = itemView.findViewById(R.id.tv_track_name);
                tvInstrument = itemView.findViewById(R.id.tv_instrument);
                btnMute = itemView.findViewById(R.id.btn_mute);
                btnSolo = itemView.findViewById(R.id.btn_solo);
                btnEffects = itemView.findViewById(R.id.btn_effects);
                btnDelete = itemView.findViewById(R.id.btn_delete);
                trackControls = itemView.findViewById(R.id.track_controls);
            }
            
            void bind(Track track, int position) {
                tvTrackName.setText(track.name);
                tvInstrument.setText(track.instrument);
                
                updateMuteButton(track);
                updateSoloButton(track);
                
                btnMute.setOnClickListener(v -> {
                    track.isMuted = !track.isMuted;
                    if (track.isMuted) track.isSolo = false;
                    updateMuteButton(track);
                    updateSoloButton(track);
                });
                
                btnSolo.setOnClickListener(v -> {
                    track.isSolo = !track.isSolo;
                    if (track.isSolo) track.isMuted = false;
                    updateSoloButton(track);
                    updateMuteButton(track);
                });
                
                btnEffects.setOnClickListener(v -> {
                    EffectChainBottomSheet.show(context, track, effects -> {
                        track.effects = effects;
                    });
                });
                
                btnDelete.setOnClickListener(v -> {
                    ConfirmDialog.showDelete(context, track.name, () -> {
                        tracks.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, tracks.size());
                    });
                });
                
                trackControls.setVisibility(track.isMuted || track.isSolo ? View.VISIBLE : View.GONE);
                if (track.isSolo) {
                    trackControls.setVisibility(View.VISIBLE);
                }
            }
            
            private void updateMuteButton(Track track) {
                if (track.isMuted) {
                    btnMute.setBackgroundResource(R.drawable.apple_button_danger_bg);
                    btnMute.setText("Mute");
                } else {
                    btnMute.setBackgroundResource(R.drawable.apple_button_bg);
                    btnMute.setText("M");
                }
            }
            
            private void updateSoloButton(Track track) {
                if (track.isSolo) {
                    btnSolo.setBackgroundResource(R.drawable.apple_button_primary_bg);
                    btnSolo.setText("Solo");
                } else {
                    btnSolo.setBackgroundResource(R.drawable.apple_button_bg);
                    btnSolo.setText("S");
                }
            }
        }
    }
}
