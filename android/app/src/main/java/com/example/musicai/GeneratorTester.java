package com.example.musicai;

import android.content.Context;
import android.util.Log;

public class GeneratorTester {
    
    private static final String TAG = "GeneratorTester";
    private Context context;
    
    public GeneratorTester(Context context) {
        this.context = context;
    }
    
    public void runTests() {
        Log.i(TAG, "========== 开始测试生成功能 ==========");
        
        test1_ApiConnection();
        test2_MelodyGeneration();
        test3_ChordGeneration();
        test4_CompleteSongGeneration();
        
        Log.i(TAG, "========== 测试完成 ==========");
    }
    
    private void test1_ApiConnection() {
        Log.i(TAG, "\n[测试1] API连通性测试");
        ModelConfig config = new ModelConfig(context);
        
        Log.i(TAG, "API URL: " + config.getApiUrl());
        Log.i(TAG, "API Key: " + (config.getApiKey().isEmpty() ? "未设置" : "已设置(" + config.getApiKey().substring(0, Math.min(10, config.getApiKey().length())) + "...)"));
        Log.i(TAG, "Model: " + config.getModelName());
        
        if (config.getApiKey().isEmpty()) {
            Log.e(TAG, "测试失败: API Key 未配置");
            return;
        }
        
        new Thread(() -> {
            try {
                AIResponse response = config.requestAI("请回复: 测试成功");
                if (response.isSuccess()) {
                    Log.i(TAG, "API连接测试成功!");
                    Log.i(TAG, "AI回复: " + response.content);
                } else {
                    Log.e(TAG, "API连接测试失败: " + response.errorMessage);
                }
            } catch (Exception e) {
                Log.e(TAG, "API连接异常: " + e.getMessage());
            }
        }).start();
    }
    
    private void test2_MelodyGeneration() {
        Log.i(TAG, "\n[测试2] 旋律生成测试");
        
        if (!isConfigValid()) return;
        
        MusicGenerator generator = new MusicGenerator(context);
        
        new Thread(() -> {
            try {
                Log.i(TAG, "正在生成8音符的流行风格旋律...");
                MusicData.Melody melody = generator.generateMelodyWithDescription("流行", 8, null, "测试旋律");
                
                if (melody != null && !melody.notes.isEmpty()) {
                    Log.i(TAG, "旋律生成成功! 共 " + melody.notes.size() + " 个音符");
                    for (int i = 0; i < Math.min(5, melody.notes.size()); i++) {
                        MusicData.Note note = melody.notes.get(i);
                        Log.i(TAG, "  音符" + (i+1) + ": " + note.pitch + note.octave + " 时值:" + note.duration);
                    }
                } else {
                    Log.e(TAG, "旋律生成失败: 返回数据为空");
                }
            } catch (Exception e) {
                Log.e(TAG, "旋律生成异常: " + e.getMessage());
            }
        }).start();
    }
    
    private void test3_ChordGeneration() {
        Log.i(TAG, "\n[测试3] 和弦生成测试");
        
        if (!isConfigValid()) return;
        
        MusicGenerator generator = new MusicGenerator(context);
        
        new Thread(() -> {
            try {
                Log.i(TAG, "正在生成4个和弦的流行风格和弦进行...");
                MusicData.ChordProgression chords = generator.generateCustomChords("流行", 4, "C", "欢快", "测试和弦");
                
                if (chords != null && !chords.chords.isEmpty()) {
                    Log.i(TAG, "和弦生成成功! 共 " + chords.chords.size() + " 个和弦");
                    for (int i = 0; i < chords.chords.size(); i++) {
                        MusicData.Chord chord = chords.chords.get(i);
                        Log.i(TAG, "  和弦" + (i+1) + ": " + chord.name + " " + chord.type);
                    }
                } else {
                    Log.e(TAG, "和弦生成失败: 返回数据为空");
                }
            } catch (Exception e) {
                Log.e(TAG, "和弦生成异常: " + e.getMessage());
            }
        }).start();
    }
    
    private void test4_CompleteSongGeneration() {
        Log.i(TAG, "\n[测试4] 完整曲子生成测试");
        
        if (!isConfigValid()) return;
        
        MusicGenerator generator = new MusicGenerator(context);
        
        new Thread(() -> {
            try {
                Log.i(TAG, "步骤1: 先生成旋律...");
                MusicData.Melody melody = generator.generateMelodyWithDescription("流行", 8, null, "测试");
                
                Log.i(TAG, "步骤2: 生成和弦...");
                MusicData.ChordProgression chords = generator.generateChordsWithMelody("流行", 4, null, melody);
                
                Log.i(TAG, "步骤3: 生成完整曲子...");
                MusicData.Song song = generator.generateCompleteSong("流行", melody, chords);
                
                if (song != null) {
                    Log.i(TAG, "曲子生成成功!");
                    Log.i(TAG, "  标题: " + song.title);
                    Log.i(TAG, "  艺术家: " + song.artist);
                    Log.i(TAG, "  风格: " + song.style);
                    Log.i(TAG, "  旋律音符: " + (song.melody != null ? song.melody.notes.size() : 0) + "个");
                    Log.i(TAG, "  和弦: " + (song.chords != null ? song.chords.chords.size() : 0) + "个");
                } else {
                    Log.e(TAG, "曲子生成失败: 返回数据为空");
                }
            } catch (Exception e) {
                Log.e(TAG, "曲子生成异常: " + e.getMessage());
            }
        }).start();
    }
    
    private boolean isConfigValid() {
        ModelConfig config = new ModelConfig(context);
        if (config.getApiKey().isEmpty()) {
            Log.e(TAG, "配置无效: API Key 未设置");
            return false;
        }
        return true;
    }
}
