package com.safeland.chat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.safeland.chat.ui.screens.LoginScreen
import com.safeland.chat.ui.screens.MainChatScreen
import com.safeland.chat.ui.theme.NoiseDiffuseChatTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

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
    private var isConnected by mutableStateOf(false)
    private var isHost by mutableStateOf(false)
    private var roomName by mutableStateOf("局域网聊天室")

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
            NoiseDiffuseChatTheme {
                var isLoggedIn by remember { mutableStateOf(false) }

                if (isLoggedIn && currentUser != null) {
                    MainChatScreen(
                        currentUser = currentUser,
                        messages = messages,
                        users = users,
                        isConnected = isConnected,
                        isHost = isHost,
                        roomName = roomName,
                        localIp = udpManager.getLocalIpAddress(),
                        onSendMessage = { content ->
                            sendTextMessage(content)
                        },
                        onSendImage = {
                            // TODO: 实现图片发送
                        }
                    )
                } else {
                    LoginScreen(onLogin = { nickname: String, asHost: Boolean ->
                        login(nickname, asHost)
                        isLoggedIn = true
                    })
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
            Manifest.permission.ACCESS_NETWORK_STATE
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
            ipAddress = udpManager.getLocalIpAddress()
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

                when (type) {
                    "MESSAGE" -> {
                        // 添加消息
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
                        // TODO: 实现已读回执
                    }
                    "HOST_VERIFY" -> {
                        // 验证HOST
                        if (!isHost && !hostVerifier.hasVerifiedHost) {
                            val host = hostVerifier.verifyHost(packet, senderIp)
                            host?.let {
                                users.add(it)
                                roomName = it.name

                                // 与HOST交换密钥
                                performKeyExchange(it)
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
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
        udpManager.stop()
        packetQueue.stop()
    }
}
