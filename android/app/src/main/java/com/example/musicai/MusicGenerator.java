package com.example.musicai;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MusicGenerator {
    
    private static final String TAG = "MusicGenerator";
    private ModelConfig modelConfig;
    
    public MusicGenerator(Context context) {
        this.modelConfig = new ModelConfig(context);
    }
    
    public MusicData.Melody generateMelodyWithDescription(String style, int length, MusicData.Melody userMelody, String description) throws IOException {
        StringBuilder prompt = new StringBuilder();
        prompt.append("创作一段").append(style).append("风格的旋律。\n");
        prompt.append("请严格按以下JSON格式返回，不要有任何其他内容：\n\n");
        prompt.append("[{\"pitch\":\"C\",\"octave\":4,\"duration\":4}]\n\n");
        prompt.append("说明：pitch可选C C# D D# E F F# G G# A A# B，duration为时值(1=全 2=二分 4=四分 8=八分 16=十六分)。\n");
        prompt.append("请生成").append(length).append("个音符，确保旋律好听有起伏。");
        
        if (description != null && !description.isEmpty()) {
            prompt.append("\n用户要求：").append(description);
        }
        
        try {
            AIResponse response = modelConfig.requestAI(prompt.toString());
            
            if (!response.isSuccess()) {
                throw new IOException(response.errorMessage);
            }
            
            Log.d(TAG, "Melody response: " + response.content);
            
            JSONArray jsonArray = extractMusicJsonArray(response.content);
            
            if (jsonArray == null || jsonArray.length() == 0) {
                throw new IOException("AI未生成有效旋律内容，请重试");
            }
            
            MusicData.Melody melody = new MusicData.Melody();
            melody.id = String.valueOf(System.currentTimeMillis());
            melody.createdAt = System.currentTimeMillis();
            melody.style = style;
            melody.name = "AI Generated " + style;
            
            int time = 0;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject noteObj = jsonArray.getJSONObject(i);
                MusicData.Note note = new MusicData.Note(
                    noteObj.optString("pitch", "C"),
                    noteObj.optInt("octave", 4),
                    noteObj.optInt("duration", 4),
                    time
                );
                melody.notes.add(note);
                time = note.startTime + note.duration;
            }
            
            if (melody.notes.isEmpty()) {
                throw new IOException("生成失败：AI返回内容为空，请重试");
            }
            
            return melody;
        } catch (JSONException e) {
            Log.e(TAG, "Parse error", e);
            throw new IOException("解析AI返回失败，请重试", e);
        }
    }
    
    public MusicData.ChordProgression generateChordsWithMelody(String style, int length, MusicData.ChordProgression userChords, MusicData.Melody melody) throws IOException {
        StringBuilder prompt = new StringBuilder();
        prompt.append("根据以下旋律生成").append(length).append("个和弦的和弦进行。\n");
        prompt.append("请严格按JSON格式返回，不要有任何其他内容：\n\n");
        prompt.append("[{\"name\":\"C\",\"type\":\"major\",\"duration\":4}]\n\n");
        prompt.append("type可选：major minor seventh diminished augmented sus2 sus4\n");
        
        if (melody != null && !melody.notes.isEmpty()) {
            prompt.append("\n旋律：");
            try {
                JSONArray melodyArray = new JSONArray();
                for (MusicData.Note note : melody.notes) {
                    melodyArray.put(note.toJson());
                }
                prompt.append(melodyArray.toString());
            } catch (JSONException e) {
                Log.e(TAG, "Serialize error", e);
            }
        }
        
        try {
            AIResponse response = modelConfig.requestAI(prompt.toString());
            
            if (!response.isSuccess()) {
                throw new IOException(response.errorMessage);
            }
            
            Log.d(TAG, "Chords response: " + response.content);
            
            JSONArray jsonArray = extractMusicJsonArray(response.content);
            
            if (jsonArray == null || jsonArray.length() == 0) {
                throw new IOException("AI未生成有效和弦内容，请重试");
            }
            
            MusicData.ChordProgression progression = new MusicData.ChordProgression();
            progression.id = String.valueOf(System.currentTimeMillis());
            progression.createdAt = System.currentTimeMillis();
            progression.style = style;
            progression.name = "AI Generated " + style;
            
            int time = 0;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject chordObj = jsonArray.getJSONObject(i);
                MusicData.Chord chord = new MusicData.Chord(
                    chordObj.optString("name", "C"),
                    chordObj.optString("type", "major"),
                    chordObj.optInt("duration", 4),
                    time
                );
                progression.chords.add(chord);
                time = chord.startTime + chord.duration;
            }
            
            if (progression.chords.isEmpty()) {
                throw new IOException("生成失败：AI返回内容为空，请重试");
            }
            
            return progression;
        } catch (JSONException e) {
            Log.e(TAG, "Parse error", e);
            throw new IOException("解析AI返回失败，请重试", e);
        }
    }
    
    public MusicData.Song generateCompleteSong(String style, MusicData.Melody melody, MusicData.ChordProgression chords) throws IOException {
        if (melody == null || melody.notes.isEmpty()) {
            throw new IOException("旋律数据不能为空");
        }
        if (chords == null || chords.chords.isEmpty()) {
            throw new IOException("和弦数据不能为空");
        }
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("根据以下信息生成歌曲标题。\n");
        prompt.append("请严格按JSON格式返回，不要有任何其他内容：\n\n");
        prompt.append("{\"title\":\"歌曲标题\",\"artist\":\"艺术家名\"}\n\n");
        prompt.append("风格：").append(style);
        
        try {
            AIResponse response = modelConfig.requestAI(prompt.toString());
            
            if (!response.isSuccess()) {
                throw new IOException(response.errorMessage);
            }
            
            Log.d(TAG, "Song response: " + response.content);
            
            JSONObject jsonObj = extractMusicJsonObject(response.content);
            
            if (jsonObj == null) {
                throw new IOException("AI未生成有效歌曲信息，请重试");
            }
            
            MusicData.Song song = new MusicData.Song();
            song.title = jsonObj.optString("title", "AI创作歌曲");
            song.artist = jsonObj.optString("artist", "MusicAI");
            song.style = style;
            song.melody = melody;
            song.chords = chords;
            
            return song;
        } catch (JSONException e) {
            Log.e(TAG, "Parse error", e);
            throw new IOException("解析AI返回失败，请重试", e);
        }
    }
    
    public MusicData.ChordProgression generateCustomChords(String style, int length, String keySignature, String mood, String description) throws IOException {
        StringBuilder prompt = new StringBuilder();
        prompt.append("生成").append(length).append("个").append(style).append("风格的和弦。\n");
        prompt.append("请严格按JSON格式返回，不要有任何其他内容：\n\n");
        prompt.append("[{\"name\":\"C\",\"type\":\"major\",\"duration\":4}]\n\n");
        prompt.append("type可选：major minor seventh diminished augmented sus2 sus4");
        
        try {
            AIResponse response = modelConfig.requestAI(prompt.toString());
            
            if (!response.isSuccess()) {
                throw new IOException(response.errorMessage);
            }
            
            Log.d(TAG, "Custom chords response: " + response.content);
            
            JSONArray jsonArray = extractMusicJsonArray(response.content);
            
            if (jsonArray == null || jsonArray.length() == 0) {
                throw new IOException("AI未生成有效和弦内容，请重试");
            }
            
            MusicData.ChordProgression progression = new MusicData.ChordProgression();
            progression.id = String.valueOf(System.currentTimeMillis());
            progression.createdAt = System.currentTimeMillis();
            progression.style = style;
            progression.name = "自定义和弦 " + style;
            
            int time = 0;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject chordObj = jsonArray.getJSONObject(i);
                MusicData.Chord chord = new MusicData.Chord(
                    chordObj.optString("name", "C"),
                    chordObj.optString("type", "major"),
                    chordObj.optInt("duration", 4),
                    time
                );
                progression.chords.add(chord);
                time = chord.startTime + chord.duration;
            }
            
            if (progression.chords.isEmpty()) {
                throw new IOException("生成失败：AI返回内容为空，请重试");
            }
            
            return progression;
        } catch (JSONException e) {
            Log.e(TAG, "Parse error", e);
            throw new IOException("解析AI返回失败，请重试", e);
        }
    }
    
    public MusicData.Melody generateMelody(String style, int length, MusicData.Melody userMelody) throws IOException {
        return generateMelodyWithDescription(style, length, userMelody, null);
    }
    
    public MusicData.ChordProgression generateChords(String style, int length, MusicData.ChordProgression userChords) throws IOException {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate ").append(length).append(" chords for ").append(style).append(" style.\n");
        prompt.append("Return ONLY valid JSON array: [{\"name\":\"C\",\"type\":\"major\",\"duration\":4}]\n");
        prompt.append("type: major minor seventh diminished augmented sus2 sus4");
        
        try {
            AIResponse response = modelConfig.requestAI(prompt.toString());
            
            if (!response.isSuccess()) {
                throw new IOException(response.errorMessage);
            }
            
            Log.d(TAG, "Chords response: " + response.content);
            
            JSONArray jsonArray = extractMusicJsonArray(response.content);
            
            if (jsonArray == null || jsonArray.length() == 0) {
                throw new IOException("AI未生成有效和弦内容，请重试");
            }
            
            MusicData.ChordProgression progression = new MusicData.ChordProgression();
            progression.id = String.valueOf(System.currentTimeMillis());
            progression.createdAt = System.currentTimeMillis();
            progression.style = style;
            progression.name = "AI Generated " + style;
            
            int time = 0;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject chordObj = jsonArray.getJSONObject(i);
                MusicData.Chord chord = new MusicData.Chord(
                    chordObj.optString("name", "C"),
                    chordObj.optString("type", "major"),
                    chordObj.optInt("duration", 4),
                    time
                );
                progression.chords.add(chord);
                time = chord.startTime + chord.duration;
            }
            
            if (progression.chords.isEmpty()) {
                throw new IOException("生成失败：AI返回内容为空，请重试");
            }
            
            return progression;
        } catch (JSONException e) {
            Log.e(TAG, "Parse error", e);
            throw new IOException("解析AI返回失败，请重试", e);
        }
    }
    
    public MusicData.Song generateSong(String style, MusicData.Melody melody, MusicData.ChordProgression chords) throws IOException {
        return generateCompleteSong(style, melody, chords);
    }
    
    private JSONArray extractMusicJsonArray(String response) {
        if (response == null || response.trim().isEmpty()) {
            Log.w(TAG, "Empty response");
            return null;
        }
        
        String text = response.trim();
        Log.d(TAG, "Processing response length: " + text.length());
        
        text = text.replaceAll("(?i)```json", "");
        text = text.replaceAll("(?i)```javascript", "");
        text = text.replaceAll("(?i)```", "");
        text = text.replaceAll("`+", "");
        
        Pattern arrayPattern = Pattern.compile("\\[\\s*\\{[^\\}]+\\}\\s*(,\\s*\\{[^\\}]+\\}\\s*)*\\]");
        Matcher arrayMatcher = arrayPattern.matcher(text);
        
        if (arrayMatcher.find()) {
            String found = arrayMatcher.group();
            try {
                JSONArray arr = new JSONArray(found);
                Log.d(TAG, "Extracted array with " + arr.length() + " items");
                return arr;
            } catch (JSONException e) {
                Log.w(TAG, "Array found but parse failed: " + found.substring(0, Math.min(100, found.length())));
            }
        }
        
        int firstBracket = text.indexOf('[');
        int lastBracket = text.lastIndexOf(']');
        
        if (firstBracket != -1 && lastBracket > firstBracket) {
            String potential = text.substring(firstBracket, lastBracket + 1);
            try {
                JSONArray arr = new JSONArray(potential);
                if (arr.length() > 0) {
                    Log.d(TAG, "Extracted array from brackets, length: " + arr.length());
                    return arr;
                }
            } catch (JSONException e) {
            }
        }
        
        String[] lines = text.split("\n");
        StringBuilder jsonBuilder = new StringBuilder("[");
        boolean found = false;
        
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("{") && line.endsWith("}")) {
                if (found) jsonBuilder.append(",");
                jsonBuilder.append(line);
                found = true;
            }
        }
        jsonBuilder.append("]");
        
        if (found) {
            try {
                JSONArray arr = new JSONArray(jsonBuilder.toString());
                Log.d(TAG, "Extracted array from lines, length: " + arr.length());
                return arr;
            } catch (JSONException e) {
            }
        }
        
        Log.w(TAG, "Failed to extract JSON array");
        return null;
    }
    
    private JSONObject extractMusicJsonObject(String response) {
        if (response == null || response.trim().isEmpty()) {
            Log.w(TAG, "Empty response");
            return null;
        }
        
        String text = response.trim();
        
        text = text.replaceAll("(?i)```json", "");
        text = text.replaceAll("(?i)```javascript", "");
        text = text.replaceAll("(?i)```", "");
        text = text.replaceAll("`+", "");
        
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        
        if (firstBrace != -1 && lastBrace > firstBrace) {
            String potential = text.substring(firstBrace, lastBrace + 1);
            try {
                JSONObject obj = new JSONObject(potential);
                Log.d(TAG, "Extracted JSON object");
                return obj;
            } catch (JSONException e) {
            }
        }
        
        Pattern objPattern = Pattern.compile("\\{[^{}]*title[^{}]*\\}");
        Matcher objMatcher = objPattern.matcher(text);
        
        if (objMatcher.find()) {
            try {
                JSONObject obj = new JSONObject(objMatcher.group());
                Log.d(TAG, "Extracted JSON object via pattern");
                return obj;
            } catch (JSONException e) {
            }
        }
        
        Log.w(TAG, "Failed to extract JSON object");
        return null;
    }
}
