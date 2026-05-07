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
    
    public MusicData.Melody generateMelodyWithDescription(String style, int length, MusicData.Melody userMelody, String description) throws IOException {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名旋律作曲家。根据用户的需求创作一段").append(style).append("风格的单音主旋律。\n\n");
        
        if (description != null && !description.isEmpty()) {
            prompt.append("用户描述：").append(description).append("\n\n");
        }
        
        prompt.append("要求：\n");
        prompt.append("- 生成").append(length).append("个音符的旋律\n");
        
        if (userMelody != null && !userMelody.notes.isEmpty()) {
            prompt.append("- 参考用户提供的旋律动机：");
            try {
                JSONArray userNotesArray = new JSONArray();
                for (MusicData.Note note : userMelody.notes) {
                    userNotesArray.put(note.toJson());
                }
                prompt.append(userNotesArray.toString()).append("\n");
                prompt.append("- 在此基础上发展创作，保持其音乐特征\n");
            } catch (JSONException e) {
                Log.e(TAG, "Failed to serialize user melody", e);
            }
        }
        
        prompt.append("\n返回格式要求：\n");
        prompt.append("只返回纯JSON数组，不要包含任何解释文字。\n");
        prompt.append("格式：[{\"pitch\":\"C\",\"octave\":4,\"duration\":4,\"startTime\":0}]\n");
        prompt.append("- pitch可选：C, C#, D, D#, E, F, F#, G, G#, A, A#, B\n");
        prompt.append("- duration值：1=全音符, 2=二分, 4=四分, 8=八分, 16=十六分\n");
        prompt.append("- startTime表示开始时间，从0开始递增");
        
        try {
            AIResponse response = modelConfig.requestAI(prompt.toString());
            
            if (!response.isSuccess()) {
                throw new IOException(response.errorMessage);
            }
            
            Log.d(TAG, "Melody response: " + response.content);
            
            String cleanedResponse = extractJsonFromResponse(response.content);
            
            if (cleanedResponse.isEmpty() || cleanedResponse.equals("[]")) {
                throw new IOException("AI未生成有效旋律内容（返回空数组），请重试或稍后尝试");
            }
            
            JSONArray jsonArray = new JSONArray(cleanedResponse);
            
            if (jsonArray.length() == 0) {
                throw new IOException("AI未生成有效旋律内容，请重试或稍后尝试");
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
                    noteObj.optInt("startTime", time)
                );
                if (note.startTime == 0 && i > 0) {
                    note.startTime = time;
                }
                melody.notes.add(note);
                time = note.startTime + note.duration;
            }
            
            if (melody.notes.isEmpty()) {
                throw new IOException("生成失败：AI返回内容为空，请重试");
            }
            
            return melody;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse melody JSON", e);
            throw new IOException("解析AI返回失败，请检查API配置后重试。原始错误: " + e.getMessage(), e);
        }
    }
    
    public MusicData.ChordProgression generateChordsWithMelody(String style, int length, MusicData.ChordProgression userChords, MusicData.Melody melody) throws IOException {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一名专业的和声编配师。严格按要求生成和弦走向。\n\n");
        prompt.append("风格：").append(style).append("\n\n");
        
        if (melody != null && !melody.notes.isEmpty()) {
            prompt.append("主旋律音符：\n");
            try {
                JSONArray melodyArray = new JSONArray();
                for (MusicData.Note note : melody.notes) {
                    melodyArray.put(note.toJson());
                }
                prompt.append(melodyArray.toString()).append("\n\n");
            } catch (JSONException e) {
                Log.e(TAG, "Failed to serialize melody", e);
            }
        }
        
        prompt.append("要求：\n");
        prompt.append("- 生成").append(length).append("个和弦的和弦进行\n");
        prompt.append("- 和弦必须与旋律配合和谐\n");
        prompt.append("- 遵循").append(style).append("风格的典型和弦进行\n\n");
        
        prompt.append("强制输出格式：\n");
        prompt.append("只返回一个JSON数组，不包含任何其他文字。\n");
        prompt.append("格式：[{\"name\":\"C\",\"type\":\"major\",\"duration\":4,\"startTime\":0}]\n");
        prompt.append("- name：根音（C, C#, D, D#, E, F, F#, G, G#, A, A#, B）\n");
        prompt.append("- type：和弦类型（major, minor, seventh, diminished, augmented, sus2, sus4）\n");
        prompt.append("- duration：持续时值\n");
        prompt.append("- startTime：开始时间\n");
        
        try {
            AIResponse response = modelConfig.requestAI(prompt.toString());
            
            if (!response.isSuccess()) {
                throw new IOException(response.errorMessage);
            }
            
            Log.d(TAG, "Chords response: " + response.content);
            
            String cleanedResponse = extractJsonFromResponse(response.content);
            
            if (cleanedResponse.isEmpty() || cleanedResponse.equals("[]")) {
                throw new IOException("AI未生成有效和弦内容（返回空数组），请重试或稍后尝试");
            }
            
            JSONArray jsonArray = new JSONArray(cleanedResponse);
            
            if (jsonArray.length() == 0) {
                throw new IOException("AI未生成有效和弦内容，请重试或稍后尝试");
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
                    chordObj.optInt("startTime", time)
                );
                if (chord.startTime == 0 && i > 0) {
                    chord.startTime = time;
                }
                progression.chords.add(chord);
                time = chord.startTime + chord.duration;
            }
            
            if (progression.chords.isEmpty()) {
                throw new IOException("生成失败：AI返回内容为空，请重试");
            }
            
            return progression;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse chords JSON", e);
            throw new IOException("解析AI返回失败，请检查API配置后重试。原始错误: " + e.getMessage(), e);
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
        prompt.append("你是一名编曲师。根据下面的旋律和和弦，生成一首完整的多乐器伴奏歌曲。\n\n");
        prompt.append("风格：").append(style).append("\n\n");
        
        prompt.append("主旋律：\n");
        try {
            JSONArray melodyArray = new JSONArray();
            for (MusicData.Note note : melody.notes) {
                melodyArray.put(note.toJson());
            }
            prompt.append(melodyArray.toString()).append("\n\n");
        } catch (JSONException e) {
            Log.e(TAG, "Failed to serialize melody", e);
        }
        
        prompt.append("和弦走向：\n");
        try {
            JSONArray chordsArray = new JSONArray();
            for (MusicData.Chord chord : chords.chords) {
                chordsArray.put(chord.toJson());
            }
            prompt.append(chordsArray.toString()).append("\n\n");
        } catch (JSONException e) {
            Log.e(TAG, "Failed to serialize chords", e);
        }
        
        prompt.append("要求：\n");
        prompt.append("- 旋律与和弦要协调一致\n");
        prompt.append("- 创作一个合适的歌曲标题和艺术家名\n");
        prompt.append("- 使整体编曲符合").append(style).append("风格\n\n");
        
        prompt.append("返回格式要求：\n");
        prompt.append("只返回纯JSON对象，不要包含任何解释文字。\n");
        prompt.append("格式：{\"title\":\"歌曲标题\",\"artist\":\"艺术家名\"}\n");
        prompt.append("- title：歌曲标题（中文，2-10个字）\n");
        prompt.append("- artist：艺术家名（1-5个字）");
        
        try {
            AIResponse response = modelConfig.requestAI(prompt.toString());
            
            if (!response.isSuccess()) {
                throw new IOException(response.errorMessage);
            }
            
            Log.d(TAG, "Song response: " + response.content);
            
            String cleanedResponse = extractJsonFromResponse(response.content);
            
            if (cleanedResponse.isEmpty() || cleanedResponse.equals("{}") || cleanedResponse.equals("[]")) {
                throw new IOException("AI未生成有效歌曲信息，请调整条件后重试");
            }
            
            JSONObject jsonObj = new JSONObject(cleanedResponse);
            
            MusicData.Song song = new MusicData.Song();
            song.title = jsonObj.optString("title", "AI创作歌曲");
            song.artist = jsonObj.optString("artist", "MusicAI");
            song.style = style;
            song.melody = melody;
            song.chords = chords;
            
            return song;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse song JSON", e);
            throw new IOException("解析AI返回失败，请检查API配置后重试。原始错误: " + e.getMessage(), e);
        }
    }
    
    public MusicData.ChordProgression generateCustomChords(String style, int length, String keySignature, String mood, String description) throws IOException {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名和声编配师。根据以下条件独立生成一段和弦走向。\n\n");
        
        prompt.append("风格：").append(style).append("\n");
        
        if (keySignature != null && !keySignature.isEmpty()) {
            prompt.append("调性：").append(keySignature).append("\n");
        }
        
        if (mood != null && !mood.isEmpty()) {
            prompt.append("情绪：").append(mood).append("\n");
        }
        
        if (description != null && !description.isEmpty()) {
            prompt.append("描述：").append(description).append("\n");
        }
        
        prompt.append("\n要求：\n");
        prompt.append("- 生成").append(length).append("个和弦的和弦进行\n");
        prompt.append("- 遵循").append(style).append("风格的典型和弦进行\n");
        prompt.append("- 创造有音乐性的和弦连接\n\n");
        
        prompt.append("返回格式要求：\n");
        prompt.append("只返回纯JSON数组，不要包含任何解释文字。\n");
        prompt.append("格式：[{\"name\":\"C\",\"type\":\"major\",\"duration\":4,\"startTime\":0}]\n");
        prompt.append("- name：根音\n");
        prompt.append("- type：和弦类型（major, minor, seventh, diminished, augmented, sus2, sus4）\n");
        prompt.append("- duration：持续时值\n");
        prompt.append("- startTime：开始时间");
        
        try {
            AIResponse response = modelConfig.requestAI(prompt.toString());
            
            if (!response.isSuccess()) {
                throw new IOException(response.errorMessage);
            }
            
            Log.d(TAG, "Custom chords response: " + response.content);
            
            String cleanedResponse = extractJsonFromResponse(response.content);
            
            if (cleanedResponse.isEmpty() || cleanedResponse.equals("[]")) {
                throw new IOException("AI未生成有效和弦内容（返回空数组），请重试或稍后尝试");
            }
            
            JSONArray jsonArray = new JSONArray(cleanedResponse);
            
            if (jsonArray.length() == 0) {
                throw new IOException("AI未生成有效和弦内容，请重试或稍后尝试");
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
                    chordObj.optInt("startTime", time)
                );
                if (chord.startTime == 0 && i > 0) {
                    chord.startTime = time;
                }
                progression.chords.add(chord);
                time = chord.startTime + chord.duration;
            }
            
            if (progression.chords.isEmpty()) {
                throw new IOException("生成失败：AI返回内容为空，请重试");
            }
            
            return progression;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse custom chords JSON", e);
            throw new IOException("解析AI返回失败，请检查API配置后重试。原始错误: " + e.getMessage(), e);
        }
    }
    
    public MusicData.Melody generateMelody(String style, int length, MusicData.Melody userMelody) throws IOException {
        return generateMelodyWithDescription(style, length, userMelody, null);
    }
    
    public MusicData.ChordProgression generateChords(String style, int length, MusicData.ChordProgression userChords) throws IOException {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a ").append(style).append(" style chord progression with ").append(length).append(" chords. ");
        
        if (userChords != null && !userChords.chords.isEmpty()) {
            prompt.append("Use the following chord progression as a foundation: ");
            try {
                JSONArray userChordsArray = new JSONArray();
                for (MusicData.Chord chord : userChords.chords) {
                    userChordsArray.put(chord.toJson());
                }
                prompt.append(userChordsArray.toString()).append(". ");
            } catch (JSONException e) {
                Log.e(TAG, "Failed to serialize user chords", e);
            }
        }
        
        prompt.append("Return ONLY a valid JSON array: [{\"name\":\"C\",\"type\":\"major\",\"duration\":4,\"startTime\":0}]. ");
        prompt.append("Chord types: major, minor, seventh, diminished, augmented, sus2, sus4. ");
        prompt.append("Use common chord progressions for ").append(style).append(" style.");
        
        try {
            AIResponse response = modelConfig.requestAI(prompt.toString());
            
            if (!response.isSuccess()) {
                throw new IOException(response.errorMessage);
            }
            
            Log.d(TAG, "Chords response: " + response.content);
            
            String cleanedResponse = extractJsonFromResponse(response.content);
            
            if (cleanedResponse.isEmpty() || cleanedResponse.equals("[]")) {
                throw new IOException("AI未生成有效和弦内容（返回空数组），请重试或稍后尝试");
            }
            
            JSONArray jsonArray = new JSONArray(cleanedResponse);
            
            if (jsonArray.length() == 0) {
                throw new IOException("AI未生成有效和弦内容，请重试或稍后尝试");
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
                    chordObj.optInt("startTime", time)
                );
                if (chord.startTime == 0 && i > 0) {
                    chord.startTime = time;
                }
                progression.chords.add(chord);
                time = chord.startTime + chord.duration;
            }
            
            if (progression.chords.isEmpty()) {
                throw new IOException("生成失败：AI返回内容为空，请重试");
            }
            
            return progression;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse chords JSON", e);
            throw new IOException("解析AI返回失败，请检查API配置后重试。原始错误: " + e.getMessage(), e);
        }
    }
    
    public MusicData.Song generateSong(String style, MusicData.Melody melody, MusicData.ChordProgression chords) throws IOException {
        if (melody == null || melody.notes.isEmpty() || chords == null || chords.chords.isEmpty()) {
            throw new IOException("旋律和和弦都必须提供");
        }
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a complete ").append(style).append(" song. ");
        prompt.append("Use this melody: ");
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
        prompt.append("Return ONLY JSON: {\"title\":\"Song Title\",\"artist\":\"Artist\"}");
        
        try {
            AIResponse response = modelConfig.requestAI(prompt.toString());
            
            if (!response.isSuccess()) {
                throw new IOException(response.errorMessage);
            }
            
            Log.d(TAG, "Song response: " + response.content);
            
            String cleanedResponse = extractJsonFromResponse(response.content);
            
            if (cleanedResponse.isEmpty() || cleanedResponse.equals("{}") || cleanedResponse.equals("[]")) {
                throw new IOException("AI未生成有效歌曲信息，请调整条件后重试");
            }
            
            JSONObject jsonObj = new JSONObject(cleanedResponse);
            MusicData.Song song = new MusicData.Song();
            song.title = jsonObj.optString("title", "AI Generated Song");
            song.artist = jsonObj.optString("artist", "MusicAI");
            song.style = style;
            song.melody = melody;
            song.chords = chords;
            
            return song;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse song JSON", e);
            throw new IOException("解析AI返回失败，请检查API配置后重试。原始错误: " + e.getMessage(), e);
        }
    }
    
    private String extractJsonFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            Log.w(TAG, "Empty response received");
            return "";
        }
        
        String cleaned = response.trim();
        Log.d(TAG, "Original response: " + cleaned.substring(0, Math.min(200, cleaned.length())));
        
        cleaned = cleaned.replaceAll("(?s)```json\\s*", "");
        cleaned = cleaned.replaceAll("(?s)```javascript\\s*", "");
        cleaned = cleaned.replaceAll("(?s)```\\s*", "");
        cleaned = cleaned.replaceAll("`{3}", "");
        
        cleaned = cleaned.replaceAll("^[^\\[\\{]*", "");
        cleaned = cleaned.replaceAll("[^\\]\\}]*$", "");
        
        int jsonStart = cleaned.indexOf('[');
        int jsonObjectStart = cleaned.indexOf('{');
        
        if (jsonStart != -1 && (jsonObjectStart == -1 || jsonStart < jsonObjectStart)) {
            int jsonEnd = findMatchingBracket(cleaned, jsonStart, '[', ']');
            if (jsonEnd != -1 && jsonEnd > jsonStart) {
                String result = cleaned.substring(jsonStart, jsonEnd + 1);
                if (isValidJsonArray(result)) {
                    try { Log.d(TAG, "Extracted JSON array, length: " + new JSONArray(result).length()); } catch (Exception ignored) { }
                    return result;
                } else {
                    Log.w(TAG, "Found brackets but invalid JSON: " + result.substring(0, Math.min(100, result.length())));
                }
            }
        }
        
        if (jsonObjectStart != -1) {
            int jsonEnd = findMatchingBracket(cleaned, jsonObjectStart, '{', '}');
            if (jsonEnd != -1 && jsonEnd > jsonObjectStart) {
                String result = cleaned.substring(jsonObjectStart, jsonEnd + 1);
                if (isValidJsonObject(result)) {
                    Log.d(TAG, "Extracted JSON object");
                    return result;
                } else {
                    Log.w(TAG, "Found braces but invalid JSON: " + result.substring(0, Math.min(100, result.length())));
                }
            }
        }
        
        String[] lines = cleaned.split("\n");
        StringBuilder extractedJson = new StringBuilder("[");
        boolean foundNotes = false;
        
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("{") && line.endsWith("}")) {
                if (foundNotes) extractedJson.append(",");
                extractedJson.append(line);
                foundNotes = true;
            }
        }
        extractedJson.append("]");
        
        if (foundNotes && isValidJsonArray(extractedJson.toString())) {
            try { Log.d(TAG, "Extracted JSON via line parsing, length: " + new JSONArray(extractedJson.toString()).length()); } catch (Exception ignored) { }
            return extractedJson.toString();
        }
        
        Log.w(TAG, "Failed to extract valid JSON, returning empty");
        return "";
    }
    
    private int findMatchingBracket(String text, int startIndex, char openBracket, char closeBracket) {
        int count = 1;
        for (int i = startIndex + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == openBracket) {
                count++;
            } else if (c == closeBracket) {
                count--;
                if (count == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    private boolean isValidJsonArray(String json) {
        try {
            new JSONArray(json);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }
    
    private boolean isValidJsonObject(String json) {
        try {
            new JSONObject(json);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }
}
