export type MusicStyle = 'pop' | 'classical' | 'jazz' | 'electronic';

export interface Note {
  id: string;
  pitch: string;
  duration: number;
  start: number;
}

export interface Chord {
  id: string;
  name: string;
  duration: number;
  start: number;
}

export interface Track {
  id: string;
  name: string;
  instrument: string;
  notes: Note[];
}

export interface Song {
  id: string;
  name: string;
  style: MusicStyle;
  duration: number;
  tracks: Track[];
  chords: Chord[];
}

export interface MelodyState {
  notes: Note[];
  isGenerating: boolean;
}

export interface ChordState {
  chords: Chord[];
  isGenerating: boolean;
}

export interface SongState {
  song: Song | null;
  isGenerating: boolean;
  isPlaying: boolean;
  currentTime: number;
}

export interface AppState {
  style: MusicStyle;
  melody: MelodyState;
  chords: ChordState;
  song: SongState;
  setStyle: (style: MusicStyle) => void;
  setMelodyNotes: (notes: Note[]) => void;
  addMelodyNote: (note: Omit<Note, 'id'>) => void;
  updateMelodyNote: (id: string, updates: Partial<Omit<Note, 'id'>>) => void;
  deleteMelodyNote: (id: string) => void;
  setMelodyGenerating: (isGenerating: boolean) => void;
  setChords: (chords: Chord[]) => void;
  addChord: (chord: Omit<Chord, 'id'>) => void;
  updateChord: (id: string, updates: Partial<Omit<Chord, 'id'>>) => void;
  deleteChord: (id: string) => void;
  setChordsGenerating: (isGenerating: boolean) => void;
  setSong: (song: Song | null) => void;
  setSongGenerating: (isGenerating: boolean) => void;
  setPlaying: (isPlaying: boolean) => void;
  setCurrentTime: (currentTime: number) => void;
}
