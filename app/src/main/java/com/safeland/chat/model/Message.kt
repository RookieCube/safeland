package com.safeland.chat.model

import androidx.compose.runtime.Immutable
import java.util.UUID

/**
 * 消息状态
 */
enum class MessageStatus {
    SENDING,      // 单勾 - 发送中
    DELIVERED,    // 双勾 - 已送达
    READ          // 实心双勾 - 已读
}

/**
 * 消息类型
 */
enum class MessageType {
    TEXT,
    IMAGE
}

/**
 * 消息数据类 - 不可变，确保线程安全
 */
@Immutable
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderName: String,
    val content: String,
    val type: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENDING,
    val isFromMe: Boolean = false,
    // 图片专用字段
    val imageBlocks: List<ImageBlock> = emptyList(),
    val totalBlocks: Int = 0
) {
    /**
     * 获取格式化的时间字符串
     */
    fun getFormattedTime(): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return format.format(date)
    }

    /**
     * 文本分段显示用
     */
    fun getTextSegments(): List<String> {
        if (type != MessageType.TEXT) return emptyList()

        val segments = mutableListOf<String>()
        val text = content
        var currentPos = 0

        while (currentPos < text.length) {
            // 尝试按标点分段，最大20字
            val endPos = minOf(currentPos + 20, text.length)
            val segment = text.substring(currentPos, endPos)
            segments.add(segment)
            currentPos = endPos
        }

        return segments
    }
}

/**
 * 图片分块数据
 */
@Immutable
data class ImageBlock(
    val index: Int,
    val x: Int,           // 16x16网格中的x坐标
    val y: Int,           // 16x16网格中的y坐标
    val data: ByteArray,  // 块数据
    val isReceived: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageBlock

        if (index != other.index) return false
        if (x != other.x) return false
        if (y != other.y) return false
        if (!data.contentEquals(other.data)) return false
        if (isReceived != other.isReceived) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + x
        result = 31 * result + y
        result = 31 * result + data.contentHashCode()
        result = 31 * result + isReceived.hashCode()
        return result
    }
}

/**
 * 图片消息元数据
 */
@Immutable
data class ImageMetadata(
    val messageId: String,
    val width: Int,
    val height: Int,
    val totalBlocks: Int,
    val mimeType: String
)
