package com.safeland.chat.crypto

import org.bouncycastle.crypto.engines.ChaCha7539Engine
import org.bouncycastle.crypto.modes.ChaCha20Poly1305
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Base64

/**
 * ChaCha20-Poly1305 AEAD 加密引擎
 * 提供认证加密，确保数据机密性和完整性
 */
object ChaCha20Engine {

    private const val NONCE_SIZE = 12  // ChaCha20-Poly1305使用12字节nonce
    private const val KEY_SIZE = 32    // 256位密钥
    private const val TAG_SIZE = 16    // Poly1305认证标签

    private val secureRandom = SecureRandom()

    /**
     * 加密数据
     * @param key 32字节密钥
     * @param plaintext 明文数据
     * @param associatedData 附加认证数据（可选）
     * @return Base64编码的密文（nonce + ciphertext + tag）
     */
    fun encrypt(
        key: ByteArray,
        plaintext: ByteArray,
        associatedData: ByteArray? = null
    ): String {
        require(key.size == KEY_SIZE) { "Key must be 32 bytes" }

        // 生成随机nonce
        val nonce = ByteArray(NONCE_SIZE)
        secureRandom.nextBytes(nonce)

        // 使用ChaCha20-Poly1305
        val cipher = ChaCha20Poly1305()
        val params = AEADParameters(KeyParameter(key), TAG_SIZE * 8, nonce, associatedData ?: byteArrayOf())
        cipher.init(true, params)

        val output = ByteArray(cipher.getOutputSize(plaintext.size))
        val len = cipher.processBytes(plaintext, 0, plaintext.size, output, 0)
        cipher.doFinal(output, len)

        // 组合: nonce (12) + ciphertext + tag (16)
        val result = ByteArray(NONCE_SIZE + output.size)
        System.arraycopy(nonce, 0, result, 0, NONCE_SIZE)
        System.arraycopy(output, 0, result, NONCE_SIZE, output.size)

        return Base64.getEncoder().encodeToString(result)
    }

    /**
     * 解密数据
     * @param key 32字节密钥
     * @param ciphertext Base64编码的密文
     * @param associatedData 附加认证数据（可选）
     * @return 明文数据，验证失败返回null
     */
    fun decrypt(
        key: ByteArray,
        ciphertext: String,
        associatedData: ByteArray? = null
    ): ByteArray? {
        require(key.size == KEY_SIZE) { "Key must be 32 bytes" }

        return try {
            val data = Base64.getDecoder().decode(ciphertext)

            if (data.size < NONCE_SIZE + TAG_SIZE) {
                return null
            }

            // 分离nonce和密文
            val nonce = data.copyOfRange(0, NONCE_SIZE)
            val encrypted = data.copyOfRange(NONCE_SIZE, data.size)

            // 解密
            val cipher = ChaCha20Poly1305()
            val params = AEADParameters(KeyParameter(key), TAG_SIZE * 8, nonce, associatedData ?: byteArrayOf())
            cipher.init(false, params)

            val output = ByteArray(cipher.getOutputSize(encrypted.size))
            val len = cipher.processBytes(encrypted, 0, encrypted.size, output, 0)
            cipher.doFinal(output, len)

            output
        } catch (e: Exception) {
            // 验证失败或解密错误
            null
        }
    }

    /**
     * 使用XChaCha20（24字节nonce）加密
     * 用于需要更大nonce空间的场景
     */
    fun encryptXChaCha(
        key: ByteArray,
        plaintext: ByteArray
    ): String {
        require(key.size == KEY_SIZE) { "Key must be 32 bytes" }

        val nonce = ByteArray(24)
        secureRandom.nextBytes(nonce)

        // Use ChaCha7539Engine which supports 24-byte nonces (XChaCha20)
        val cipher = ChaCha7539Engine()
        val params = ParametersWithIV(KeyParameter(key), nonce)
        cipher.init(true, params)

        val output = ByteArray(plaintext.size)
        cipher.processBytes(plaintext, 0, plaintext.size, output, 0)

        // 组合: nonce (24) + ciphertext
        val result = ByteArray(24 + output.size)
        System.arraycopy(nonce, 0, result, 0, 24)
        System.arraycopy(output, 0, result, 24, output.size)

        return Base64.getEncoder().encodeToString(result)
    }

    /**
     * 派生加密密钥
     * 从共享密钥派生适合ChaCha20的32字节密钥
     */
    fun deriveKey(sharedSecret: ByteArray, salt: ByteArray? = null): ByteArray {
        return SignatureUtil.hkdfDerive(sharedSecret, salt ?: ByteArray(0), "NoiseDiffuseChat".toByteArray(), KEY_SIZE)
    }
}
