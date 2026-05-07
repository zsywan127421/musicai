package com.example.musicai;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class GeneratorTestActivity extends AppCompatActivity {
    
    private TextView tvLog;
    private ScrollView scrollView;
    private Button btnTestConnection;
    private Button btnTestMelody;
    private Button btnTestChords;
    private Button btnTestFullSong;
    private Button btnTestAll;
    private ProgressBar progressBar;
    
    private ModelConfig modelConfig;
    private MusicGenerator musicGenerator;
    private StringBuilder logBuilder = new StringBuilder();
    private boolean isRunning = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generator_test);
        
        modelConfig = new ModelConfig(this);
        musicGenerator = new MusicGenerator(this);
        
        initViews();
        setupListeners();
        
        log("========== 生成功能测试 ==========");
        log("API: " + modelConfig.getApiUrl());
        log("Model: " + modelConfig.getModelName());
        log("API Key: " + (modelConfig.getApiKey().isEmpty() ? "未设置" : "已设置"));
        log("");
    }
    
    private void initViews() {
        tvLog = findViewById(R.id.tv_log);
        scrollView = findViewById(R.id.scroll_view);
        btnTestConnection = findViewById(R.id.btn_test_connection);
        btnTestMelody = findViewById(R.id.btn_test_melody);
        btnTestChords = findViewById(R.id.btn_test_chords);
        btnTestFullSong = findViewById(R.id.btn_test_full_song);
        btnTestAll = findViewById(R.id.btn_test_all);
        progressBar = findViewById(R.id.progress_bar);
    }
    
    private void setupListeners() {
        btnTestConnection.setOnClickListener(v -> testConnection());
        btnTestMelody.setOnClickListener(v -> testMelodyGeneration());
        btnTestChords.setOnClickListener(v -> testChordGeneration());
        btnTestFullSong.setOnClickListener(v -> testFullSongGeneration());
        btnTestAll.setOnClickListener(v -> testAll());
    }
    
    private void log(String message) {
        logBuilder.append(message).append("\n");
        tvLog.setText(logBuilder.toString());
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
    
    private void setButtonsEnabled(boolean enabled) {
        isRunning = enabled;
        btnTestConnection.setEnabled(!enabled);
        btnTestMelody.setEnabled(!enabled);
        btnTestChords.setEnabled(!enabled);
        btnTestFullSong.setEnabled(!enabled);
        btnTestAll.setEnabled(!enabled);
        progressBar.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }
    
    private void testConnection() {
        if (isRunning) return;
        
        if (modelConfig.getApiKey().isEmpty()) {
            log("❌ 测试失败: API Key 未配置");
            return;
        }
        
        log("\n========== [测试1] API连通性测试 ==========");
        setButtonsEnabled(true);
        
        new Thread(() -> {
            try {
                AIResponse response = modelConfig.requestAI("请回复: 测试成功");
                
                runOnUiThread(() -> {
                    setButtonsEnabled(false);
                    if (response.isSuccess()) {
                        log("✅ API连接成功!");
                        log("   AI回复: " + response.content);
                    } else {
                        log("❌ API连接失败!");
                        log("   错误类型: " + response.errorType);
                        log("   错误信息: " + response.errorMessage);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setButtonsEnabled(false);
                    log("❌ 连接异常: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void testMelodyGeneration() {
        if (isRunning) return;
        
        if (modelConfig.getApiKey().isEmpty()) {
            log("❌ 测试失败: API Key 未配置");
            return;
        }
        
        log("\n========== [测试2] 旋律生成测试 ==========");
        setButtonsEnabled(true);
        
        new Thread(() -> {
            try {
                log("正在生成8音符的流行风格旋律...");
                
                MusicData.Melody melody = musicGenerator.generateMelodyWithDescription("流行", 8, null, "测试旋律");
                
                runOnUiThread(() -> {
                    setButtonsEnabled(false);
                    if (melody != null && !melody.notes.isEmpty()) {
                        log("✅ 旋律生成成功! 共 " + melody.notes.size() + " 个音符");
                        for (int i = 0; i < Math.min(5, melody.notes.size()); i++) {
                            MusicData.Note note = melody.notes.get(i);
                            log("   音符" + (i+1) + ": " + note.pitch + note.octave + " 时值:" + note.duration);
                        }
                        if (melody.notes.size() > 5) {
                            log("   ... 还有 " + (melody.notes.size() - 5) + " 个音符");
                        }
                    } else {
                        log("❌ 旋律生成失败: 返回数据为空");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setButtonsEnabled(false);
                    log("❌ 旋律生成失败: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void testChordGeneration() {
        if (isRunning) return;
        
        if (modelConfig.getApiKey().isEmpty()) {
            log("❌ 测试失败: API Key 未配置");
            return;
        }
        
        log("\n========== [测试3] 和弦生成测试 ==========");
        setButtonsEnabled(true);
        
        new Thread(() -> {
            try {
                log("正在生成4个和弦的流行风格和弦进行...");
                
                MusicData.ChordProgression chords = musicGenerator.generateCustomChords("流行", 4, "C", "欢快", "测试和弦");
                
                runOnUiThread(() -> {
                    setButtonsEnabled(false);
                    if (chords != null && !chords.chords.isEmpty()) {
                        log("✅ 和弦生成成功! 共 " + chords.chords.size() + " 个和弦");
                        for (int i = 0; i < chords.chords.size(); i++) {
                            MusicData.Chord chord = chords.chords.get(i);
                            log("   和弦" + (i+1) + ": " + chord.name + " " + chord.type);
                        }
                    } else {
                        log("❌ 和弦生成失败: 返回数据为空");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setButtonsEnabled(false);
                    log("❌ 和弦生成失败: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void testFullSongGeneration() {
        if (isRunning) return;
        
        if (modelConfig.getApiKey().isEmpty()) {
            log("❌ 测试失败: API Key 未配置");
            return;
        }
        
        log("\n========== [测试4] 完整曲子生成测试 ==========");
        setButtonsEnabled(true);
        
        new Thread(() -> {
            try {
                log("步骤1: 生成旋律...");
                MusicData.Melody melody = musicGenerator.generateMelodyWithDescription("流行", 8, null, "测试");
                log("   旋律生成成功: " + melody.notes.size() + " 个音符");
                
                log("步骤2: 生成和弦...");
                MusicData.ChordProgression chords = musicGenerator.generateChordsWithMelody("流行", 4, null, melody);
                log("   和弦生成成功: " + chords.chords.size() + " 个和弦");
                
                log("步骤3: 生成完整曲子...");
                MusicData.Song song = musicGenerator.generateCompleteSong("流行", melody, chords);
                
                runOnUiThread(() -> {
                    setButtonsEnabled(false);
                    if (song != null) {
                        log("✅ 曲子生成成功!");
                        log("   标题: " + song.title);
                        log("   艺术家: " + song.artist);
                        log("   风格: " + song.style);
                        log("   旋律音符: " + (song.melody != null ? song.melody.notes.size() : 0) + "个");
                        log("   和弦: " + (song.chords != null ? song.chords.chords.size() : 0) + "个");
                    } else {
                        log("❌ 曲子生成失败: 返回数据为空");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setButtonsEnabled(false);
                    log("❌ 曲子生成失败: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void testAll() {
        if (isRunning) return;
        
        if (modelConfig.getApiKey().isEmpty()) {
            log("❌ 测试失败: API Key 未配置");
            return;
        }
        
        log("\n========== [完整测试流程] ==========");
        setButtonsEnabled(true);
        
        new Thread(() -> {
            try {
                log("\n--- 测试1: API连通性 ---");
                AIResponse connResponse = modelConfig.requestAI("测试");
                if (!connResponse.isSuccess()) {
                    runOnUiThread(() -> {
                        log("❌ API连接失败: " + connResponse.errorMessage);
                        setButtonsEnabled(false);
                    });
                    return;
                }
                log("✅ API连接正常");
                
                log("\n--- 测试2: 旋律生成 ---");
                MusicData.Melody melody = musicGenerator.generateMelodyWithDescription("流行", 8, null, "测试");
                if (melody == null || melody.notes.isEmpty()) {
                    runOnUiThread(() -> {
                        log("❌ 旋律生成失败");
                        setButtonsEnabled(false);
                    });
                    return;
                }
                log("✅ 旋律生成成功: " + melody.notes.size() + "个音符");
                
                log("\n--- 测试3: 和弦生成 ---");
                MusicData.ChordProgression chords = musicGenerator.generateChordsWithMelody("流行", 4, null, melody);
                if (chords == null || chords.chords.isEmpty()) {
                    runOnUiThread(() -> {
                        log("❌ 和弦生成失败");
                        setButtonsEnabled(false);
                    });
                    return;
                }
                log("✅ 和弦生成成功: " + chords.chords.size() + "个和弦");
                
                log("\n--- 测试4: 完整曲子生成 ---");
                MusicData.Song song = musicGenerator.generateCompleteSong("流行", melody, chords);
                if (song == null) {
                    runOnUiThread(() -> {
                        log("❌ 曲子生成失败");
                        setButtonsEnabled(false);
                    });
                    return;
                }
                
                runOnUiThread(() -> {
                    log("\n========== 全部测试通过! ==========");
                    log("✅ API连接: 成功");
                    log("✅ 旋律生成: 成功 (" + melody.notes.size() + "个音符)");
                    log("✅ 和弦生成: 成功 (" + chords.chords.size() + "个和弦)");
                    log("✅ 曲子生成: 成功");
                    log("   标题: " + song.title);
                    log("   艺术家: " + song.artist);
                    setButtonsEnabled(false);
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    log("\n❌ 测试失败: " + e.getMessage());
                    setButtonsEnabled(false);
                });
            }
        }).start();
    }
}
