package com.safeland.chat.model

import androidx.compose.runtime.Immutable
import java.util.UUID

/**
 * 用户角色
 */
enum class UserRole {
    HOST,     // WiFi热点创建者
    CLIENT    // 普通客户端
}

/**
 * 用户状态
 */
enum class UserStatus {
    ONLINE,
    OFFLINE,
    AWAY
}

/**
 * 用户数据类
 */
@Immutable
data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val publicKey: String = "",
    val role: UserRole = UserRole.CLIENT,
    val status: UserStatus = UserStatus.ONLINE,
    val lastSeen: Long = System.currentTimeMillis(),
    val ipAddress: String = "",
    val isBlocked: Boolean = false
) {
    /**
     * 是否为HOST
     */
    fun isHost(): Boolean = role == UserRole.HOST

    /**
     * 获取显示名称（带角色标识）
     */
    fun getDisplayName(): String {
        return if (role == UserRole.HOST) {
            "$name 👑"
        } else {
            name
        }
    }

    companion object {
        /**
         * 创建本地用户
         */
        fun createLocal(name: String, publicKey: String, isHost: Boolean = false): User {
            return User(
                name = name,
                publicKey = publicKey,
                role = if (isHost) UserRole.HOST else UserRole.CLIENT
            )
        }
    }
}

/**
 * 本地用户会话信息
 */
data class LocalUserSession(
    val user: User,
    val privateKey: ByteArray,
    val sharedSecret: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalUserSession

        if (user != other.user) return false
        if (!privateKey.contentEquals(other.privateKey)) return false
        if (sharedSecret != null) {
            if (other.sharedSecret == null) return false
            if (!sharedSecret.contentEquals(other.sharedSecret)) return false
        } else if (other.sharedSecret != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = user.hashCode()
        result = 31 * result + privateKey.contentHashCode()
        result = 31 * result + (sharedSecret?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * 聊天室数据类
 */
@Immutable
data class ChatRoom(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val hostName: String,
    val hostIp: String,
    val userCount: Int = 0,
    val hasPassword: Boolean = false,
    val signalStrength: Int = 100,  // 信号强度 0-100
    val createdAt: Long = System.currentTimeMillis()
)
