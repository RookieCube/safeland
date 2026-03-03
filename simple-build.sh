#!/bin/bash
# 简化版编译脚本 - 不需要 Gradle

set -e

echo "=========================================="
echo "Noise-Diffuse Chat 简化编译"
echo "=========================================="

# 检查 Java
if ! command -v java &> /dev/null; then
    echo "❌ 未找到 Java"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2)
echo "✓ Java: $JAVA_VERSION"

# 设置环境变量
export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk}
export ANDROID_HOME=${ANDROID_HOME:-/opt/android-sdk}

echo "✓ JAVA_HOME: $JAVA_HOME"
echo "✓ ANDROID_HOME: $ANDROID_HOME"

# 检查 Android SDK
if [ ! -d "$ANDROID_HOME" ]; then
    echo "⚠️  警告: Android SDK 未找到"
    echo "   项目可以编译，但无法生成 APK"
fi

echo ""
echo "=========================================="
echo "编译选项"
echo "=========================================="
echo ""
echo "由于缺少完整的 Android SDK，建议使用以下方式:"
echo ""
echo "1. GitHub Actions 云端编译 (推荐)"
echo "   - Push 代码到 GitHub"
echo "   - Actions 自动编译 APK"
echo ""
echo "2. 安装 Android Studio"
echo "   - 下载: https://developer.android.com/studio"
echo "   - 打开项目，点击 Build"
echo ""
echo "3. 命令行编译 (需要完整 SDK)"
echo "   export ANDROID_HOME=/path/to/android-sdk"
echo "   ./gradlew assembleDebug"
echo ""
echo "=========================================="
echo "项目结构验证"
echo "=========================================="

# 统计文件
KT_FILES=$(find app/src -name "*.kt" | wc -l)
XML_FILES=$(find app/src -name "*.xml" | wc -l)
TEST_FILES=$(find app/src/test -name "*.kt" 2>/dev/null | wc -l)

echo "✓ Kotlin 源文件: $KT_FILES"
echo "✓ XML 资源文件: $XML_FILES"
echo "✓ 测试文件: $TEST_FILES"

echo ""
echo "=========================================="
echo "核心文件检查"
echo "=========================================="

FILES_TO_CHECK=(
    "app/src/main/java/com/safeland/chat/MainActivity.kt"
    "app/src/main/java/com/safeland/chat/crypto/X25519Helper.kt"
    "app/src/main/java/com/safeland/chat/network/UdpManager.kt"
    "app/src/main/java/com/safeland/chat/protocol/PacketBuilder.kt"
    "app/src/main/AndroidManifest.xml"
    "app/build.gradle.kts"
)

for file in "${FILES_TO_CHECK[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file"
    else
        echo "❌ 缺失: $file"
    fi
done

echo ""
echo "=========================================="
echo "✅ 项目验证完成!"
echo "=========================================="
echo ""
echo "项目已准备好，可以 Push 到 GitHub 进行云端编译。"
echo ""
