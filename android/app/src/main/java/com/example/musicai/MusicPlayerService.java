package com.example.musicai;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.List;

public class MusicPlayerService extends Service {
    
    private static final String TAG = "MusicPlayerService";
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    
    public interface PlaybackListener {
        void onPlaybackProgress(int positionMs, int totalMs, int currentNoteIndex);
        void onPlaybackStateChanged(boolean isPlaying);
        void onPlaybackCompleted();
    }
    
    private final IBinder binder = new LocalBinder();
    private AudioTrack audioTrack;
    private Thread playbackThread;
    private volatile boolean isPlaying = false;
    private float volume = 1.0f;
    private float speed = 1.0f;
    private PlaybackListener playbackListener;
    private Handler listenerHandler;
    
    private MusicData.Song currentSong;
    private volatile int currentPositionMs = 0;
    private int totalDurationMs = 0;
    private int currentNoteIndex = -1;
    
    public class LocalBinder extends Binder {
        MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        listenerHandler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "Service created");
    }
    
    @Override
    public void onDestroy() {
        stopPlayback();
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
    }
    
    public void setPlaybackListener(PlaybackListener listener) {
        this.playbackListener = listener;
    }
    
    public void removePlaybackListener() {
        this.playbackListener = null;
    }
    
    public void playSong(MusicData.Song song) {
        stopPlayback();
        currentSong = song;
        currentPositionMs = 0;
        currentNoteIndex = -1;
        calculateTotalDuration();
        startPlayback();
    }
    
    public void playMelody(MusicData.Melody melody) {
        stopPlayback();
        currentSong = new MusicData.Song();
        currentSong.melody = melody;
        currentPositionMs = 0;
        currentNoteIndex = -1;
        calculateTotalDuration();
        startPlayback();
    }
    
    private void calculateTotalDuration() {
        totalDurationMs = 0;
        if (currentSong != null && currentSong.melody != null && !currentSong.melody.notes.isEmpty()) {
            MusicData.Note lastNote = currentSong.melody.notes.get(currentSong.melody.notes.size() - 1);
            totalDurationMs = (int) ((lastNote.startTime + lastNote.duration) * 250 / speed);
        }
    }
    
    public void pause() {
        isPlaying = false;
        if (audioTrack != null) {
            audioTrack.pause();
        }
        notifyStateChanged(false);
    }
    
    public void resume() {
        if (audioTrack != null && !isPlaying) {
            isPlaying = true;
            audioTrack.play();
            notifyStateChanged(true);
        }
    }
    
    public void stopPlayback() {
        isPlaying = false;
        currentPositionMs = 0;
        currentNoteIndex = -1;
        
        if (playbackThread != null) {
            playbackThread.interrupt();
            try {
                playbackThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            playbackThread = null;
        }
        
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
        
        notifyStateChanged(false);
    }
    
    public boolean isPlaying() {
        return isPlaying;
    }
    
    public float getVolume() {
        return volume;
    }
    
    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        if (audioTrack != null) {
            audioTrack.setVolume(this.volume);
        }
    }
    
    public int getCurrentPosition() {
        return currentPositionMs;
    }
    
    public int getDuration() {
        return totalDurationMs;
    }
    
    public int getCurrentNoteIndex() {
        return currentNoteIndex;
    }
    
    public float getSpeed() {
        return speed;
    }
    
    public void setSpeed(float speed) {
        float oldSpeed = this.speed;
        this.speed = Math.max(0.5f, Math.min(2.0f, speed));

        if (totalDurationMs > 0) {
            float ratio = oldSpeed / this.speed;
            currentPositionMs = (int) (currentPositionMs * ratio);
            totalDurationMs = (int) (totalDurationMs * ratio);
        }
    }
    
    private void notifyProgress(int positionMs, int noteIndex) {
        if (playbackListener != null && listenerHandler != null) {
            final int pos = positionMs;
            final int note = noteIndex;
            listenerHandler.post(() -> {
                if (playbackListener != null) {
                    playbackListener.onPlaybackProgress(pos, totalDurationMs, note);
                }
            });
        }
    }
    
    private void notifyStateChanged(boolean playing) {
        if (playbackListener != null && listenerHandler != null) {
            final boolean p = playing;
            listenerHandler.post(() -> {
                if (playbackListener != null) {
                    playbackListener.onPlaybackStateChanged(p);
                }
            });
        }
    }
    
    private void notifyCompleted() {
        if (playbackListener != null && listenerHandler != null) {
            listenerHandler.post(() -> {
                if (playbackListener != null) {
                    playbackListener.onPlaybackCompleted();
                }
            });
        }
    }
    
    private void startPlayback() {
        if (currentSong == null || currentSong.melody == null || currentSong.melody.notes.isEmpty()) {
            return;
        }
        
        int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        audioTrack = new AudioTrack(
            AudioManager.STREAM_MUSIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize,
            AudioTrack.MODE_STREAM
        );
        
        audioTrack.setVolume(volume);
        audioTrack.play();
        isPlaying = true;
        currentPositionMs = 0;
        currentNoteIndex = 0;
        
        notifyStateChanged(true);
        playbackThread = new Thread(new PlaybackRunnable());
        playbackThread.start();
    }
    
    private class PlaybackRunnable implements Runnable {
        @Override
        public void run() {
            List<MusicData.Note> notes = currentSong.melody.notes;
            int samplePos = 0;
            int noteIndex = 0;
            
            try {
                for (MusicData.Note note : notes) {
                    if (!isPlaying) break;
                    
                    noteIndex = notes.indexOf(note);
                    currentNoteIndex = noteIndex;
                    
                    int midi = MusicData.pitchToMidi(note.pitch, note.octave);
                    double frequency = 440.0 * Math.pow(2.0, (midi - 69) / 12.0);
                    
                    int baseNoteDurationMs = note.duration * 250;
                    int noteDurationMs = (int) (baseNoteDurationMs / speed);
                    int noteSamples = (int) (SAMPLE_RATE * (noteDurationMs / 1000.0));
                    
                    short[] buffer = new short[noteSamples * 2];
                    for (int i = 0; i < noteSamples; i++) {
                        if (!isPlaying) break;
                        
                        double t = (double) i / SAMPLE_RATE;
                        double sample = Math.sin(2 * Math.PI * frequency * t) * 0.3;
                        sample *= Math.exp(-t * 5.0 * speed);
                        
                        short value = (short) (sample * Short.MAX_VALUE);
                        buffer[i * 2] = value;
                        buffer[i * 2 + 1] = value;
                        
                        if (i % 100 == 0) {
                            int progressMs = (int) ((note.startTime * 250 / speed) + (i * 1000.0 / SAMPLE_RATE));
                            currentPositionMs = progressMs;
                            notifyProgress(progressMs, noteIndex);
                        }
                    }
                    
                    audioTrack.write(buffer, 0, buffer.length);
                    samplePos += noteSamples;
                    int endMs = (int) ((note.startTime + note.duration) * 250 / speed);
                    currentPositionMs = endMs;
                    notifyProgress(endMs, noteIndex);
                }
            } catch (Exception e) {
                Log.e(TAG, "Playback error", e);
            }
            
            isPlaying = false;
            currentNoteIndex = -1;
            if (audioTrack != null) {
                audioTrack.stop();
            }
            notifyStateChanged(false);
            notifyCompleted();
        }
    }
}
