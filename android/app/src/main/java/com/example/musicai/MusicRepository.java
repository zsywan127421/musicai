package com.example.musicai;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MusicRepository {
    
    private static final String TAG = "MusicRepository";
    private static final String PREFS_NAME = "MusicRepositoryPrefs";
    private static final String KEY_MELODIES = "melody_library";
    private static final String KEY_CHORDS = "chord_library";
    private static final String KEY_SONGS = "song_library";
    
    private static MusicRepository instance;
    private SharedPreferences prefs;
    
    private List<MelodyEntry> melodyLibrary;
    private List<ChordEntry> chordLibrary;
    private List<SongEntry> songLibrary;
    
    public static class MelodyEntry {
        public String id;
        public String name;
        public String style;
        public List<NoteData> notes;
        public long createdAt;
        
        public MelodyEntry() {
            this.id = generateId();
            this.notes = new ArrayList<>();
            this.createdAt = System.currentTimeMillis();
        }
        
        public MelodyEntry(String name, String style, MusicData.Melody melody) {
            this.id = generateId();
            this.name = name;
            this.style = style;
            this.notes = new ArrayList<>();
            this.createdAt = System.currentTimeMillis();
            
            if (melody != null && melody.notes != null) {
                for (MusicData.Note note : melody.notes) {
                    this.notes.add(new NoteData(note));
                }
            }
        }
        
        private static String generateId() {
            return "mel_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
        }
        
        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", id);
                obj.put("name", name);
                obj.put("style", style);
                obj.put("createdAt", createdAt);
                
                JSONArray notesArray = new JSONArray();
                for (NoteData note : notes) {
                    notesArray.put(note.toJson());
                }
                obj.put("notes", notesArray);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to serialize melody entry", e);
            }
            return obj;
        }
        
        public static MelodyEntry fromJson(JSONObject obj) {
            MelodyEntry entry = new MelodyEntry();
            try {
                entry.id = obj.optString("id", generateId());
                entry.name = obj.optString("name", "未命名旋律");
                entry.style = obj.optString("style", "流行");
                entry.createdAt = obj.optLong("createdAt", System.currentTimeMillis());
                
                if (obj.has("notes")) {
                    JSONArray notesArray = obj.getJSONArray("notes");
                    for (int i = 0; i < notesArray.length(); i++) {
                        entry.notes.add(NoteData.fromJson(notesArray.getJSONObject(i)));
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse melody entry", e);
            }
            return entry;
        }
        
        public MusicData.Melody toMelody() {
            MusicData.Melody melody = new MusicData.Melody();
            melody.name = name;
            melody.style = style;
            
            int time = 0;
            for (NoteData noteData : notes) {
                MusicData.Note note = new MusicData.Note(
                    noteData.pitch,
                    noteData.octave,
                    noteData.duration,
                    time
                );
                melody.notes.add(note);
                time = note.startTime + note.duration;
            }
            return melody;
        }
        
        public String getPreviewText() {
            if (notes.isEmpty()) return "空旋律";
            StringBuilder sb = new StringBuilder();
            int count = Math.min(notes.size(), 6);
            for (int i = 0; i < count; i++) {
                if (sb.length() > 0) sb.append(" → ");
                NoteData note = notes.get(i);
                sb.append(note.pitch).append(note.octave);
            }
            if (notes.size() > 6) {
                sb.append(" ... (+").append(notes.size() - 6).append(")");
            }
            return sb.toString();
        }
    }
    
    public static class NoteData {
        public String pitch;
        public int octave;
        public int duration;
        public int startTime;
        
        public NoteData() {}
        
        public NoteData(String pitch, int octave, int duration, int startTime) {
            this.pitch = pitch;
            this.octave = octave;
            this.duration = duration;
            this.startTime = startTime;
        }
        
        public NoteData(MusicData.Note note) {
            this.pitch = note.pitch;
            this.octave = note.octave;
            this.duration = note.duration;
            this.startTime = note.startTime;
        }
        
        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("pitch", pitch);
                obj.put("octave", octave);
                obj.put("duration", duration);
                obj.put("startTime", startTime);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to serialize note", e);
            }
            return obj;
        }
        
        public static NoteData fromJson(JSONObject obj) {
            return new NoteData(
                obj.optString("pitch", "C"),
                obj.optInt("octave", 4),
                obj.optInt("duration", 4),
                obj.optInt("startTime", 0)
            );
        }
    }
    
    public static class ChordEntry {
        public String id;
        public String name;
        public String style;
        public String keySignature;
        public String mood;
        public List<ChordData> chords;
        public long createdAt;
        
        public ChordEntry() {
            this.id = generateId();
            this.chords = new ArrayList<>();
            this.createdAt = System.currentTimeMillis();
        }
        
        public ChordEntry(String name, String style, MusicData.ChordProgression progression) {
            this.id = generateId();
            this.name = name;
            this.style = style;
            this.chords = new ArrayList<>();
            this.createdAt = System.currentTimeMillis();
            
            if (progression != null && progression.chords != null) {
                for (MusicData.Chord chord : progression.chords) {
                    this.chords.add(new ChordData(chord));
                }
            }
        }
        
        private static String generateId() {
            return "chd_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
        }
        
        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", id);
                obj.put("name", name);
                obj.put("style", style);
                obj.put("keySignature", keySignature);
                obj.put("mood", mood);
                obj.put("createdAt", createdAt);
                
                JSONArray chordsArray = new JSONArray();
                for (ChordData chord : chords) {
                    chordsArray.put(chord.toJson());
                }
                obj.put("chords", chordsArray);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to serialize chord entry", e);
            }
            return obj;
        }
        
        public static ChordEntry fromJson(JSONObject obj) {
            ChordEntry entry = new ChordEntry();
            try {
                entry.id = obj.optString("id", generateId());
                entry.name = obj.optString("name", "未命名和弦");
                entry.style = obj.optString("style", "流行");
                entry.keySignature = obj.optString("keySignature", "");
                entry.mood = obj.optString("mood", "");
                entry.createdAt = obj.optLong("createdAt", System.currentTimeMillis());
                
                if (obj.has("chords")) {
                    JSONArray chordsArray = obj.getJSONArray("chords");
                    for (int i = 0; i < chordsArray.length(); i++) {
                        entry.chords.add(ChordData.fromJson(chordsArray.getJSONObject(i)));
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse chord entry", e);
            }
            return entry;
        }
        
        public MusicData.ChordProgression toChordProgression() {
            MusicData.ChordProgression progression = new MusicData.ChordProgression();
            progression.name = name;
            progression.style = style;
            
            int time = 0;
            for (ChordData chordData : chords) {
                MusicData.Chord chord = new MusicData.Chord(
                    chordData.name,
                    chordData.type,
                    chordData.duration,
                    time
                );
                progression.chords.add(chord);
                time = chord.startTime + chord.duration;
            }
            return progression;
        }
        
        public String getPreviewText() {
            if (chords.isEmpty()) return "空和弦";
            StringBuilder sb = new StringBuilder();
            int count = Math.min(chords.size(), 6);
            for (int i = 0; i < count; i++) {
                if (sb.length() > 0) sb.append(" → ");
                sb.append(chords.get(i).name);
            }
            if (chords.size() > 6) {
                sb.append(" ... (+").append(chords.size() - 6).append(")");
            }
            return sb.toString();
        }
    }
    
    public static class ChordData {
        public String name;
        public String type;
        public int duration;
        public int startTime;
        
        public ChordData() {}
        
        public ChordData(String name, String type, int duration, int startTime) {
            this.name = name;
            this.type = type;
            this.duration = duration;
            this.startTime = startTime;
        }
        
        public ChordData(MusicData.Chord chord) {
            this.name = chord.name;
            this.type = chord.type;
            this.duration = chord.duration;
            this.startTime = chord.startTime;
        }
        
        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("name", name);
                obj.put("type", type);
                obj.put("duration", duration);
                obj.put("startTime", startTime);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to serialize chord", e);
            }
            return obj;
        }
        
        public static ChordData fromJson(JSONObject obj) {
            return new ChordData(
                obj.optString("name", "C"),
                obj.optString("type", "major"),
                obj.optInt("duration", 4),
                obj.optInt("startTime", 0)
            );
        }
    }
    
    private MusicRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        melodyLibrary = new ArrayList<>();
        chordLibrary = new ArrayList<>();
        songLibrary = new ArrayList<>();
        loadFromPrefs();
    }
    
    public static synchronized MusicRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MusicRepository(context.getApplicationContext());
        }
        return instance;
    }
    
    public List<MelodyEntry> getMelodyLibrary() {
        return new ArrayList<>(melodyLibrary);
    }
    
    public List<ChordEntry> getChordLibrary() {
        return new ArrayList<>(chordLibrary);
    }
    
    public void addMelody(MelodyEntry entry) {
        melodyLibrary.add(0, entry);
        saveMelodiesToPrefs();
    }
    
    public void addChord(ChordEntry entry) {
        chordLibrary.add(0, entry);
        saveChordsToPrefs();
    }
    
    public MelodyEntry getMelodyById(String id) {
        for (MelodyEntry entry : melodyLibrary) {
            if (entry.id.equals(id)) {
                return entry;
            }
        }
        return null;
    }
    
    public ChordEntry getChordById(String id) {
        for (ChordEntry entry : chordLibrary) {
            if (entry.id.equals(id)) {
                return entry;
            }
        }
        return null;
    }
    
    public void deleteMelody(String id) {
        for (int i = 0; i < melodyLibrary.size(); i++) {
            if (melodyLibrary.get(i).id.equals(id)) {
                melodyLibrary.remove(i);
                saveMelodiesToPrefs();
                return;
            }
        }
    }
    
    public void deleteChord(String id) {
        for (int i = 0; i < chordLibrary.size(); i++) {
            if (chordLibrary.get(i).id.equals(id)) {
                chordLibrary.remove(i);
                saveChordsToPrefs();
                return;
            }
        }
    }
    
    public void updateMelody(String id, String newName) {
        for (MelodyEntry entry : melodyLibrary) {
            if (entry.id.equals(id)) {
                entry.name = newName;
                saveMelodiesToPrefs();
                return;
            }
        }
    }
    
    public void updateChord(String id, String newName) {
        for (ChordEntry entry : chordLibrary) {
            if (entry.id.equals(id)) {
                entry.name = newName;
                saveChordsToPrefs();
                return;
            }
        }
    }
    
    public boolean melodyNameExists(String name) {
        for (MelodyEntry entry : melodyLibrary) {
            if (entry.name.equals(name)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean chordNameExists(String name) {
        for (ChordEntry entry : chordLibrary) {
            if (entry.name.equals(name)) {
                return true;
            }
        }
        return false;
    }
    
    public MusicData.Melody getMelody(String id) {
        MelodyEntry entry = getMelodyById(id);
        return entry != null ? entry.toMelody() : null;
    }
    
    public MusicData.ChordProgression getChordProgression(String id) {
        ChordEntry entry = getChordById(id);
        return entry != null ? entry.toChordProgression() : null;
    }
    
    public void deleteMelodyById(String id) {
        deleteMelody(id);
    }
    
    public void deleteChordProgressionById(String id) {
        deleteChord(id);
    }
    
    public void clearMelodyLibrary() {
        melodyLibrary.clear();
        saveMelodiesToPrefs();
    }
    
    public void clearChordLibrary() {
        chordLibrary.clear();
        saveChordsToPrefs();
    }
    
    public int getMelodyCount() {
        return melodyLibrary.size();
    }
    
    public int getChordCount() {
        return chordLibrary.size();
    }
    
    public List<SongEntry> getSongLibrary() {
        return new ArrayList<>(songLibrary);
    }
    
    public void addSong(SongEntry entry) {
        songLibrary.add(0, entry);
        saveSongsToPrefs();
    }
    
    public SongEntry getSongById(String id) {
        for (SongEntry entry : songLibrary) {
            if (entry.id.equals(id)) {
                return entry;
            }
        }
        return null;
    }
    
    public void deleteSong(String id) {
        for (int i = 0; i < songLibrary.size(); i++) {
            if (songLibrary.get(i).id.equals(id)) {
                songLibrary.remove(i);
                saveSongsToPrefs();
                return;
            }
        }
    }
    
    public void updateSong(String id, String newName) {
        for (SongEntry entry : songLibrary) {
            if (entry.id.equals(id)) {
                entry.name = newName;
                entry.updatedAt = System.currentTimeMillis();
                saveSongsToPrefs();
                return;
            }
        }
    }
    
    public boolean songNameExists(String name) {
        for (SongEntry entry : songLibrary) {
            if (entry.name.equals(name)) {
                return true;
            }
        }
        return false;
    }
    
    public int getSongCount() {
        return songLibrary.size();
    }
    
    public void saveSongsToPrefs() {
        JSONArray array = new JSONArray();
        for (SongEntry entry : songLibrary) {
            array.put(entry.toJson());
        }
        prefs.edit().putString(KEY_SONGS, array.toString()).apply();
    }
    
    public void saveMelodiesToPrefs() {
        JSONArray array = new JSONArray();
        for (MelodyEntry entry : melodyLibrary) {
            array.put(entry.toJson());
        }
        prefs.edit().putString(KEY_MELODIES, array.toString()).apply();
    }
    
    public void saveChordsToPrefs() {
        JSONArray array = new JSONArray();
        for (ChordEntry entry : chordLibrary) {
            array.put(entry.toJson());
        }
        prefs.edit().putString(KEY_CHORDS, array.toString()).apply();
    }
    
    private void loadFromPrefs() {
        melodyLibrary.clear();
        chordLibrary.clear();
        songLibrary.clear();
        
        String melodiesJson = prefs.getString(KEY_MELODIES, "[]");
        String chordsJson = prefs.getString(KEY_CHORDS, "[]");
        String songsJson = prefs.getString(KEY_SONGS, "[]");
        
        try {
            JSONArray melodiesArray = new JSONArray(melodiesJson);
            for (int i = 0; i < melodiesArray.length(); i++) {
                melodyLibrary.add(MelodyEntry.fromJson(melodiesArray.getJSONObject(i)));
            }
            
            JSONArray chordsArray = new JSONArray(chordsJson);
            for (int i = 0; i < chordsArray.length(); i++) {
                chordLibrary.add(ChordEntry.fromJson(chordsArray.getJSONObject(i)));
            }
            
            JSONArray songsArray = new JSONArray(songsJson);
            for (int i = 0; i < songsArray.length(); i++) {
                songLibrary.add(SongEntry.fromJson(songsArray.getJSONObject(i)));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to load from prefs", e);
        }
    }
}
