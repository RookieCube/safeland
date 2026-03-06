package com.safeland.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safeland.chat.model.User
import com.safeland.chat.model.UserRole
import com.safeland.chat.model.UserStatus

/**
 * 侧拉抽屉组件
 * 包含用户列表、黑名单、设置、网络状态
 */
@Composable
fun UserDrawer(
    currentUser: User?,
    users: List<User>,
    onlineCount: Int,
    isHost: Boolean,
    onUserClick: (User) -> Unit,
    onSettingsClick: () -> Unit,
    onBlacklistClick: () -> Unit,
    onNetworkStatusClick: () -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))

        // 当前用户信息
        CurrentUserHeader(
            user = currentUser,
            isHost = isHost
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // 在线用户列表
        Text(
            text = "在线用户 ($onlineCount)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn {
            items(
                items = users,
                key = { it.id }
            ) { user ->
                androidx.compose.animation.AnimatedVisibility(
                    visible = true,
                    enter = androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(300)
                    ) + androidx.compose.animation.scaleIn(
                        initialScale = 0.9f,
                        animationSpec = androidx.compose.animation.core.tween(300)
                    )
                ) {
                    UserListItem(
                        user = user,
                        onClick = { onUserClick(user) }
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // 功能菜单
        DrawerMenuItem(
            icon = Icons.Default.Block,
            label = "黑名单",
            onClick = onBlacklistClick
        )

        DrawerMenuItem(
            icon = Icons.Default.Settings,
            label = "设置",
            onClick = onSettingsClick
        )

        DrawerMenuItem(
            icon = Icons.Default.Wifi,
            label = "网络状态",
            onClick = onNetworkStatusClick
        )

        Spacer(modifier = Modifier.weight(1f))

        // 退出登录
        DrawerMenuItem(
            icon = Icons.Default.ExitToApp,
            label = "退出登录",
            onClick = onLogout
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // 版本信息
        Text(
            text = "SafeLand v1.0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * 当前用户头部信息
 */
@Composable
private fun CurrentUserHeader(
    user: User?,
    isHost: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 头像和HOST徽章
        Box {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = user?.name?.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // HOST徽章
            if (isHost) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "HOST",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 用户名
        Text(
            text = user?.name ?: "未登录",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // 角色标识
        Text(
            text = if (isHost) "👑 房间 HOST" else "普通用户",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 用户列表项
 */
@Composable
private fun UserListItem(
    user: User,
    onClick: () -> Unit
) {
    val statusColor = when (user.status) {
        UserStatus.ONLINE -> MaterialTheme.colorScheme.primary
        UserStatus.AWAY -> MaterialTheme.colorScheme.tertiary
        UserStatus.OFFLINE -> MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        Box {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = user.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 在线状态指示器
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(12.dp),
                shape = CircleShape,
                color = statusColor,
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.surface
                )
            ) {}
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 用户信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.getDisplayName(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = when (user.status) {
                    UserStatus.ONLINE -> "在线"
                    UserStatus.AWAY -> "离开"
                    UserStatus.OFFLINE -> "离线"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // HOST标识
        if (user.isHost()) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "HOST",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 抽屉菜单项
 */
@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 网络状态卡片
 */
@Composable
fun NetworkStatusCard(
    isConnected: Boolean,
    localIp: String,
    port: Int,
    latency: Long?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = if (isConnected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (isConnected) "网络已连接" else "网络未连接",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isConnected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "本地IP: $localIp:$port",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            latency?.let {
                Text(
                    text = "延迟: ${it}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}
