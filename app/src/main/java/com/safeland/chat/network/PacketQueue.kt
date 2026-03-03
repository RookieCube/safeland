package com.safeland.chat.network

import com.safeland.chat.model.Packet
import com.safeland.chat.protocol.ValidityCheck
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 数据包队列处理器
 * 实现FIFO队列和每秒最多处理12个包的限流机制
 * 核心安全逻辑：队列永不主动清空，确保低算力设备不丢内容
 */
class PacketQueue(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val onValidPacket: suspend (Packet, String, ByteArray) -> Unit
) {
    companion object {
        private const val MAX_PROCESSING_RATE = 12  // 每秒最多处理12个包
        private const val QUEUE_CAPACITY = 1000     // 队列上限，防止OOM
    }

    // FIFO队列，存储待处理的数据包
    private val packetQueue = ConcurrentLinkedQueue<QueueEntry>()

    // 处理后的有效载荷流
    private val _validPayloads = MutableSharedFlow<Triple<Packet, String, ByteArray>>(extraBufferCapacity = 100)
    val validPayloads: SharedFlow<Triple<Packet, String, ByteArray>> = _validPayloads.asSharedFlow()

    // 控制协程
    private var processingJob: Job? = null
    private var isRunning = false

    // 解密函数引用（由外部注入）
    var decryptFunction: ((String) -> ByteArray?)? = null

    /**
     * 队列条目
     */
    private data class QueueEntry(
        val packet: Packet,
        val senderIp: String,
        val enqueueTime: Long = System.currentTimeMillis()
    )

    /**
     * 启动队列处理器
     */
    fun start() {
        if (isRunning) return
        isRunning = true

        processingJob = scope.launch(Dispatchers.IO) {
            processQueue()
        }
    }

    /**
     * 停止队列处理器
     */
    fun stop() {
        isRunning = false
        processingJob?.cancel()
        processingJob = null
    }

    /**
     * 将数据包加入队列
     * 如果队列已满，丢弃最早的包（不是最新的）
     */
    fun enqueue(packet: Packet, senderIp: String): Boolean {
        // 检查队列容量
        if (packetQueue.size >= QUEUE_CAPACITY) {
            // 队列已满，移除最早的条目
            packetQueue.poll()
        }

        return packetQueue.offer(QueueEntry(packet, senderIp))
    }

    /**
     * 获取队列大小
     */
    fun getQueueSize(): Int = packetQueue.size

    /**
     * 队列处理循环
     * 实现每秒最多处理12个包的限流
     */
    private suspend fun processQueue() {
        var packetsProcessedInSecond = 0
        var lastResetTime = System.currentTimeMillis()

        while (isRunning) {
            try {
                val now = System.currentTimeMillis()

                // 每秒重置计数器
                if (now - lastResetTime >= 1000) {
                    packetsProcessedInSecond = 0
                    lastResetTime = now
                }

                // 限流检查
                if (packetsProcessedInSecond >= MAX_PROCESSING_RATE) {
                    delay(50)  // 等待50ms再检查
                    continue
                }

                // 从队列取出包
                val entry = packetQueue.poll()
                if (entry == null) {
                    delay(10)  // 队列为空，短暂等待
                    continue
                }

                // 处理包
                processPacket(entry)
                packetsProcessedInSecond++

            } catch (e: Exception) {
                if (isRunning) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 处理单个数据包
     */
    private suspend fun processPacket(entry: QueueEntry) {
        val decrypt = decryptFunction ?: return

        try {
            // 尝试解密
            val decrypted = decrypt(entry.packet.encryptedPayload)

            if (decrypted != null) {
                // 验证有效性: sum(字节) % 173 == 111
                if (ValidityCheck.isValidPayload(decrypted)) {
                    // 有效包，通知回调
                    onValidPacket(entry.packet, entry.senderIp, decrypted)
                    _validPayloads.emit(Triple(entry.packet, entry.senderIp, decrypted))
                }
                // 无效包直接丢弃，不做任何处理
            }
            // 解密失败直接丢弃

        } catch (e: Exception) {
            // 处理异常，不崩溃
            e.printStackTrace()
        }
    }

    /**
     * 清空队列（仅用于测试或紧急恢复）
     * 注意：正常情况下不应调用此方法
     */
    fun clearQueue() {
        packetQueue.clear()
    }
}
