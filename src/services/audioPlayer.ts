import { Note, Song } from '../types';
import type { Track } from '../types';

const noteToFrequency = (note: string): number => {
  const noteMap: Record<string, number> = {
    'C3': 130.81, 'C#3': 138.59, 'D3': 146.83, 'D#3': 155.56, 'E3': 164.81, 'F3': 174.61,
    'F#3': 185.00, 'G3': 196.00, 'G#3': 207.65, 'A3': 220.00, 'A#3': 233.08, 'B3': 246.94,
    'C4': 261.63, 'C#4': 277.18, 'D4': 293.66, 'D#4': 311.13, 'E4': 329.63, 'F4': 349.23,
    'F#4': 369.99, 'G4': 392.00, 'G#4': 415.30, 'A4': 440.00, 'A#4': 466.16, 'B4': 493.88,
    'C5': 523.25, 'C#5': 554.37, 'D5': 587.33, 'D#5': 622.25, 'E5': 659.25, 'F5': 698.46,
    'F#5': 739.99, 'G5': 783.99, 'G#5': 830.61, 'A5': 880.00, 'A#5': 932.33, 'B5': 987.77,
    'C6': 1046.50, 'C#6': 1108.73, 'D6': 1174.66, 'D#6': 1244.51, 'E6': 1318.51, 'F6': 1396.91,
    'kick': 60, 'snare': 200, 'hihat': 800,
  };
  return noteMap[note] || 440;
};

let audioContext: AudioContext | null = null;
let gainNode: GainNode | null = null;
let activeOscillators = new Set<OscillatorNode>();
let isPaused = false;
let pauseTimeMs = 0;
let playStartTime = 0;
let playbackTimer: ReturnType<typeof setTimeout> | null = null;
let progressInterval: ReturnType<typeof setInterval> | null = null;
let currentSong: Song | null = null;
let onTimeUpdate: ((time: number) => void) | null = null;
let totalDurationMs = 0;

const getAudioContext = (): AudioContext => {
  if (!audioContext || audioContext.state === 'closed') {
    audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
    gainNode = audioContext.createGain();
    gainNode.connect(audioContext.destination);
    gainNode.gain.value = 0.5;
  }
  if (audioContext.state === 'suspended') {
    audioContext.resume();
  }
  return audioContext;
};

const playNote = (
  note: Note,
  scheduleFromTime: number,
  instrument: string = 'piano'
): void => {
  const ctx = getAudioContext();
  const startTime = ctx.currentTime + scheduleFromTime;
  const duration = note.duration;
  const freq = noteToFrequency(note.pitch);

  const osc = ctx.createOscillator();
  const noteGain = ctx.createGain();

  osc.connect(noteGain);
  noteGain.connect(gainNode!);

  if (instrument === 'drums') {
    osc.type = 'square';
    noteGain.gain.setValueAtTime(0.3, startTime);
    noteGain.gain.exponentialRampToValueAtTime(0.01, startTime + duration);
  } else if (instrument === 'bass') {
    osc.type = 'sine';
    noteGain.gain.setValueAtTime(0.4, startTime);
    noteGain.gain.exponentialRampToValueAtTime(0.01, startTime + duration);
  } else if (instrument === 'synthesizer') {
    osc.type = 'sawtooth';
    noteGain.gain.setValueAtTime(0.2, startTime);
    noteGain.gain.exponentialRampToValueAtTime(0.01, startTime + duration);
  } else if (instrument === 'lead') {
    osc.type = 'triangle';
    noteGain.gain.setValueAtTime(0.25, startTime);
    noteGain.gain.exponentialRampToValueAtTime(0.01, startTime + duration);
  } else {
    osc.type = 'sine';
    noteGain.gain.setValueAtTime(0.3, startTime);
    noteGain.gain.exponentialRampToValueAtTime(0.01, startTime + duration);
  }

  osc.frequency.setValueAtTime(freq, startTime);

  if (instrument === 'piano') {
    osc.frequency.exponentialRampToValueAtTime(freq * 0.99, startTime + 0.01);
  }

  osc.start(startTime);
  osc.stop(startTime + duration + 0.05);

  activeOscillators.add(osc);
  osc.onended = () => {
    activeOscillators.delete(osc);
  };
};

