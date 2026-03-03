package com.safeland.chat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.safeland.chat.model.Message
import com.safeland.chat.model.MessageStatus
import com.safeland.chat.model.MessageType
import com.safeland.chat.ui.theme.BlueCheckmark
import com.safeland.chat.ui.theme.GrayCheckmark
import com.safeland.chat.ui.theme.LightColors
import com.safeland.chat.ui.theme.MessageTypography
import kotlinx.coroutines.delay

/**
 * 聊天消息气泡组件
 * Telegram风格：圆角16dp，自己右对齐绿色，他人左对齐白色/深色
 */
@Composable
fun ChatBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    val isFromMe = message.isFromMe
    val backgroundColor = if (isFromMe) {
        LightColors.MyMessageBubble
    } else {
        MaterialTheme.colorScheme.surface
    }
    val textColor = if (isFromMe) {
        LightColors.MyMessageText
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    // 气泡形状：自己左下直角，他人右下直角
    val shape = if (isFromMe) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .widthIn(max = 280.dp)
        ) {
            // 发送者昵称（仅显示他人的）
            if (!isFromMe) {
                Text(
                    text = message.senderName,
                    style = MessageTypography.SenderName,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            // 消息气泡
            Box(
                modifier = Modifier
                    .background(backgroundColor, shape)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                when (message.type) {
                    MessageType.TEXT -> {
                        TextMessageContent(
                            message = message,
                            textColor = textColor
                        )
                    }
                    MessageType.IMAGE -> {
                        ImageMessageContent(
                            message = message
                        )
                    }
                }
            }
        }
    }
}

/**
 * 文本消息内容（分段淡入显示）
 */
@Composable
private fun TextMessageContent(
    message: Message,
    textColor: Color
) {
    val segments = remember(message.id) { message.getTextSegments() }
    var visibleSegments by remember { mutableStateOf(0) }

    // 分段显示动画
    LaunchedEffect(segments.size) {
        while (visibleSegments < segments.size) {
            delay((500..1000).random().toLong())  // 随机延迟
            visibleSegments++
        }
    }

    Column {
        segments.take(visibleSegments).forEachIndexed { index, segment ->
            AnimatedVisibility(
                visible = index < visibleSegments,
                enter = fadeIn(animationSpec = tween(400)) +
                        scaleIn(
                            initialScale = 0.9f,
                            animationSpec = tween(400)
                        )
            ) {
                Text(
                    text = segment,
                    style = MessageTypography.MessageText,
                    color = textColor,
                    textAlign = TextAlign.Start
                )
            }
        }

        // 时间和状态
        Row(
            modifier = Modifier
                .padding(top = 4.dp)
                .align(Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = message.getFormattedTime(),
                style = MessageTypography.Timestamp,
                color = if (message.isFromMe) Color.White.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            if (message.isFromMe) {
                MessageStatusIcon(status = message.status)
            }
        }
    }
}

/**
 * 图片消息内容（扩散式渲染）
 */
@Composable
private fun ImageMessageContent(
    message: Message
) {
    Box(
        modifier = Modifier
            .size(200.dp)
    ) {
        // 使用扩散图片组件
        DiffuseImage(
            message = message,
            modifier = Modifier.fillMaxSize()
        )

        // 时间和状态（叠加在图片上）
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = message.getFormattedTime(),
                style = MessageTypography.Timestamp,
                color = Color.White.copy(alpha = 0.9f)
            )

            if (message.isFromMe) {
                MessageStatusIcon(status = message.status, isOnImage = true)
            }
        }
    }
}

/**
 * 消息状态图标
 * 单勾（发送中）→ 双勾（已送达）→ 实心双勾（已读）
 */
@Composable
private fun MessageStatusIcon(
    status: MessageStatus,
    isOnImage: Boolean = false
) {
    val iconColor = when (status) {
        MessageStatus.SENDING -> if (isOnImage) Color.White.copy(alpha = 0.7f)
        else GrayCheckmark.copy(alpha = 0.7f)
        MessageStatus.DELIVERED -> if (isOnImage) Color.White.copy(alpha = 0.9f)
        else GrayCheckmark
        MessageStatus.READ -> BlueCheckmark
    }

    val scale by animateFloatAsState(
        targetValue = if (status == MessageStatus.READ) 1.1f else 1f,
        animationSpec = tween(200),
        label = "status_scale"
    )

    Box(
        modifier = Modifier.scale(scale)
    ) {
        when (status) {
            MessageStatus.SENDING -> {
                // 单勾
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "发送中",
                    tint = iconColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            MessageStatus.DELIVERED -> {
                // 双勾（灰色）
                Row {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已送达",
                        tint = iconColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier
                            .size(14.dp)
                            .offset(x = (-6).dp)
                    )
                }
            }
            MessageStatus.READ -> {
                // 实心双勾（蓝色）
                Row {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "已读",
                        tint = iconColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
