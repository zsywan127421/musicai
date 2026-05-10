import { MusicStyle } from '../types';
import { useStore } from '../store';
import { Music, Piano, Guitar, Disc } from 'lucide-react';

const styles: { value: MusicStyle; label: string; icon: typeof Music }[] = [
  { value: 'pop', label: '流行', icon: Music },
  { value: 'classical', label: '古典', icon: Piano },
  { value: 'jazz', label: '爵士', icon: Guitar },
  { value: 'electronic', label: '电子', icon: Disc },
];

export const StyleSelector = () => {
  const { style, setStyle } = useStore();

  return (
    <div className="card p-4">
      <div className="flex items-center gap-2 mb-3">
        <Music size={16} className="text-[var(--color-primary)]" />
        <span className="text-sm font-medium text-[var(--color-text-secondary)]">音乐风格</span>
      </div>
      <div className="flex gap-2 flex-wrap">
        {styles.map(({ value, label, icon: Icon }) => (
          <button
            key={value}
            onClick={() => setStyle(value)}
            className={`chord-tag text-sm font-medium ${
              style === value
                ? 'text-white shadow-lg'
                : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text)]'
            }`}
            style={style === value
              ? { background: 'linear-gradient(135deg, var(--color-primary), var(--color-primary-dark))', boxShadow: '0 4px 14px rgba(99,102,241,0.3)' }
              : { background: 'var(--color-surface)', border: '1px solid var(--color-border)' }
            }
          >
            <Icon size={15} />
            {label}
          </button>
        ))}
      </div>
    </div>
  );
};
