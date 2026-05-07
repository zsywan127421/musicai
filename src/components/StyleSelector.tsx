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
    <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-4">
      <h3 className="text-white text-sm font-medium mb-3">音乐风格</h3>
      <div className="flex gap-2 flex-wrap">
        {styles.map(({ value, label, icon: Icon }) => (
          <button
            key={value}
            onClick={() => setStyle(value)}
            className={`style-chip flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium transition-all ${
              style === value
                ? 'bg-gradient-to-r from-indigo-500 to-purple-500 text-white shadow-lg shadow-indigo-500/30'
                : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
            }`}
          >
            <Icon size={16} />
            {label}
          </button>
        ))}
      </div>
    </div>
  );
};
