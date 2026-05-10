import { useState } from 'react';
import { useStore } from '../store';
import { generateChords } from '../services/musicGenerator';
import { playTrack, stopPlayback } from '../services/audioPlayer';
import { Note, Track } from '../types';
import { GitBranch, Plus, Trash2, RefreshCw, Play, Square } from 'lucide-react';

const chordNames = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B'];
const chordQualities = ['', 'm', '7', 'm7', 'M7', 'dim', 'aug'];
const durations = [0.5, 1, 2, 4];

const extractRootPitch = (chordName: string): string => {
  const sharpRoots = ['C#', 'D#', 'F#', 'G#', 'A#'];
  const prefix = chordName.substring(0, 2);
  if (sharpRoots.includes(prefix)) return prefix;
  return chordName.charAt(0);
};

export const ChordEditor = () => {
  const { chords, setChords, addChord, updateChord, deleteChord, setChordsGenerating, style } = useStore();
  const [selectedChordId, setSelectedChordId] = useState<string | null>(null);

  const handleGenerate = async () => {
    setChordsGenerating(true);
    try {
      const newChords = await generateChords(style);
      setChords(newChords);
    } finally {
      setChordsGenerating(false);
    }
  };

  const handlePlayChords = () => {
    if (chords.chords.length === 0) return;
    stopPlayback();
    const chordNotes: Note[] = chords.chords.map(c => ({
      id: c.id,
      pitch: extractRootPitch(c.name) + '4',
      duration: c.duration,
      start: c.start,
    }));
    const track: Track = {
      id: 'chord-preview',
      name: '和弦预览',
      instrument: 'piano',
      notes: chordNotes,
    };
    playTrack(track);
  };

  const handleAddChord = () => {
    const lastChord = chords.chords[chords.chords.length - 1];
    const newStart = lastChord ? lastChord.start + lastChord.duration : 0;
    addChord({ name: 'C', duration: 1, start: newStart });
  };

  const handleUpdateChord = (id: string, field: 'name' | 'duration', value: string | number) => {
    updateChord(id, { [field]: value });
  };

  const formatDuration = (d: number) => {
    if (d === 0.5) return '半拍';
    if (d === 1) return '1拍';
    if (d === 2) return '2拍';
    return `${d}拍`;
  };

  return (
    <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xl font-bold text-white flex items-center gap-2">
          <GitBranch className="text-amber-400" />
          和弦走向
        </h2>
        <div className="flex gap-2">
          <button
            onClick={handleGenerate}
            disabled={chords.isGenerating}
            className="play-button flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-amber-500 to-orange-500 text-white rounded-lg font-medium disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <RefreshCw size={16} className={chords.isGenerating ? 'animate-spin' : ''} />
            {chords.isGenerating ? '生成中...' : 'AI生成'}
          </button>
          <button
            onClick={handleAddChord}
            className="play-button flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg font-medium hover:bg-green-700"
          >
            <Plus size={16} />
            添加
          </button>
        </div>
      </div>

      <div className="flex flex-wrap gap-2 mb-4">
        {chords.chords.map((chord) => (
          <div
            key={chord.id}
            onClick={() => setSelectedChordId(chord.id)}
            className={`chord-tag px-4 py-2 rounded-full text-white font-medium cursor-pointer ${
              selectedChordId === chord.id
                ? 'bg-gradient-to-r from-amber-500 to-orange-500 ring-2 ring-amber-400'
                : 'bg-gray-700 hover:bg-gray-600'
            }`}
          >
            {chord.name}
          </div>
        ))}
      </div>

      <div className="space-y-2 max-h-48 overflow-y-auto pr-2">
        {chords.chords.map((chord, index) => (
          <div
            key={chord.id}
            onClick={() => setSelectedChordId(chord.id)}
            className={`note-cell flex items-center gap-3 p-3 rounded-lg cursor-pointer ${
              selectedChordId === chord.id ? 'bg-amber-500/30 border border-amber-400' : 'bg-gray-700/50'
            }`}
          >
            <span className="text-gray-400 text-sm w-8">#{index + 1}</span>
            <select
              value={chord.name}
              onChange={(e) => handleUpdateChord(chord.id, 'name', e.target.value)}
              className="note-input bg-gray-600 text-white px-3 py-1.5 rounded-lg text-sm"
            >
              {chordNames.map((name) =>
                chordQualities.map((quality) => (
                  <option key={`${name}${quality}`} value={`${name}${quality}`}>
                    {name}{quality}
                  </option>
                ))
              )}
            </select>
            <select
              value={chord.duration}
              onChange={(e) => handleUpdateChord(chord.id, 'duration', parseFloat(e.target.value))}
              className="note-input bg-gray-600 text-white px-3 py-1.5 rounded-lg text-sm"
            >
              {durations.map((d) => (
                <option key={d} value={d}>
                  {formatDuration(d)}
                </option>
              ))}
            </select>
            <span className="text-gray-400 text-sm w-16">
              {chord.start.toFixed(2)}s
            </span>
            <button
              onClick={(e) => {
                e.stopPropagation();
                deleteChord(chord.id);
              }}
              className="ml-auto text-red-400 hover:text-red-300 transition-colors"
            >
              <Trash2 size={18} />
            </button>
          </div>
        ))}
      </div>

      <button
        onClick={handlePlayChords}
        className="mt-4 w-full flex items-center justify-center gap-2 px-4 py-2 bg-gray-700 text-white rounded-lg font-medium hover:bg-gray-600 transition-colors"
      >
        <Play size={16} />
        播放和弦
      </button>
      <button
        onClick={stopPlayback}
        className="mt-2 w-full flex items-center justify-center gap-2 px-4 py-2 bg-gray-700 text-white rounded-lg font-medium hover:bg-red-700 transition-colors"
      >
        <Square size={16} />
        停止
      </button>
    </div>
  );
};
