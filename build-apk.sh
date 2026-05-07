#!/bin/bash
# Music Composer - APK构建脚本 (Linux/macOS)

echo "=========================================="
echo "  🎵 音乐创作大师 - APK构建脚本"
echo "=========================================="
echo ""

# 检查Node.js
if ! command -v node &> /dev/null; then
    echo "❌ 错误: 请先安装 Node.js"
    echo "   下载地址: https://nodejs.org/"
    exit 1
fi

# 检查Java
if ! command -v java &> /dev/null; then
    echo "❌ 错误: 请先安装 JDK 17+"
    echo "   下载地址: https://adoptium.net/"
    exit 1
fi

echo "✅ 环境检查通过"
echo ""

# 检查Android SDK
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "⚠️  警告: ANDROID_HOME 未设置"
    echo "   请安装 Android Studio 或设置 ANDROID_HOME"
    echo "   下载地址: https://developer.android.com/studio"
    echo ""
    read -p "是否继续？(y/n): " confirm
    if [ "$confirm" != "y" ]; then
        exit 1
    fi
fi

echo "📦 步骤1/5: 安装项目依赖..."
npm install

echo ""
echo "🌐 步骤2/5: 构建Web应用..."
npm run build

echo ""
echo "📱 步骤3/5: 添加Android平台..."
npx cap add android || true

echo ""
echo "🔄 步骤4/5: 同步到Android项目..."
npx cap sync android

echo ""
echo "🔨 步骤5/5: 构建APK..."
cd android

# 给gradlew执行权限
chmod +x gradlew

# 构建Debug APK
./gradlew assembleDebug

cd ..

echo ""
echo "=========================================="
echo "  ✅ 构建完成！"
echo "=========================================="
echo ""
echo "📍 APK文件位置:"
echo "   android/app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "📱 安装到设备:"
echo "   adb install android/app/build/outputs/apk/debug/app-debug.apk"
echo ""
