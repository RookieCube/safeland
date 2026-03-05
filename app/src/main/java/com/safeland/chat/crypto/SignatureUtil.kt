package com.safeland.chat.crypto

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.params.KeyParameter
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.experimental.xor

/**
 * 签名和哈希工具类
 * 提供SHA256、HMAC-SHA256、HKDF等功能
 */
object SignatureUtil {

    private val secureRandom = SecureRandom()

    /**
     * 计算SHA256哈希，返回hex字符串（前16位）
     * 用于包签名
     */
    fun sha256Hex(data: ByteArray): String {
        val digest = SHA256Digest()
        digest.update(data, 0, data.size)
        val hash = ByteArray(digest.digestSize)
        digest.doFinal(hash, 0)
        return hash.toHex().take(16)
    }

    /**
     * 计算SHA256哈希（完整）
     */
    fun sha256(data: ByteArray): ByteArray {
        val digest = SHA256Digest()
        digest.update(data, 0, data.size)
        val hash = ByteArray(digest.digestSize)
        digest.doFinal(hash, 0)
        return hash
    }

    /**
     * 计算HMAC-SHA256
     */
    fun hmacSha256(key: ByteArray, data: ByteArray): String {
        val hmac = HMac(SHA256Digest())
        hmac.init(KeyParameter(key))
        hmac.update(data, 0, data.size)
        val result = ByteArray(hmac.macSize)
        hmac.doFinal(result, 0)
        return result.toHex()
    }

    /**
     * 计算包签名
     * 格式: SHA256(共享密钥 + ts + seq + payload).hex().take(16)
     */
    fun calculatePacketSignature(
        sharedSecret: ByteArray,
        timestamp: Long,
        seq: Long,
        payload: String
    ): String {
        val data = buildString {
            append(sharedSecret.toHex())
            append(timestamp)
            append(seq)
            append(payload)
        }.toByteArray()
        return sha256Hex(data)
    }

    /**
     * 验证包签名
     */
    fun verifyPacketSignature(
        sharedSecret: ByteArray,
        timestamp: Long,
        seq: Long,
        payload: String,
        signature: String
    ): Boolean {
        val expected = calculatePacketSignature(sharedSecret, timestamp, seq, payload)
        return constantTimeEquals(expected, signature)
    }

    /**
     * HKDF密钥派生
     * RFC 5869实现
     */
    fun hkdfDerive(
        ikm: ByteArray,
        salt: ByteArray,
        info: ByteArray,
        length: Int
    ): ByteArray {
        // Extract
        val saltToUse = if (salt.isEmpty()) ByteArray(32) { 0 } else salt
        val prk = hmacSha256Raw(saltToUse, ikm)

        // Expand
        val result = ByteArray(length)
        var previousBlock = byteArrayOf()
        var counter: Byte = 1
        var position = 0

        while (position < length) {
            val data = previousBlock + info + byteArrayOf(counter)
            previousBlock = hmacSha256Raw(prk, data)

            val toCopy = minOf(previousBlock.size, length - position)
            System.arraycopy(previousBlock, 0, result, position, toCopy)
            position += toCopy
            counter++
        }

        return result
    }

    /**
     * 原始HMAC-SHA256（返回字节数组）
     */
    private fun hmacSha256Raw(key: ByteArray, data: ByteArray): ByteArray {
        val hmac = HMac(SHA256Digest())
        hmac.init(KeyParameter(key))
        hmac.update(data, 0, data.size)
        val result = ByteArray(hmac.macSize)
        hmac.doFinal(result, 0)
        return result
    }

    /**
     * 常量时间比较，防止时序攻击
     */
    fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    /**
     * 字节数组转Hex字符串
     */
    fun ByteArray.toHex(): String {
        return joinToString("") { "%02x".format(it) }
    }

    /**
     * Hex字符串转字节数组
     */
    fun String.hexToBytes(): ByteArray {
        require(length % 2 == 0) { "Hex string must have even length" }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    /**
     * 生成随机字节
     */
    fun randomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    /**
     * 生成随机字符串
     */
    fun randomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars[secureRandom.nextInt(chars.length)] }
            .joinToString("")
    }
}
