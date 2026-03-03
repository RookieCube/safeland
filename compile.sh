#!/bin/bash

# Noise-Diffuse Chat 本地编译脚本
# 需要: JDK 17+, Android SDK (可选，用于完整编译)

set -e

echo "=========================================="
echo "Noise-Diffuse Chat 编译脚本"
echo "=========================================="

# 检查 Java
if ! command -v java &> /dev/null; then
    echo "❌ 错误: 未找到 Java"
    echo "请安装 JDK 17+:"
    echo "  - Ubuntu/Debian: sudo apt install openjdk-17-jdk"
    echo "  - Arch: sudo pacman -S jdk-openjdk"
    echo "  - macOS: brew install openjdk@17"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2)
echo "✓ Java 版本: $JAVA_VERSION"

# 检查 JAVA_HOME
if [ -z "$JAVA_HOME" ]; then
    export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
    echo "✓ 设置 JAVA_HOME=$JAVA_HOME"
fi

# 给予 gradlew 执行权限
chmod +x ./gradlew

echo ""
echo "=========================================="
echo "开始编译"
echo "=========================================="

# 清理
./gradlew clean

# 运行测试
echo ""
echo "→ 运行单元测试..."
./gradlew test

# 编译 Debug APK
echo ""
echo "→ 编译 Debug APK..."
./gradlew assembleDebug

# 编译 Release APK (需要签名配置)
echo ""
echo "→ 编译 Release APK (未签名)..."
./gradlew assembleRelease

echo ""
echo "=========================================="
echo "✅ 编译完成!"
echo "=========================================="
echo ""
echo "输出文件:"
echo "  Debug:   app/build/outputs/apk/debug/app-debug.apk"
echo "  Release: app/build/outputs/apk/release/app-release-unsigned.apk"
echo ""
echo "测试报告:"
echo "  app/build/reports/tests/"
echo ""
