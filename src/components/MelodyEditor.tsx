import { useState } from 'react';
import { useStore } from '../store';
import { generateMelody } from '../services/musicGenerator';
import { playTrack, stopPlayback } from '../services/audioPlayer';
import { Track } from '../types';
import { Music, Plus, Trash2, RefreshCw, Play, Square } from 'lucide-react';

const pitches = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B'];
const octaves = [3, 4, 5, 6];
const durations = [0.25, 0.5, 1, 2];

export const MelodyEditor = () => {
  const { melody, setMelodyNotes, addMelodyNote, updateMelodyNote, deleteMelodyNote, setMelodyGenerating, style } = useStore();
  const [selectedNoteId, setSelectedNoteId] = useState<string | null>(null);

  const handleGenerate = async () => {
    setMelodyGenerating(true);
    try {
      const newMelody = await generateMelody(style);
      setMelodyNotes(newMelody);
    } finally {
      setMelodyGenerating(false);
    }
  };

  const handlePlayMelody = () => {
    if (melody.notes.length === 0) return;
    stopPlayback();
    const track: Track = {
      id: 'melody-preview',
      name: '旋律预览',
      instrument: 'piano',
      notes: melody.notes,
    };
    playTrack(track);
  };

  const handleAddNote = () => {
    const lastNote = melody.notes[melody.notes.length - 1];
    const newStart = lastNote ? lastNote.start + lastNote.duration : 0;
    addMelodyNote({ pitch: 'C4', duration: 0.5, start: newStart });
  };

  const handleUpdateNote = (id: string, field: 'pitch' | 'duration', value: string | number) => {
    updateMelodyNote(id, { [field]: value });
  };

  return (
    <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xl font-bold text-white flex items-center gap-2">
          <Music className="text-indigo-400" />
          旋律编辑器
        </h2>
        <div className="flex gap-2">
          <button
            onClick={handleGenerate}
            disabled={melody.isGenerating}
            className="play-button flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-indigo-500 to-purple-500 text-white rounded-lg font-medium disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <RefreshCw size={16} className={melody.isGenerating ? 'animate-spin' : ''} />
            {melody.isGenerating ? '生成中...' : 'AI生成'}
          </button>
          <button
            onClick={handleAddNote}
            className="play-button flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg font-medium hover:bg-green-700"
          >
            <Plus size={16} />
            添加
          </button>
        </div>
      </div>

      <div className="space-y-2 max-h-64 overflow-y-auto pr-2">
        {melody.notes.map((note, index) => (
          <div
            key={note.id}
            onClick={() => setSelectedNoteId(note.id)}
            className={`note-cell flex items-center gap-3 p-3 rounded-lg cursor-pointer ${
              selectedNoteId === note.id ? 'bg-indigo-500/30 border border-indigo-400' : 'bg-gray-700/50'
            }`}
          >
            <span className="text-gray-400 text-sm w-8">#{index + 1}</span>
            <select
              value={note.pitch}
              onChange={(e) => handleUpdateNote(note.id, 'pitch', e.target.value)}
              className="note-input bg-gray-600 text-white px-3 py-1.5 rounded-lg text-sm"
            >
              {octaves.map((octave) =>
                pitches.map((pitch) => (
                  <option key={`${pitch}${octave}`} value={`${pitch}${octave}`}>
                    {pitch}{octave}
                  </option>
                ))
              )}
            </select>
            <select
              value={note.duration}
              onChange={(e) => handleUpdateNote(note.id, 'duration', parseFloat(e.target.value))}
              className="note-input bg-gray-600 text-white px-3 py-1.5 rounded-lg text-sm"
            >
              {durations.map((d) => (
                <option key={d} value={d}>
                  {d === 0.25 ? '1/4' : d === 0.5 ? '1/2' : d === 1 ? '1拍' : '2拍'}
                </option>
              ))}
            </select>
            <span className="text-gray-400 text-sm w-16">
              {note.start.toFixed(2)}s
            </span>
            <button
              onClick={(e) => {
                e.stopPropagation();
                deleteMelodyNote(note.id);
              }}
              className="ml-auto text-red-400 hover:text-red-300 transition-colors"
            >
              <Trash2 size={18} />
            </button>
          </div>
        ))}
      </div>

      <button
        onClick={handlePlayMelody}
        className="mt-4 w-full flex items-center justify-center gap-2 px-4 py-2 bg-gray-700 text-white rounded-lg font-medium hover:bg-gray-600 transition-colors"
      >
        <Play size={16} />
        播放旋律
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
