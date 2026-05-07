import { useStore } from '../store';
import { generateSong } from '../services/musicGenerator';
import { Music2, Sparkles, FileMusic } from 'lucide-react';

export const SongGenerator = () => {
  const { melody, chords, song, setSong, setSongGenerating, style } = useStore();

  const handleGenerateSong = async () => {
    if (melody.notes.length === 0 || chords.chords.length === 0) {
      alert('请先添加旋律或和弦');
      return;
    }

    setSongGenerating(true);
    try {
      const newSong = await generateSong(melody.notes, chords.chords, style);
      setSong(newSong);
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
    <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xl font-bold text-white flex items-center gap-2">
          <Music2 className="text-green-400" />
          曲子生成
        </h2>
        <button
          onClick={handleGenerateSong}
          disabled={song.isGenerating}
          className="play-button flex items-center gap-2 px-6 py-3 bg-gradient-to-r from-green-500 to-emerald-500 text-white rounded-xl font-bold text-lg disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-green-500/30"
        >
          <Sparkles size={20} className={song.isGenerating ? 'animate-spin' : ''} />
          {song.isGenerating ? '生成中...' : '生成完整曲子'}
        </button>
      </div>

      {song.song && (
        <div className="mt-4 p-4 bg-gray-700/50 rounded-xl">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-lg font-semibold text-white">{song.song.name}</h3>
            <span className="text-gray-400 text-sm">时长: {formatDuration(song.song.duration)}</span>
          </div>
          <div className="space-y-2">
            {song.song.tracks.map((track) => (
              <div key={track.id} className="flex items-center justify-between p-2 bg-gray-600/50 rounded-lg">
                <span className="text-white text-sm">{track.name}</span>
                <span className="text-gray-400 text-xs">{track.instrument}</span>
                <span className="text-gray-400 text-xs">{track.notes.length} 音符</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {!song.song && (
        <div className="mt-4 p-8 bg-gray-700/30 rounded-xl text-center">
          <FileMusic className="mx-auto text-gray-500 mb-4" size={48} />
          <p className="text-gray-400">点击上方按钮生成完整曲子</p>
        </div>
      )}
    </div>
  );
};
