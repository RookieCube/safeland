#!/bin/bash
##############################################################################
## Gradle Wrapper Script - 自动下载 Gradle
##############################################################################

# Gradle 版本
GRADLE_VERSION="8.5"
GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

# 目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GRADLE_USER_HOME="${SCRIPT_DIR}/.gradle"
GRADLE_HOME="${GRADLE_USER_HOME}/gradle-${GRADLE_VERSION}"
GRADLE_ZIP="${GRADLE_USER_HOME}/gradle-${GRADLE_VERSION}-bin.zip"

# Java 检查
if [ -z "$JAVA_HOME" ]; then
    export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
fi

if [ ! -x "$JAVA_HOME/bin/java" ]; then
    echo "❌ 错误: 找不到 Java"
    echo "请安装 JDK 17+: sudo pacman -S jdk17-openjdk"
    exit 1
fi

# 下载 Gradle（如果不存在）
if [ ! -d "$GRADLE_HOME" ]; then
    echo "→ 首次运行，正在下载 Gradle ${GRADLE_VERSION}..."
    mkdir -p "$GRADLE_USER_HOME"
    
    if command -v wget >/dev/null 2>&1; then
        wget --show-progress -q -O "$GRADLE_ZIP" "$GRADLE_URL"
    elif command -v curl >/dev/null 2>&1; then
        curl -L --progress-bar -o "$GRADLE_ZIP" "$GRADLE_URL"
    else
        echo "❌ 错误: 需要 wget 或 curl 来下载 Gradle"
        exit 1
    fi
    
    echo "→ 解压 Gradle..."
    unzip -q -o "$GRADLE_ZIP" -d "$GRADLE_USER_HOME"
    rm -f "$GRADLE_ZIP"
    echo "✓ Gradle 准备完成"
fi

# 运行 Gradle
export GRADLE_USER_HOME
exec "$GRADLE_HOME/bin/gradle" "$@"