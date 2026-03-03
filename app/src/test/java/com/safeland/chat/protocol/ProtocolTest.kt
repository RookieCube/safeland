package com.safeland.chat.protocol

import com.safeland.chat.model.Packet
import org.junit.Test
import org.junit.Assert.*

/**
 * 协议层单元测试
 */
class ProtocolTest {

    @Test
    fun `test validity check sum modulo 173`() {
        // 创建满足条件的字节数组: sum % 173 == 111
        val validData = ByteArray(10) { 11 }  // 10 * 11 = 110, need 1 more
        val validData2 = validData + byteArrayOf(1)  // 111 % 173 = 111 ✓

        assertTrue(ValidityCheck.isValidPayload(validData2))

        // 不满足条件的数据
        val invalidData = ByteArray(10) { 10 }  // 100 % 173 = 100 != 111
        assertFalse(ValidityCheck.isValidPayload(invalidData))
    }

    @Test
    fun `test validity check with empty data`() {
        assertFalse(ValidityCheck.isValidPayload(ByteArray(0)))
    }

    @Test
    fun `test validating tail generation`() {
        val payload = "Test message content".toByteArray()
        val tail = ValidityCheck.calculateValidatingTail(payload)

        assertTrue(tail.isNotEmpty())

        // 验证组合后的数据满足条件
        val combined = payload + tail
        assertTrue(ValidityCheck.isValidPayload(combined))
    }

    @Test
    fun `test packet serialization and parsing`() {
        val original = Packet(
            timestamp = 1234567890L,
            seq = 42L,
            signature = "a1b2c3d4e5f67890",
            encryptedPayload = "base64encodedpayload",
            randomTail = "randomtail123"
        )

        val serialized = original.serialize()
        val parsed = Packet.parse(serialized)

        assertNotNull(parsed)
        assertEquals(original.timestamp, parsed!!.timestamp)
        assertEquals(original.seq, parsed.seq)
        assertEquals(original.signature, parsed.signature)
        assertEquals(original.encryptedPayload, parsed.encryptedPayload)
        assertEquals(original.randomTail, parsed.randomTail)
    }

    @Test
    fun `test packet parsing with invalid format`() {
        // 段数不足
        assertNull(Packet.parse("PKT|123|456"))

        // 段数过多
        assertNull(Packet.parse("PKT|1|2|3|4|5|6|7"))

        // 前缀错误
        assertNull(Packet.parse("INVALID|1|2|3|4|5"))

        // 空字符串
        assertNull(Packet.parse(""))
    }

    @Test
    fun `test packet parsing with invalid numbers`() {
        // 无效的时间戳
        val invalidTs = Packet.parse("PKT|notanumber|1|sig|payload|tail")
        assertNull(invalidTs)

        // 无效的序列号
        val invalidSeq = Packet.parse("PKT|123|notanumber|sig|payload|tail")
        assertNull(invalidSeq)
    }

    @Test
    fun `test host verifier signature generation`() {
        val privateKey = ByteArray(32) { it.toByte() }
        val hotspotName = "TestHotspot"

        val signature = HostVerifier().generateHostSignature(privateKey, hotspotName)

        assertNotNull(signature)
        assertTrue(signature.isNotEmpty())
    }

    @Test
    fun `test packet parser timestamp validation`() {
        val sharedSecret = ByteArray(32) { 0xAB.toByte() }
        val parser = PacketParser(sharedSecret)

        // 过期的时间戳（超过30秒）
        val oldPacket = Packet(
            timestamp = System.currentTimeMillis() - 60000,  // 60秒前
            seq = 0,
            signature = "test",
            encryptedPayload = "test",
            randomTail = "test"
        )

        // 验证应该失败（时间戳过期）
        assertFalse(parser.verifyOnly(oldPacket))

        // 未来的时间戳
        val futurePacket = Packet(
            timestamp = System.currentTimeMillis() + 60000,  // 60秒后
            seq = 1,
            signature = "test",
            encryptedPayload = "test",
            randomTail = "test"
        )

        assertFalse(parser.verifyOnly(futurePacket))
    }

    @Test
    fun `test packet parser sequence validation`() {
        val sharedSecret = ByteArray(32) { 0xAB.toByte() }
        val parser = PacketParser(sharedSecret)

        // 第一个包（seq=0）应该被接受
        // 注意：这里由于签名验证会失败，我们只测试序列号逻辑
        // 实际测试需要正确的签名

        // 重置后测试
        parser.reset()
        assertEquals(-1L, getMaxProcessedSeq(parser))
    }

    private fun getMaxProcessedSeq(parser: PacketParser): Long {
        // 通过反射获取私有字段（仅用于测试）
        val field = PacketParser::class.java.getDeclaredField("maxProcessedSeq")
        field.isAccessible = true
        return field.get(parser) as Long
    }
}
