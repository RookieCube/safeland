package com.safeland.chat.ui.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

/**
 * 扩散模型风格的加载动画
 * 模拟噪声逐步去噪生成图像的过程
 */
@Composable
fun DiffusionLoadingAnimation(
    modifier: Modifier = Modifier,
    isLoading: Boolean = true
) {
    if (!isLoading) return

    val infiniteTransition = rememberInfiniteTransition(label = "diffusion")

    // 多个噪声波的动画
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )

    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )

    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave3"
    )

    // 扩散进度
    val diffusionProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "diffusion"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = minOf(size.width, size.height) / 2

        // 绘制多层扩散圆环
        drawDiffusionRings(
            centerX = centerX,
            centerY = centerY,
            maxRadius = maxRadius,
            wave1 = wave1,
            wave2 = wave2,
            wave3 = wave3,
            progress = diffusionProgress
        )

        // 绘制噪声粒子
        drawNoiseParticles(
            centerX = centerX,
            centerY = centerY,
            radius = maxRadius * 0.6f,
            time = wave1,
            progress = diffusionProgress
        )

        // 绘制中心核心
        drawCenterCore(
            centerX = centerX,
            centerY = centerY,
            radius = maxRadius * 0.2f,
            pulse = diffusionProgress
        )
    }
}

/**
 * 绘制扩散圆环
 */
private fun DrawScope.drawDiffusionRings(
    centerX: Float,
    centerY: Float,
    maxRadius: Float,
    wave1: Float,
    wave2: Float,
    wave3: Float,
    progress: Float
) {
    val ringCount = 5

    for (i in 0 until ringCount) {
        val ringProgress = (i / ringCount.toFloat() + progress) % 1f
        val radius = maxRadius * ringProgress
        val alpha = 1f - ringProgress
        val strokeWidth = 4f * (1f - ringProgress) + 1f

        // 使用正弦波调制颜色
        val hue = (wave1 * 10 + i * 60) % 360
        val color = Color.hsl(hue, 0.7f, 0.5f, alpha)

        drawCircle(
            color = color,
            radius = radius,
            center = Offset(centerX, centerY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
    }
}

/**
 * 绘制噪声粒子
 */
private fun DrawScope.drawNoiseParticles(
    centerX: Float,
    centerY: Float,
    radius: Float,
    time: Float,
    progress: Float
) {
    val particleCount = 20

    for (i in 0 until particleCount) {
        val angle = (i / particleCount.toFloat()) * 2f * PI.toFloat() + time * 0.5f
        val distance = radius * (0.5f + 0.5f * sin(time + i))
        val x = centerX + cos(angle) * distance
        val y = centerY + sin(angle) * distance

        val particleSize = 3f + 5f * (1f - progress)
        val alpha = 0.3f + 0.7f * progress

        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = particleSize,
            center = Offset(x, y)
        )
    }
}

/**
 * 绘制中心核心
 */
private fun DrawScope.drawCenterCore(
    centerX: Float,
    centerY: Float,
    radius: Float,
    pulse: Float
) {
    val pulsedRadius = radius * (0.8f + 0.2f * pulse)

    // 外发光
    drawCircle(
        color = Color.Cyan.copy(alpha = 0.3f),
        radius = pulsedRadius * 1.5f,
        center = Offset(centerX, centerY)
    )

    // 核心
    drawCircle(
        color = Color.Cyan.copy(alpha = 0.8f),
        radius = pulsedRadius,
        center = Offset(centerX, centerY)
    )

    // 内部高光
    drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = pulsedRadius * 0.4f,
        center = Offset(centerX - pulsedRadius * 0.2f, centerY - pulsedRadius * 0.2f)
    )
}

/**
 * 加密/解密动画效果
 * 模拟数据被加密/解密的过程
 */
