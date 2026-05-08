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
        
        String keyInfo = "";
        String chordInfo = "";
        
        if (userMelody != null && !userMelody.notes.isEmpty()) {
            prompt.append("【参考旋律】\n");
            prompt.append("请基于以下参考旋律创作新旋律：\n");
            JSONArray melodyArray = new JSONArray();
            for (MusicData.Note note : userMelody.notes) {
                try { melodyArray.put(note.toJson()); } catch (Exception ignored) { }
            }
            prompt.append(melodyArray.toString()).append("\n\n");
            
            String firstPitch = userMelody.notes.get(0).pitch;
            String lastPitch = userMelody.notes.get(userMelody.notes.size() - 1).pitch;
            keyInfo = "Key: C Major\n";
            if (lastPitch.equals("A") || lastPitch.equals("D") || lastPitch.equals("E")) {
                keyInfo = "Key: A minor\n";
            }
        }
        
        prompt.append("【输出格式要求】\n");
        prompt.append("必须返回一个JSON数组，格式如下：\n");
        prompt.append("[{\"pitch\":\"C\",\"octave\":4,\"duration\":4},{\"pitch\":\"E\",\"octave\":4,\"duration\":4}]\n\n");
        prompt.append("【字段说明】\n");
        prompt.append("- pitch: 音高，取值范围 C C# D D# E F F# G G# A A# B\n");
        prompt.append("- octave: 八度，取值范围 2-7\n");
        prompt.append("- duration: 时值，1=全音符 2=二分 4=四分 8=八分 16=十六分\n\n");
        prompt.append(keyInfo);
        prompt.append(chordInfo);
        prompt.append("【创作要求】\n");
        prompt.append("- 请创作").append(length).append("个音符的旋律\n");
        prompt.append("- 旋律要有起伏，节奏要有变化\n");
        prompt.append("- 音符应与当前调性匹配，优先使用调内音\n");
        prompt.append("- 协和音程（纯四五度、大小三度）优先\n");
        prompt.append("- 不协和音程（大二、增减）谨慎使用\n");
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
        prompt.append("请根据以下旋律生成高质量的和弦进行。\n\n");
        
        String keySignature = "C Major";
        String chordProgression = "I - V - vi - IV";
        
        if (melody != null && !melody.notes.isEmpty()) {
            prompt.append("【旋律分析】\n");
            
            String firstPitch = melody.notes.get(0).pitch;
            String lastPitch = melody.notes.get(melody.notes.size() - 1).pitch;
            int firstOctave = melody.notes.get(0).octave;
            
            boolean startsHigh = firstPitch.contains("#") || firstOctave >= 5;
            boolean endsOnRoot = lastPitch.equals("C") || lastPitch.equals("F") || lastPitch.equals("G") || 
                                 lastPitch.equals("D") || lastPitch.equals("E");
            boolean endsOnMinor = lastPitch.equals("A") || lastPitch.equals("D") || lastPitch.equals("E");
            
            if (startsHigh && endsOnRoot) {
                prompt.append("旋律特点：大调风格（以高音/主音开始和结束）\n");
                prompt.append("调性分析：确定为大调调式\n");
                prompt.append("请使用大调功能和声：主和弦(I)、下属和弦(IV)、属和弦(V)为主\n");
                keySignature = "C Major";
                chordProgression = "I - V - vi - IV (C - G - Am - F)";
            } else if (firstOctave <= 3 && endsOnMinor) {
                prompt.append("旋律特点：小调风格（以低音开始）\n");
                prompt.append("调性分析：确定为基础小调调式\n");
                prompt.append("请使用小调功能和声：主和弦(i)、下属和弦(iv)、属和弦(V)为主\n");
                keySignature = "A minor";
                chordProgression = "i - VI - III - VII (Am - F - C - G)";
            } else {
                prompt.append("旋律特点：混合/爵士风格\n");
                prompt.append("请使用丰富多样的和弦进行\n");
                keySignature = "C Major";
                chordProgression = "ii - V - I - vi (Dm - G - C - Am)";
            }
            
            prompt.append("\n【和弦功能圈规则】\n");
            prompt.append("功能和声三要素：\n");
            prompt.append("- 主功能（Tonic）: I, iii, vi - 给人稳定感\n");
            prompt.append("- 下属功能（Subdominant）: IV, ii - 有上升感\n");
            prompt.append("- 属功能（Dominant）: V, vii° - 有强烈解决欲望\n\n");
            prompt.append("【推荐和弦进行】\n");
            prompt.append("C大调经典进行：\n");
            prompt.append("1. C - G - Am - F (I - V - vi - IV) - 最流行\n");
            prompt.append("2. C - Am - F - G (I - vi - IV - V) - 黄金比例\n");
            prompt.append("3. Am - F - C - G (vi - IV - I - V) - 上行力量\n");
            prompt.append("4. C - Em - F - G (I - iii - IV - V) - 下行流动\n");
            prompt.append("5. C - F - G - C (I - IV - V - I) - 古典终结\n\n");
        }
        
        prompt.append("【输出格式要求】\n");
        prompt.append("必须返回一个JSON数组，格式如下：\n");
        prompt.append("[{\"name\":\"C\",\"type\":\"major\",\"duration\":4},{\"name\":\"G\",\"type\":\"major\",\"duration\":4}]\n\n");
        prompt.append("【字段说明】\n");
        prompt.append("- name: 根音，C C# D D# E F F# G G# A A# B\n");
        prompt.append("- type: 和弦类型 major minor seventh diminished augmented sus2 sus4\n");
        prompt.append("- duration: 时值（以四分音符为单位）\n\n");
        prompt.append("【Key信息】\n");
        prompt.append("Key: ").append(keySignature).append("\n");
        prompt.append("参考进行: ").append(chordProgression).append("\n\n");
        prompt.append("【创作要求】\n");
        prompt.append("- 请生成").append(length).append("个和弦\n");
        prompt.append("- 必须遵循功能圈规则，确保和声进行流畅\n");
        prompt.append("- 每个和弦的根音必须在调性内\n");
        prompt.append("- 和弦之间要有层次感和流动性\n\n");
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
    
    public interface SegmentProgressCallback {
        void onSegmentProgress(int segmentIndex, int totalSegments);
    }
    
    public MusicData.Song generateSongWithSegments(String style, MusicData.Melody melody, 
            MusicData.ChordProgression chords, SegmentProgressCallback callback) throws IOException {
        
        if (chords == null || chords.chords.isEmpty()) {
            throw new IOException("和弦数据不能为空");
        }
        
        MusicData.Song song = new MusicData.Song();
        song.title = "AI创作歌曲";
        song.artist = "MusicAI";
        song.style = style;
        
        int totalSegments = chords.chords.size();
        int currentTimeMs = 0;
        
        for (int i = 0; i < totalSegments; i++) {
            MusicData.Chord chord = chords.chords.get(i);
            MusicData.Melody segmentMelody;
            
            if (i == 0) {
                segmentMelody = melody;
            } else {
                String segmentPrompt = buildSegmentPrompt(style, melody, chords, i);
                
                try {
                    AIResponse response = modelConfig.requestAIWithSystemPrompt(segmentPrompt, MUSIC_SYSTEM_PROMPT);
                    
                    if (response.isSuccess() && response.content != null && !response.content.trim().isEmpty()) {
                        segmentMelody = parseSegmentMelody(response.content, currentTimeMs);
                    } else {
                        segmentMelody = generateDefaultSegmentMelody(chords, i, currentTimeMs);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Segment " + i + " generation failed, using default", e);
                    segmentMelody = generateDefaultSegmentMelody(chords, i, currentTimeMs);
                }
            }
            
            MusicData.ChordProgression segmentChord = new MusicData.ChordProgression();
            segmentChord.chords.add(chord);
            
            MusicData.Segment segment = new MusicData.Segment(i, segmentMelody, segmentChord, currentTimeMs);
            song.segments.add(segment);
            
            currentTimeMs += segment.durationMs;
            
            if (callback != null) {
                callback.onSegmentProgress(i, totalSegments);
            }
        }
        
        song.totalDurationMs = currentTimeMs;
        song.melody = mergeMelodies(song.segments);
        song.chords = chords;
        
        return song;
    }
    
    private String buildSegmentPrompt(String style, MusicData.Melody originalMelody, 
            MusicData.ChordProgression chords, int segmentIndex) {
        
        MusicData.Chord currentChord = chords.chords.get(segmentIndex);
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下信息生成一段与和弦匹配的新旋律。\n\n");
        prompt.append("【输出格式要求】\n");
        prompt.append("必须返回一个JSON数组，格式如下：\n");
        prompt.append("[{\"pitch\":\"C\",\"octave\":4,\"duration\":4}]\n\n");
        prompt.append("【字段说明】\n");
        prompt.append("- pitch: 音高，取值范围 C C# D D# E F F# G G# A A# B\n");
        prompt.append("- octave: 八度，取值范围 2-7\n");
        prompt.append("- duration: 时值，1=全音符 2=二分 4=四分 8=八分 16=十六分\n\n");
        prompt.append("【当前段落信息】\n");
        prompt.append("- 段落序号：").append(segmentIndex + 1).append("\n");
        prompt.append("- 当前和弦：").append(currentChord.name).append(" ").append(currentChord.type).append("\n");
        prompt.append("- 和弦功能：").append(getChordFunction(currentChord.name, segmentIndex)).append("\n\n");
        prompt.append("【参考旋律】\n");
        prompt.append("原始主旋律包含").append(originalMelody.notes.size()).append("个音符\n");
        prompt.append("请生成与当前和弦协调的旋律变体\n\n");
        prompt.append("【创作要求】\n");
        prompt.append("- 旋律应与当前和弦的声音相协调\n");
        prompt.append("- 可以使用和弦内音或经过音\n");
        prompt.append("- 保持与整体风格的统一\n");
        prompt.append("- 生成4-8个音符的短旋律\n\n");
        prompt.append("直接输出JSON数组，不要任何其他文字：");
        
        return prompt.toString();
    }
    
    private String getChordFunction(String chordName, int index) {
        if (chordName == null) return "未知";
        
        switch (chordName) {
            case "C": case "F": case "G":
                return "主功能（Tonic）或下属/属功能";
            case "Am": case "Dm": case "Em":
                return "下属功能或辅助功能";
            case "D": case "E": case "A":
                return "属功能或主功能";
            default:
                return "辅助功能（位置：" + (index + 1) + "）";
        }
    }
    
    private MusicData.Melody parseSegmentMelody(String content, int startTimeMs) {
        MusicData.Melody melody = new MusicData.Melody();
        melody.name = "Segment melody";
        
        try {
            JSONArray jsonArray = extractMusicJsonArray(content);
            if (jsonArray != null && jsonArray.length() > 0) {
                int time = 0;
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject noteObj = jsonArray.getJSONObject(i);
                    String pitch = noteObj.optString("pitch", "C");
                    int octave = noteObj.optInt("octave", 4);
                    int duration = noteObj.optInt("duration", 4);
                    
                    MusicData.Note note = new MusicData.Note(pitch, octave, duration, time);
                    melody.notes.add(note);
                    time = note.startTime + note.duration;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse segment melody", e);
        }
        
        if (melody.notes.isEmpty()) {
            melody = generateDefaultSegmentMelody(null, 0, startTimeMs);
        }
        
        return melody;
    }
    
    private MusicData.Melody generateDefaultSegmentMelody(MusicData.ChordProgression chords, int segmentIndex, int startTimeMs) {
        MusicData.Melody melody = new MusicData.Melody();
        melody.name = "Default segment melody " + segmentIndex;
        
        String[] defaultPitches = {"C", "E", "G", "C"};
        
        int time = 0;
        for (int i = 0; i < 4; i++) {
            String pitch = defaultPitches[i % defaultPitches.length];
            int octave = 4 + (i / 2);
            int duration = 4;
            
            MusicData.Note note = new MusicData.Note(pitch, octave, duration, time);
            melody.notes.add(note);
            time += duration;
        }
        
        return melody;
    }
    
    private MusicData.Melody mergeMelodies(List<MusicData.Segment> segments) {
        MusicData.Melody merged = new MusicData.Melody();
        merged.name = "Merged melody";
        
        for (MusicData.Segment segment : segments) {
            for (MusicData.Note note : segment.melody.notes) {
                MusicData.Note newNote = new MusicData.Note(
                    note.pitch,
                    note.octave,
                    note.duration,
                    segment.startTimeMs + note.startTime
                );
                merged.notes.add(newNote);
            }
        }
        
        return merged;
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
