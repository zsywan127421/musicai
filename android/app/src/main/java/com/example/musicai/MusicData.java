package com.example.musicai;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MusicData {
    
    public static class Note {
        public String pitch;
        public int octave;
        public int duration;
        public int startTime;
        
        public Note(String pitch, int octave, int duration, int startTime) {
            this.pitch = pitch;
            this.octave = octave;
            this.duration = duration;
            this.startTime = startTime;
        }
        
        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("pitch", pitch);
            obj.put("octave", octave);
            obj.put("duration", duration);
            obj.put("startTime", startTime);
            return obj;
        }
        
        public static Note fromJson(JSONObject obj) throws JSONException {
            return new Note(
                obj.getString("pitch"),
                obj.getInt("octave"),
                obj.getInt("duration"),
                obj.getInt("startTime")
            );
        }
        
        @Override
        public String toString() {
            return pitch + octave + ":" + duration;
        }
    }
    
    public static class Melody {
        public List<Note> notes;
        public String style;
        public String name;
        
        public Melody() {
            notes = new ArrayList<>();
            style = "pop";
            name = "Untitled";
        }
        
        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("style", style);
            JSONArray notesArray = new JSONArray();
            for (Note note : notes) {
                notesArray.put(note.toJson());
            }
            obj.put("notes", notesArray);
            return obj;
        }
        
        public static Melody fromJson(JSONObject obj) throws JSONException {
            Melody melody = new Melody();
            melody.name = obj.optString("name", "Untitled");
            melody.style = obj.optString("style", "pop");
            JSONArray notesArray = obj.getJSONArray("notes");
            for (int i = 0; i < notesArray.length(); i++) {
                melody.notes.add(Note.fromJson(notesArray.getJSONObject(i)));
            }
            return melody;
        }
    }
    
    public static class Chord {
        public String name;
        public String type;
        public int duration;
        public int startTime;
        
        public Chord(String name, String type, int duration, int startTime) {
            this.name = name;
            this.type = type;
            this.duration = duration;
            this.startTime = startTime;
        }
        
        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("type", type);
            obj.put("duration", duration);
            obj.put("startTime", startTime);
            return obj;
        }
        
        public static Chord fromJson(JSONObject obj) throws JSONException {
            return new Chord(
                obj.getString("name"),
                obj.optString("type", "major"),
                obj.getInt("duration"),
                obj.getInt("startTime")
            );
        }
        
        @Override
        public String toString() {
            return name + "(" + type + "):" + duration;
        }
    }
    
    public static class ChordProgression {
        public List<Chord> chords;
        public String style;
        public String name;
        
        public ChordProgression() {
            chords = new ArrayList<>();
            style = "pop";
            name = "Untitled";
        }
        
        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("style", style);
            JSONArray chordsArray = new JSONArray();
            for (Chord chord : chords) {
                chordsArray.put(chord.toJson());
            }
            obj.put("chords", chordsArray);
            return obj;
        }
        
        public static ChordProgression fromJson(JSONObject obj) throws JSONException {
            ChordProgression progression = new ChordProgression();
            progression.name = obj.optString("name", "Untitled");
            progression.style = obj.optString("style", "pop");
            JSONArray chordsArray = obj.getJSONArray("chords");
            for (int i = 0; i < chordsArray.length(); i++) {
                progression.chords.add(Chord.fromJson(chordsArray.getJSONObject(i)));
            }
            return progression;
        }
    }
    
    public static class Song {
        public String title;
        public String artist;
        public String style;
        public Melody melody;
        public ChordProgression chords;
        public List<Note> bassLine;
        public List<Note> drums;
        public long createdAt;
        
        public Song() {
            title = "Untitled Song";
            artist = "Unknown";
            style = "pop";
            melody = new Melody();
            chords = new ChordProgression();
            bassLine = new ArrayList<>();
            drums = new ArrayList<>();
            createdAt = System.currentTimeMillis();
        }
        
        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("title", title);
            obj.put("artist", artist);
            obj.put("style", style);
            obj.put("melody", melody.toJson());
            obj.put("chords", chords.toJson());
            obj.put("createdAt", createdAt);
            return obj;
        }
        
        public static Song fromJson(JSONObject obj) throws JSONException {
            Song song = new Song();
            song.title = obj.optString("title", "Untitled Song");
            song.artist = obj.optString("artist", "Unknown");
            song.style = obj.optString("style", "pop");
            song.melody = Melody.fromJson(obj.getJSONObject("melody"));
            song.chords = ChordProgression.fromJson(obj.getJSONObject("chords"));
            song.createdAt = obj.optLong("createdAt", System.currentTimeMillis());
            return song;
        }
    }
    
    public static final String[] PITCHES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    public static final String[] CHORD_TYPES = {"major", "minor", "seventh", "diminished", "augmented", "sus2", "sus4"};
    public static final String[] MUSIC_STYLES = {"pop", "classical", "jazz", "electronic", "rock", "blues", "country"};
    
    public static int pitchToMidi(String pitch, int octave) {
        int[] offsets = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        for (int i = 0; i < PITCHES.length; i++) {
            if (PITCHES[i].equals(pitch)) {
                return (octave + 1) * 12 + offsets[i];
            }
        }
        return 60;
    }
    
    public static String midiToPitch(int midi) {
        int octave = (midi / 12) - 1;
        int offset = midi % 12;
        return PITCHES[offset] + octave;
    }
}
