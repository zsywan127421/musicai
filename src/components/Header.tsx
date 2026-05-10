import { Music, Headphones } from 'lucide-react';

export const Header = () => {
  return (
    <header className="sticky top-0 z-50 border-b border-[var(--color-border)]" style={{ background: 'rgba(11,11,26,0.9)', backdropFilter: 'blur(16px)' }}>
      <div className="max-w-7xl mx-auto px-6 py-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-lg flex items-center justify-center" style={{ background: 'linear-gradient(135deg, var(--color-primary), var(--color-primary-dark))' }}>
              <Music className="text-white" size={20} />
            </div>
            <div>
              <h1 className="text-lg font-bold text-[var(--color-text)]">音乐创作大师</h1>
              <p className="text-xs text-[var(--color-text-secondary)]">AI 驱动的音乐创作工具</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Headphones className="text-[var(--color-primary)]" size={20} />
            <span className="text-xs text-[var(--color-text-secondary)]">创作你的音乐</span>
          </div>
        </div>
      </div>
    </header>
  );
};
