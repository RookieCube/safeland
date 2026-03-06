package com.safeland.chat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 设置对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onClearData: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭"
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 加密设置
                SettingsSection(title = "加密设置") {
                    SettingsItem(
                        title = "协议版本",
                        value = "X25519 + ChaCha20-Poly1305"
                    )
                    SettingsItem(
                        title = "密钥交换",
                        value = "已启用"
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // 网络设置
                SettingsSection(title = "网络设置") {
                    SettingsItem(
                        title = "端口",
                        value = "1338"
                    )
                    SettingsItem(
                        title = "广播地址",
                        value = "255.255.255.255"
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // 关于
                SettingsSection(title = "关于") {
                    Text(
                        text = "SafeLand v1.0",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "端到端加密 P2P 局域网聊天",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✓ X25519 密钥交换\n✓ ChaCha20-Poly1305 加密\n✓ 噪声扩散保护",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 清除数据按钮
                OutlinedButton(
                    onClick = onClearData,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("清除所有数据")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        },
        dismissButton = null
    )
}

/**
 * 设置分组
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

/**
 * 设置项
 */
@Composable
private fun SettingsItem(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
