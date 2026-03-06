package com.safeland.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.safeland.chat.crypto.X25519Helper
import com.safeland.chat.model.*
import com.safeland.chat.network.PacketQueue
import com.safeland.chat.network.UdpManager
import com.safeland.chat.protocol.HostVerifier
import com.safeland.chat.protocol.PacketBuilder
import com.safeland.chat.protocol.PacketParser
import com.safeland.chat.ui.screens.*
import com.safeland.chat.ui.theme.SafeLandTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.UUID
import android.util.Base64

/**
 * 主Activity
 * 单Activity Compose架构入口
 */
class MainActivity : ComponentActivity() {

    // 网络组件
    private lateinit var udpManager: UdpManager
    private lateinit var packetQueue: PacketQueue

    // 加密组件
    private var keyPair: X25519Helper.KeyPair? = null
    private var sharedSecret: ByteArray? = null

    // 协议组件
    private var packetBuilder: PacketBuilder? = null
    private var packetParser: PacketParser? = null
    private val hostVerifier = HostVerifier()

    // 状态
    private var currentUser by mutableStateOf<User?>(null)
    private var messages = mutableStateListOf<Message>()
    private var users = mutableStateListOf<User>()
    private var availableRooms = mutableStateListOf<ChatRoom>()
    private var isConnected by mutableStateOf(false)
    private var isHost by mutableStateOf(false)
    private var roomName by mutableStateOf("SafeLand 聊天室")
    private var blacklist = mutableStateListOf<String>()
    private var networkLatency by mutableStateOf<Long?>(null)

    // 图片传输
    private val pendingImages = mutableMapOf<String, MutableList<ImageBlock>>()

    // 协程作用域
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 权限请求
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            initializeNetwork()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 检查权限
        checkPermissions()

