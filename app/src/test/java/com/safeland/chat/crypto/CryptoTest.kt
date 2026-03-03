package com.safeland.chat.crypto

import org.junit.Test
import org.junit.Assert.*

/**
 * 加密模块单元测试
 */
class CryptoTest {

    @Test
    fun `test X25519 key pair generation`() {
        val keyPair1 = X25519Helper.generateKeyPair()
        val keyPair2 = X25519Helper.generateKeyPair()

        // 验证密钥对不为空
        assertNotNull(keyPair1)
        assertNotNull(keyPair2)

        // 验证密钥长度
        assertEquals(32, keyPair1.privateKey.size)
        assertEquals(32, keyPair1.publicKey.size)

        // 验证不同密钥对不相同
        assertFalse(keyPair1.publicKey.contentEquals(keyPair2.publicKey))
    }

    @Test
    fun `test X25519 shared secret generation`() {
        val alice = X25519Helper.generateKeyPair()
        val bob = X25519Helper.generateKeyPair()

        // Alice 和 Bob 交换公钥，生成共享密钥
        val aliceShared = X25519Helper.generateSharedSecret(alice.privateKey, bob.publicKey)
        val bobShared = X25519Helper.generateSharedSecret(bob.privateKey, alice.publicKey)

        // 验证共享密钥相同
        assertNotNull(aliceShared)
        assertNotNull(bobShared)
        assertEquals(32, aliceShared.size)
        assertTrue(aliceShared.contentEquals(bobShared))
    }

    @Test
    fun `test ChaCha20 encryption and decryption`() {
        val key = ByteArray(32) { it.toByte() }
        val plaintext = "Hello, Noise-Diffuse Chat!"

        // 加密
        val encrypted = ChaCha20Engine.encrypt(key, plaintext.toByteArray())
        assertNotNull(encrypted)
        assertTrue(encrypted.isNotEmpty())

        // 解密
        val decrypted = ChaCha20Engine.decrypt(key, encrypted)
        assertNotNull(decrypted)
        assertEquals(plaintext, String(decrypted!!))
    }

    @Test
    fun `test ChaCha20 decryption with wrong key fails`() {
        val key1 = ByteArray(32) { 1 }
        val key2 = ByteArray(32) { 2 }
        val plaintext = "Secret message"

        val encrypted = ChaCha20Engine.encrypt(key1, plaintext.toByteArray())
        val decrypted = ChaCha20Engine.decrypt(key2, encrypted)

        // 使用错误密钥解密应该失败
        assertNull(decrypted)
    }

    @Test
    fun `test SHA256 signature`() {
        val data1 = "test data 1"
        val data2 = "test data 2"

        val sig1 = SignatureUtil.sha256Hex(data1.toByteArray())
        val sig2 = SignatureUtil.sha256Hex(data2.toByteArray())
        val sig1Again = SignatureUtil.sha256Hex(data1.toByteArray())

        // 相同数据产生相同签名
        assertEquals(sig1, sig1Again)

        // 不同数据产生不同签名
        assertNotEquals(sig1, sig2)

        // 签名长度为16位hex
        assertEquals(16, sig1.length)
    }

    @Test
    fun `test HMAC SHA256`() {
        val key = ByteArray(32) { 0xAB.toByte() }
        val data = "message to sign"

        val hmac1 = SignatureUtil.hmacSha256(key, data.toByteArray())
        val hmac2 = SignatureUtil.hmacSha256(key, data.toByteArray())
        val hmac3 = SignatureUtil.hmacSha256(key, "different".toByteArray())

        // 相同输入产生相同HMAC
        assertEquals(hmac1, hmac2)

        // 不同输入产生不同HMAC
        assertNotEquals(hmac1, hmac3)
    }

    @Test
    fun `test constant time equals`() {
        val a = "test_string_1234"
        val b = "test_string_1234"
        val c = "test_string_5678"

        assertTrue(SignatureUtil.constantTimeEquals(a, b))
        assertFalse(SignatureUtil.constantTimeEquals(a, c))
        assertFalse(SignatureUtil.constantTimeEquals(a, a.substring(0, 8)))
    }

    @Test
    fun `test HKDF key derivation`() {
        val ikm = ByteArray(32) { 0x0A }
        val salt = ByteArray(16) { 0x0B }
        val info = "NoiseDiffuseChat".toByteArray()

        val key1 = SignatureUtil.hkdfDerive(ikm, salt, info, 32)
        val key2 = SignatureUtil.hkdfDerive(ikm, salt, info, 32)
        val key3 = SignatureUtil.hkdfDerive(ikm, salt, "Different".toByteArray(), 32)

        // 相同输入产生相同密钥
        assertTrue(key1.contentEquals(key2))

        // 不同info产生不同密钥
        assertFalse(key1.contentEquals(key3))

        // 验证密钥长度
        assertEquals(32, key1.size)
    }
}
