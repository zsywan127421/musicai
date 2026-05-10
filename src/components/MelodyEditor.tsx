import { useState } from 'react';
import { useStore } from '../store';
import { generateMelody } from '../services/musicGenerator';
import { playTrack, stopPlayback } from '../services/audioPlayer';
import { Track } from '../types';
import { Music, Plus, Trash2, RefreshCw, Play } from 'lucide-react';

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

  return (
    <div className="card p-5">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-base font-semibold text-[var(--color-text)] flex items-center gap-2">
          <Music size={18} className="text-[var(--color-primary)]" />
          旋律编辑器
          <span className="text-xs text-[var(--color-text-muted)] font-normal">({melody.notes.length} 音符)</span>
        </h2>
        <div className="flex gap-2">
          <button onClick={handlePlayMelody} disabled={melody.notes.length === 0} className="btn btn-primary" style={{ padding: '6px 14px', fontSize: '13px' }}>
            <Play size={14} />
            播放
          </button>
          <button onClick={handleGenerate} disabled={melody.isGenerating} className="btn btn-secondary" style={{ padding: '6px 14px', fontSize: '13px' }}>
            <RefreshCw size={14} className={melody.isGenerating ? 'animate-spin' : ''} />
            {melody.isGenerating ? '生成中' : 'AI 生成'}
          </button>
          <button onClick={handleAddNote} className="btn btn-secondary" style={{ padding: '6px 14px', fontSize: '13px' }}>
            <Plus size={14} />
            添加
          </button>
        </div>
      </div>

      <div className="space-y-1.5 max-h-60 overflow-y-auto pr-1">
        {melody.notes.length === 0 && (
          <div className="text-center py-8 text-[var(--color-text-muted)] text-sm">
            点击「AI 生成」或「添加」开始创建旋律
          </div>
        )}
        {melody.notes.map((note, index) => (
          <div
            key={note.id}
            onClick={() => setSelectedNoteId(note.id)}
            className={`item-row ${selectedNoteId === note.id ? 'selected' : ''}`}
          >
            <span className="text-xs text-[var(--color-text-muted)] w-6 text-right font-mono">{index + 1}</span>
            <select
              value={note.pitch}
              onChange={(e) => updateMelodyNote(note.id, { pitch: e.target.value })}
              className="flex-1"
              style={{ minWidth: '80px', maxWidth: '110px' }}
            >
              {octaves.map((octave) =>
                pitches.map((pitch) => (
                  <option key={`${pitch}${octave}`} value={`${pitch}${octave}`}>{pitch}{octave}</option>
                ))
              )}
            </select>
            <select
              value={note.duration}
              onChange={(e) => updateMelodyNote(note.id, { duration: parseFloat(e.target.value) })}
              style={{ minWidth: '70px', maxWidth: '90px' }}
            >
              {durations.map((d) => (
                <option key={d} value={d}>{d === 0.25 ? '1/4' : d === 0.5 ? '1/2' : d === 1 ? '1 拍' : '2 拍'}</option>
              ))}
            </select>
            <span className="text-xs text-[var(--color-text-muted)] font-mono w-12 text-right">{note.start.toFixed(1)}s</span>
            <button onClick={(e) => { e.stopPropagation(); deleteMelodyNote(note.id); }} className="btn btn-danger" style={{ padding: '4px 8px', minWidth: 'auto' }}>
              <Trash2 size={14} />
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};