const scheduleSongNotes = (
  song: Song,
  fromTimeMs: number = 0
): number => {
  let maxEndTime = 0;

  song.tracks.forEach((track) => {
    track.notes.forEach((note) => {
      const noteEndMs = (note.start + note.duration) * 1000;
      if (noteEndMs <= fromTimeMs) return;

      const adjustedStart = Math.max(0, (note.start * 1000 - fromTimeMs) / 1000);
      const remainingDuration = (noteEndMs - fromTimeMs) / 1000;

      if (remainingDuration <= 0) return;

      const adjustedNote: Note = {
        ...note,
        start: adjustedStart,
        duration: remainingDuration,
      };
      playNote(adjustedNote, 0, track.instrument);
    });

    const trackEnd = Math.max(...track.notes.map(n => n.start + n.duration));
    if (trackEnd > maxEndTime) maxEndTime = trackEnd;
  });

  return maxEndTime;
};

export const playTrack = (track: Track): void => {
  stopPlayback();
  getAudioContext();
  const scheduleDelay = 0.05;
  track.notes.forEach((note) => {
    playNote(note, scheduleDelay, track.instrument);
  });
};

export const playSong = (
  song: Song,
  callback?: (time: number) => void
): void => {
  stopPlayback();

  currentSong = song;
  onTimeUpdate = callback || null;
  isPaused = false;
  pauseTimeMs = 0;
  totalDurationMs = song.duration * 1000;

  getAudioContext();
  playStartTime = Date.now();

  scheduleSongNotes(song, 0);

  if (onTimeUpdate) {
    progressInterval = setInterval(() => {
      const elapsed = Date.now() - playStartTime;
      onTimeUpdate!(elapsed / 1000);
      if (elapsed >= totalDurationMs) {
        clearInterval(progressInterval!);
        progressInterval = null;
        onTimeUpdate!(song.duration);
        cleanup();
      }
    }, 50);
  }
};

export const pausePlayback = (): void => {
  if (isPaused || !audioContext) return;
  isPaused = true;
  pauseTimeMs = Date.now() - playStartTime;

  if (progressInterval) {
    clearInterval(progressInterval);
    progressInterval = null;
  }

  activeOscillators.forEach((osc) => {
    try { osc.stop(); } catch (e) { /* already stopped */ }
  });
  activeOscillators.clear();
};

export const resumePlayback = (): void => {
  if (!isPaused || !currentSong) return;
  isPaused = false;

  getAudioContext();
  playStartTime = Date.now() - pauseTimeMs;

  const fromSeconds = pauseTimeMs / 1000;
  const remainingDuration = currentSong.duration - fromSeconds;

  if (remainingDuration <= 0) {
    cleanup();
    return;
  }

  scheduleSongNotes(currentSong, pauseTimeMs);

  if (onTimeUpdate) {
    progressInterval = setInterval(() => {
      const totalElapsed = Date.now() - playStartTime;
      onTimeUpdate!(totalElapsed / 1000);
      if (totalElapsed >= totalDurationMs) {
        clearInterval(progressInterval!);
        progressInterval = null;
        onTimeUpdate!(currentSong!.duration);
        cleanup();
      }
    }, 50);
  }
};

export const stopPlayback = (): void => {
  activeOscillators.forEach((osc) => {
    try { osc.stop(); } catch (e) { /* already stopped */ }
  });
  activeOscillators.clear();

  if (progressInterval) {
    clearInterval(progressInterval);
    progressInterval = null;
  }
  if (playbackTimer) {
    clearTimeout(playbackTimer);
    playbackTimer = null;
  }

  isPaused = false;
  pauseTimeMs = 0;
  currentSong = null;
  onTimeUpdate = null;
};

export const setVolume = (volume: number): void => {
  if (gainNode) {
    gainNode.gain.value = Math.max(0, Math.min(1, volume));
  }
};

export const getPlaybackState = () => ({
  isPaused,
  currentTimeMs: isPaused ? pauseTimeMs : (audioContext ? (Date.now() - playStartTime) : 0),
});

export const isCurrentlyPlaying = (): boolean => {
  return activeOscillators.size > 0 || isPaused;
};

const cleanup = (): void => {
  currentSong = null;
  onTimeUpdate = null;
  isPaused = false;
  pauseTimeMs = 0;
};
