@echo off
REM Music Composer - APK构建脚本 (Windows)

echo ==========================================
echo   🎵 音乐创作大师 - APK构建脚本
echo ==========================================
echo.

REM 检查Node.js
where node >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ 错误: 请先安装 Node.js
    echo    下载地址: https://nodejs.org/
    pause
    exit /b 1
)

REM 检查Java
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ 错误: 请先安装 JDK 17+
    echo    下载地址: https://adoptium.net/
    pause
    exit /b 1
)

echo ✅ 环境检查通过
echo.

echo 📦 步骤1/5: 安装项目依赖...
call npm install

echo.
echo 🌐 步骤2/5: 构建Web应用...
call npm run build

echo.
echo 📱 步骤3/5: 添加Android平台...
call npx cap add android

echo.
echo 🔄 步骤4/5: 同步到Android项目...
call npx cap sync android

echo.
echo 🔨 步骤5/5: 构建APK...
cd android
call gradlew assembleDebug
cd ..

echo.
echo ==========================================
echo   ✅ 构建完成！
echo ==========================================
echo.
echo 📍 APK文件位置:
echo    android\app\build\outputs\apk\debug\app-debug.apk
echo.
echo 📱 安装到设备:
echo    adb install android\app\build\outputs\apk\debug\app-debug.apk
echo.
pause
