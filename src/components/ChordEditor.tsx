import { useState } from 'react';
import { useStore } from '../store';
import { generateChords } from '../services/musicGenerator';
import { playTrack, stopPlayback } from '../services/audioPlayer';
import { Note, Track } from '../types';
import { GitBranch, Plus, Trash2, RefreshCw, Play } from 'lucide-react';

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

  return (
    <div className="card p-5">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-base font-semibold text-[var(--color-text)] flex items-center gap-2">
          <GitBranch size={18} className="text-[var(--color-primary)]" />
          和弦走向
          <span className="text-xs text-[var(--color-text-muted)] font-normal">({chords.chords.length} 和弦)</span>
        </h2>
        <div className="flex gap-2">
          <button onClick={handlePlayChords} disabled={chords.chords.length === 0} className="btn btn-primary" style={{ padding: '6px 14px', fontSize: '13px' }}>
            <Play size={14} />
            播放
          </button>
          <button onClick={handleGenerate} disabled={chords.isGenerating} className="btn btn-secondary" style={{ padding: '6px 14px', fontSize: '13px' }}>
            <RefreshCw size={14} className={chords.isGenerating ? 'animate-spin' : ''} />
            {chords.isGenerating ? '生成中' : 'AI 生成'}
          </button>
          <button onClick={handleAddChord} className="btn btn-secondary" style={{ padding: '6px 14px', fontSize: '13px' }}>
            <Plus size={14} />
            添加
          </button>
        </div>
      </div>

      <div className="flex flex-wrap gap-1.5 mb-4">
        {chords.chords.map((chord) => (
          <div
            key={chord.id}
            onClick={() => setSelectedChordId(chord.id)}
            className="chord-tag text-xs"
            style={selectedChordId === chord.id
              ? { background: 'linear-gradient(135deg, var(--color-primary), var(--color-primary-dark))', color: 'white', boxShadow: '0 2px 8px rgba(99,102,241,0.3)' }
              : { background: 'var(--color-surface)', color: 'var(--color-text-secondary)', border: '1px solid var(--color-border)' }
            }
          >
            {chord.name}
          </div>
        ))}
      </div>

      <div className="space-y-1.5 max-h-48 overflow-y-auto pr-1">
        {chords.chords.length === 0 && (
          <div className="text-center py-8 text-[var(--color-text-muted)] text-sm">
            点击「AI 生成」或「添加」开始创建和弦
          </div>
        )}
        {chords.chords.map((chord, index) => (
          <div
            key={chord.id}
            onClick={() => setSelectedChordId(chord.id)}
            className={`item-row ${selectedChordId === chord.id ? 'selected' : ''}`}
          >
            <span className="text-xs text-[var(--color-text-muted)] w-6 text-right font-mono">{index + 1}</span>
            <select
              value={chord.name}
              onChange={(e) => updateChord(chord.id, { name: e.target.value })}
              className="flex-1"
              style={{ minWidth: '80px', maxWidth: '120px' }}
            >
              {chordNames.map((name) =>
                chordQualities.map((quality) => (
                  <option key={`${name}${quality}`} value={`${name}${quality}`}>{name}{quality}</option>
                ))
              )}
            </select>
            <select
              value={chord.duration}
              onChange={(e) => updateChord(chord.id, { duration: parseFloat(e.target.value) })}
              style={{ minWidth: '70px', maxWidth: '90px' }}
            >
              {durations.map((d) => (
                <option key={d} value={d}>{d === 0.5 ? '半拍' : d === 1 ? '1 拍' : d === 2 ? '2 拍' : '4 拍'}</option>
              ))}
            </select>
            <span className="text-xs text-[var(--color-text-muted)] font-mono w-12 text-right">{chord.start.toFixed(1)}s</span>
            <button onClick={(e) => { e.stopPropagation(); deleteChord(chord.id); }} className="btn btn-danger" style={{ padding: '4px 8px', minWidth: 'auto' }}>
              <Trash2 size={14} />
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};
