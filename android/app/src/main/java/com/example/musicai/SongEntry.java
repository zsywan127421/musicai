package com.example.musicai;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SongEntry {

    private static final String TAG = "SongEntry";

    public String id;
    public String name;
    public String style;
    public String sourceMelodyId;
    public String sourceMelodyName;
    public String sourceChordId;
    public String sourceChordName;
    public List<SegmentData> segments;
    public int totalDurationMs;
    public int bpm;
    public String keySignature;
    public long createdAt;
    public long updatedAt;

    public static class SegmentData {
        public int index;
        public String melodyJson;
        public String chordJson;
        public int startTimeMs;
        public int durationMs;

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("index", index);
                obj.put("melodyJson", melodyJson);
                obj.put("chordJson", chordJson);
                obj.put("startTimeMs", startTimeMs);
                obj.put("durationMs", durationMs);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to serialize segment", e);
            }
            return obj;
        }

        public static SegmentData fromJson(JSONObject obj) {
            SegmentData segment = new SegmentData();
            segment.index = obj.optInt("index", 0);
            segment.melodyJson = obj.optString("melodyJson", "[]");
            segment.chordJson = obj.optString("chordJson", "{}");
            segment.startTimeMs = obj.optInt("startTimeMs", 0);
            segment.durationMs = obj.optInt("durationMs", 0);
            return segment;
        }
    }

    public SongEntry() {
        this.id = generateId();
        this.segments = new ArrayList<>();
        this.bpm = 120;
        this.keySignature = "C";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public SongEntry(String name, String style) {
        this();
        this.name = name;
        this.style = style;
    }

    private static String generateId() {
        return "song_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 10000);
    }

    public void addSegment(MusicData.Melody melody, MusicData.ChordProgression chord, int startTimeMs) {
        SegmentData segment = new SegmentData();
        segment.index = segments.size();
        segment.melodyJson = melodyToJson(melody);
        segment.chordJson = chordToJson(chord);
        segment.startTimeMs = startTimeMs;
        segment.durationMs = calculateMelodyDuration(melody);
        segments.add(segment);
        totalDurationMs = startTimeMs + segment.durationMs;
    }

    private String melodyToJson(MusicData.Melody melody) {
        JSONArray array = new JSONArray();
        if (melody != null && melody.notes != null) {
            for (MusicData.Note note : melody.notes) {
                try {
                    array.put(note.toJson());
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to add note to json", e);
                }
            }
        }
        return array.toString();
    }

    private String chordToJson(MusicData.ChordProgression chord) {
        if (chord == null) return "{}";
        JSONObject obj = new JSONObject();
        try {
            obj.put("name", chord.name);
            obj.put("style", chord.style);
            JSONArray array = new JSONArray();
            if (chord.chords != null) {
                for (MusicData.Chord c : chord.chords) {
                    array.put(c.toJson());
                }
            }
            obj.put("chords", array);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to serialize chord", e);
        }
        return obj.toString();
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
        int actualBpm = (bpm > 0) ? bpm : 120;
        return lastNoteEnd * (60000 / actualBpm);
    }

    public MusicData.Melody getSegmentMelody(int index) {
        if (index < 0 || index >= segments.size()) return null;
        SegmentData segment = segments.get(index);
        MusicData.Melody melody = new MusicData.Melody();
        melody.name = "Segment " + (index + 1);
        melody.style = style;
        try {
            JSONArray array = new JSONArray(segment.melodyJson);
            int time = 0;
            for (int i = 0; i < array.length(); i++) {
                JSONObject noteObj = array.getJSONObject(i);
                MusicData.Note note = new MusicData.Note(
                    noteObj.optString("pitch", "C"),
                    noteObj.optInt("octave", 4),
                    noteObj.optInt("duration", 4),
                    time
                );
                melody.notes.add(note);
                time = note.startTime + note.duration;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse melody json", e);
        }
        return melody;
    }

    public MusicData.ChordProgression getSegmentChord(int index) {
        if (index < 0 || index >= segments.size()) return null;
        SegmentData segment = segments.get(index);
        MusicData.ChordProgression chord = new MusicData.ChordProgression();
        chord.name = "Segment " + (index + 1);
        chord.style = style;
        try {
            JSONObject obj = new JSONObject(segment.chordJson);
            chord.name = obj.optString("name", chord.name);
            chord.style = obj.optString("style", chord.style);
            if (obj.has("chords")) {
                JSONArray array = obj.getJSONArray("chords");
                int time = 0;
                for (int i = 0; i < array.length(); i++) {
                    JSONObject chordObj = array.getJSONObject(i);
                    MusicData.Chord c = new MusicData.Chord(
                        chordObj.optString("name", "C"),
                        chordObj.optString("type", "major"),
                        chordObj.optInt("duration", 4),
                        time
                    );
                    chord.chords.add(c);
                    time = c.startTime + c.duration;
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse chord json", e);
        }
        return chord;
    }

    public MusicData.Song toMusicDataSong() {
        MusicData.Song song = new MusicData.Song();
        song.title = name;
        song.artist = "MusicAI";
        song.style = style;
        song.bpm = bpm;

        if (!segments.isEmpty()) {
            MusicData.Melody fullMelody = new MusicData.Melody();
            fullMelody.name = name;
            fullMelody.style = style;

            MusicData.ChordProgression fullChord = new MusicData.ChordProgression();
            fullChord.name = name;
            fullChord.style = style;

            int time = 0;
            for (int i = 0; i < segments.size(); i++) {
                MusicData.Melody segmentMelody = getSegmentMelody(i);
                MusicData.ChordProgression segmentChord = getSegmentChord(i);

                if (segmentMelody != null && segmentMelody.notes != null) {
                    for (MusicData.Note note : segmentMelody.notes) {
                        note.startTime += time;
                        fullMelody.notes.add(note);
                    }
                }

                if (segmentChord != null && segmentChord.chords != null) {
                    for (MusicData.Chord c : segmentChord.chords) {
                        c.startTime += time;
                        fullChord.chords.add(c);
                    }
                }

                time += segments.get(i).durationMs;
            }

            song.melody = fullMelody;
            song.chords = fullChord;
        }

        return song;
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("name", name);
            obj.put("style", style);
            obj.put("sourceMelodyId", sourceMelodyId);
            obj.put("sourceMelodyName", sourceMelodyName);
            obj.put("sourceChordId", sourceChordId);
            obj.put("sourceChordName", sourceChordName);
            obj.put("bpm", bpm);
            obj.put("keySignature", keySignature);
            obj.put("totalDurationMs", totalDurationMs);
            obj.put("createdAt", createdAt);
            obj.put("updatedAt", updatedAt);

            JSONArray segmentsArray = new JSONArray();
            for (SegmentData segment : segments) {
                segmentsArray.put(segment.toJson());
            }
            obj.put("segments", segmentsArray);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to serialize song", e);
        }
        return obj;
    }

    public static SongEntry fromJson(JSONObject obj) {
        SongEntry entry = new SongEntry();
        try {
            entry.id = obj.optString("id", generateId());
            entry.name = obj.optString("name", "未命名歌曲");
            entry.style = obj.optString("style", "流行");
            entry.sourceMelodyId = obj.optString("sourceMelodyId", "");
            entry.sourceMelodyName = obj.optString("sourceMelodyName", "");
            entry.sourceChordId = obj.optString("sourceChordId", "");
            entry.sourceChordName = obj.optString("sourceChordName", "");
            entry.bpm = obj.optInt("bpm", 120);
            entry.keySignature = obj.optString("keySignature", "C");
            entry.totalDurationMs = obj.optInt("totalDurationMs", 0);
            entry.createdAt = obj.optLong("createdAt", System.currentTimeMillis());
            entry.updatedAt = obj.optLong("updatedAt", System.currentTimeMillis());

            if (obj.has("segments")) {
                JSONArray segmentsArray = obj.getJSONArray("segments");
                for (int i = 0; i < segmentsArray.length(); i++) {
                    entry.segments.add(SegmentData.fromJson(segmentsArray.getJSONObject(i)));
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse song entry", e);
        }
        return entry;
    }

    public String getPreviewText() {
        StringBuilder sb = new StringBuilder();
        sb.append("来源旋律: ").append(sourceMelodyName != null ? sourceMelodyName : "未知").append("\n");
        sb.append("来源和弦: ").append(sourceChordName != null ? sourceChordName : "未知").append("\n");
        sb.append("段落数: ").append(segments.size()).append("\n");
        sb.append("时长: ").append(formatDuration(totalDurationMs));
        return sb.toString();
    }

    public String getSegmentSummary() {
        if (segments.isEmpty()) return "无段落";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) sb.append(" → ");
            sb.append("段").append(i + 1);
        }
        return sb.toString();
    }

    private String formatDuration(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