@Composable
fun EncryptionAnimation(
    isEncrypting: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "encryption")

    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "encryption_progress"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val size = minOf(size.width, size.height) * 0.6f

        // 绘制加密矩阵
        drawEncryptionMatrix(
            centerX = centerX,
            centerY = centerY,
            size = size,
            progress = progress,
            rotation = rotation,
            isEncrypting = isEncrypting
        )
    }
}

/**
 * 绘制加密矩阵
 */
private fun DrawScope.drawEncryptionMatrix(
    centerX: Float,
    centerY: Float,
    size: Float,
    progress: Float,
    rotation: Float,
    isEncrypting: Boolean
) {
    val gridSize = 8
    val cellSize = size / gridSize

    for (i in 0 until gridSize) {
        for (j in 0 until gridSize) {
            val x = centerX - size / 2 + i * cellSize + cellSize / 2
            val y = centerY - size / 2 + j * cellSize + cellSize / 2

            // 根据进度和位置计算透明度
            val cellProgress = (i * gridSize + j) / (gridSize * gridSize).toFloat()
            val adjustedProgress = (progress + cellProgress) % 1f
            val alpha = if (isEncrypting) {
                if (adjustedProgress < 0.5f) 1f else 0.3f
            } else {
                if (adjustedProgress < 0.5f) 0.3f else 1f
            }

            // 旋转效果
            val rotatedX = centerX + (x - centerX) * cos(rotation * PI / 180) -
                    (y - centerY) * sin(rotation * PI / 180)
            val rotatedY = centerY + (x - centerX) * sin(rotation * PI / 180) +
                    (y - centerY) * cos(rotation * PI / 180)

            val color = if (isEncrypting) {
                Color.Green.copy(alpha = alpha)
            } else {
                Color.Cyan.copy(alpha = alpha)
            }

            drawRect(
                color = color,
                topLeft = Offset(rotatedX - cellSize / 3, rotatedY - cellSize / 3),
                size = androidx.compose.ui.geometry.Size(cellSize / 1.5f, cellSize / 1.5f)
            )
        }
    }
}

/**
 * 消息发送扩散效果
 * 消息发送时的波纹扩散动画
 */
@Composable
fun MessageSentRipple(
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit = {}
) {
    var currentRipple by remember { mutableStateOf(0) }
    val maxRipples = 3

    LaunchedEffect(Unit) {
        repeat(maxRipples) {
            currentRipple = it
            delay(200)
        }
        delay(500)
        onAnimationComplete()
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        for (i in 0 until maxRipples) {
            if (i <= currentRipple) {
                RippleCircle(delay = i * 200)
            }
        }
    }
}

/**
 * 单个波纹圆
 */
@Composable
private fun RippleCircle(delay: Int) {
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delay.toLong())
        startAnimation = true
    }

    if (!startAnimation) return

    val infiniteTransition = rememberInfiniteTransition(label = "ripple")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(100.dp)
            .scale(scale)
            .alpha(alpha)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                shape = androidx.compose.foundation.shape.CircleShape
            )
    )
}

/**
 * 噪声扩散背景动画
 * 用于聊天界面的动态背景
 */
@Composable
fun NoiseDiffuseBackground(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "background")

    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX"
    )

    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 绘制渐变噪声背景
        for (i in 0..10) {
            for (j in 0..10) {
                val x = (i / 10f) * width + offsetX % 50
                val y = (j / 10f) * height + offsetY % 50

                val noise = Random(i * 11 + j).nextFloat()
                val alpha = 0.02f + noise * 0.03f

                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = 20f + noise * 30f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

/**
 * 打字指示器动画
 * 显示对方正在输入的动画效果
 */
@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    val dots = List(3) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = index * 150),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot$index"
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dots.forEachIndexed { index, anim ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(0.5f + anim.value * 0.5f)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.5f + anim.value * 0.5f
                        ),
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * 脉冲发光效果
 */
@Composable
fun PulsingGlow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(modifier = modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }) {
        // 发光层
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    shape = MaterialTheme.shapes.medium
                )
        )

        // 内容
        content()
    }
}
