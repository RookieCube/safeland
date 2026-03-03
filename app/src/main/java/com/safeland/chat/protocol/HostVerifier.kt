package com.safeland.chat.protocol

import com.safeland.chat.crypto.SignatureUtil
import com.safeland.chat.crypto.X25519Helper
import com.safeland.chat.model.Packet
import com.safeland.chat.model.User
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/**
 * HOST验证器
 * 核心安全逻辑：
 * - 真HOST = WiFi热点创建者
 * - HOST证明：用X25519私钥签名 "HOST_VERIFY|热点名|timestamp"
 * - 客户端只接受第一个合法签名的HOST
 * - 后续所有自称HOST的包全部忽略
 */
class HostVerifier {
    companion object {
        private const val HOST_VERIFY_PREFIX = "HOST_VERIFY"
    }

    // 已验证的HOST（只允许一个）
    private var verifiedHost: User? = null
    private var verifiedHostPublicKey: String? = null

    // 是否已确定HOST
    val hasVerifiedHost: Boolean
        get() = verifiedHost != null

    /**
     * 验证HOST身份
     * @param packet HOST验证包
     * @param senderIp 发送者IP
     * @return 验证成功返回HOST用户对象，失败返回null
     */
    fun verifyHost(packet: Packet, senderIp: String): User? {
        // 如果已验证过HOST，拒绝所有新的HOST声明
        if (verifiedHost != null) {
            return null
        }

        return try {
            // 解析包内容（HOST验证包需要特殊处理）
            val payload = Json.parseToJsonElement(packet.encryptedPayload).jsonObject

            val type = payload["type"]?.jsonPrimitive?.content
            if (type != "HOST_VERIFY") return null

            val hotspotName = payload["hotspotName"]?.jsonPrimitive?.content ?: return null
            val timestamp = payload["timestamp"]?.jsonPrimitive?.long ?: return null
            val publicKey = payload["publicKey"]?.jsonPrimitive?.content ?: return null
            val signature = payload["signature"]?.jsonPrimitive?.content ?: return null

            // 验证时间戳
            val now = System.currentTimeMillis()
            if (kotlin.math.abs(now - timestamp) > 30000) {
                return null  // 超过30秒窗口
            }

            // 验证签名
            val verifyData = "$HOST_VERIFY_PREFIX|$hotspotName|$timestamp"
            if (!verifyHostSignature(publicKey, verifyData, signature)) {
                return null
            }

            // 创建HOST用户
            val host = User(
                name = "HOST-$hotspotName",
                publicKey = publicKey,
                role = com.safeland.chat.model.UserRole.HOST,
                ipAddress = senderIp
            )

            // 记录已验证的HOST
            verifiedHost = host
            verifiedHostPublicKey = publicKey

            host

        } catch (e: Exception) {
            null
        }
    }

    /**
     * 生成HOST验证签名
     * @param privateKey HOST私钥
     * @param hotspotName 热点名称
     * @return 签名数据
     */
    fun generateHostSignature(privateKey: ByteArray, hotspotName: String): String {
        val timestamp = System.currentTimeMillis()
        val data = "$HOST_VERIFY_PREFIX|$hotspotName|$timestamp"
        return X25519Helper.signWithPrivateKey(privateKey, data)
    }

    /**
     * 验证HOST签名
     * @param publicKeyBase64 HOST公钥(Base64)
     * @param data 原始数据
     * @param signature 签名
     * @return 验证结果
     */
    private fun verifyHostSignature(publicKeyBase64: String, data: String, signature: String): Boolean {
        return try {
            val publicKey = X25519Helper.publicKeyFromBase64(publicKeyBase64)
            X25519Helper.verifyHostSignature(publicKey, data, signature)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查给定用户是否为已验证的HOST
     */
    fun isVerifiedHost(publicKey: String): Boolean {
        return verifiedHostPublicKey == publicKey
    }

    /**
     * 获取已验证的HOST
     */
    fun getVerifiedHost(): User? = verifiedHost

    /**
     * 重置验证状态（用于测试或重新连接）
     */
    fun reset() {
        verifiedHost = null
        verifiedHostPublicKey = null
    }

    /**
     * 创建HOST验证包内容（用于发送）
     */
    fun createHostVerifyContent(
        hotspotName: String,
        publicKey: String,
        privateKey: ByteArray
    ): String {
        val timestamp = System.currentTimeMillis()
        val data = "$HOST_VERIFY_PREFIX|$hotspotName|$timestamp"
        val signature = X25519Helper.signWithPrivateKey(privateKey, data)

        return """{"type":"HOST_VERIFY","hotspotName":"$hotspotName","timestamp":$timestamp,"publicKey":"$publicKey","signature":"$signature"}"""
    }
}
