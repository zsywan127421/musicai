package com.example.musicai.util;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Environment;
import android.util.Log;

import com.example.musicai.MusicData;
import com.example.musicai.SongEntry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class AudioExporter {

    private static final String TAG = "AudioExporter";
    private static final int SAMPLE_RATE = 44100;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS = 2;

    private Context context;
    private ExportListener listener;
    private volatile boolean isCancelled = false;

    public interface ExportListener {
        void onProgress(int progress, String message);
        void onSuccess(File file);
        void onError(String error);
    }

    public AudioExporter(Context context) {
        this.context = context;
    }

    public void setListener(ExportListener listener) {
        this.listener = listener;
    }

    public void cancel() {
        isCancelled = true;
    }

    public File exportToMidi(SongEntry songEntry) throws IOException {
        isCancelled = false;
        
        if (listener != null) {
            listener.onProgress(0, "正在生成MIDI文件...");
        }
        
        File exportDir = getExportDirectory();
        String fileName = sanitizeFileName(songEntry.name) + ".mid";
        File midiFile = new File(exportDir, fileName);
        
        if (isCancelled) throw new IOException("导出已取消");
        
        try {
            writeMidiFile(midiFile, songEntry);
            
            if (listener != null) {
                listener.onProgress(100, "MIDI导出完成");
                listener.onSuccess(midiFile);
            }
        } catch (IOException e) {
            if (listener != null) {
                listener.onError("MIDI导出失败: " + e.getMessage());
            }
            throw e;
        }
        
        return midiFile;
    }

    public File exportToWav(SongEntry songEntry) throws IOException {
        isCancelled = false;
        
        if (listener != null) {
            listener.onProgress(0, "正在合成WAV音频...");
        }
        
        File exportDir = getExportDirectory();
        String fileName = sanitizeFileName(songEntry.name) + ".wav";
        File wavFile = new File(exportDir, fileName);
        
        try {
            MusicData.Song song = songEntry.toMusicDataSong();
            
            if (listener != null) {
                listener.onProgress(20, "正在生成旋律轨道...");
            }
            if (isCancelled) throw new IOException("导出已取消");
            
            short[] melodySamples = synthesizeMelody(song.melody, songEntry.bpm > 0 ? songEntry.bpm : 120);
            
            if (listener != null) {
                listener.onProgress(50, "正在生成和弦轨道...");
            }
            if (isCancelled) throw new IOException("导出已取消");
            
            short[] chordSamples = synthesizeChords(song.chords, songEntry.bpm > 0 ? songEntry.bpm : 120);
            
            if (listener != null) {
                listener.onProgress(70, "正在混音...");
            }
            if (isCancelled) throw new IOException("导出已取消");
            
            short[] mixedSamples = mixAudio(melodySamples, chordSamples, 0.7f, 0.5f);
            
            if (listener != null) {
                listener.onProgress(90, "正在写入WAV文件...");
            }
            
            writeWavFile(wavFile, mixedSamples, SAMPLE_RATE);
            
            if (listener != null) {
                listener.onProgress(100, "WAV导出完成");
                listener.onSuccess(wavFile);
            }
        } catch (IOException e) {
            if (listener != null) {
                listener.onError("WAV导出失败: " + e.getMessage());
            }
            throw e;
        }
        
        return wavFile;
    }

    private short[] synthesizeMelody(MusicData.Melody melody, int bpm) {
        if (melody == null || melody.notes == null || melody.notes.isEmpty()) {
            return new short[0];
        }

        float beatDurationMs = 60000.0f / bpm;
        int maxDurationMs = 0;

        for (MusicData.Note note : melody.notes) {
            int noteEndMs = (int) ((note.startTime + note.duration) * beatDurationMs);
            if (noteEndMs > maxDurationMs) {
                maxDurationMs = noteEndMs;
            }
        }

        int totalSamples = (int) ((maxDurationMs + 1000) * SAMPLE_RATE * CHANNELS / 1000.0);
        short[] samples = new short[totalSamples];

        for (MusicData.Note note : melody.notes) {
            if (isCancelled) break;
            
            int noteStartMs = (int) (note.startTime * beatDurationMs);
            int noteDurationMs = (int) (note.duration * beatDurationMs);
            
            int midiNote = MusicData.pitchToMidi(note.pitch, note.octave);
            float frequency = 440.0f * (float) Math.pow(2, (midiNote - 69) / 12.0);
            
            synthesizeNote(samples, noteStartMs, noteDurationMs, frequency, 0.6f);
        }

        return samples;
    }

    private short[] synthesizeChords(MusicData.ChordProgression chords, int bpm) {
        if (chords == null || chords.chords == null || chords.chords.isEmpty()) {
            return new short[0];
        }

        float beatDurationMs = 60000.0f / bpm;
        int maxDurationMs = 0;

        for (MusicData.Chord chord : chords.chords) {
            int chordEndMs = (int) ((chord.startTime + chord.duration) * beatDurationMs);
            if (chordEndMs > maxDurationMs) {
                maxDurationMs = chordEndMs;
            }
        }

        int totalSamples = (int) ((maxDurationMs + 1000) * SAMPLE_RATE * CHANNELS / 1000.0);
        short[] samples = new short[totalSamples];

        for (MusicData.Chord chord : chords.chords) {
            if (isCancelled) break;
            
            int chordStartMs = (int) (chord.startTime * beatDurationMs);
            int chordDurationMs = (int) (chord.duration * beatDurationMs);
            
            float[] frequencies = getChordFrequencies(chord.name, chord.type);
            
            for (float freq : frequencies) {
                if (freq > 0) {
                    synthesizeNote(samples, chordStartMs, chordDurationMs, freq, 0.3f);
                }
            }
        }

        return samples;
    }

    private float[] getChordFrequencies(String chordName, String chordType) {
        float baseFreq = getNoteFrequency(chordName);
        float[] frequencies = new float[4];
        
        switch (chordType.toLowerCase()) {
            case "major":
                frequencies[0] = baseFreq;
                frequencies[1] = baseFreq * 5f / 4f;
                frequencies[2] = baseFreq * 3f / 2f;
                break;
            case "minor":
                frequencies[0] = baseFreq;
                frequencies[1] = baseFreq * 6f / 5f;
                frequencies[2] = baseFreq * 3f / 2f;
                break;
            case "seventh":
                frequencies[0] = baseFreq;
                frequencies[1] = baseFreq * 5f / 4f;
                frequencies[2] = baseFreq * 3f / 2f;
                frequencies[3] = baseFreq * 9f / 8f * 5f / 4f;
                break;
            case "diminished":
                frequencies[0] = baseFreq;
                frequencies[1] = baseFreq * 6f / 5f;
                frequencies[2] = baseFreq * 3f / 2f * 6f / 5f;
                break;
            default:
                frequencies[0] = baseFreq;
                frequencies[1] = baseFreq * 5f / 4f;
                frequencies[2] = baseFreq * 3f / 2f;
                break;
        }
        
        return frequencies;
    }

    private float getNoteFrequency(String noteName) {
        if (noteName == null || noteName.isEmpty()) return 440f;
        
        noteName = noteName.replace("#", "#").toUpperCase();
        float baseFreq = 261.63f;
        
        switch (noteName) {
            case "C": baseFreq = 261.63f; break;
            case "C#": case "DB": baseFreq = 277.18f; break;
            case "D": baseFreq = 293.66f; break;
            case "D#": case "EB": baseFreq = 311.13f; break;
            case "E": baseFreq = 329.63f; break;
            case "F": baseFreq = 349.23f; break;
            case "F#": case "GB": baseFreq = 369.99f; break;
            case "G": baseFreq = 392.00f; break;
            case "G#": case "AB": baseFreq = 415.30f; break;
            case "A": baseFreq = 440.00f; break;
            case "A#": case "BB": baseFreq = 466.16f; break;
            case "B": baseFreq = 493.88f; break;
        }
        
        return baseFreq;
    }

    private void synthesizeNote(short[] samples, int startMs, int durationMs, float frequency, float amplitude) {
        int startSample = (int) (startMs * SAMPLE_RATE * CHANNELS / 1000.0);
        int numSamples = (int) (durationMs * SAMPLE_RATE * CHANNELS / 1000.0);
        
        for (int i = 0; i < numSamples && (startSample + i) < samples.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            
            double envelope = 1.0;
            double attackSamples = numSamples * 0.05;
            double releaseSamples = numSamples * 0.2;
            
            if (i < attackSamples) {
                envelope = i / attackSamples;
            } else if (i > numSamples - releaseSamples) {
                envelope = (numSamples - i) / releaseSamples;
            }
            
            double wave = Math.sin(2 * Math.PI * frequency * t);
            wave += 0.3 * Math.sin(2 * Math.PI * frequency * 2 * t);
            wave += 0.15 * Math.sin(2 * Math.PI * frequency * 3 * t);
            wave *= envelope * amplitude;
            
            short sampleValue = (short) (wave * Short.MAX_VALUE);
            
            samples[startSample + i * CHANNELS] = sampleValue;
            if (startSample + i * CHANNELS + 1 < samples.length) {
                samples[startSample + i * CHANNELS + 1] = sampleValue;
            }
        }
    }

    private short[] mixAudio(short[] left, short[] right, float leftGain, float rightGain) {
        int maxLength = Math.max(left.length, right.length);
        short[] mixed = new short[maxLength];
        
        for (int i = 0; i < maxLength; i++) {
            float leftSample = 0;
            float rightSample = 0;
            
            if (i < left.length) {
                leftSample = left[i] * leftGain;
            }
            if (i < right.length) {
                rightSample = right[i] * rightGain;
            }
            
            float mixedSample = leftSample + rightSample;
            
            mixedSample = Math.max(-Short.MAX_VALUE, Math.min(Short.MAX_VALUE, mixedSample));
            mixed[i] = (short) mixedSample;
        }
        
        return mixed;
    }

    private void writeMidiFile(File file, SongEntry songEntry) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        
        byte[] header = new byte[] {
            0x4D, 0x54, 0x68, 0x64,
            0x00, 0x00, 0x00, 0x06,
            0x00, 0x01,
            0x00, 0x02,
            0x00, (byte) 0x60,
            0x00, (byte) 0x18
        };
        fos.write(header);
        
        byte[] trackHeader = new byte[] {
            0x4D, 0x54, 0x72, 0x6B,
            0x00, 0x00, 0x00, 0x00
        };
        
        byte[] trackData = new byte[10000];
        int trackLength = 0;
        
        byte[] tempoEvent = new byte[] {
            0x00, (byte) 0xFF, 0x51, 0x03,
            0x07, (byte) 0xA1, 0x20
        };
        System.arraycopy(tempoEvent, 0, trackData, trackLength, tempoEvent.length);
        trackLength += tempoEvent.length;
        
        byte[] programChange = new byte[] {
            0x00, (byte) 0xC0, 0x00
        };
        System.arraycopy(programChange, 0, trackData, trackLength, programChange.length);
        trackLength += programChange.length;
        
        int bpm = songEntry.bpm > 0 ? songEntry.bpm : 120;
        
        for (int i = 0; i < songEntry.segments.size(); i++) {
            if (isCancelled) break;
            
            SongEntry.SegmentData segment = songEntry.segments.get(i);
            
            try {
                org.json.JSONArray melodyNotes = new org.json.JSONArray(segment.melodyJson);
                
                for (int j = 0; j < melodyNotes.length(); j++) {
                    org.json.JSONObject noteObj = melodyNotes.getJSONObject(j);
                    
                    String pitch = noteObj.optString("pitch", "C");
                    int octave = noteObj.optInt("octave", 4);
                    int duration = noteObj.optInt("duration", 4);
                    int deltaTime = j == 0 ? 0 : duration / 2;
                    
                    int midiNote = MusicData.pitchToMidi(pitch, octave);
                    
                    trackData[trackLength++] = (byte) deltaTime;
                    trackData[trackLength++] = (byte) 0x90;
                    trackData[trackLength++] = (byte) midiNote;
                    trackData[trackLength++] = 0x64;
                    
                    int noteDeltaTime = duration / 2;
                    trackData[trackLength++] = (byte) noteDeltaTime;
                    trackData[trackLength++] = (byte) 0x80;
                    trackData[trackLength++] = (byte) midiNote;
                    trackData[trackLength++] = 0x00;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error writing melody notes", e);
            }
        }
        
        trackData[trackLength++] = 0x00;
        trackData[trackLength++] = (byte) 0xFF;
        trackData[trackLength++] = 0x2F;
        trackData[trackLength++] = 0x00;
        
        byte[] trackLengthBytes = ByteBuffer.allocate(4)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(trackLength)
            .array();
        System.arraycopy(trackLengthBytes, 0, trackHeader, 4, 4);
        
        fos.write(trackHeader);
        fos.write(trackData, 0, trackLength);
        
        fos.close();
    }

    private void writeWavFile(File file, short[] samples, int sampleRate) throws IOException {
        int byteRate = sampleRate * CHANNELS * BITS_PER_SAMPLE / 8;
        int dataSize = samples.length * BITS_PER_SAMPLE / 8;
        
        FileOutputStream fos = new FileOutputStream(file);
        
        byte[] header = new byte[44];
        
        System.arraycopy("RIFF".getBytes(), 0, header, 0, 4);
        System.arraycopy(intToBytes(36 + dataSize), 0, header, 4, 4);
        System.arraycopy("WAVE".getBytes(), 0, header, 8, 4);
        System.arraycopy("fmt ".getBytes(), 0, header, 12, 4);
        System.arraycopy(intToBytes(16), 0, header, 16, 4);
        header[20] = 1;
        header[22] = (byte) CHANNELS;
        System.arraycopy(intToBytes(sampleRate), 0, header, 24, 4);
        System.arraycopy(intToBytes(byteRate), 0, header, 28, 4);
        header[32] = (byte) (CHANNELS * BITS_PER_SAMPLE / 8);
        header[34] = (byte) BITS_PER_SAMPLE;
        System.arraycopy("data".getBytes(), 0, header, 36, 4);
        System.arraycopy(intToBytes(dataSize), 0, header, 40, 4);
        
        fos.write(header);
        
        byte[] data = new byte[samples.length * 2];
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(samples);
        fos.write(data);
        
        fos.close();
    }

    private byte[] intToBytes(int value) {
        return new byte[] {
            (byte) (value & 0xFF),
            (byte) ((value >> 8) & 0xFF),
            (byte) ((value >> 16) & 0xFF),
            (byte) ((value >> 24) & 0xFF)
        };
    }

    private File getExportDirectory() {
        File musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (musicDir == null) {
            musicDir = new File(context.getFilesDir(), "exports");
        }
        if (!musicDir.exists()) {
            musicDir.mkdirs();
        }
        return musicDir;
    }

    private String sanitizeFileName(String name) {
        if (name == null || name.isEmpty()) {
            return "untitled_" + System.currentTimeMillis();
        }
        return name.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]", "_");
    }
}
