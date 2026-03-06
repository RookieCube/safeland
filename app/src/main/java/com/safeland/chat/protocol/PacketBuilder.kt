package com.safeland.chat.protocol

import com.safeland.chat.crypto.ChaCha20Engine
import com.safeland.chat.crypto.SignatureUtil
import com.safeland.chat.model.Packet
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.util.Base64

/**
 * 数据包构建器
 * 负责组装有效包和生成噪声包
 * 核心安全逻辑：10%有效包，90%噪声包，无有效/无效标记位
 */
class PacketBuilder(
    private val sharedSecret: ByteArray,
    private val senderId: String,
    private val senderName: String
) {
    companion object {
        private const val VALID_PACKET_RATIO = 0.1  // 10%有效包
        private const val PACKETS_PER_BATCH_MIN = 20
        private const val PACKETS_PER_BATCH_MAX = 30
    }

    private val secureRandom = SecureRandom()
    private var sequenceNumber = 0L
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 加密密钥（从共享密钥派生）
     */
    private val encryptionKey by lazy {
        ChaCha20Engine.deriveKey(sharedSecret)
    }

    /**
     * 创建一批数据包（含噪声扩散）
     * @param type 消息类型
     * @param content 消息内容
     * @return 包含有效包和噪声包的列表
     */
    fun createPacketBatch(
        type: String,
        content: String,
        isValid: Boolean = true
    ): List<Packet> {
        val batchSize = (PACKETS_PER_BATCH_MIN..PACKETS_PER_BATCH_MAX).random()
        val validPacketCount = if (isValid) {
            maxOf(1, (batchSize * VALID_PACKET_RATIO).toInt())
        } else {
            0
        }

        val packets = mutableListOf<Packet>()

        // 生成有效包
        repeat(validPacketCount) {
            packets.add(createValidPacket(type, content))
        }

        // 生成噪声包
        val noiseCount = batchSize - validPacketCount
        repeat(noiseCount) {
            packets.add(createNoisePacket())
        }

        // 打乱顺序
        return packets.shuffled(secureRandom)
    }

    /**
     * 创建单个有效包（简化接口）
     */
    fun createPacket(type: String, content: String): Packet {
        return createValidPacket(type, content)
    }

    /**
     * 创建单个有效包
     */
    fun createValidPacket(type: String, content: String): Packet {
        val payload = createPayload(type, content)
        return createPacketWithPayload(payload, true)
    }

    /**
     * 创建噪声包（随机内容，无法通过有效性验证）
     */
    fun createNoisePacket(): Packet {
        val fakePayload = SignatureUtil.randomBytes((32..128).random())
        // 确保噪声包不会通过有效性验证
        val invalidTail = generateInvalidTail(fakePayload)

        val timestamp = System.currentTimeMillis()
        val seq = getNextSequence()
        val encryptedPayload = ChaCha20Engine.encrypt(encryptionKey, fakePayload)
        val signature = SignatureUtil.calculatePacketSignature(sharedSecret, timestamp, seq, encryptedPayload)

        return Packet(
            timestamp = timestamp,
            seq = seq,
            signature = signature,
            encryptedPayload = encryptedPayload,
            randomTail = Base64.getEncoder().encodeToString(invalidTail)
        )
    }

    /**
     * 创建带特定载荷的包
     */
    private fun createPacketWithPayload(payload: ByteArray, shouldBeValid: Boolean): Packet {
        val timestamp = System.currentTimeMillis()
        val seq = getNextSequence()

        // 如果需要有效包，计算使总和满足条件的tail
        val randomTail = if (shouldBeValid) {
            ValidityCheck.calculateValidatingTail(payload)
        } else {
            SignatureUtil.randomBytes((8..32).random())
        }

        // 组合payload和tail
        val fullPayload = payload + randomTail

        // 加密
        val encryptedPayload = ChaCha20Engine.encrypt(encryptionKey, fullPayload)

        // 计算签名
        val signature = SignatureUtil.calculatePacketSignature(sharedSecret, timestamp, seq, encryptedPayload)

        return Packet(
            timestamp = timestamp,
            seq = seq,
            signature = signature,
            encryptedPayload = encryptedPayload,
            randomTail = Base64.getEncoder().encodeToString(randomTail)
        )
    }

    /**
     * 创建JSON载荷
     */
    private fun createPayload(type: String, content: String): ByteArray {
        val payloadMap = mapOf(
            "type" to type,
            "senderId" to senderId,
            "senderName" to senderName,
            "content" to content,
            "timestamp" to System.currentTimeMillis()
        )
        return json.encodeToString(payloadMap).toByteArray(Charsets.UTF_8)
    }

    /**
     * 获取下一个序列号
     */
    private fun getNextSequence(): Long {
        return sequenceNumber++
    }

    /**
     * 生成无效的tail（确保不会通过有效性验证）
     */
    private fun generateInvalidTail(payload: ByteArray): ByteArray {
        val tail = SignatureUtil.randomBytes((8..32).random())
        val currentSum = ValidityCheck.calculateSum(payload) + ValidityCheck.calculateSum(tail)
        val remainder = currentSum % 173

        // 调整使不满足 111 的条件
        if (remainder == 111) {
            tail[0] = ((tail[0].toInt() + 1) % 256).toByte()
        }
        return tail
    }

    /**
     * 创建HOST验证包
     */
    fun createHostVerifyPacket(hotspotName: String, hostPublicKey: String, hostSignature: String): Packet {
        val payload = mapOf(
            "type" to "HOST_VERIFY",
            "hotspotName" to hotspotName,
            "timestamp" to System.currentTimeMillis(),
            "publicKey" to hostPublicKey,
            "signature" to hostSignature
        )
        val payloadBytes = json.encodeToString(payload).toByteArray(Charsets.UTF_8)
        return createPacketWithPayload(payloadBytes, true)
    }

    /**
     * 重置序列号（新会话开始时调用）
     */
    fun resetSequence() {
        sequenceNumber = 0L
    }
}
