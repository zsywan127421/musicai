import { Track, Song } from '../types';

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

const getAudioContext = (): AudioContext => {
  if (!audioContext) {
    audioContext = new (window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext)();
    gainNode = audioContext.createGain();
    gainNode.connect(audioContext.destination);
    gainNode.gain.value = 0.5;
  }
  if (audioContext.state === 'suspended') {
    audioContext.resume();
  }
  return audioContext;
};

const playNote = (note: string, duration: number, startTime: number, instrument: string = 'piano') => {
  const ctx = getAudioContext();
  const oscillator = ctx.createOscillator();
  const noteGain = ctx.createGain();
  
  oscillator.connect(noteGain);
  noteGain.connect(gainNode!);
  
  const freq = noteToFrequency(note);
  
  if (instrument === 'drums') {
    oscillator.type = 'square';
    noteGain.gain.setValueAtTime(0.3, startTime);
    noteGain.gain.exponentialRampToValueAtTime(0.01, startTime + duration);
  } else if (instrument === 'bass') {
    oscillator.type = 'sine';
    noteGain.gain.setValueAtTime(0.4, startTime);
    noteGain.gain.exponentialRampToValueAtTime(0.01, startTime + duration);
  } else if (instrument === 'synthesizer') {
    oscillator.type = 'sawtooth';
    noteGain.gain.setValueAtTime(0.2, startTime);
    noteGain.gain.exponentialRampToValueAtTime(0.01, startTime + duration);
  } else if (instrument === 'lead') {
    oscillator.type = 'triangle';
    noteGain.gain.setValueAtTime(0.25, startTime);
    noteGain.gain.exponentialRampToValueAtTime(0.01, startTime + duration);
  } else {
    oscillator.type = 'sine';
    noteGain.gain.setValueAtTime(0.3, startTime);
    noteGain.gain.exponentialRampToValueAtTime(0.01, startTime + duration);
  }
  
  oscillator.frequency.setValueAtTime(freq, startTime);
  
  if (instrument === 'piano') {
    oscillator.frequency.exponentialRampToValueAtTime(freq * 0.99, startTime + 0.01);
  }
  
  oscillator.start(startTime);
  oscillator.stop(startTime + duration);
};

export const playTrack = (track: Track, onTimeUpdate?: (time: number) => void) => {
  const ctx = getAudioContext();
  const startTime = ctx.currentTime;
  
  track.notes.forEach((note) => {
    playNote(note.pitch, note.duration, startTime + note.start, track.instrument);
  });
  
  if (onTimeUpdate) {
    const maxTime = Math.max(...track.notes.map((n) => n.start + n.duration));
    const interval = setInterval(() => {
      const elapsed = ctx.currentTime - startTime;
      onTimeUpdate(elapsed);
      if (elapsed >= maxTime) {
        clearInterval(interval);
        onTimeUpdate(maxTime);
      }
    }, 50);
  }
  
  return startTime + Math.max(...track.notes.map((n) => n.start + n.duration));
};

export const playSong = async (song: Song, onTimeUpdate?: (time: number) => void): Promise<void> => {
  const ctx = getAudioContext();
  const startTime = ctx.currentTime;
  let maxEndTime = startTime;
  
  song.tracks.forEach((track) => {
    track.notes.forEach((note) => {
      playNote(note.pitch, note.duration, startTime + note.start, track.instrument);
    });
    
    const trackEndTime = startTime + Math.max(...track.notes.map((n) => n.start + n.duration));
    if (trackEndTime > maxEndTime) {
      maxEndTime = trackEndTime;
    }
  });
  
  if (onTimeUpdate) {
    const interval = setInterval(() => {
      const elapsed = ctx.currentTime - startTime;
      onTimeUpdate(elapsed);
      if (elapsed >= song.duration) {
        clearInterval(interval);
        onTimeUpdate(song.duration);
      }
    }, 50);
  }
  
  await new Promise((resolve) => setTimeout(resolve, (maxEndTime - startTime) * 1000 + 100));
};

export const stopPlayback = (): void => {
  if (audioContext) {
    audioContext.close();
    audioContext = null;
    gainNode = null;
  }
};

export const setVolume = (volume: number): void => {
  if (gainNode) {
    gainNode.gain.value = volume;
  }
};