        setContent {
            SafeLandTheme {
                var currentScreen by remember { mutableStateOf(Screen.ROOM_LIST) }
                var showSettings by remember { mutableStateOf(false) }
                var showBlacklist by remember { mutableStateOf(false) }
                var showNetworkStatus by remember { mutableStateOf(false) }
                var showUserProfile by remember { mutableStateOf<User?>(null) }

                // 图片选择器
                val imagePicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let { sendImageMessage(it) }
                }

                when (currentScreen) {
                    Screen.ROOM_LIST -> {
                        RoomListScreen(
                            rooms = availableRooms,
                            onCreateRoom = { nickname ->
                                isHost = true
                                login(nickname, true)
                                currentScreen = Screen.CHAT
                            },
                            onJoinRoom = { room, nickname ->
                                isHost = false
                                login(nickname, false)
                                currentScreen = Screen.CHAT
                            },
                            onRefresh = { discoverRooms() }
                        )
                    }
                    Screen.CHAT -> {
                        MainChatScreen(
                            currentUser = currentUser,
                            messages = messages,
                            users = users,
                            isConnected = isConnected,
                            isHost = isHost,
                            roomName = roomName,
                            localIp = if (::udpManager.isInitialized) udpManager.getLocalIpAddress() else "",
                            networkLatency = networkLatency,
                            onSendMessage = { content ->
                                sendTextMessage(content)
                            },
                            onSendImage = {
                                imagePicker.launch("image/*")
                            },
                            onSettingsClick = { showSettings = true },
                            onBlacklistClick = { showBlacklist = true },
                            onNetworkStatusClick = { showNetworkStatus = true },
                            onLogout = {
                                logout()
                                currentScreen = Screen.ROOM_LIST
                            },
                            onUserClick = { user ->
                                showUserProfile = user
                            }
                        )
                    }
                }

                // 设置对话框
                if (showSettings) {
                    com.safeland.chat.ui.components.SettingsDialog(
                        onDismiss = { showSettings = false },
                        onClearData = {
                            clearAllData()
                            showSettings = false
                        }
                    )
                }

                // 黑名单对话框
                if (showBlacklist) {
                    com.safeland.chat.ui.components.BlacklistDialog(
                        blacklist = blacklist,
                        onDismiss = { showBlacklist = false },
                        onRemove = { item ->
                            blacklist.remove(item)
                        }
                    )
                }

                // 网络状态对话框
                if (showNetworkStatus) {
                    com.safeland.chat.ui.components.NetworkStatusDialog(
                        isConnected = isConnected,
                        localIp = if (::udpManager.isInitialized) udpManager.getLocalIpAddress() else "",
                        port = 1338,
                        latency = networkLatency,
                        onDismiss = { showNetworkStatus = false }
                    )
                }

                // 用户资料对话框
                showUserProfile?.let { user ->
                    com.safeland.chat.ui.components.UserProfileDialog(
                        user = user,
                        onDismiss = { showUserProfile = null },
                        onSendPrivateMessage = {
                            // TODO: 实现私聊
                            showUserProfile = null
                        },
                        onAddToBlacklist = {
                            if (!blacklist.contains(user.id)) {
                                blacklist.add(user.id)
                            }
                            showUserProfile = null
                        }
                    )
                }
            }
        }
    }

    /**
     * 检查并请求必要权限
     */
    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        val needsPermission = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needsPermission) {
            permissionLauncher.launch(permissions)
        } else {
            initializeNetwork()
        }
    }

    /**
     * 初始化网络组件
     */
    private fun initializeNetwork() {
        udpManager = UdpManager(port = 1338, scope = scope)

        packetQueue = PacketQueue(scope = scope) { packet, senderIp, decrypted ->
            handleValidPacket(packet, senderIp, decrypted)
        }

        // 设置解密函数
        packetQueue.decryptFunction = { encrypted ->
            packetParser?.decryptOnly(
                com.safeland.chat.model.Packet(
                    timestamp = 0,
                    seq = 0,
                    signature = "",
                    encryptedPayload = encrypted,
                    randomTail = ""
                )
            )
        }

        // 启动网络
        udpManager.start()
        packetQueue.start()
        isConnected = true

        // 监听传入数据包
        scope.launch {
            udpManager.incomingPackets.collectLatest { (packet, senderIp) ->
                // 验证包格式
                if (packetParser?.verifyOnly(packet) == true) {
                    packetQueue.enqueue(packet, senderIp)
                }
            }
        }

        // 开始发现房间和网络检测
        discoverRooms()
        startNetworkLatencyCheck()
    }

    /**
     * 网络延迟检测
     */
    private fun startNetworkLatencyCheck() {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                val startTime = System.currentTimeMillis()
                // 发送ping包
                val builder = packetBuilder
                if (builder != null) {
                    try {
                        val pingPacket = builder.createPacket(
                            type = "PING",
                            content = startTime.toString()
                        )
                        udpManager.broadcastPacket(pingPacket)
                    } catch (e: Exception) {
                        // 忽略错误
                    }
                }
                delay(5000) // 每5秒检测一次
            }
        }
    }

    /**
     * 发现聊天室
     */
    private fun discoverRooms() {
        scope.launch(Dispatchers.IO) {
            // 模拟发现房间（实际应通过广播发现）
            // TODO: 实现真实的房间发现逻辑
        }
    }

    /**
     * 用户登录
     */
    private fun login(nickname: String, asHost: Boolean) {
        isHost = asHost

        // 生成X25519密钥对
        keyPair = X25519Helper.generateKeyPair()

        // 创建用户
        currentUser = User(
            id = UUID.randomUUID().toString(),
            name = nickname,
            publicKey = keyPair?.publicKeyBase64() ?: "",
            role = if (asHost) UserRole.HOST else UserRole.CLIENT,
            ipAddress = if (::udpManager.isInitialized) udpManager.getLocalIpAddress() else ""
        )

        // 如果是HOST，生成共享密钥（自己和自己）
        if (asHost) {
            sharedSecret = keyPair?.let { kp ->
                X25519Helper.generateSharedSecret(kp.privateKey, kp.publicKey)
            }
            roomName = "$nickname 的房间"
        }

        // 初始化协议组件
        sharedSecret?.let { secret ->
            packetBuilder = PacketBuilder(
                sharedSecret = secret,
                senderId = currentUser?.id ?: "",
                senderName = currentUser?.name ?: ""
            )
            packetParser = PacketParser(sharedSecret = secret)
        }

        // 如果是HOST，广播HOST验证
        if (asHost) {
            broadcastHostVerification()
        }
    }

    /**
     * 退出登录
     */
    private fun logout() {
        currentUser = null
        messages.clear()
        users.clear()
        isHost = false
        roomName = "SafeLand 聊天室"
        keyPair = null
        sharedSecret = null
        packetBuilder = null
        packetParser = null
    }

    /**
     * 清除所有数据
     */
    private fun clearAllData() {
        logout()
        blacklist.clear()
    }

    /**
     * 广播HOST验证
     */
    private fun broadcastHostVerification() {
        scope.launch(Dispatchers.IO) {
            val kp = keyPair ?: return@launch
            val user = currentUser ?: return@launch

            val signature = hostVerifier.generateHostSignature(
                kp.privateKey,
                roomName
            )

            val hostContent = hostVerifier.createHostVerifyContent(
                hotspotName = roomName,
                publicKey = user.publicKey,
                privateKey = kp.privateKey
            )

            // 创建并发送HOST验证包
            val builder = packetBuilder ?: return@launch
            // 这里简化处理，实际应使用builder创建包
        }
    }

    /**
     * 发送文本消息
     */
    private fun sendTextMessage(content: String) {
        val builder = packetBuilder ?: return
        val user = currentUser ?: return

        // 添加到本地消息列表
        val message = Message(
            senderId = user.id,
            senderName = user.name,
            content = content,
            type = MessageType.TEXT,
            isFromMe = true,
            status = MessageStatus.SENDING
        )
        messages.add(message)

        // 发送数据包（带噪声扩散）
        scope.launch(Dispatchers.IO) {
            try {
                val packets = builder.createPacketBatch(
                    type = "MESSAGE",
                    content = content
                )

                // 广播所有包
                packets.forEach { packet ->
                    udpManager.broadcastPacket(packet)
                }

                // 更新状态为已送达
                updateMessageStatus(message.id, MessageStatus.DELIVERED)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 发送图片消息
     */
    private fun sendImageMessage(uri: Uri) {
        val builder = packetBuilder ?: return
        val user = currentUser ?: return

        scope.launch(Dispatchers.IO) {
            try {
                // 读取图片并压缩
                val inputStream = contentResolver.openInputStream(uri)
                val imageBytes = inputStream?.readBytes() ?: return@launch
                inputStream.close()

                // 压缩图片
                val compressedImage = compressImage(imageBytes, maxSize = 500 * 1024) // 最大500KB
                val base64Image = Base64.encodeToString(compressedImage, Base64.DEFAULT)

                // 创建消息
                val message = Message(
                    senderId = user.id,
                    senderName = user.name,
                    content = base64Image,
                    type = MessageType.IMAGE,
                    isFromMe = true,
                    status = MessageStatus.SENDING
                )

                withContext(Dispatchers.Main) {
                    messages.add(message)
                }

                // 分块发送
                val chunkSize = 1024 // 每块1KB
                val chunks = base64Image.chunked(chunkSize)

                chunks.forEachIndexed { index, chunk ->
                    val packet = builder.createPacket(
                        type = "IMAGE_CHUNK",
                        content = "$index:${chunks.size}:$chunk"
                    )
                    udpManager.broadcastPacket(packet)
                    delay(50) // 避免发送过快
                }

                // 发送完成标记
                val completePacket = builder.createPacket(
                    type = "IMAGE_COMPLETE",
                    content = message.id
                )
                udpManager.broadcastPacket(completePacket)

                withContext(Dispatchers.Main) {
                    updateMessageStatus(message.id, MessageStatus.DELIVERED)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 压缩图片
     */
    private fun compressImage(imageBytes: ByteArray, maxSize: Int): ByteArray {
        // 简化实现：如果图片太大，进行简单压缩
        return if (imageBytes.size > maxSize) {
            // 实际应该使用 BitmapFactory 进行真正的图片压缩
            // 这里简化处理
            imageBytes.take(maxSize).toByteArray()
        } else {
            imageBytes
        }
    }

    /**
     * 处理有效数据包
     */
    private suspend fun handleValidPacket(
        packet: com.safeland.chat.model.Packet,
        senderIp: String,
        decrypted: ByteArray
    ) {
        withContext(Dispatchers.Main) {
            try {
                val payload = packetParser?.parsePayload(decrypted)
                    ?: return@withContext

                val type = payload["type"] ?: return@withContext
                val senderId = payload["senderId"] ?: return@withContext
                val senderName = payload["senderName"] ?: "未知用户"
                val content = payload["content"] ?: ""

                // 检查黑名单
                if (blacklist.contains(senderId)) {
                    return@withContext
                }

                when (type) {
                    "MESSAGE" -> {
                        handleIncomingMessage(senderId, senderName, content)
                    }
                    "IMAGE_CHUNK" -> {
                        handleImageChunk(senderId, content)
                    }
                    "IMAGE_COMPLETE" -> {
                        handleImageComplete(senderId, content)
                    }
                    "READ_RECEIPT" -> {
                        handleReadReceipt(content)
                    }
                    "PING" -> {
                        handlePing(senderIp, content)
                    }
                    "PONG" -> {
                        handlePong(content)
                    }
                    "HOST_VERIFY" -> {
                        handleHostVerification(packet, senderIp)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 处理文本消息
     */
    private fun handleIncomingMessage(senderId: String, senderName: String, content: String) {
        val message = Message(
            senderId = senderId,
            senderName = senderName,
            content = content,
            type = MessageType.TEXT,
            isFromMe = false,
            status = MessageStatus.READ
        )
        messages.add(message)

        // 发送已读回执
        sendReadReceipt(message.id)
    }

    /**
     * 处理图片分块
     */
    private fun handleImageChunk(senderId: String, content: String) {
        val parts = content.split(":", limit = 3)
        if (parts.size < 3) return

        val index = parts[0].toIntOrNull() ?: return
        val total = parts[1].toIntOrNull() ?: return
        val chunkData = parts[2]

        val blocks = pendingImages.getOrPut(senderId) { mutableListOf() }
        // 存储分块数据
    }

    /**
     * 处理图片完成
     */
    private fun handleImageComplete(senderId: String, messageId: String) {
        // 组装图片并显示
        // 简化实现
    }

    /**
     * 发送已读回执
     */
    private fun sendReadReceipt(messageId: String) {
        val builder = packetBuilder ?: return

        scope.launch(Dispatchers.IO) {
            try {
                val packet = builder.createPacket(
                    type = "READ_RECEIPT",
                    content = messageId
                )
                udpManager.broadcastPacket(packet)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 处理已读回执
     */
    private fun handleReadReceipt(messageId: String) {
        updateMessageStatus(messageId, MessageStatus.READ)
    }

    /**
     * 处理Ping
     */
    private fun handlePing(senderIp: String, timestamp: String) {
        val builder = packetBuilder ?: return

        scope.launch(Dispatchers.IO) {
            try {
                val packet = builder.createPacket(
                    type = "PONG",
                    content = timestamp
                )
                udpManager.sendPacket(packet, senderIp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 处理Pong
     */
    private fun handlePong(timestamp: String) {
        val sentTime = timestamp.toLongOrNull() ?: return
        val latency = System.currentTimeMillis() - sentTime
        networkLatency = latency
    }

    /**
     * 处理HOST验证
     */
    private fun handleHostVerification(packet: com.safeland.chat.model.Packet, senderIp: String) {
        if (!isHost && !hostVerifier.hasVerifiedHost) {
            val host = hostVerifier.verifyHost(packet, senderIp)
            host?.let {
                users.add(it)
                roomName = it.name
                performKeyExchange(it)
            }
        }
    }

    /**
     * 与HOST执行密钥交换
     */
    private fun performKeyExchange(host: User) {
        scope.launch(Dispatchers.IO) {
            try {
                val kp = keyPair ?: return@launch
                val hostPublicKey = X25519Helper.publicKeyFromBase64(host.publicKey)

                // 生成共享密钥
                sharedSecret = X25519Helper.generateSharedSecret(kp.privateKey, hostPublicKey)

                // 重新初始化协议组件
                sharedSecret?.let { secret ->
                    packetBuilder = PacketBuilder(
                        sharedSecret = secret,
                        senderId = currentUser?.id ?: "",
                        senderName = currentUser?.name ?: ""
                    )
                    packetParser = PacketParser(sharedSecret = secret)

                    // 更新队列的解密函数
                    packetQueue.decryptFunction = { encrypted ->
                        packetParser?.decryptOnly(
                            com.safeland.chat.model.Packet(
                                timestamp = 0,
                                seq = 0,
                                signature = "",
                                encryptedPayload = encrypted,
                                randomTail = ""
                            )
                        )
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 更新消息状态
     */
    private fun updateMessageStatus(messageId: String, status: MessageStatus) {
        val index = messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            messages[index] = messages[index].copy(status = status)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        if (::udpManager.isInitialized) {
            udpManager.stop()
        }
        if (::packetQueue.isInitialized) {
            packetQueue.stop()
        }
    }
}

/**
 * 屏幕枚举
 */
enum class Screen {
    ROOM_LIST,
    CHAT
}
