import { useStore } from '../store';
import { playSong, pausePlayback, resumePlayback, stopPlayback, setVolume } from '../services/audioPlayer';
import { Play, Pause, Square, Volume2, VolumeX } from 'lucide-react';
import { useState, useEffect, useRef, useCallback } from 'react';

export const PlayerControls = () => {
  const { song, setPlaying, setCurrentTime } = useStore();
  const [volume, setVolumeState] = useState(50);
  const [isMuted, setIsMuted] = useState(false);
  const [isPaused, setIsPaused] = useState(false);
  const progressBarRef = useRef<HTMLDivElement>(null);

  const handlePlayPause = useCallback(() => {
    if (!song.song) return;

    if (isPaused) {
      resumePlayback();
      setIsPaused(false);
      setPlaying(true);
    } else if (song.isPlaying) {
      pausePlayback();
      setIsPaused(true);
      setPlaying(false);
    } else {
      setIsPaused(false);
      setPlaying(true);
      playSong(song.song, (time) => {
        setCurrentTime(time);
      });
    }
  }, [song.song, song.isPlaying, isPaused, setPlaying, setCurrentTime]);

  const handleStop = () => {
    stopPlayback();
    setIsPaused(false);
    setPlaying(false);
    setCurrentTime(0);
  };

  const handleVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newVolume = parseInt(e.target.value);
    setVolumeState(newVolume);
    setVolume(newVolume / 100);
    if (newVolume > 0 && isMuted) {
      setIsMuted(false);
    }
  };

  const handleMute = () => {
    const newMuted = !isMuted;
    setIsMuted(newMuted);
    setVolume(newMuted ? 0 : volume / 100);
  };

  const handleProgressClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!progressBarRef.current || !song.song) return;
    const rect = progressBarRef.current.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const pct = Math.max(0, Math.min(1, x / rect.width));
    // In a real app we'd seek; for now just visual feedback
    setCurrentTime(song.song.duration * pct);
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
    <div className="card p-5">
      <h2 className="text-base font-semibold text-[var(--color-text)] mb-4 flex items-center gap-2">
        <Play size={16} className="text-[var(--color-primary)]" />
        播放控制
      </h2>

      {song.song ? (
        <div className="space-y-4">
          {/* Controls */}
          <div className="flex items-center justify-center gap-4">
            <button
              onClick={handlePlayPause}
              className="btn btn-icon btn-primary"
              style={{ width: '52px', height: '52px' }}
            >
              {song.isPlaying ? <Pause size={24} /> : <Play size={24} />}
            </button>
            <button onClick={handleStop} className="btn btn-icon btn-secondary">
              <Square size={20} />
            </button>
          </div>

          {/* Progress bar */}
          <div className="space-y-1">
            <div
              ref={progressBarRef}
              onClick={handleProgressClick}
              className="h-2 rounded-full overflow-hidden cursor-pointer"
              style={{ background: 'var(--color-border)' }}
            >
              <div
                className="progress-bar h-full rounded-full"
                style={{ width: `${progress}%`, background: 'linear-gradient(90deg, var(--color-primary), var(--color-primary-hover))' }}
              />
            </div>
            <div className="flex items-center justify-between">
              <span className="text-xs font-mono" style={{ color: 'var(--color-text-muted)' }}>{formatTime(song.currentTime)}</span>
              <span className="text-xs font-mono" style={{ color: 'var(--color-text-muted)' }}>{formatTime(song.song.duration)}</span>
            </div>
          </div>

          {/* Volume */}
          <div className="flex items-center gap-3 px-1">
            <button onClick={handleMute} className="btn btn-ghost" style={{ padding: '4px' }}>
              {isMuted || volume === 0 ? <VolumeX size={18} /> : <Volume2 size={18} />}
            </button>
            <input
              type="range"
              min="0"
              max="100"
              value={isMuted ? 0 : volume}
              onChange={handleVolumeChange}
              className="flex-1"
            />
            <span className="text-xs font-mono w-8 text-right" style={{ color: 'var(--color-text-muted)' }}>
              {isMuted ? 0 : volume}%
            </span>
          </div>
        </div>
      ) : (
        <div className="py-10 text-center">
          <p className="text-sm" style={{ color: 'var(--color-text-muted)' }}>请先生成一首曲子</p>
        </div>
      )}
    </div>
  );
};
