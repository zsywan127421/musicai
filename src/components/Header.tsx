import { Music, Headphones } from 'lucide-react';

export const Header = () => {
  return (
    <header className="bg-gray-900/80 backdrop-blur-md border-b border-gray-700 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-gradient-to-br from-indigo-500 to-purple-500 rounded-xl flex items-center justify-center">
              <Music className="text-white" size={24} />
            </div>
            <div>
              <h1 className="text-xl font-bold text-white">音乐创作大师</h1>
              <p className="text-sm text-gray-400">AI驱动的音乐创作工具</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Headphones className="text-indigo-400" size={24} />
            <span className="text-gray-400 text-sm">🎵 创作你的音乐</span>
          </div>
        </div>
      </div>
    </header>
  );
};
