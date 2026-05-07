# 音乐创作大师 (Music Composer)

一款AI驱动的Web音乐创作工具，支持多种音乐风格，包括旋律编辑、和弦走向、完整曲子生成和音频播放功能。

## 功能特性

- 🎵 **旋律编辑器** - 手动输入或AI自动生成旋律
- 🎹 **和弦走向编辑器** - 手动配置或AI生成和弦进行
- 🎧 **曲子生成器** - 一键生成完整歌曲（含伴奏、贝斯、鼓点）
- 🎼 **播放控制** - 完整的音频播放、音量调节
- 🎨 **多种风格** - 支持流行、古典、爵士、电子四种风格

## 技术栈

- **前端**: React 18 + TypeScript + Vite
- **UI**: TailwindCSS
- **状态管理**: Zustand
- **音频**: Web Audio API
- **移动打包**: Capacitor

## 本地开发

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build
```

## 上传到GitHub

1. 在 GitHub 上创建一个新仓库
2. 在项目根目录执行以下命令：

```bash
# 添加远程仓库（替换为你的仓库地址）
git remote add origin https://github.com/你的用户名/music-composer.git

# 推送到GitHub
git branch -M main
git push -u origin main
```

## 打包APK

### 前置要求

- Android Studio
- Java Development Kit (JDK) 17+
- Android SDK

### 构建步骤

1. **确保构建Web资源**
```bash
npm run build
```

2. **同步到Android项目**
```bash
npx cap sync android
```

3. **构建APK**
```bash
cd android

# Debug版本
./gradlew assembleDebug

# Release版本
./gradlew assembleRelease
```

4. **找到APK文件**
- Debug APK: `android/app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `android/app/build/outputs/apk/release/app-release.apk`

## 使用说明

1. 选择音乐风格（流行/古典/爵士/电子）
2. 在旋律编辑器中添加或生成旋律
3. 在和弦编辑器中配置或生成和弦走向
4. 点击「生成完整曲子」
5. 使用播放控制试听和调整

## 项目结构

```
music-composer/
├── src/
│   ├── components/       # UI组件
│   │   ├── ChordEditor.tsx
│   │   ├── Header.tsx
│   │   ├── MelodyEditor.tsx
│   │   ├── PlayerControls.tsx
│   │   ├── SongGenerator.tsx
│   │   └── StyleSelector.tsx
│   ├── services/         # 核心服务
│   │   ├── audioPlayer.ts
│   │   └── musicGenerator.ts
│   ├── store/            # 状态管理
│   │   └── index.ts
│   ├── types/            # TypeScript类型
│   │   └── index.ts
│   ├── App.tsx
│   ├── main.tsx
│   └── index.css
├── android/              # Capacitor Android项目
├── package.json
├── tsconfig.json
├── vite.config.ts
└── capacitor.config.json
```

## License

MIT
