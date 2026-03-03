package com.safeland.chat.model

import org.junit.Test
import org.junit.Assert.*

/**
 * 数据模型单元测试
 */
class ModelTest {

    @Test
    fun `test message text segmentation`() {
        // 短消息
        val shortMsg = Message(
            senderId = "1",
            senderName = "Test",
            content = "Hello"
        )
        val shortSegments = shortMsg.getTextSegments()
        assertEquals(1, shortSegments.size)
        assertEquals("Hello", shortSegments[0])

        // 长消息（超过20字）
        val longContent = "这是一段很长的消息内容，需要被分段显示测试分段功能是否正常工作"
        val longMsg = Message(
            senderId = "1",
            senderName = "Test",
            content = longContent
        )
        val longSegments = longMsg.getTextSegments()
        assertTrue(longSegments.size > 1)
    }

    @Test
    fun `test message formatted time`() {
        val msg = Message(
            senderId = "1",
            senderName = "Test",
            content = "Test",
            timestamp = 0L  // 1970-01-01 00:00:00
        )

        val timeStr = msg.getFormattedTime()
        assertNotNull(timeStr)
        assertTrue(timeStr.contains(":"))  // 应该包含时间分隔符
    }

    @Test
    fun `test user host detection`() {
        val host = User(
            name = "Host",
            role = UserRole.HOST
        )
        val client = User(
            name = "Client",
            role = UserRole.CLIENT
        )

        assertTrue(host.isHost())
        assertFalse(client.isHost())
    }

    @Test
    fun `test user display name`() {
        val host = User(
            name = "TestHost",
            role = UserRole.HOST
        )
        val client = User(
            name = "TestClient",
            role = UserRole.CLIENT
        )

        assertTrue(host.getDisplayName().contains("👑"))
        assertEquals("TestClient", client.getDisplayName())
    }

    @Test
    fun `test user status enum`() {
        assertEquals(3, UserStatus.values().size)
        assertNotNull(UserStatus.ONLINE)
        assertNotNull(UserStatus.OFFLINE)
        assertNotNull(UserStatus.AWAY)
    }

    @Test
    fun `test message status enum`() {
        assertEquals(3, MessageStatus.values().size)
        assertNotNull(MessageStatus.SENDING)
        assertNotNull(MessageStatus.DELIVERED)
        assertNotNull(MessageStatus.READ)
    }

    @Test
    fun `test message type enum`() {
        assertEquals(2, MessageType.values().size)
        assertNotNull(MessageType.TEXT)
        assertNotNull(MessageType.IMAGE)
    }

    @Test
    fun `test image block equality`() {
        val block1 = ImageBlock(
            index = 0,
            x = 1,
            y = 2,
            data = byteArrayOf(1, 2, 3)
        )
        val block2 = ImageBlock(
            index = 0,
            x = 1,
            y = 2,
            data = byteArrayOf(1, 2, 3)
        )
        val block3 = ImageBlock(
            index = 1,
            x = 1,
            y = 2,
            data = byteArrayOf(1, 2, 3)
        )

        assertEquals(block1, block2)
        assertNotEquals(block1, block3)
        assertEquals(block1.hashCode(), block2.hashCode())
    }

    @Test
    fun `test packet equality`() {
        val packet1 = Packet(
            timestamp = 123L,
            seq = 1L,
            signature = "sig",
            encryptedPayload = "payload",
            randomTail = "tail"
        )
        val packet2 = Packet(
            timestamp = 123L,
            seq = 1L,
            signature = "sig",
            encryptedPayload = "payload",
            randomTail = "tail"
        )
        val packet3 = Packet(
            timestamp = 456L,
            seq = 2L,
            signature = "sig2",
            encryptedPayload = "payload2",
            randomTail = "tail2"
        )

        assertEquals(packet1, packet2)
        assertNotEquals(packet1, packet3)
    }

    @Test
    fun `test user creation`() {
        val user = User.createLocal(
            name = "TestUser",
            publicKey = "pubkey123",
            isHost = true
        )

        assertEquals("TestUser", user.name)
        assertEquals("pubkey123", user.publicKey)
        assertEquals(UserRole.HOST, user.role)
        assertNotNull(user.id)
    }
}
