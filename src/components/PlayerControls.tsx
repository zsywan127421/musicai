import { useStore } from '../store';
import { playSong, stopPlayback, setVolume } from '../services/audioPlayer';
import { Play, Pause, Square, Volume2, VolumeX } from 'lucide-react';
import { useState, useEffect } from 'react';

export const PlayerControls = () => {
  const { song, setPlaying, setCurrentTime } = useStore();
  const [volume, setVolumeState] = useState(50);
  const [isMuted, setIsMuted] = useState(false);

  const handlePlay = async () => {
    if (!song.song) return;

    setPlaying(true);
    try {
      await playSong(song.song, (time) => {
        setCurrentTime(time);
      });
    } finally {
      setPlaying(false);
      setCurrentTime(0);
    }
  };

  const handlePause = () => {
    stopPlayback();
    setPlaying(false);
  };

  const handleStop = () => {
    stopPlayback();
    setPlaying(false);
    setCurrentTime(0);
  };

  const handleVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newVolume = parseInt(e.target.value);
    setVolumeState(newVolume);
    setVolume(newVolume / 100);
  };

  const handleMute = () => {
    setIsMuted(!isMuted);
    setVolume(isMuted ? volume / 100 : 0);
  };

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const progress = song.song ? (song.currentTime / song.song.duration) * 100 : 0;

  useEffect(() => {
    return () => {
      stopPlayback();
    };
  }, []);

  return (
    <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6">
      <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
        <Play className="text-indigo-400" />
        播放控制
      </h2>

      {song.song ? (
        <div className="space-y-4">
          <div className="flex items-center gap-3">
            <button
              onClick={song.isPlaying ? handlePause : handlePlay}
              disabled={!song.song}
              className="play-button w-14 h-14 rounded-full bg-gradient-to-r from-indigo-500 to-purple-500 text-white flex items-center justify-center shadow-lg shadow-indigo-500/30 disabled:opacity-50"
            >
              {song.isPlaying ? <Pause size={28} /> : <Play size={28} />}
            </button>
            <button
              onClick={handleStop}
              disabled={!song.song}
              className="play-button w-12 h-12 rounded-full bg-gray-700 text-white flex items-center justify-center hover:bg-gray-600 disabled:opacity-50"
            >
              <Square size={22} />
            </button>
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between text-gray-400 text-sm">
              <span>{formatTime(song.currentTime)}</span>
              <span>{formatTime(song.song.duration)}</span>
            </div>
            <div className="h-2 bg-gray-700 rounded-full overflow-hidden">
              <div
                className="progress-bar h-full bg-gradient-to-r from-indigo-500 to-purple-500 rounded-full"
                style={{ width: `${progress}%` }}
              />
            </div>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={handleMute}
              className="text-gray-400 hover:text-white transition-colors"
            >
              {isMuted ? <VolumeX size={20} /> : <Volume2 size={20} />}
            </button>
            <input
              type="range"
              min="0"
              max="100"
              value={isMuted ? 0 : volume}
              onChange={handleVolumeChange}
              className="flex-1 slider-track"
            />
            <span className="text-gray-400 text-sm w-8">{isMuted ? 0 : volume}%</span>
          </div>
        </div>
      ) : (
        <div className="text-center py-8">
          <p className="text-gray-400">请先生成一首曲子</p>
        </div>
      )}
    </div>
  );
};
