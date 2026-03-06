package com.safeland.chat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.safeland.chat.model.ImageBlock
import com.safeland.chat.model.Message
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

/**
 * 扩散式图片渲染组件
 * 16x16 dp网格，随机位置逐步亮起，300ms淡入缩放动画
 * 添加扩散模型风格的炫酷效果
 */
@Composable
fun DiffuseImage(
    message: Message,
    modifier: Modifier = Modifier,
    isLoaded: Boolean = false
) {
    // 模拟图片块数据（实际应从message.imageBlocks获取）
    val blocks = remember(message.id) {
        generateMockBlocks()
    }

    // 已接收的块索引
    var receivedBlocks by remember { mutableStateOf(setOf<Int>()) }

    // 扩散波纹中心点
    var diffusionCenters by remember { mutableStateOf(listOf<Offset>()) }

    // 动画状态
    val infiniteTransition = rememberInfiniteTransition(label = "diffusion")

    val diffusionWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "diffusion_wave"
    )

    // 扩散动画：从多个中心点向外扩散
    LaunchedEffect(blocks.size) {
        // 生成几个扩散中心
        val centers = List(3) {
            Offset(
                x = Random.nextFloat() * 16f,
                y = Random.nextFloat() * 16f
            )
        }
        diffusionCenters = centers

        // 按距离中心的远近排序，模拟扩散效果
        val sortedIndices = blocks.indices.sortedBy { index ->
            val block = blocks[index]
            val blockPos = Offset(block.x.toFloat(), block.y.toFloat())
            centers.minOf { center ->
                val dx = blockPos.x - center.x
                val dy = blockPos.y - center.y
                sqrt(dx * dx + dy * dy)
            }
        }

        // 分批显示块
        for (batch in sortedIndices.chunked(16)) {
            delay(100)
            receivedBlocks = receivedBlocks + batch
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // 计算块大小（16x16网格）
        val blockWidth = canvasWidth / 16f
        val blockHeight = canvasHeight / 16f

        // 绘制背景扩散波纹
        diffusionCenters.forEach { center ->
            drawDiffusionWave(
                centerX = center.x * blockWidth,
                centerY = center.y * blockHeight,
                wave = diffusionWave,
                maxRadius = minOf(canvasWidth, canvasHeight) / 2
            )
        }

        // 绘制每个已接收的块
        blocks.forEachIndexed { index, block ->
            if (index in receivedBlocks) {
                val x = block.x * blockWidth
                val y = block.y * blockHeight

                // 计算到最近中心的距离
                val blockPos = Offset(block.x.toFloat(), block.y.toFloat())
                val minDistance = diffusionCenters.minOf { center ->
                    val dx = blockPos.x - center.x
                    val dy = blockPos.y - center.y
                    sqrt(dx * dx + dy * dy)
                }

                // 基于距离的动画进度
                val animationProgress = (1f - minDistance / 22f).coerceIn(0f, 1f)

                drawEnhancedBlock(
                    x = x,
                    y = y,
                    width = blockWidth,
                    height = blockHeight,
                    progress = animationProgress,
                    block = block,
                    wave = diffusionWave
                )
            }
        }

        // 如果所有块都收到了，绘制完整图片覆盖
        if (receivedBlocks.size == blocks.size && isLoaded) {
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
 * 绘制扩散波纹
 */
private fun DrawScope.drawDiffusionWave(
    centerX: Float,
    centerY: Float,
    wave: Float,
    maxRadius: Float
) {
    val ringCount = 3
    for (i in 0 until ringCount) {
        val offset = i * (2f * PI.toFloat() / ringCount)
        val radius = maxRadius * (0.3f + 0.7f * ((wave + offset) % (2f * PI.toFloat())) / (2f * PI.toFloat()))
        val alpha = 0.1f * (1f - radius / maxRadius)

        drawCircle(
            color = Color.Cyan.copy(alpha = alpha),
            radius = radius,
            center = Offset(centerX, centerY)
        )
    }
}

/**
 * 绘制增强块（带发光效果）
 */
private fun DrawScope.drawEnhancedBlock(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    progress: Float,
    block: ImageBlock,
    wave: Float
) {
    // 淡入 + 缩放动画
    val alpha = progress
    val scale = 0.5f + (0.5f * progress)

    val scaledWidth = width * scale
    val scaledHeight = height * scale
    val offsetX = (width - scaledWidth) / 2
    val offsetY = (height - scaledHeight) / 2

    // 计算发光强度
    val glowIntensity = (0.5f + 0.5f * sin(wave + block.index * 0.1f)) * progress

    // 绘制发光效果
    if (glowIntensity > 0.3f) {
        drawRect(
            color = Color.Cyan.copy(alpha = glowIntensity * 0.3f),
            topLeft = Offset(x + offsetX - 2f, y + offsetY - 2f),
            size = Size(scaledWidth + 4f, scaledHeight + 4f)
        )
    }

    // 绘制块（使用随机颜色模拟图片内容）
    val color = generateEnhancedColorFromData(block.data, alpha, wave, block.index)

    drawRect(
        color = color,
        topLeft = Offset(x + offsetX, y + offsetY),
        size = Size(scaledWidth, scaledHeight)
    )
}

/**
 * 从数据生成增强颜色（带动态效果）
 */
private fun generateEnhancedColorFromData(
    data: ByteArray,
    alpha: Float,
    wave: Float,
    index: Int
): Color {
    if (data.isEmpty()) return Color.Gray.copy(alpha = alpha)

    val r = (data[0].toInt() and 0xFF) / 255f
    val g = if (data.size > 1) (data[1].toInt() and 0xFF) / 255f else r
    val b = if (data.size > 2) (data[2].toInt() and 0xFF) / 255f else r

    // 添加基于时间和索引的颜色变化
    val hueShift = sin(wave + index * 0.05f) * 0.1f

    return Color(
        red = (r + hueShift).coerceIn(0f, 1f),
        green = (g + hueShift).coerceIn(0f, 1f),
        blue = (b + hueShift).coerceIn(0f, 1f),
        alpha = alpha
    )
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
