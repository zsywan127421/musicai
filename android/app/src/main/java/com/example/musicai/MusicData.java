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
        public String id;
        public long createdAt;
        public String name;
        public String style;
        public List<Note> notes;
    
    public Melody() {
        this.id = java.util.UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.name = "Untitled";
        this.style = "pop";
        this.notes = new ArrayList<>();
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
        public String id;
        public long createdAt;
        public String name;
        public String style;
        public List<Chord> chords;
    
    public ChordProgression() {
        this.id = java.util.UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.name = "Untitled";
        this.style = "pop";
        this.chords = new ArrayList<>();
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
        public List<Segment> segments;
        public int totalDurationMs;
        public List<Note> bassLine;
        public List<Note> drums;
        public int bpm;
        public long createdAt;
        
        public Song() {
            title = "Untitled Song";
            artist = "Unknown";
            style = "pop";
            melody = new Melody();
            chords = new ChordProgression();
            segments = new ArrayList<>();
            bassLine = new ArrayList<>();
            drums = new ArrayList<>();
            bpm = 120;
            createdAt = System.currentTimeMillis();
            totalDurationMs = 0;
        }
        
        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("title", title);
            obj.put("artist", artist);
            obj.put("style", style);
            obj.put("melody", melody.toJson());
            obj.put("chords", chords.toJson());
            obj.put("totalDurationMs", totalDurationMs);
            obj.put("createdAt", createdAt);
            
            JSONArray segmentsArray = new JSONArray();
            for (Segment segment : segments) {
                segmentsArray.put(segment.toJson());
            }
            obj.put("segments", segmentsArray);
            
            return obj;
        }
        
        public static Song fromJson(JSONObject obj) throws JSONException {
            Song song = new Song();
            song.title = obj.optString("title", "Untitled Song");
            song.artist = obj.optString("artist", "Unknown");
            song.style = obj.optString("style", "pop");
            song.totalDurationMs = obj.optInt("totalDurationMs", 0);
            song.createdAt = obj.optLong("createdAt", System.currentTimeMillis());
            
            if (obj.has("melody")) {
                song.melody = Melody.fromJson(obj.getJSONObject("melody"));
            }
            if (obj.has("chords")) {
                song.chords = ChordProgression.fromJson(obj.getJSONObject("chords"));
            }
            if (obj.has("segments")) {
                JSONArray segmentsArray = obj.getJSONArray("segments");
                for (int i = 0; i < segmentsArray.length(); i++) {
                    song.segments.add(Segment.fromJson(segmentsArray.getJSONObject(i)));
                }
            }
            
            return song;
        }
    }
    
    public static class Segment {
        public int index;
        public Melody melody;
        public ChordProgression chord;
        public int startTimeMs;
        public int durationMs;
        
        public Segment() {
            this.index = 0;
            this.melody = new Melody();
            this.chord = new ChordProgression();
            this.startTimeMs = 0;
            this.durationMs = 0;
        }
        
        public Segment(int index, Melody melody, ChordProgression chord, int startTimeMs) {
            this.index = index;
            this.melody = melody;
            this.chord = chord;
            this.startTimeMs = startTimeMs;
            this.durationMs = calculateDuration(melody);
        }
        
        private int calculateDuration(Melody melody) {
            if (melody == null || melody.notes == null || melody.notes.isEmpty()) {
                return 8000;
            }
            int lastEnd = 0;
            for (Note note : melody.notes) {
                int end = note.startTime + note.duration;
                if (end > lastEnd) lastEnd = end;
            }
            return lastEnd * (60000 / 120);
        }
        
        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("index", index);
            obj.put("melody", melody.toJson());
            obj.put("chord", chord.toJson());
            obj.put("startTimeMs", startTimeMs);
            obj.put("durationMs", durationMs);
            return obj;
        }
        
        public static Segment fromJson(JSONObject obj) throws JSONException {
            Segment segment = new Segment();
            segment.index = obj.optInt("index", 0);
            if (obj.has("melody")) {
                segment.melody = Melody.fromJson(obj.getJSONObject("melody"));
            }
            if (obj.has("chord")) {
                segment.chord = ChordProgression.fromJson(obj.getJSONObject("chord"));
            }
            segment.startTimeMs = obj.optInt("startTimeMs", 0);
            segment.durationMs = obj.optInt("durationMs", 8000);
            return segment;
        }
    }
    
    public static final String[] PITCHES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    public static final String[] CHORD_TYPES = {"major", "minor", "seventh", "diminished", "augmented", "sus2", "sus4"};
    public static final String[] MUSIC_STYLES = {"流行", "古典", "爵士", "电子", "摇滚", "蓝调", "乡村"};
    
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
