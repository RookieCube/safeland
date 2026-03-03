package com.safeland.chat.crypto

import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.SecureRandom
import java.security.Security
import java.util.Base64

/**
 * X25519 密钥交换帮助类
 * 负责生成密钥对、执行ECDH密钥协商
 */
object X25519Helper {

    init {
        // 确保BouncyCastle提供者已注册
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    private val secureRandom = SecureRandom()

    /**
     * X25519 密钥对
     */
    data class KeyPair(
        val privateKey: ByteArray,
        val publicKey: ByteArray
    ) {
        fun privateKeyBase64(): String = Base64.getEncoder().encodeToString(privateKey)
        fun publicKeyBase64(): String = Base64.getEncoder().encodeToString(publicKey)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as KeyPair
            return privateKey.contentEquals(other.privateKey) &&
                    publicKey.contentEquals(other.publicKey)
        }

        override fun hashCode(): Int {
            var result = privateKey.contentHashCode()
            result = 31 * result + publicKey.contentHashCode()
            return result
        }
    }

    /**
     * 生成新的X25519密钥对
     * 每个会话必须生成新的密钥对，禁止使用固定密钥
     */
    fun generateKeyPair(): KeyPair {
        val generator = X25519KeyPairGenerator()
        generator.init(X25519KeyGenerationParameters(secureRandom))

        val keyPair = generator.generateKeyPair()
        val privateKey = (keyPair.private as X25519PrivateKeyParameters).encoded
        val publicKey = (keyPair.public as X25519PublicKeyParameters).encoded

        return KeyPair(privateKey, publicKey)
    }

    /**
     * 从Base64编码的字符串恢复公钥
     */
    fun publicKeyFromBase64(base64: String): ByteArray {
        return Base64.getDecoder().decode(base64)
    }

    /**
     * 从Base64编码的字符串恢复私钥
     */
    fun privateKeyFromBase64(base64: String): ByteArray {
        return Base64.getDecoder().decode(base64)
    }

    /**
     * 执行X25519密钥协商，生成共享密钥
     * @param privateKey 己方私钥
     * @param peerPublicKey 对方公钥
     * @return 32字节共享密钥
     */
    fun generateSharedSecret(privateKey: ByteArray, peerPublicKey: ByteArray): ByteArray {
        val agreement = X25519Agreement()
        val privParams = X25519PrivateKeyParameters(privateKey, 0)
        agreement.init(privParams)

        val pubParams = X25519PublicKeyParameters(peerPublicKey, 0)
        val sharedSecret = ByteArray(agreement.agreementSize)
        agreement.calculateAgreement(pubParams, sharedSecret, 0)

        return sharedSecret
    }

    /**
     * 使用私钥对数据进行签名（用于HOST验证）
     * 注意：这不是真正的数字签名，而是使用私钥派生的密钥进行HMAC-SHA256
     */
    fun signWithPrivateKey(privateKey: ByteArray, data: String): String {
        val sharedSecret = generateSharedSecret(privateKey, privateKey)
        return SignatureUtil.hmacSha256(sharedSecret, data.toByteArray())
    }

    /**
     * 验证HOST签名
     * @param publicKey HOST公钥
     * @param data 原始数据
     * @param signature 待验证的签名
     * @return 验证结果
     */
    fun verifyHostSignature(publicKey: ByteArray, data: String, signature: String): Boolean {
        // 使用公钥派生验证密钥
        val verifyKey = generateSharedSecret(ByteArray(32) { 1 }, publicKey)
        val expectedSignature = SignatureUtil.hmacSha256(verifyKey, data.toByteArray())
        return SignatureUtil.constantTimeEquals(signature, expectedSignature)
    }
}
