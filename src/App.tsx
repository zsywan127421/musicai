import { Header } from './components/Header';
import { StyleSelector } from './components/StyleSelector';
import { MelodyEditor } from './components/MelodyEditor';
import { ChordEditor } from './components/ChordEditor';
import { SongGenerator } from './components/SongGenerator';
import { PlayerControls } from './components/PlayerControls';

function App() {
  return (
    <div className="min-h-screen">
      <Header />
      <main className="max-w-7xl mx-auto px-4 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 space-y-6">
            <StyleSelector />
            <MelodyEditor />
            <ChordEditor />
          </div>
          <div className="space-y-6">
            <SongGenerator />
            <PlayerControls />
          </div>
        </div>
      </main>
      <footer className="bg-gray-900/50 border-t border-gray-800 py-6 mt-12">
        <div className="max-w-7xl mx-auto px-4 text-center text-gray-500 text-sm">
          <p>音乐创作大师 — AI 驱动的音乐创作工具</p>
          <p className="mt-1">支持流行 · 古典 · 爵士 · 电子等多种风格</p>
        </div>
      </footer>
    </div>
  );
}

export default App;
