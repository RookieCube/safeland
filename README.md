# SafeLand

🔒 **端到端加密 P2P 局域网聊天应用**

基于 Kotlin + Jetpack Compose 开发的纯 P2P 局域网聊天应用，采用噪声扩散机制保护通信安全。

[![Android CI](https://github.com/YOUR_USERNAME/safeland/actions/workflows/android-build.yml/badge.svg)](https://github.com/YOUR_USERNAME/safeland/actions/workflows/android-build.yml)

## ✨ 核心特性

### 🔐 安全机制

| 特性 | 实现 |
|------|------|
| **密钥交换** | X25519 ECDH |
| **对称加密** | ChaCha20-Poly1305 (AEAD) |
| **签名算法** | SHA256，取 hex 前 16 位 |
| **噪声扩散** | 10% 有效包 + 90% 噪声包，无标记位 |
| **HOST 验证** | 私钥签名防假 HOST |

### 📡 网络架构

- **协议**: UDP，端口 1338
- **架构**: 纯 P2P，无服务器
- **平台**: Android / PS Vita / PC / Linux（Kotlin Multiplatform 支持）

### 🎨 UI 设计

- **风格**: Telegram 风格 Material3 设计
- **主题**: 支持深色/浅色双主题
- **主色**: #29A58A（Telegram 绿）
- **动画**: 扩散式图片渲染、文本分段淡入

## 🚀 快速开始

### 云端编译（推荐）

本项目支持 GitHub Actions 云端编译，无需本地 Android SDK：

1. Fork 本仓库
2. 推送代码到 `main` 分支
3. GitHub Actions 自动编译
4. 从 Actions 页面下载 `app-release-unsigned.apk`

### 本地编译

#### 环境要求

- JDK 17+
- Android Studio Hedgehog (2023.1.1) 或更高版本
- Android SDK 34

#### 编译步骤

```bash
# 克隆仓库
git clone https://github.com/YOUR_USERNAME/safeland.git
cd SafeLand

# 编译 Release APK
./gradlew assembleRelease

# 输出路径
# app/build/outputs/apk/release/app-release-unsigned.apk
```

## 📁 项目结构

```
com.safeland.chat/
├── ui/
│   ├── theme/           # Material3 主题
│   ├── components/      # 可复用组件
│   └── screens/         # 界面
├── network/             # UDP 网络层
├── crypto/              # 加密模块
├── protocol/            # 协议实现
├── model/               # 数据模型
└── MainActivity.kt      # 入口
```

## 🔒 安全机制详解

### 噪声扩散机制

发送端每轮发送 20-30 个包：
- **10%** 有效包（满足 `sum(字节) % 173 == 111`）
- **90%** 噪声包（随机数据）
- **禁止**任何 valid=0/1 标记位

接收端：
- 所有包进入 **FIFO 队列**
- **每秒最多处理 12 个包**（限流防算力耗尽）
- 解密后验证 `sum%173==111`，有效则显示
- **队列永不主动清空**，确保低算力设备不丢内容

### HOST 身份验证

- 真 HOST = WiFi 热点创建者
- HOST 证明：用 X25519 私钥签名 `"HOST_VERIFY|热点名|timestamp"`
- 客户端只接受第一个合法签名的 HOST
- 假 HOST 无法伪造签名，全网无效

### 包格式

```
PKT|<timestamp>|<seq>|<signature>|<encrypted_payload>|<random_tail>
```

校验规则（任一失败直接丢弃）：
1. 格式：必须 6 段，管道符分隔
2. 时间戳：±30 秒有效窗口
3. Seq：严格递增，不回退
4. 签名：`SHA256(共享密钥 + ts + seq + payload).hex().take(16)`

## 🖼️ UI 预览

### 登录界面
- 昵称输入
- HOST 选项
- 安全特性展示

### 主聊天界面
- Telegram 风格消息气泡
- 扩散式图片渲染（16x16 网格）
- 文本分段淡入显示
- 侧拉用户列表
- 状态图标（单勾/双勾/实心双勾）

## 🛠️ 技术栈

- **语言**: Kotlin 1.9+
- **UI**: Jetpack Compose 1.5+, Material3
- **协程**: Kotlinx Coroutines
- **加密**: BouncyCastle (X25519, ChaCha20-Poly1305)
- **图片**: Coil 3.0
- **存储**: DataStore

## 🧪 测试

```bash
# 运行单元测试
./gradlew test

# 运行 lint 检查
./gradlew lint

# 生成测试报告
./gradlew jacocoTestReport
```

## 📋 验证清单

- [x] UDP 1338 端口能收发数据
- [x] 10 个包中只有约 1 个满足 `sum%173==111`
- [x] 假 HOST 无法通过验证
- [x] 图片分 16x16 块，随机顺序显示
- [x] 文本分段淡入显示
- [x] GitHub Actions 成功编译出 APK
- [x] 深色/浅色主题切换正常

## ⚠️ 禁止事项

❌ 在包中添加任何有效/无效标记位  
❌ 使用固定密钥而非每次会话生成 X25519 密钥对  
❌ 队列超时清空或丢包  
❌ 接受多个 HOST 或允许 HOST 身份切换  
❌ 图片不分块直接传输  
❌ 使用 Android SDK 特有 API（限制跨平台能力）

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 🙏 致谢

- [BouncyCastle](https://www.bouncycastle.org/) - 加密库
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - UI 框架
- [Telegram](https://telegram.org/) - UI 设计灵感
