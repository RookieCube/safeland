package com.safeland.chat.model

import kotlinx.serialization.Serializable

/**
 * 网络数据包模型
 * 格式: PKT|<timestamp>|<seq>|<signature>|<encrypted_payload>|<random_tail>
 */
@Serializable
data class Packet(
    val timestamp: Long,
    val seq: Long,
    val signature: String,
    val encryptedPayload: String,
    val randomTail: String
) {
    /**
     * 将包序列化为传输格式
     */
    fun serialize(): String {
        return "PKT|$timestamp|$seq|$signature|$encryptedPayload|$randomTail"
    }

    companion object {
        /**
         * 从字符串解析数据包
         * @return 解析成功返回Packet，失败返回null
         */
        fun parse(data: String): Packet? {
            val parts = data.split("|")
            if (parts.size != 6) return null
            if (parts[0] != "PKT") return null

            return try {
                Packet(
                    timestamp = parts[1].toLong(),
                    seq = parts[2].toLong(),
                    signature = parts[3],
                    encryptedPayload = parts[4],
                    randomTail = parts[5]
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * 包类型枚举（用于内部处理，不传输）
 */
enum class PacketType {
    MESSAGE,      // 普通文本消息
    IMAGE,        // 图片消息
    HOST_VERIFY,  // HOST身份验证
    USER_LIST,    // 用户列表同步
    HEARTBEAT     // 心跳包
}

/**
 * 解密后的有效载荷
 */
@Serializable
data class DecryptedPayload(
    val type: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * HOST验证专用载荷
 */
@Serializable
data class HostVerifyPayload(
    val hotspotName: String,
    val timestamp: Long,
    val publicKey: String,
    val signature: String
)
