import { create } from 'zustand';
import { Note, Chord, Song, MusicStyle, AppState } from '../types';

const generateId = () => Math.random().toString(36).substring(2, 9);

const defaultMelodyNotes: Note[] = [
  { id: generateId(), pitch: 'C4', duration: 0.5, start: 0 },
  { id: generateId(), pitch: 'D4', duration: 0.5, start: 0.5 },
  { id: generateId(), pitch: 'E4', duration: 0.5, start: 1 },
  { id: generateId(), pitch: 'F4', duration: 0.5, start: 1.5 },
  { id: generateId(), pitch: 'G4', duration: 1, start: 2 },
];

const defaultChords: Chord[] = [
  { id: generateId(), name: 'C', duration: 1, start: 0 },
  { id: generateId(), name: 'G', duration: 1, start: 1 },
  { id: generateId(), name: 'Am', duration: 1, start: 2 },
  { id: generateId(), name: 'F', duration: 1, start: 3 },
];

export const useStore = create<AppState>((set) => ({
  style: 'pop',
  melody: {
    notes: defaultMelodyNotes,
    isGenerating: false,
  },
  chords: {
    chords: defaultChords,
    isGenerating: false,
  },
  song: {
    song: null,
    isGenerating: false,
    isPlaying: false,
    currentTime: 0,
  },
  setStyle: (style: MusicStyle) => set({ style }),
  setMelodyNotes: (notes: Note[]) => set((state) => ({ melody: { ...state.melody, notes } })),
  addMelodyNote: (note: Omit<Note, 'id'>) => set((state) => ({
    melody: {
      ...state.melody,
      notes: [...state.melody.notes, { ...note, id: generateId() }],
    },
  })),
  updateMelodyNote: (id: string, updates: Partial<Omit<Note, 'id'>>) => set((state) => ({
    melody: {
      ...state.melody,
      notes: state.melody.notes.map((n) => (n.id === id ? { ...n, ...updates } : n)),
    },
  })),
  deleteMelodyNote: (id: string) => set((state) => ({
    melody: {
      ...state.melody,
      notes: state.melody.notes.filter((n) => n.id !== id),
    },
  })),
  setMelodyGenerating: (isGenerating: boolean) => set((state) => ({
    melody: { ...state.melody, isGenerating },
  })),
  setChords: (chords: Chord[]) => set((state) => ({ chords: { ...state.chords, chords } })),
  addChord: (chord: Omit<Chord, 'id'>) => set((state) => ({
    chords: {
      ...state.chords,
      chords: [...state.chords.chords, { ...chord, id: generateId() }],
    },
  })),
  updateChord: (id: string, updates: Partial<Omit<Chord, 'id'>>) => set((state) => ({
    chords: {
      ...state.chords,
      chords: state.chords.chords.map((c) => (c.id === id ? { ...c, ...updates } : c)),
    },
  })),
  deleteChord: (id: string) => set((state) => ({
    chords: {
      ...state.chords,
      chords: state.chords.chords.filter((c) => c.id !== id),
    },
  })),
  setChordsGenerating: (isGenerating: boolean) => set((state) => ({
    chords: { ...state.chords, isGenerating },
  })),
  setSong: (song: Song | null) => set((state) => ({ song: { ...state.song, song } })),
  setSongGenerating: (isGenerating: boolean) => set((state) => ({
    song: { ...state.song, isGenerating },
  })),
  setPlaying: (isPlaying: boolean) => set((state) => ({
    song: { ...state.song, isPlaying },
  })),
  setCurrentTime: (currentTime: number) => set((state) => ({
    song: { ...state.song, currentTime },
  })),
}));
