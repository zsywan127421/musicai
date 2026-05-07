import { Note, Chord, Song, Track, MusicStyle } from '../types';

const generateId = () => Math.random().toString(36).substring(2, 9);

const pitches = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B'];

const getRandomElement = <T>(arr: T[]): T => arr[Math.floor(Math.random() * arr.length)];

const generateMelodyPattern = (style: MusicStyle): Note[] => {
  const notes: Note[] = [];
  let currentTime = 0;
  
  const patterns: Record<MusicStyle, { durations: number[]; range: [number, number] }> = {
    pop: { durations: [0.25, 0.5, 0.5, 0.25, 1], range: [4, 5] },
    classical: { durations: [1, 1, 0.5, 0.5, 2], range: [3, 5] },
    jazz: { durations: [0.25, 0.25, 0.5, 0.25, 0.25, 0.5], range: [3, 6] },
    electronic: { durations: [0.125, 0.125, 0.25, 0.5], range: [4, 7] },
  };
  
  const { durations, range } = patterns[style];
  const length = style === 'electronic' ? 16 : 8;
  
  for (let i = 0; i < length; i++) {
    const duration = getRandomElement(durations);
    const octave = Math.floor(Math.random() * (range[1] - range[0] + 1)) + range[0];
    const pitch = `${getRandomElement(pitches)}${octave}`;
    
    notes.push({
      id: generateId(),
      pitch,
      duration,
      start: currentTime,
    });
    
    currentTime += duration;
  }
  
  return notes.sort((a, b) => a.start - b.start);
};

const generateChordProgression = (style: MusicStyle): Chord[] => {
  const chords: Chord[] = [];
  let currentTime = 0;
  
  const progressions: Record<MusicStyle, string[]> = {
    pop: ['C', 'G', 'Am', 'F', 'C', 'G', 'Am', 'F'],
    classical: ['C', 'F', 'G', 'C', 'F', 'G', 'C', 'C'],
    jazz: ['C', 'Dm7', 'G7', 'C', 'F', 'Dm7', 'G7', 'C'],
    electronic: ['C', 'G', 'Am', 'F', 'C', 'G', 'Em', 'F'],
  };
  
  const progression = progressions[style];
  
  for (let i = 0; i < progression.length; i++) {
    chords.push({
      id: generateId(),
      name: progression[i],
      duration: 1,
      start: currentTime,
    });
    currentTime += 1;
  }
  
  return chords;
};

const generateAccompaniment = (chords: Chord[], style: MusicStyle): Track => {
  const notes: Note[] = [];
  
  const chordNotes: Record<string, string[]> = {
    'C': ['C4', 'E4', 'G4'],
    'G': ['G4', 'B4', 'D5'],
    'Am': ['A4', 'C5', 'E5'],
    'F': ['F4', 'A4', 'C5'],
    'Dm7': ['D4', 'F4', 'A4', 'C5'],
    'G7': ['G4', 'B4', 'D5', 'F5'],
    'Em': ['E4', 'G4', 'B4'],
  };
  
  chords.forEach((chord) => {
    const chordTones = chordNotes[chord.name] || ['C4', 'E4', 'G4'];
    
    if (style === 'classical') {
      notes.push({ id: generateId(), pitch: chordTones[0], duration: chord.duration, start: chord.start });
      notes.push({ id: generateId(), pitch: chordTones[1], duration: chord.duration, start: chord.start });
      notes.push({ id: generateId(), pitch: chordTones[2], duration: chord.duration, start: chord.start });
    } else if (style === 'jazz') {
      const arpeggioDur = chord.duration / chordTones.length;
      chordTones.forEach((tone, i) => {
        notes.push({
          id: generateId(),
          pitch: tone,
          duration: arpeggioDur,
          start: chord.start + arpeggioDur * i,
        });
      });
    } else {
      notes.push({ id: generateId(), pitch: chordTones[0], duration: chord.duration / 2, start: chord.start });
      notes.push({ id: generateId(), pitch: chordTones[1], duration: chord.duration / 2, start: chord.start });
      notes.push({ id: generateId(), pitch: chordTones[2], duration: chord.duration / 2, start: chord.start + chord.duration / 2 });
    }
  });
  
  return {
    id: generateId(),
    name: '和弦伴奏',
    instrument: style === 'electronic' ? 'synthesizer' : 'piano',
    notes,
  };
};

const generateBassLine = (chords: Chord[]): Track => {
  const notes: Note[] = [];
  
  const bassNotes: Record<string, string> = {
    'C': 'C3',
    'G': 'G3',
    'Am': 'A3',
    'F': 'F3',
    'Dm7': 'D3',
    'G7': 'G3',
    'Em': 'E3',
  };
  
  chords.forEach((chord) => {
    const bassNote = bassNotes[chord.name] || 'C3';
    notes.push({
      id: generateId(),
      pitch: bassNote,
      duration: chord.duration,
      start: chord.start,
    });
  });
  
  return {
    id: generateId(),
    name: '贝斯',
    instrument: 'bass',
    notes,
  };
};

const generateDrumPattern = (style: MusicStyle, duration: number): Track => {
  const notes: Note[] = [];
  const beatCount = Math.floor(duration / 0.25);
  
  for (let i = 0; i < beatCount; i++) {
    const time = i * 0.25;
    
    if (i % 4 === 0) {
      notes.push({ id: generateId(), pitch: 'kick', duration: 0.25, start: time });
    }
    
    if (style !== 'classical' && (i % 2 === 1)) {
      notes.push({ id: generateId(), pitch: 'snare', duration: 0.25, start: time });
    }
    
    if (style === 'electronic') {
      notes.push({ id: generateId(), pitch: 'hihat', duration: 0.125, start: time });
    }
  }
  
  return {
    id: generateId(),
    name: '鼓点',
    instrument: 'drums',
    notes,
  };
};

export const generateMelody = async (style: MusicStyle): Promise<Note[]> => {
  await new Promise((resolve) => setTimeout(resolve, 1000));
  return generateMelodyPattern(style);
};

export const generateChords = async (style: MusicStyle): Promise<Chord[]> => {
  await new Promise((resolve) => setTimeout(resolve, 800));
  return generateChordProgression(style);
};

export const generateSong = async (
  melody: Note[],
  chords: Chord[],
  style: MusicStyle
): Promise<Song> => {
  await new Promise((resolve) => setTimeout(resolve, 1500));
  
  const melodyTrack: Track = {
    id: generateId(),
    name: '主旋律',
    instrument: style === 'electronic' ? 'lead' : 'piano',
    notes: melody,
  };
  
  const accompanimentTrack = generateAccompaniment(chords, style);
  const bassTrack = generateBassLine(chords);
  
  const maxMelodyTime = melody.length > 0 ? Math.max(...melody.map((n) => n.start + n.duration)) : 4;
  const maxChordTime = chords.length > 0 ? Math.max(...chords.map((c) => c.start + c.duration)) : 4;
  const totalDuration = Math.max(maxMelodyTime, maxChordTime);
  
  const tracks: Track[] = [melodyTrack, accompanimentTrack, bassTrack];
  
  if (style !== 'classical') {
    tracks.push(generateDrumPattern(style, totalDuration));
  }
  
  return {
    id: generateId(),
    name: '生成的曲子',
    style,
    duration: totalDuration,
    tracks,
    chords,
  };
};
