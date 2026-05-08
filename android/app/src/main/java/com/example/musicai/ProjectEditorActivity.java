package com.example.musicai;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicai.util.ConfirmDialog;
import com.example.musicai.util.EffectChainBottomSheet;
import com.example.musicai.util.SelectItemBottomSheet;
import com.example.musicai.util.ToastHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProjectEditorActivity extends BaseActivity {
    
    private static final String PREFS_NAME = "MusicAIProjects";
    private static final String KEY_PROJECTS = "projects";
    
    private EditText etProjectName;
    private TextView tvBpmValue;
    private SeekBar seekbarBpm;
    private Spinner spTimeSignature;
    private Spinner spKeySignature;
    private RecyclerView rvTracks;
    private TextView tvTrackCount;
    private View emptyState;
    private Button btnAddTrack;
    private Button btnSave;
    
    private Project currentProject;
    private TrackAdapter trackAdapter;
    private SharedPreferences prefs;
    private boolean isNewProject = true;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_editor);
        
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        String projectId = getIntent().getStringExtra("project_id");
        if (projectId != null) {
            currentProject = loadProject(projectId);
            isNewProject = false;
        } else {
            currentProject = new Project();
        }
        
        initViews();
        setupSpinners();
        setupListeners();
        updateUI();
    }
    
    private void initViews() {
        etProjectName = findViewById(R.id.et_project_name);
        tvBpmValue = findViewById(R.id.tv_bpm_value);
        seekbarBpm = findViewById(R.id.seekbar_bpm);
        spTimeSignature = findViewById(R.id.sp_time_signature);
        spKeySignature = findViewById(R.id.sp_key_signature);
        rvTracks = findViewById(R.id.rv_tracks);
        tvTrackCount = findViewById(R.id.tv_track_count);
        emptyState = findViewById(R.id.empty_state);
        btnAddTrack = findViewById(R.id.btn_add_track);
        btnSave = findViewById(R.id.btn_save);
        
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());
        
        trackAdapter = new TrackAdapter(this, currentProject.tracks);
        rvTracks.setLayoutManager(new LinearLayoutManager(this));
        rvTracks.setAdapter(trackAdapter);
    }
    
    private void setupSpinners() {
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(
            this,
            R.layout.spinner_item,
            Project.TIME_SIGNATURES
        );
        timeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spTimeSignature.setAdapter(timeAdapter);
        
        ArrayAdapter<String> keyAdapter = new ArrayAdapter<>(
            this,
            R.layout.spinner_item,
            Project.KEY_SIGNATURES
        );
        keyAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spKeySignature.setAdapter(keyAdapter);
    }
    
    private void setupListeners() {
        etProjectName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentProject.name = s.toString();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        seekbarBpm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int bpm = Math.max(40, progress);
                tvBpmValue.setText(String.valueOf(bpm));
                currentProject.bpm = bpm;
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        spTimeSignature.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentProject.timeSignature = Project.TIME_SIGNATURES[position];
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        spKeySignature.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentProject.keySignature = Project.KEY_SIGNATURES[position];
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        btnAddTrack.setOnClickListener(v -> addNewTrack());
        btnSave.setOnClickListener(v -> saveProject());
    }
    
    private void updateUI() {
        etProjectName.setText(currentProject.name);
        tvBpmValue.setText(String.valueOf(currentProject.bpm));
        seekbarBpm.setProgress(currentProject.bpm);
        
        for (int i = 0; i < Project.TIME_SIGNATURES.length; i++) {
            if (Project.TIME_SIGNATURES[i].equals(currentProject.timeSignature)) {
                spTimeSignature.setSelection(i);
                break;
            }
        }
        
        for (int i = 0; i < Project.KEY_SIGNATURES.length; i++) {
            if (Project.KEY_SIGNATURES[i].equals(currentProject.keySignature)) {
                spKeySignature.setSelection(i);
                break;
            }
        }
        
        updateTrackList();
    }
    
    private void updateTrackList() {
        int count = currentProject.getTrackCount();
        tvTrackCount.setText(count + " 条");
        emptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        rvTracks.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        trackAdapter.notifyDataSetChanged();
    }
    
    private void addNewTrack() {
        List<String> instruments = new ArrayList<>();
        for (String instrument : Track.INSTRUMENTS) {
            instruments.add(instrument);
        }
        
        SelectItemBottomSheet.show(this, instruments, (index) -> {
            Track track = new Track("Track " + (currentProject.getTrackCount() + 1), Track.INSTRUMENTS[index]);
            currentProject.addTrack(track);
            updateTrackList();
        });
    }
    
    private void deleteTrack(int position) {
        Track track = currentProject.tracks.get(position);
        ConfirmDialog.showDelete(this, track.name, () -> {
            currentProject.removeTrack(track.id);
            updateTrackList();
            ToastHelper.showSuccess(this, "轨道已删除");
        });
    }
    
    private void saveProject() {
        if (currentProject.name.trim().isEmpty()) {
            ToastHelper.showWarning(this, "请输入项目名称");
            return;
        }
        
        currentProject.updateTimestamp();
        saveToPrefs(currentProject);
        ToastHelper.showSuccess(this, "项目已保存");
        
        if (isNewProject) {
            finish();
        }
    }
    
    private void saveToPrefs(Project project) {
        try {
            String projectsJson = prefs.getString(KEY_PROJECTS, "[]");
            JSONArray projectsArray = new JSONArray(projectsJson);
            
            boolean found = false;
            for (int i = 0; i < projectsArray.length(); i++) {
                JSONObject obj = projectsArray.getJSONObject(i);
                if (obj.getString("id").equals(project.id)) {
                    projectsArray.put(i, project.toJson());
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                projectsArray.put(project.toJson());
            }
            
            prefs.edit().putString(KEY_PROJECTS, projectsArray.toString()).apply();
            
            saveProjectToFile(project);
            
        } catch (JSONException e) {
            e.printStackTrace();
            ToastHelper.showError(this, "保存失败");
        }
    }
    
    private void saveProjectToFile(Project project) {
        File dir = new File(getFilesDir(), "projects");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        File file = new File(dir, project.id + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(project.toJson().toString(2));
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }
    
    private Project loadProject(String projectId) {
        File file = new File(new File(getFilesDir(), "projects"), projectId + ".json");
        if (file.exists()) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                return Project.fromJson(new JSONObject(content));
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }
        
        try {
            String projectsJson = prefs.getString(KEY_PROJECTS, "[]");
            JSONArray projectsArray = new JSONArray(projectsJson);
            for (int i = 0; i < projectsArray.length(); i++) {
                JSONObject obj = projectsArray.getJSONObject(i);
                if (obj.getString("id").equals(projectId)) {
                    return Project.fromJson(obj);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        return new Project();
    }
    
    @Override
    public void onBackPressed() {
        if (!currentProject.name.equals("New Project") || currentProject.getTrackCount() > 0) {
            ConfirmDialog.show(this, "是否保存更改？", "未保存的更改将丢失", () -> {
                saveProject();
                super.onBackPressed();
            });
        } else {
            super.onBackPressed();
        }
    }
    
    private static class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {
        
        private Context context;
        private List<Track> tracks;
        
        TrackAdapter(Context context, List<Track> tracks) {
            this.context = context;
            this.tracks = tracks;
        }
        
        @NonNull
        @Override
        public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_track, parent, false);
            return new TrackViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
            Track track = tracks.get(position);
            holder.bind(track, position);
        }
        
        @Override
        public int getItemCount() {
            return tracks.size();
        }
        
        class TrackViewHolder extends RecyclerView.ViewHolder {
            
            private EditText etTrackName;
            private Spinner spInstrument;
            private Button btnMute;
            private Button btnSolo;
            private Button btnEffects;
            private SeekBar seekbarVolume;
            private TextView tvVolumeValue;
            private ImageButton btnDelete;
            
            TrackViewHolder(@NonNull View itemView) {
                super(itemView);
                etTrackName = itemView.findViewById(R.id.et_track_name);
                spInstrument = itemView.findViewById(R.id.sp_instrument);
                btnMute = itemView.findViewById(R.id.btn_mute);
                btnSolo = itemView.findViewById(R.id.btn_solo);
                btnEffects = itemView.findViewById(R.id.btn_effects);
                seekbarVolume = itemView.findViewById(R.id.seekbar_volume);
                tvVolumeValue = itemView.findViewById(R.id.tv_volume_value);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
            
            void bind(Track track, int position) {
                etTrackName.setText(track.name);
                etTrackName.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        track.name = s.toString();
                    }
                    
                    @Override
                    public void afterTextChanged(Editable s) {}
                });
                
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    context,
                    R.layout.spinner_item,
                    Track.INSTRUMENTS
                );
                adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                spInstrument.setAdapter(adapter);
                
                for (int i = 0; i < Track.INSTRUMENTS.length; i++) {
                    if (Track.INSTRUMENTS[i].equals(track.instrument)) {
                        spInstrument.setSelection(i);
                        break;
                    }
                }
                
                spInstrument.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                        track.instrument = Track.INSTRUMENTS[pos];
                    }
                    
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
                
                updateMuteButton(track);
                updateSoloButton(track);
                
                btnMute.setOnClickListener(v -> {
                    track.isMuted = !track.isMuted;
                    updateMuteButton(track);
                });
                
                btnSolo.setOnClickListener(v -> {
                    track.isSolo = !track.isSolo;
                    updateSoloButton(track);
                });
                
                btnEffects.setOnClickListener(v -> {
                    if (context instanceof ProjectEditorActivity) {
                        EffectChainBottomSheet.show(context, track, null);
                    }
                });
                
                int volumePercent = (int) (track.volume * 100);
                seekbarVolume.setProgress(volumePercent);
                tvVolumeValue.setText(volumePercent + "%");
                
                seekbarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        track.volume = progress / 100f;
                        tvVolumeValue.setText(progress + "%");
                    }
                    
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });
                
                btnDelete.setOnClickListener(v -> {
                    if (context instanceof ProjectEditorActivity) {
                        ((ProjectEditorActivity) context).deleteTrack(position);
                    }
                });
            }
            
            private void updateMuteButton(Track track) {
                if (track.isMuted) {
                    btnMute.setBackgroundResource(R.drawable.apple_button_danger_bg);
                    btnMute.setTextColor(context.getResources().getColor(R.color.apple_white));
                } else {
                    btnMute.setBackgroundResource(R.drawable.apple_button_minor_bg);
                    btnMute.setTextColor(context.getResources().getColor(R.color.apple_text_secondary));
                }
            }
            
            private void updateSoloButton(Track track) {
                if (track.isSolo) {
                    btnSolo.setBackgroundResource(R.drawable.apple_button_primary_bg);
                    btnSolo.setTextColor(context.getResources().getColor(R.color.apple_white));
                } else {
                    btnSolo.setBackgroundResource(R.drawable.apple_button_minor_bg);
                    btnSolo.setTextColor(context.getResources().getColor(R.color.apple_text_secondary));
                }
            }
        }
    }
}
