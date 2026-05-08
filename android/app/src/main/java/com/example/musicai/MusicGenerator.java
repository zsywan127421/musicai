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
    
    private static final String MUSIC_SYSTEM_PROMPT = 
        "你是一个专业的音乐创作助手，擅长创作高质量的旋律和和弦进行。\n" +
        "重要规则：\n" +
        "1. 只返回有效的JSON数组或JSON对象，不要包含任何解释性文字\n" +
        "2. 不要使用Markdown代码块标记（如```json```）\n" +
        "3. 不要有任何开场白或自我介绍\n" +
        "4. 直接输出JSON格式的数据\n" +
        "5. 确保返回的JSON数组至少包含1个有效音符/和弦";
    
    public MusicGenerator(Context context) {
        this.modelConfig = new ModelConfig(context);
    }
    
    public MusicData.Melody generateMelodyWithDescription(String style, int length, MusicData.Melody userMelody, String description) throws IOException {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为").append(style).append("风格创作一段旋律。\n\n");
        prompt.append("【输出格式要求】\n");
        prompt.append("必须返回一个JSON数组，格式如下：\n");
        prompt.append("[{\"pitch\":\"C\",\"octave\":4,\"duration\":4},{\"pitch\":\"E\",\"octave\":4,\"duration\":4}]\n\n");
        prompt.append("【字段说明】\n");
        prompt.append("- pitch: 音高，取值范围 C C# D D# E F F# G G# A A# B\n");
        prompt.append("- octave: 八度，取值范围 2-7\n");
        prompt.append("- duration: 时值，1=全音符 2=二分 4=四分 8=八分 16=十六分\n\n");
        prompt.append("【创作要求】\n");
        prompt.append("- 请创作").append(length).append("个音符的旋律\n");
        prompt.append("- 旋律要有起伏，节奏要有变化\n");
        prompt.append("- 确保音符数据完整且格式正确\n");
        prompt.append("- 旋律应该优美动听，有音乐性\n\n");
        prompt.append("直接输出JSON数组，不要任何其他文字：");
        
        if (description != null && !description.isEmpty()) {
            prompt.append("\n\n用户补充要求：").append(description);
        }
        
        try {
            AIResponse response = modelConfig.requestAIWithSystemPrompt(prompt.toString(), MUSIC_SYSTEM_PROMPT);
            
            if (!response.isSuccess()) {
                throw new IOException(response.errorMessage);
            }
            
            Log.d(TAG, "Melody response: " + response.content);
            
            JSONArray jsonArray = extractMusicJsonArray(response.content);
            
            if (jsonArray == null || jsonArray.length() == 0) {
                throw new IOException("AI未生成有效旋律内容，请重试");
            }
            
            if (jsonArray.length() < 2) {
                throw new IOException("AI生成的旋律过短（少于2个音符），请重试");
            }
            
            MusicData.Melody melody = new MusicData.Melody();
            melody.id = String.valueOf(System.currentTimeMillis());
            melody.createdAt = System.currentTimeMillis();
            melody.style = style;
            melody.name = "AI Generated " + style;
            
            int time = 0;
            boolean hasValidNote = false;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject noteObj = jsonArray.getJSONObject(i);
                String pitch = noteObj.optString("pitch", "");
                int octave = noteObj.optInt("octave", 0);
                int duration = noteObj.optInt("duration", 0);
                
                if (pitch.isEmpty() || octave < 2 || octave > 7 || duration <= 0) {
                    Log.w(TAG, "Skipping invalid note at index " + i);
                    continue;
                }
                
                MusicData.Note note = new MusicData.Note(pitch, octave, duration, time);
                melody.notes.add(note);
                time = note.startTime + note.duration;
                hasValidNote = true;
            }
            
            if (!hasValidNote || melody.notes.isEmpty()) {
                throw new IOException("生成失败：AI返回内容格式无效，请重试");
            }
            
            return melody;
        } catch (JSONException e) {
            Log.e(TAG, "Parse error", e);
            throw new IOException("解析AI返回失败: " + e.getMessage() + "，请重试");
        }
    }
    
    public MusicData.ChordProgression generateChordsWithMelody(String style, int length, MusicData.ChordProgression userChords, MusicData.Melody melody) throws IOException {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下旋律生成对应的和弦进行。\n\n");
        
        if (melody != null && !melody.notes.isEmpty()) {
            prompt.append("【旋律分析】\n");
            
            String firstPitch = melody.notes.get(0).pitch;
            String lastPitch = melody.notes.get(melody.notes.get(melody.notes.size() - 1).pitch + "").pitch;
            int firstOctave = melody.notes.get(0).octave;
            boolean startsHigh = firstPitch.contains("#") || firstOctave >= 5;
            boolean endsOnRoot = lastPitch.equals("C") || lastPitch.equals("F") || lastPitch.equals("G");
            
            if (startsHigh && endsOnRoot) {
                prompt.append("旋律特点：大调风格（以高音开始，结束在主音）\n");
                prompt.append("请使用大调功能和声：主和弦(I)、下属和弦(IV)、属和弦(V)为主\n\n");
            } else if (!startsHigh && lastPitch.equals("A")) {
                prompt.append("旋律特点：小调风格（以低音开始）\n");
                prompt.append("请使用小调功能和声：主和弦(i)、下属和弦(iv)、属和弦(V)为主\n\n");
            } else {
                prompt.append("旋律特点：混合风格\n");
                prompt.append("请使用灵活的和声进行\n\n");
            }
            
            prompt.append("【Key信息】\n");
            prompt.append("请分析旋律确定调性后，选择合适的和弦\n\n");
        }
        
        prompt.append("【输出格式要求】\n");
        prompt.append("必须返回一个JSON数组，格式如下：\n");
        prompt.append("[{\"name\":\"C\",\"type\":\"major\",\"duration\":4},{\"name\":\"G\",\"type\":\"major\",\"duration\":4}]\n\n");
        prompt.append("【字段说明】\n");
        prompt.append("- name: 根音，取值范围 C C# D D# E F F# G G# A A# B\n");
        prompt.append("- type: 和弦类型 major minor seventh diminished augmented sus2 sus4\n");
        prompt.append("- duration: 时值（以四分音符为单位）\n\n");
        prompt.append("【和弦功能圈】\n");
        prompt.append("C大调常用进行：C - G - Am - F (I - V - vi - IV)\n");
        prompt.append("C大调经典进行：Am - F - C - G (vi - IV - I - V)\n");
        prompt.append("C大调下行进行：C - Em - F - G (I - iii - IV - V)\n\n");
        prompt.append("【创作要求】\n");
        prompt.append("- 请生成").append(length).append("个和弦\n");
        prompt.append("- 和弦进行要符合音乐理论，与旋律风格匹配\n");
        prompt.append("- 优先使用功能圈进行（I-V-vi-IV 或变体）\n");
        prompt.append("- 确保每个和弦都与旋律的调性协调\n\n");
        prompt.append("直接输出JSON数组，不要任何其他文字：");
        
        if (melody != null && !melody.notes.isEmpty()) {
            prompt.append("\n\n参考旋律（").append(melody.notes.size()).append("个音符）：");
            JSONArray melodyArray = new JSONArray();
            for (MusicData.Note note : melody.notes) {
                try { melodyArray.put(note.toJson()); } catch (Exception ignored) { }
            }
            prompt.append("\n").append(melodyArray.toString());
        }
        
        try {
            AIResponse response = modelConfig.requestAIWithSystemPrompt(prompt.toString(), MUSIC_SYSTEM_PROMPT);
            
            if (!response.isSuccess()) {
                throw new IOException(response.errorMessage);
            }
            
            Log.d(TAG, "Chords response: " + response.content);
            
            JSONArray jsonArray = extractMusicJsonArray(response.content);
            
            if (jsonArray == null || jsonArray.length() == 0) {
                throw new IOException("AI未生成有效和弦内容，请重试");
            }
            
            if (jsonArray.length() < 2) {
                throw new IOException("AI生成的和弦过少（少于2个），请重试");
            }
            
            MusicData.ChordProgression progression = new MusicData.ChordProgression();
            progression.id = String.valueOf(System.currentTimeMillis());
            progression.createdAt = System.currentTimeMillis();
            progression.style = style;
            progression.name = "AI Generated " + style;
            
            int time = 0;
            boolean hasValidChord = false;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject chordObj = jsonArray.getJSONObject(i);
                String name = chordObj.optString("name", "");
                String type = chordObj.optString("type", "");
                int duration = chordObj.optInt("duration", 0);
                
                if (name.isEmpty() || type.isEmpty() || duration <= 0) {
                    Log.w(TAG, "Skipping invalid chord at index " + i);
                    continue;
                }
                
                MusicData.Chord chord = new MusicData.Chord(name, type, duration, time);
                progression.chords.add(chord);
                time = chord.startTime + chord.duration;
                hasValidChord = true;
            }
            
            if (!hasValidChord || progression.chords.isEmpty()) {
                throw new IOException("生成失败：AI返回内容格式无效，请重试");
            }
            
            return progression;
        } catch (JSONException e) {
            Log.e(TAG, "Parse error", e);
            throw new IOException("解析AI返回失败: " + e.getMessage() + "，请重试");
        }
    }
    
    public MusicData.Song generateCompleteSong(String style, MusicData.Melody melody, MusicData.ChordProgression chords) throws IOException {
        if (melody == null || melody.notes.isEmpty()) {
            throw new IOException("旋律数据不能为空");
        }
        if (chords == null || chords.chords.isEmpty()) {
            throw new IOException("和弦数据不能为空");
        }
        
        String prompt = 
            "请根据以下歌曲信息生成一个标题。\n\n" +
            "【输出格式要求】\n" +
            "必须返回一个JSON对象，格式如下：\n" +
            "{\"title\":\"歌曲标题\",\"artist\":\"艺术家名\"}\n\n" +
            "【歌曲信息】\n" +
            "- 风格：" + style + "\n" +
            "- 旋律音符数：" + melody.notes.size() + "\n" +
            "- 和弦数：" + chords.chords.size() + "\n\n" +
            "直接输出JSON对象，不要任何其他文字：";
        
        try {
            AIResponse response = modelConfig.requestAIWithSystemPrompt(prompt, MUSIC_SYSTEM_PROMPT);
            
            if (!response.isSuccess()) {
                throw new IOException(response.errorMessage);
            }
            
            Log.d(TAG, "Song response: " + response.content);
            
            JSONObject jsonObj = extractMusicJsonObject(response.content);
            
            if (jsonObj == null) {
                throw new IOException("AI未生成有效歌曲信息，使用默认标题");
            }
            
            MusicData.Song song = new MusicData.Song();
            song.title = jsonObj.optString("title", "AI创作歌曲");
            song.artist = jsonObj.optString("artist", "MusicAI");
            song.style = style;
            song.melody = melody;
            song.chords = chords;
            
            return song;
        } catch (Exception e) {
            Log.e(TAG, "Parse error", e);
            MusicData.Song song = new MusicData.Song();
            song.title = "AI创作歌曲";
            song.artist = "MusicAI";
            song.style = style;
            song.melody = melody;
            song.chords = chords;
            return song;
        }
    }
    
    public MusicData.ChordProgression generateCustomChords(String style, int length, String keySignature, String mood, String description) throws IOException {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为").append(style).append("风格创作一组和弦。\n\n");
        
        if (keySignature != null && !keySignature.isEmpty()) {
            prompt.append("【调性信息】\n");
            prompt.append("指定调性：").append(keySignature).append("\n");
            
            if (keySignature.contains("m") || keySignature.contains("Minor")) {
                prompt.append("小调常用进行：Am - F - C - G (i - VI - III - VII)\n");
                prompt.append("小调下行进行：Am - Em - F - G (i - iv - V - VI)\n\n");
            } else {
                prompt.append("大调功能圈：I - V - vi - IV 是最常用的进行\n");
                prompt.append("大调经典进行：vi - IV - I - V\n");
                prompt.append("大调变体：C - G - Am - Em - F - C (I - V - vi - iii - IV - I)\n\n");
            }
        }
        
        prompt.append("【输出格式要求】\n");
        prompt.append("必须返回一个JSON数组，格式如下：\n");
        prompt.append("[{\"name\":\"C\",\"type\":\"major\",\"duration\":4},{\"name\":\"G\",\"type\":\"major\",\"duration\":4}]\n\n");
        prompt.append("【字段说明】\n");
        prompt.append("- name: 根音，取值范围 C C# D D# E F F# G G# A A# B\n");
        prompt.append("- type: 和弦类型 major minor seventh diminished augmented sus2 sus4\n");
        prompt.append("- duration: 时值（以四分音符为单位）\n\n");
        prompt.append("【创作要求】\n");
        prompt.append("- 请生成").append(length).append("个和弦\n");
        prompt.append("- 确保和弦进行自然流畅，有音乐性\n");
        prompt.append("- 优先使用经典功能圈进行\n\n");
        
        if (mood != null && !mood.isEmpty()) {
            prompt.append("【风格/情绪】\n");
            prompt.append("用户要求：").append(mood).append("\n");
            prompt.append("根据情绪选择合适的和声进行\n\n");
        }
        
        prompt.append("直接输出JSON数组，不要任何其他文字：");
        
        try {
            AIResponse response = modelConfig.requestAIWithSystemPrompt(prompt.toString(), MUSIC_SYSTEM_PROMPT);
            
            if (!response.isSuccess()) {
                throw new IOException(response.errorMessage);
            }
            
            Log.d(TAG, "Custom chords response: " + response.content);
            
            JSONArray jsonArray = extractMusicJsonArray(response.content);
            
            if (jsonArray == null || jsonArray.length() == 0) {
                throw new IOException("AI未生成有效和弦内容，请重试");
            }
            
            if (jsonArray.length() < 2) {
                throw new IOException("AI生成的和弦过少（少于2个），请重试");
            }
            
            MusicData.ChordProgression progression = new MusicData.ChordProgression();
            progression.id = String.valueOf(System.currentTimeMillis());
            progression.createdAt = System.currentTimeMillis();
            progression.style = style;
            progression.name = "自定义和弦 " + style;
            
            int time = 0;
            boolean hasValidChord = false;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject chordObj = jsonArray.getJSONObject(i);
                String name = chordObj.optString("name", "");
                String type = chordObj.optString("type", "");
                int duration = chordObj.optInt("duration", 0);
                
                if (name.isEmpty() || type.isEmpty() || duration <= 0) {
                    Log.w(TAG, "Skipping invalid chord at index " + i);
                    continue;
                }
                
                MusicData.Chord chord = new MusicData.Chord(
                    name,
                    type,
                    duration,
                    time
                );
                progression.chords.add(chord);
                time = chord.startTime + chord.duration;
                hasValidChord = true;
            }
            
            if (!hasValidChord || progression.chords.isEmpty()) {
                throw new IOException("生成失败：AI返回内容格式无效，请重试");
            }
            
            return progression;
        } catch (JSONException e) {
            Log.e(TAG, "Parse error", e);
            throw new IOException("解析AI返回失败: " + e.getMessage() + "，请重试");
        }
    }
    
    public MusicData.Melody generateMelody(String style, int length, MusicData.Melody userMelody) throws IOException {
        return generateMelodyWithDescription(style, length, userMelody, null);
    }
    
    public MusicData.ChordProgression generateChords(String style, int length, MusicData.ChordProgression userChords) throws IOException {
        return generateCustomChords(style, length, null, null, null);
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
        Log.d(TAG, "Processing response, length: " + text.length());
        
        text = text.replaceAll("(?i)```json", "");
        text = text.replaceAll("(?i)```javascript", "");
        text = text.replaceAll("(?i)```", "");
        text = text.replaceAll("`+", "");
        
        int firstBracket = text.indexOf('[');
        int lastBracket = text.lastIndexOf(']');
        
        if (firstBracket != -1 && lastBracket > firstBracket) {
            String potential = text.substring(firstBracket, lastBracket + 1);
            try {
                JSONArray arr = new JSONArray(potential);
                if (arr.length() > 0) {
                    Log.d(TAG, "Extracted JSON array with " + arr.length() + " items");
                    return arr;
                }
            } catch (JSONException e) {
                Log.w(TAG, "Failed to parse extracted JSON: " + e.getMessage());
            }
        }
        
        Pattern arrayPattern = Pattern.compile("\\[\\s*\\{[^\\}]+\\}\\s*(,\\s*\\{[^\\}]+\\}\\s*)*\\]");
        Matcher arrayMatcher = arrayPattern.matcher(text);
        
        if (arrayMatcher.find()) {
            String found = arrayMatcher.group();
            try {
                JSONArray arr = new JSONArray(found);
                Log.d(TAG, "Extracted JSON array via pattern, length: " + arr.length());
                return arr;
            } catch (JSONException e) {
                Log.w(TAG, "Pattern match parse failed: " + e.getMessage());
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
                Log.d(TAG, "Extracted JSON array from lines, length: " + arr.length());
                return arr;
            } catch (JSONException e) {
                Log.w(TAG, "Lines parse failed: " + e.getMessage());
            }
        }
        
        Log.w(TAG, "Failed to extract JSON array from response");
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
                Log.w(TAG, "Object parse failed: " + e.getMessage());
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
                Log.w(TAG, "Pattern object parse failed: " + e.getMessage());
            }
        }
        
        Log.w(TAG, "Failed to extract JSON object from response");
        return null;
    }
}
