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
    private int pausedPositionMs = 0;
    
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
        pausedPositionMs = 0;
        currentNoteIndex = -1;
        calculateTotalDuration();
        startPlayback();
    }
    
    public void setMelody(MusicData.Melody melody) {
        if (currentSong == null) {
            currentSong = new MusicData.Song();
        }
        currentSong.melody = melody;
        currentSong.chords = null;
        calculateTotalDuration();
    }
    
    public void setChordProgression(MusicData.ChordProgression chords) {
        if (currentSong == null) {
            currentSong = new MusicData.Song();
        }
        currentSong.chords = chords;
    }
    
    public void play() {
        if (currentSong != null && currentSong.melody != null && !currentSong.melody.notes.isEmpty()) {
            if (pausedPositionMs > 0) {
                resumeFromPosition();
            } else {
                startPlayback();
            }
        }
    }
    
    private void resumeFromPosition() {
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
        
        notifyStateChanged(true);
        playbackThread = new Thread(new PlaybackRunnable(pausedPositionMs));
        playbackThread.start();
    }
    
    public void stop() {
        stopPlayback();
    }
    
    public void seekTo(int positionMs) {
        currentPositionMs = positionMs;
    }
    
    public void setPlaybackSpeed(float speed) {
        setSpeed(speed);
    }
    
    private void calculateTotalDuration() {
        totalDurationMs = 0;
        if (currentSong != null && currentSong.melody != null && !currentSong.melody.notes.isEmpty()) {
            MusicData.Note lastNote = currentSong.melody.notes.get(currentSong.melody.notes.size() - 1);
            totalDurationMs = (int) ((lastNote.startTime + lastNote.duration) * 250 / speed);
        }
    }
    
    public void pause() {
        if (isPlaying) {
            pausedPositionMs = currentPositionMs;
        }
        isPlaying = false;
        if (audioTrack != null) {
            audioTrack.pause();
        }
        notifyStateChanged(false);
    }
    
    public void resume() {
        if (audioTrack != null && pausedPositionMs > 0) {
            isPlaying = true;
            audioTrack.play();
            notifyStateChanged(true);
        } else if (audioTrack != null && !isPlaying) {
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
        this.speed = Math.max(0.25f, Math.min(4.0f, speed));

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
        pausedPositionMs = 0;
        currentNoteIndex = 0;
        
        notifyStateChanged(true);
        playbackThread = new Thread(new PlaybackRunnable(0));
        playbackThread.start();
    }
    
    private class PlaybackRunnable implements Runnable {
        private int startPositionMs;
        
        public PlaybackRunnable(int startPositionMs) {
            this.startPositionMs = startPositionMs;
        }
        
        @Override
        public void run() {
            List<MusicData.Note> notes = currentSong.melody.notes;
            int samplePos = 0;
            int noteIndex = 0;
            int skipSamples = 0;
            
            if (startPositionMs > 0) {
                skipSamples = (int) (SAMPLE_RATE * (startPositionMs / 1000.0));
            }
            
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
                    int samplesToSkip = 0;
                    
                    if (skipSamples > 0) {
                        samplesToSkip = Math.min(skipSamples, noteSamples);
                        skipSamples -= samplesToSkip;
                    }
                    
                    for (int i = samplesToSkip; i < noteSamples; i++) {
                        if (!isPlaying) break;
                        
                        double t = (double) i / SAMPLE_RATE;
                        double sample = Math.sin(2 * Math.PI * frequency * t) * 0.3;
                        sample *= Math.exp(-t * 5.0 * speed);
                        
                        short value = (short) (sample * Short.MAX_VALUE);
                        buffer[i * 2] = value;
                        buffer[i * 2 + 1] = value;
                        
                        if (i % 100 == 0) {
                            int progressMs = (int) ((note.startTime * 250 / speed) + ((i - samplesToSkip) * 1000.0 / SAMPLE_RATE));
                            currentPositionMs = progressMs + startPositionMs;
                            notifyProgress(currentPositionMs, noteIndex);
                        }
                    }
                    
                    audioTrack.write(buffer, samplesToSkip * 2, (noteSamples - samplesToSkip) * 2);
                    samplePos += noteSamples - samplesToSkip;
                    int endMs = (int) ((note.startTime + note.duration) * 250 / speed) + startPositionMs;
                    currentPositionMs = endMs;
                    notifyProgress(endMs, noteIndex);
                }
            } catch (Exception e) {
                Log.e(TAG, "Playback error", e);
            }
            
            isPlaying = false;
            currentNoteIndex = -1;
            currentPositionMs = 0;
            pausedPositionMs = 0;
            if (audioTrack != null) {
                audioTrack.stop();
            }
            notifyStateChanged(false);
            notifyCompleted();
        }
    }
}
