import { useStore } from '../store';
import { generateSong } from '../services/musicGenerator';
import { Sparkles, FileMusic, Clock, Music2 } from 'lucide-react';
import { useState } from 'react';

export const SongGenerator = () => {
  const { melody, chords, song, setSong, setSongGenerating, style } = useStore();
  const [error, setError] = useState<string | null>(null);

  const handleGenerateSong = async () => {
    if (melody.notes.length === 0 || chords.chords.length === 0) {
      setError('请先添加旋律和和弦');
      setTimeout(() => setError(null), 3000);
      return;
    }

    setSongGenerating(true);
    setError(null);
    try {
      const newSong = await generateSong(melody.notes, chords.chords, style);
      setSong(newSong);
    } catch {
      setError('生成失败，请重试');
      setTimeout(() => setError(null), 3000);
    } finally {
      setSongGenerating(false);
    }
  };

  const formatDuration = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <div className="card p-5">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-base font-semibold text-[var(--color-text)] flex items-center gap-2">
          <Music2 size={18} className="text-[var(--color-primary)]" />
          曲子生成
        </h2>
        <button
          onClick={handleGenerateSong}
          disabled={song.isGenerating}
          className="btn btn-primary"
          style={{ padding: '8px 20px', fontSize: '14px' }}
        >
          <Sparkles size={16} className={song.isGenerating ? 'animate-spin' : ''} />
          {song.isGenerating ? '生成中...' : '生成完整曲子'}
        </button>
      </div>

      {error && (
        <div className="mb-3 px-3 py-2 rounded-lg text-xs" style={{ background: 'rgba(239,68,68,0.15)', color: 'var(--color-error)', border: '1px solid rgba(239,68,68,0.3)' }}>
          {error}
        </div>
      )}

      {song.song && (
        <div className="p-3 rounded-lg" style={{ background: 'rgba(99,102,241,0.08)', border: '1px solid rgba(99,102,241,0.2)' }}>
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm font-medium text-[var(--color-text)]">{song.song.name}</span>
            <span className="text-xs text-[var(--color-text-muted)] flex items-center gap-1">
              <Clock size={12} />
              {formatDuration(song.song.duration)}
            </span>
          </div>
          <div className="space-y-1">
            {song.song.tracks.map((track) => (
              <div key={track.id} className="flex items-center justify-between px-2 py-1.5 rounded" style={{ background: 'var(--color-surface)' }}>
                <span className="text-xs text-[var(--color-text)]">{track.name}</span>
                <span className="text-xs text-[var(--color-text-muted)]">{track.instrument} · {track.notes.length} 音符</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {!song.song && !error && (
        <div className="py-10 text-center">
          <FileMusic size={36} className="mx-auto mb-3" style={{ color: 'var(--color-text-muted)' }} />
          <p className="text-sm" style={{ color: 'var(--color-text-muted)' }}>编辑旋律和和弦后，点击上方按钮生成完整曲子</p>
        </div>
      )}
    </div>
  );
};
