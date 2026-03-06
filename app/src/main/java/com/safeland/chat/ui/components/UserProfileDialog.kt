package com.safeland.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safeland.chat.model.User
import com.safeland.chat.model.UserRole

/**
 * 用户资料对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onSendPrivateMessage: () -> Unit,
    onAddToBlacklist: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 头像
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = user.name.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // 在线状态指示器
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp),
                        shape = CircleShape,
                        color = when (user.status) {
                            com.safeland.chat.model.UserStatus.ONLINE ->
                                MaterialTheme.colorScheme.primary
                            com.safeland.chat.model.UserStatus.AWAY ->
                                MaterialTheme.colorScheme.tertiary
                            com.safeland.chat.model.UserStatus.OFFLINE ->
                                MaterialTheme.colorScheme.outline
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.surface
                        )
                    ) {}
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 用户名
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // 角色标识
                if (user.role == UserRole.HOST) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "👑 HOST",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 状态
                Text(
                    text = when (user.status) {
                        com.safeland.chat.model.UserStatus.ONLINE -> "在线"
                        com.safeland.chat.model.UserStatus.AWAY -> "离开"
                        com.safeland.chat.model.UserStatus.OFFLINE -> "离线"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 私聊按钮
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FilledIconButton(
                            onClick = onSendPrivateMessage,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "私聊",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "私聊",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // 黑名单按钮
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FilledIconButton(
                            onClick = onAddToBlacklist,
                            modifier = Modifier.size(56.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "屏蔽",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "屏蔽",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 信息列表
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoItem(
                        icon = Icons.Default.Person,
                        label = "ID",
                        value = user.id.take(8) + "..."
                    )

                    InfoItem(
                        icon = Icons.Default.Schedule,
                        label = "最后在线",
                        value = formatTimestamp(user.lastSeen)
                    )

                    if (user.ipAddress.isNotEmpty()) {
                        InfoItem(
                            icon = Icons.Default.Computer,
                            label = "IP 地址",
                            value = user.ipAddress
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        dismissButton = null
    )
}

/**
 * 信息项
 */
@Composable
private fun InfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 格式化时间戳
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "刚刚"
        diff < 3600000 -> "${diff / 60000} 分钟前"
        diff < 86400000 -> "${diff / 3600000} 小时前"
        else -> {
            val date = java.util.Date(timestamp)
            val format = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
            format.format(date)
        }
    }
}
