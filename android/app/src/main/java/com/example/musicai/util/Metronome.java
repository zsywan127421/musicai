package com.example.musicai.util;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;

public class Metronome {
    
    private static final int SAMPLE_RATE = 44100;
    private static final double CLICK_FREQUENCY_HIGH = 1500.0;
    private static final double CLICK_FREQUENCY_LOW = 800.0;
    
    private AudioTrack audioTrack;
    private Thread metronomeThread;
    private volatile boolean isRunning = false;
    private volatile boolean isPaused = false;
    
    private int bpm = 120;
    private int beatsPerMeasure = 4;
    private int currentBeat = 0;
    
    private MetronomeListener listener;
    private Handler handler;
    
    private short[] highClickBuffer;
    private short[] lowClickBuffer;
    
    public interface MetronomeListener {
        void onBeat(int beat, boolean isDownbeat);
        void onTempoChanged(int bpm);
    }
    
    public Metronome() {
        handler = new Handler(Looper.getMainLooper());
        generateClickBuffers();
    }
    
    private void generateClickBuffers() {
        int clickDurationMs = 30;
        int samples = (int) (SAMPLE_RATE * clickDurationMs / 1000.0);
        
        highClickBuffer = new short[samples];
        lowClickBuffer = new short[samples];
        
        for (int i = 0; i < samples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double envelope = Math.exp(-t * 40);
            highClickBuffer[i] = (short) (Math.sin(2 * Math.PI * CLICK_FREQUENCY_HIGH * t) * envelope * Short.MAX_VALUE * 0.5);
            lowClickBuffer[i] = (short) (Math.sin(2 * Math.PI * CLICK_FREQUENCY_LOW * t) * envelope * Short.MAX_VALUE * 0.4);
        }
    }
    
    public void setListener(MetronomeListener listener) {
        this.listener = listener;
    }
    
    public void setBpm(int bpm) {
        this.bpm = Math.max(20, Math.min(300, bpm));
        if (listener != null) {
            listener.onTempoChanged(this.bpm);
        }
    }
    
    public int getBpm() {
        return bpm;
    }
    
    public void setBeatsPerMeasure(int beats) {
        this.beatsPerMeasure = Math.max(1, Math.min(12, beats));
    }
    
    public int getBeatsPerMeasure() {
        return beatsPerMeasure;
    }
    
    public void start() {
        if (isRunning) return;
        
        isRunning = true;
        isPaused = false;
        currentBeat = 0;
        
        int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack(
            AudioManager.STREAM_MUSIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
            AudioTrack.MODE_STREAM
        );
        audioTrack.play();
        
        metronomeThread = new Thread(this::runMetronome);
        metronomeThread.start();
    }
    
    public void stop() {
        isRunning = false;
        isPaused = false;
        currentBeat = 0;
        
        if (metronomeThread != null) {
            metronomeThread.interrupt();
            metronomeThread = null;
        }
        
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }
    
    public void pause() {
        isPaused = true;
    }
    
    public void resume() {
        isPaused = false;
    }
    
    public boolean isRunning() {
        return isRunning && !isPaused;
    }
    
    private void runMetronome() {
        while (isRunning) {
            if (!isPaused) {
                boolean isDownbeat = (currentBeat == 0);
                short[] clickSound = isDownbeat ? highClickBuffer : lowClickBuffer;
                
                audioTrack.write(clickSound, 0, clickSound.length);
                
                if (listener != null) {
                    final int beat = currentBeat;
                    final boolean downbeat = isDownbeat;
                    handler.post(() -> listener.onBeat(beat, downbeat));
                }
                
                currentBeat = (currentBeat + 1) % beatsPerMeasure;
            }
            
            try {
                long intervalMs = 60000 / bpm;
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    public void tap() {
        if (listener != null) {
            listener.onBeat(0, true);
        }
    }
}
