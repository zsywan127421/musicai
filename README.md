# 音乐创作大师 (MusicAI)

一款AI驱动的Android音乐创作工具，支持多种音乐风格，包括旋律编辑、和弦走向、完整曲子生成和音频播放功能。

## 功能特性

- 🎵 **旋律编辑器** - 手动输入或AI自动生成旋律
- 🎹 **和弦走向编辑器** - 手动配置或AI生成和弦进行
- 🎧 **曲子生成器** - 一键生成完整歌曲（含伴奏、贝斯、鼓点）
- 🎼 **播放控制** - 完整的音频播放、音量调节、速度控制（0.5x - 3.0x）
- 🎨 **多种风格** - 支持流行、古典、爵士、电子四种风格
- 🌙 **深色模式** - 支持跟随系统、浅色、深色三种主题模式
- 🌐 **多语言** - 支持简体中文、繁体中文、英文
- 📱 **苹果风格UI** - 简约现代的界面设计，圆角卡片布局

## 版本信息

**当前版本**: 1.0.0

## 更新日志

### v1.0.0 (最新)
- ✨ **全局主题系统** - 深色/浅色模式切换，跟随系统自动适配
- ✨ **设置页面** - 深色模式、字体大小、语言切换、自动保存等配置
- ✨ **大模型配置** - 支持自定义API地址、模型名称、API Key配置
- ✨ **播放器优化** - 速度控制从0.5x到3.0x，精确控制播放速度
- ✨ **音符编辑器** - 支持音符位置编辑（上移/下移功能）
- ✨ **UI统一优化** - 全局控件间距优化，增加呼吸感
- 🐛 **Bug修复** - 修复播放结束按钮状态、速度3.0x不生效等问题
- 🎨 **深色模式适配** - 所有页面、弹窗、按钮支持深色模式

## 技术栈

- **平台**: Android Native (Java/Kotlin)
- **UI**: Material Design + 自定义苹果风格组件
- **AI**: OpenAI API / DeepSeek API
- **音频**: WebView + JavaScript Audio API
- **状态管理**: SharedPreferences

## 本地开发

### 前置要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- Java Development Kit (JDK) 17+
- Android SDK (API 24+)

### 构建步骤

```bash
# 克隆项目
git clone <repository-url>
cd musicai

# 使用 Android Studio 打开 android 目录
# 或者命令行构建

cd android

# Debug版本
./gradlew assembleDebug

# Release版本 (需要签名配置)
./gradlew assembleRelease
```

### APK输出位置

- Debug APK: `android/app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `android/app/build/outputs/apk/release/app-release.apk`

## 功能使用

### AI生成旋律
1. 打开「高级歌曲生成器」或「快速歌曲生成」
2. 选择音乐风格
3. 输入描述（可选）
4. 点击生成按钮
5. 等待AI生成完成

### 播放器使用
1. 生成完成后，点击播放按钮试听
2. 使用速度控制调整播放速度（0.5x - 3.0x）
3. 播放过程中可随时停止

### 编辑旋律/和弦
1. 进入资源库，选择要编辑的项目
2. 点击编辑按钮
3. 在编辑器中添加、删除或调整音符/和弦
4. 保存修改

### 主题设置
1. 进入「设置」页面
2. 选择深色模式：跟随系统 / 浅色 / 深色
3. 主题将立即应用

## 项目结构

```
musicai/
├── app/
│   └── src/main/
│       ├── java/com/example/musicai/
│       │   ├── BaseActivity.java          # 基类Activity
│       │   ├── MainActivity.java           # 主界面
│       │   ├── SettingsActivity.java       # 设置页面
│       │   ├── ModelConfigActivity.java    # 大模型配置
│       │   ├── NewSongGeneratorActivity.java    # 高级生成器
│       │   ├── SongGeneratorActivity.java       # 快速生成器
│       │   ├── LibraryActivity.java             # 资源库
│       │   ├── LibraryDetailActivity.java       # 资源详情
│       │   ├── MelodyEditorActivity.java        # 旋律编辑器
│       │   ├── ChordEditorActivity.java         # 和弦编辑器
│       │   ├── util/
│       │   │   ├── ThemeManager.java      # 主题管理
│       │   │   ├── LanguageManager.java   # 语言管理
│       │   │   ├── MusicPlayerService.java # 播放器服务
│       │   │   └── ...
│       │   └── adapter/                   # 列表适配器
│       └── res/
│           ├── layout/                    # 布局文件
│           ├── drawable/                   # 图形资源
│           ├── values/                    # 浅色主题资源
│           └── values-night/              # 深色主题资源
├── android/                                # Capacitor Android项目
├── package.json
└── README.md
```

## License

MIT
