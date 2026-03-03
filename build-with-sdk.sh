#!/bin/bash
# 直接使用 Android SDK 编译，不需要 Gradle

set -e

echo "=========================================="
echo "使用 Android SDK 直接编译"
echo "=========================================="

# 环境变量
export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk}
export ANDROID_HOME=${ANDROID_HOME:-/opt/android-sdk}
export PATH=$PATH:$ANDROID_HOME/build-tools/36:$ANDROID_HOME/platform-tools

echo "✓ JAVA_HOME: $JAVA_HOME"
echo "✓ ANDROID_HOME: $ANDROID_HOME"

# 检查工具
echo ""
echo "检查工具..."
command -v aapt >/dev/null 2>&1 || { echo "❌ 需要 aapt"; exit 1; }
command -v javac >/dev/null 2>&1 || { echo "❌ 需要 javac"; exit 1; }
echo "✓ aapt: $(which aapt)"
echo "✓ javac: $(which javac)"

# 项目目录
PROJECT_DIR="/data/SafeLand/noise-diffuse-chat"
BUILD_DIR="$PROJECT_DIR/build"

echo ""
echo "=========================================="
echo "编译步骤"
echo "=========================================="

# 1. 生成 R.java
echo "→ 1. 生成 R.java..."
mkdir -p $BUILD_DIR/gen
aapt package -f -m \
    -J $BUILD_DIR/gen \
    -S $PROJECT_DIR/app/src/main/res \
    -M $PROJECT_DIR/app/src/main/AndroidManifest.xml \
    -I $ANDROID_HOME/platforms/android-36/android.jar \
    --target-sdk-version 36 \
    --min-sdk-version 26 \
    --version-code 1 \
    --version-name "1.0.0"

echo "✓ R.java 生成完成"

# 2. 编译 Kotlin 文件
echo ""
echo "→ 2. 编译 Kotlin 文件..."
# 注意：这需要 kotlinc，如果没有需要安装
if ! command -v kotlinc >/dev/null 2>&1; then
    echo "⚠️  未找到 kotlinc，需要安装 Kotlin 编译器"
    echo "   sudo pacman -S kotlin"
    exit 1
fi

mkdir -p $BUILD_DIR/classes

# 编译所有 Kotlin 文件
find $PROJECT_DIR/app/src/main/java -name "*.kt" -type f > $BUILD_DIR/sources.txt

kotlinc \
    @$BUILD_DIR/sources.txt \
    -d $BUILD_DIR/classes \
    -cp "$ANDROID_HOME/platforms/android-36/android.jar" \
    -jvm-target 17 \
    -no-jdk

echo "✓ Kotlin 编译完成"

# 3. 打包 DEX
echo ""
echo "→ 3. 打包 DEX..."
d8 $BUILD_DIR/classes/**/*.class \
    --output $BUILD_DIR \
    --lib $ANDROID_HOME/platforms/android-36/android.jar

echo "✓ DEX 打包完成"

# 4. 打包 APK
echo ""
echo "→ 4. 打包 APK..."
aapt package -f \
    -M $PROJECT_DIR/app/src/main/AndroidManifest.xml \
    -S $PROJECT_DIR/app/src/main/res \
    -I $ANDROID_HOME/platforms/android-36/android.jar \
    -F $BUILD_DIR/unaligned.apk

# 添加 DEX
cd $BUILD_DIR
zip -qj unaligned.apk classes.dex

echo "✓ APK 打包完成"

# 5. 对齐和签名
echo ""
echo "→ 5. 对齐 APK..."
zipalign -f 4 unaligned.apk app-debug.apk

echo "✓ APK 对齐完成"

echo ""
echo "=========================================="
echo "✅ 编译完成!"
echo "=========================================="
echo ""
echo "输出: $BUILD_DIR/app-debug.apk"
echo ""
echo "注意：这是未签名的 APK，安装时需要:"
echo "  adb install -t app-debug.apk"
echo ""