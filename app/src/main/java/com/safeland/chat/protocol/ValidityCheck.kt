package com.safeland.chat.protocol

/**
 * 有效性验证工具
 * 核心安全逻辑：sum(解密后字节) % 173 == 111
 * 禁止任何 valid=0/1 标记位
 */
object ValidityCheck {

    private const val MODULUS = 173
    private const val TARGET_REMAINDER = 111

    /**
     * 验证载荷是否有效
     * 有效条件：sum(字节) % 173 == 111
     */
    fun isValidPayload(data: ByteArray): Boolean {
        if (data.isEmpty()) return false
        val sum = data.sumOf { it.toInt() and 0xFF }
        return sum % MODULUS == TARGET_REMAINDER
    }

    /**
     * 计算字节数组的和
     */
    fun calculateSum(data: ByteArray): Int {
        return data.sumOf { it.toInt() and 0xFF }
    }

    /**
     * 计算需要的random_tail来使载荷满足有效性条件
     * 返回需要追加的字节数组
     */
    fun calculateValidatingTail(payload: ByteArray, minLength: Int = 8, maxLength: Int = 32): ByteArray {
        val currentSum = calculateSum(payload)
        val remainder = currentSum % MODULUS
        val needed = (TARGET_REMAINDER - remainder + MODULUS) % MODULUS

        // 生成满足条件的随机tail
        // 策略：生成随机字节，调整最后一个字节使总和满足条件
        val tailLength = (minLength..maxLength).random()
        val tail = ByteArray(tailLength)
        java.security.SecureRandom().nextBytes(tail)

        // 计算当前tail的和（除最后一个字节）
        val tailSumWithoutLast = if (tailLength > 1) {
            tail.copyOfRange(0, tailLength - 1).sumOf { it.toInt() and 0xFF }
        } else {
            0
        }

        // 计算最后一个字节需要的值
        // 公式: (tailSumWithoutLast + lastByte) % 173 == needed
        // 所以: lastByte = (needed - tailSumWithoutLast) mod 173
        // 注意：如果结果为0，我们需要173（即MODULUS），因为0对总和没有贡献
        var lastByteNeeded = (needed - tailSumWithoutLast) % MODULUS
        if (lastByteNeeded < 0) lastByteNeeded += MODULUS
        if (lastByteNeeded == 0) lastByteNeeded = MODULUS  // 需要173才能满足条件

        tail[tailLength - 1] = lastByteNeeded.toByte()

        return tail
    }

    /**
     * 验证完整的包数据（包含random_tail）
     */
    fun isValidPacketData(payload: ByteArray, randomTail: ByteArray): Boolean {
        val combined = payload + randomTail
        return isValidPayload(combined)
    }

    /**
     * 从字符串计算验证和
     */
    fun calculateSumFromString(data: String): Int {
        return calculateSum(data.toByteArray(Charsets.UTF_8))
    }
}
