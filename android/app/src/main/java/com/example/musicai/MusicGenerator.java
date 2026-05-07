package com.example.musicai;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicGenerator {
    
    private static final String TAG = "MusicGenerator";
    private ModelConfig modelConfig;
    
    public MusicGenerator(Context context) {
        this.modelConfig = new ModelConfig(context);
    }
    
    public MusicData.Melody generateMelody(String style, int length, MusicData.Melody userMelody) throws IOException {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a ").append(style).append(" style melody with ").append(length).append(" notes. ");
        
        if (userMelody != null && !userMelody.notes.isEmpty()) {
            prompt.append("Use the following user-provided melody as a starting point and inspiration: ");
            try {
                JSONArray userNotesArray = new JSONArray();
                for (MusicData.Note note : userMelody.notes) {
                    userNotesArray.put(note.toJson());
                }
                prompt.append(userNotesArray.toString()).append(". ");
            } catch (JSONException e) {
                Log.e(TAG, "Failed to serialize user melody", e);
            }
            prompt.append("Expand and develop this melody while maintaining its musical character. ");
        }
        
        prompt.append("Return ONLY a valid JSON array of notes in this EXACT format: [{\"pitch\":\"C\",\"octave\":4,\"duration\":4,\"startTime\":0}]. ");
        prompt.append("Pitch options: C, C#, D, D#, E, F, F#, G, G#, A, A#, B. ");
        prompt.append("Duration values: 1=whole, 2=half, 4=quarter, 8=eighth, 16=sixteenth. ");
        prompt.append("Ensure the startTime values form a sequential timeline without gaps.");
        
        try {
            String response = modelConfig.generateContent(prompt.toString());
            Log.d(TAG, "Melody response: " + response);
            
            String cleanedResponse = extractJsonFromResponse(response);
            JSONArray jsonArray = new JSONArray(cleanedResponse);
            MusicData.Melody melody = new MusicData.Melody();
            melody.style = style;
            melody.name = "AI Generated " + style;
            
            int time = 0;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject noteObj = jsonArray.getJSONObject(i);
                MusicData.Note note = new MusicData.Note(
                    noteObj.optString("pitch", "C"),
                    noteObj.optInt("octave", 4),
                    noteObj.optInt("duration", 4),
                    noteObj.optInt("startTime", time)
                );
                if (note.startTime == 0 && i > 0) {
                    note.startTime = time;
                }
                melody.notes.add(note);
                time = note.startTime + note.duration;
            }
            
            return melody;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse melody JSON", e);
            throw new IOException("Failed to parse AI response. Please try again with different parameters.", e);
        }
    }
    
    public MusicData.ChordProgression generateChords(String style, int length, MusicData.ChordProgression userChords) throws IOException {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a ").append(style).append(" style chord progression with ").append(length).append(" chords. ");
        
        if (userChords != null && !userChords.chords.isEmpty()) {
            prompt.append("Use the following user-provided chord progression as a foundation: ");
            try {
                JSONArray userChordsArray = new JSONArray();
                for (MusicData.Chord chord : userChords.chords) {
                    userChordsArray.put(chord.toJson());
                }
                prompt.append(userChordsArray.toString()).append(". ");
            } catch (JSONException e) {
                Log.e(TAG, "Failed to serialize user chords", e);
            }
            prompt.append("Expand and develop this progression while maintaining its harmonic character. ");
        }
        
        prompt.append("Return ONLY a valid JSON array in this EXACT format: [{\"name\":\"C\",\"type\":\"major\",\"duration\":4,\"startTime\":0}]. ");
        prompt.append("Chord types: major, minor, seventh, diminished, augmented, sus2, sus4. ");
        prompt.append("Ensure the startTime values form a sequential timeline without gaps. ");
        prompt.append("Use common chord progressions appropriate for the ").append(style).append(" style.");
        
        try {
            String response = modelConfig.generateContent(prompt.toString());
            Log.d(TAG, "Chords response: " + response);
            
            String cleanedResponse = extractJsonFromResponse(response);
            JSONArray jsonArray = new JSONArray(cleanedResponse);
            MusicData.ChordProgression progression = new MusicData.ChordProgression();
            progression.style = style;
            progression.name = "AI Generated " + style;
            
            int time = 0;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject chordObj = jsonArray.getJSONObject(i);
                MusicData.Chord chord = new MusicData.Chord(
                    chordObj.optString("name", "C"),
                    chordObj.optString("type", "major"),
                    chordObj.optInt("duration", 4),
                    chordObj.optInt("startTime", time)
                );
                if (chord.startTime == 0 && i > 0) {
                    chord.startTime = time;
                }
                progression.chords.add(chord);
                time = chord.startTime + chord.duration;
            }
            
            return progression;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse chords JSON", e);
            throw new IOException("Failed to parse AI response. Please try again with different parameters.", e);
        }
    }
    
    public MusicData.Song generateSong(String style, MusicData.Melody melody, MusicData.ChordProgression chords) throws IOException {
        if (melody.notes.isEmpty() || chords.chords.isEmpty()) {
            throw new IOException("Both melody and chords must be provided for song generation");
        }
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a complete professional ").append(style).append(" song. ");
        prompt.append("Use and develop this melody: ");
        try {
            prompt.append(melody.toJson().toString());
        } catch (JSONException e) {
            Log.e(TAG, "Failed to serialize melody", e);
        }
        prompt.append(" and these chords: ");
        try {
            prompt.append(chords.toJson().toString());
        } catch (JSONException e) {
            Log.e(TAG, "Failed to serialize chords", e);
        }
        prompt.append(". ");
        prompt.append("Return ONLY a valid JSON object with title, artist, melody, and chords fields. ");
        prompt.append("EXACT format: {\"title\":\"Song Title\",\"artist\":\"Artist\",\"melody\":[],\"chords\":[]} ");
        prompt.append("Ensure melody and chords arrays use the same format as the input. ");
        prompt.append("Make the melody and chords work together harmonically in ").append(style).append(" style.");
        
        try {
            String response = modelConfig.generateContent(prompt.toString());
            Log.d(TAG, "Song response: " + response);
            
            String cleanedResponse = extractJsonFromResponse(response);
            JSONObject jsonObj = new JSONObject(cleanedResponse);
            MusicData.Song song = new MusicData.Song();
            song.title = jsonObj.optString("title", "AI Generated Song");
            song.artist = jsonObj.optString("artist", "MusicAI");
            song.style = style;
            song.melody = melody;
            song.chords = chords;
            
            if (jsonObj.has("melody") && !jsonObj.isNull("melody")) {
                try {
                    song.melody = MusicData.Melody.fromJson(jsonObj.getJSONObject("melody"));
                } catch (JSONException e) {
                    Log.w(TAG, "Failed to parse AI melody, using original", e);
                }
            }
            
            if (jsonObj.has("chords") && !jsonObj.isNull("chords")) {
                try {
                    song.chords = MusicData.ChordProgression.fromJson(jsonObj.getJSONObject("chords"));
                } catch (JSONException e) {
                    Log.w(TAG, "Failed to parse AI chords, using original", e);
                }
            }
            
            return song;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse song JSON", e);
            throw new IOException("Failed to parse AI response. Please try again with different parameters.", e);
        }
    }
    
    private String extractJsonFromResponse(String response) {
        int jsonStart = response.indexOf('[');
        int jsonObjectStart = response.indexOf('{');
        
        if (jsonStart == -1 && jsonObjectStart == -1) {
            return response;
        }
        
        if (jsonStart != -1 && (jsonObjectStart == -1 || jsonStart < jsonObjectStart)) {
            int jsonEnd = response.lastIndexOf(']');
            if (jsonEnd != -1) {
                return response.substring(jsonStart, jsonEnd + 1);
            }
        } else if (jsonObjectStart != -1) {
            int jsonEnd = response.lastIndexOf('}');
            if (jsonEnd != -1) {
                return response.substring(jsonObjectStart, jsonEnd + 1);
            }
        }
        
        return response;
    }
}
