package com.safeland.chat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.safeland.chat.model.ImageBlock
import com.safeland.chat.model.Message
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * 扩散式图片渲染组件
 * 16x16 dp网格，随机位置逐步亮起，300ms淡入缩放动画
 */
@Composable
fun DiffuseImage(
    message: Message,
    modifier: Modifier = Modifier
) {
    // 模拟图片块数据（实际应从message.imageBlocks获取）
    val blocks = remember(message.id) {
        generateMockBlocks()
    }

    // 已接收的块索引
    var receivedBlocks by remember { mutableStateOf(setOf<Int>()) }

    // 扩散动画：随机顺序显示块
    LaunchedEffect(blocks.size) {
        val indices = blocks.indices.shuffled()
        for (index in indices) {
            delay((50..200).random().toLong())  // 随机延迟
            receivedBlocks = receivedBlocks + index
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // 计算块大小（16x16网格）
        val blockWidth = canvasWidth / 16f
        val blockHeight = canvasHeight / 16f

        // 绘制每个已接收的块
        blocks.forEachIndexed { index, block ->
            if (index in receivedBlocks) {
                val x = block.x * blockWidth
                val y = block.y * blockHeight

                // 计算动画进度
                val animationProgress = calculateAnimationProgress(
                    receivedBlocks = receivedBlocks.size,
                    totalBlocks = blocks.size,
                    blockIndex = index
                )

                drawBlock(
                    x = x,
                    y = y,
                    width = blockWidth,
                    height = blockHeight,
                    progress = animationProgress,
                    block = block
                )
            }
        }

        // 如果所有块都收到了，绘制完整图片覆盖
        if (receivedBlocks.size == blocks.size) {
            drawFullImage(canvasWidth, canvasHeight)
        }
    }
}

/**
 * 绘制单个块
 */
private fun DrawScope.drawBlock(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    progress: Float,
    block: ImageBlock
) {
    // 淡入 + 缩放动画
    val alpha = progress
    val scale = 0.5f + (0.5f * progress)

    val scaledWidth = width * scale
    val scaledHeight = height * scale
    val offsetX = (width - scaledWidth) / 2
    val offsetY = (height - scaledHeight) / 2

    // 绘制块（使用随机颜色模拟图片内容）
    val color = generateColorFromData(block.data, alpha)

    drawRect(
        color = color,
        topLeft = Offset(x + offsetX, y + offsetY),
        size = Size(scaledWidth, scaledHeight)
    )
}

/**
 * 绘制完整图片
 */
private fun DrawScope.drawFullImage(width: Float, height: Float) {
    // 实际应用中这里应该绘制真实图片
    // 这里用渐变色模拟
    drawRect(
        color = Color(0xFF29A58A).copy(alpha = 0.3f),
        topLeft = Offset(0f, 0f),
        size = Size(width, height)
    )
}

/**
 * 计算动画进度
 */
private fun calculateAnimationProgress(
    receivedBlocks: Int,
    totalBlocks: Int,
    blockIndex: Int
): Float {
    // 简单的线性进度，实际应基于时间
    return min(1f, receivedBlocks.toFloat() / totalBlocks.toFloat())
}

/**
 * 从数据生成颜色（模拟）
 */
private fun generateColorFromData(data: ByteArray, alpha: Float): Color {
    if (data.isEmpty()) return Color.Gray.copy(alpha = alpha)

    val r = (data[0].toInt() and 0xFF) / 255f
    val g = if (data.size > 1) (data[1].toInt() and 0xFF) / 255f else r
    val b = if (data.size > 2) (data[2].toInt() and 0xFF) / 255f else r

    return Color(r, g, b, alpha)
}

/**
 * 生成模拟块数据
 */
private fun generateMockBlocks(): List<ImageBlock> {
    return List(256) { index ->
        val x = index % 16
        val y = index / 16
        ImageBlock(
            index = index,
            x = x,
            y = y,
            data = byteArrayOf(
                (x * 16).toByte(),
                (y * 16).toByte(),
                ((x + y) * 8).toByte()
            ),
            isReceived = false
        )
    }
}
