package com.example.musicai;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MusicGenerator {
    
    private static final String TAG = "MusicGenerator";
    private ModelConfig modelConfig;
    private Random random;
    
    public MusicGenerator(Context context) {
        this.modelConfig = new ModelConfig(context);
        this.random = new Random();
    }
    
    public MusicData.Melody generateMelody(String style, int length) throws IOException {
        String prompt = String.format(
            "Generate a %s style melody with %d notes. Return only a JSON array of notes in this format:" +
            "[{\"pitch\":\"C\",\"octave\":4,\"duration\":4,\"startTime\":0}]. " +
            "Use standard music notation. Duration values: 1=whole, 2=half, 4=quarter, 8=eighth, 16=sixteenth.",
            style, length
        );
        
        try {
            String response = modelConfig.generateContent(prompt);
            Log.d(TAG, "Melody response: " + response);
            
            JSONArray jsonArray = new JSONArray(response);
            MusicData.Melody melody = new MusicData.Melody();
            melody.style = style;
            melody.name = "AI Generated " + style;
            
            int time = 0;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject noteObj = jsonArray.getJSONObject(i);
                MusicData.Note note = new MusicData.Note(
                    noteObj.optString("pitch", getRandomPitch()),
                    noteObj.optInt("octave", 4),
                    noteObj.optInt("duration", 4),
                    time
                );
                melody.notes.add(note);
                time += note.duration;
            }
            
            return melody;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse melody JSON", e);
            return generateRandomMelody(style, length);
        } catch (IOException e) {
            Log.e(TAG, "API call failed, generating random melody", e);
            return generateRandomMelody(style, length);
        }
    }
    
    public MusicData.ChordProgression generateChords(String style, int length) throws IOException {
        String prompt = String.format(
            "Generate a %s style chord progression with %d chords. Return only a JSON array in this format:" +
            "[{\"name\":\"C\",\"type\":\"major\",\"duration\":4,\"startTime\":0}]. " +
            "Use common chord progressions for the style.",
            style, length
        );
        
        try {
            String response = modelConfig.generateContent(prompt);
            Log.d(TAG, "Chords response: " + response);
            
            JSONArray jsonArray = new JSONArray(response);
            MusicData.ChordProgression progression = new MusicData.ChordProgression();
            progression.style = style;
            progression.name = "AI Generated " + style;
            
            int time = 0;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject chordObj = jsonArray.getJSONObject(i);
                MusicData.Chord chord = new MusicData.Chord(
                    chordObj.optString("name", getRandomChordName()),
                    chordObj.optString("type", "major"),
                    chordObj.optInt("duration", 4),
                    time
                );
                progression.chords.add(chord);
                time += chord.duration;
            }
            
            return progression;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse chords JSON", e);
            return generateRandomChords(style, length);
        } catch (IOException e) {
            Log.e(TAG, "API call failed, generating random chords", e);
            return generateRandomChords(style, length);
        }
    }
    
    public MusicData.Song generateSong(String style, MusicData.Melody melody, MusicData.ChordProgression chords) throws IOException {
        String melodyStr = melody.notes.toString();
        String chordsStr = chords.chords.toString();
        
        String prompt = String.format(
            "Generate a complete %s song. Use this melody: %s and these chords: %s. " +
            "Return only a JSON object with title, artist, melody, and chords fields. " +
            "Format: {\"title\":\"Song Title\",\"artist\":\"Artist\",\"melody\":[],\"chords\":[]}",
            style, melodyStr, chordsStr
        );
        
        try {
            String response = modelConfig.generateContent(prompt);
            Log.d(TAG, "Song response: " + response);
            
            JSONObject jsonObj = new JSONObject(response);
            MusicData.Song song = new MusicData.Song();
            song.title = jsonObj.optString("title", "AI Generated Song");
            song.artist = jsonObj.optString("artist", "MusicAI");
            song.style = style;
            song.melody = melody;
            song.chords = chords;
            
            return song;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse song JSON", e);
            return createDefaultSong(style, melody, chords);
        } catch (IOException e) {
            Log.e(TAG, "API call failed, creating default song", e);
            return createDefaultSong(style, melody, chords);
        }
    }
    
    private MusicData.Melody generateRandomMelody(String style, int length) {
        MusicData.Melody melody = new MusicData.Melody();
        melody.style = style;
        melody.name = "Random " + style;
        
        int[] octaves = {4, 4, 5, 4, 3, 4, 5, 4};
        int time = 0;
        
        for (int i = 0; i < length; i++) {
            String pitch = MusicData.PITCHES[random.nextInt(MusicData.PITCHES.length)];
            int octave = octaves[random.nextInt(octaves.length)];
            int duration = random.nextBoolean() ? 4 : 8;
            
            melody.notes.add(new MusicData.Note(pitch, octave, duration, time));
            time += duration;
        }
        
        return melody;
    }
    
    private MusicData.ChordProgression generateRandomChords(String style, int length) {
        MusicData.ChordProgression progression = new MusicData.ChordProgression();
        progression.style = style;
        progression.name = "Random " + style;
        
        String[] chordNames = {"C", "D", "E", "F", "G", "A", "B"};
        int time = 0;
        
        for (int i = 0; i < length; i++) {
            String name = chordNames[random.nextInt(chordNames.length)];
            String type = MusicData.CHORD_TYPES[random.nextInt(MusicData.CHORD_TYPES.length)];
            int duration = 4;
            
            progression.chords.add(new MusicData.Chord(name, type, duration, time));
            time += duration;
        }
        
        return progression;
    }
    
    private MusicData.Song createDefaultSong(String style, MusicData.Melody melody, MusicData.ChordProgression chords) {
        MusicData.Song song = new MusicData.Song();
        song.title = "Generated " + style + " Song";
        song.artist = "MusicAI";
        song.style = style;
        song.melody = melody;
        song.chords = chords;
        return song;
    }
    
    private String getRandomPitch() {
        return MusicData.PITCHES[random.nextInt(MusicData.PITCHES.length)];
    }
    
    private String getRandomChordName() {
        String[] names = {"C", "D", "E", "F", "G", "A", "B"};
        return names[random.nextInt(names.length)];
    }
}
