package com.safeland.chat.protocol

import com.safeland.chat.crypto.ChaCha20Engine
import com.safeland.chat.crypto.SignatureUtil
import com.safeland.chat.model.Packet
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/**
 * 数据包解析器
 * 负责解析和验证数据包
 * 校验规则：
 * 1. 格式：必须6段，管道符分隔
 * 2. 时间戳：±30秒有效窗口
 * 3. Seq：严格递增，不回退
 * 4. 签名：SHA256(共享密钥 + ts + seq + payload).hex().take(16)
 */
class PacketParser(
    private val sharedSecret: ByteArray
) {
    companion object {
        private const val TIMESTAMP_WINDOW_MS = 30000L  // ±30秒
        private const val MAX_SEQ_HISTORY = 1000
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 加密密钥（从共享密钥派生）
     */
    private val encryptionKey by lazy {
        ChaCha20Engine.deriveKey(sharedSecret)
    }

    // 已处理的序列号记录（防止重放攻击）
    private val processedSeqs = mutableSetOf<Long>()
    private var maxProcessedSeq = -1L

    /**
     * 验证并解密数据包
     * @param packet 数据包
     * @return 解密后的字节数组，验证失败返回null
     */
    fun parseAndVerify(packet: Packet): ByteArray? {
        // 1. 验证时间戳窗口
        if (!isTimestampValid(packet.timestamp)) {
            return null
        }

        // 2. 验证序列号（严格递增）
        if (!isSequenceValid(packet.seq)) {
            return null
        }

        // 3. 验证签名
        if (!isSignatureValid(packet)) {
            return null
        }

        // 4. 解密
        val decrypted = ChaCha20Engine.decrypt(encryptionKey, packet.encryptedPayload)
            ?: return null

        // 记录序列号
        recordSequence(packet.seq)

        return decrypted
    }

    /**
     * 仅验证包格式和签名（不解密）
     * 用于HOST验证包的特殊处理
     */
    fun verifyOnly(packet: Packet): Boolean {
        if (!isTimestampValid(packet.timestamp)) return false
        if (!isSequenceValid(packet.seq)) return false
        if (!isSignatureValid(packet)) return false
        return true
    }

    /**
     * 解密但不验证（用于需要特殊处理的场景）
     */
    fun decryptOnly(packet: Packet): ByteArray? {
        return ChaCha20Engine.decrypt(encryptionKey, packet.encryptedPayload)
    }

    /**
     * 验证时间戳是否在有效窗口内
     */
    private fun isTimestampValid(timestamp: Long): Boolean {
        val now = System.currentTimeMillis()
        val diff = kotlin.math.abs(now - timestamp)
        return diff <= TIMESTAMP_WINDOW_MS
    }

    /**
     * 验证序列号是否严格递增
     */
    private fun isSequenceValid(seq: Long): Boolean {
        // 序列号必须大于已处理的最大序列号
        if (seq <= maxProcessedSeq) {
            return false
        }
        // 检查是否已处理过（防止重放）
        if (processedSeqs.contains(seq)) {
            return false
        }
        return true
    }

    /**
     * 验证签名
     */
    private fun isSignatureValid(packet: Packet): Boolean {
        return SignatureUtil.verifyPacketSignature(
            sharedSecret,
            packet.timestamp,
            packet.seq,
            packet.encryptedPayload,
            packet.signature
        )
    }

    /**
     * 记录已处理的序列号
     */
    private fun recordSequence(seq: Long) {
        processedSeqs.add(seq)
        if (seq > maxProcessedSeq) {
            maxProcessedSeq = seq
        }

        // 限制历史记录大小
        if (processedSeqs.size > MAX_SEQ_HISTORY) {
            val toRemove = processedSeqs.minOrNull()
            toRemove?.let { processedSeqs.remove(it) }
        }
    }

    /**
     * 重置序列号记录（新会话开始时调用）
     */
    fun reset() {
        processedSeqs.clear()
        maxProcessedSeq = -1L
    }

    /**
     * 解析解密后的JSON载荷
     */
    fun parsePayload(decrypted: ByteArray): Map<String, String>? {
        return try {
            val json = Json.parseToJsonElement(String(decrypted, Charsets.UTF_8)).jsonObject
            json.mapValues { it.value.jsonPrimitive.content }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从解密数据中提取类型字段
     */
    fun extractType(decrypted: ByteArray): String? {
        return try {
            val json = Json.parseToJsonElement(String(decrypted, Charsets.UTF_8)).jsonObject
            json["type"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            null
        }
    }
}
