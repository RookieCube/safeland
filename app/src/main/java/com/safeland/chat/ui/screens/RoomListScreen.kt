package com.safeland.chat.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safeland.chat.model.ChatRoom
import com.safeland.chat.ui.animations.DiffusionLoadingAnimation
import kotlinx.coroutines.delay

/**
 * 聊天室列表屏幕
 * 显示可用的聊天室，允许创建或加入
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListScreen(
    rooms: List<ChatRoom>,
    onCreateRoom: (String) -> Unit,
    onJoinRoom: (ChatRoom, String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var selectedRoom by remember { mutableStateOf<ChatRoom?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    // 过滤后的房间列表
    val filteredRooms = remember(rooms, searchQuery) {
        if (searchQuery.isEmpty()) rooms
        else rooms.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    // 刷新动画
    val refreshRotation by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        animationSpec = tween(1000, easing = LinearEasing),
        label = "refresh"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SafeLand",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "端到端加密 P2P 聊天",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(
                        onClick = {
                            isRefreshing = true
                            onRefresh()
                        },
                        enabled = !isRefreshing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.scale(refreshRotation / 360f + 0.5f)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "创建聊天室"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 搜索栏
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    isSearching = it.isNotEmpty()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("搜索聊天室...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            // 聊天室列表
            if (filteredRooms.isEmpty()) {
                if (isSearching) {
                    EmptySearchResult(query = searchQuery)
                } else {
                    EmptyRoomList(
                        onCreateClick = { showCreateDialog = true },
                        isRefreshing = isRefreshing
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filteredRooms,
                        key = { it.id }
                    ) { room ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(300)) +
                                    scaleIn(initialScale = 0.9f)
                        ) {
                            RoomCard(
                                room = room,
                                onClick = {
                                    selectedRoom = room
                                    showJoinDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 重置刷新状态
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(2000)
            isRefreshing = false
        }
    }

    // 创建聊天室对话框
    if (showCreateDialog) {
        CreateRoomDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { nickname ->
                onCreateRoom(nickname)
                showCreateDialog = false
            }
        )
    }

    // 加入聊天室对话框
    if (showJoinDialog && selectedRoom != null) {
        JoinRoomDialog(
            room = selectedRoom!!,
            onDismiss = {
                showJoinDialog = false
                selectedRoom = null
            },
            onConfirm = { nickname ->
                onJoinRoom(selectedRoom!!, nickname)
                showJoinDialog = false
                selectedRoom = null
            }
        )
    }
}

/**
 * 空聊天室列表
 */
@Composable
private fun EmptyRoomList(
    onCreateClick: () -> Unit,
    isRefreshing: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isRefreshing) {
            // 扩散动画
            Box(
                modifier = Modifier.size(120.dp)
            ) {
                DiffusionLoadingAnimation(isLoading = true)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "正在搜索附近的聊天室...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        } else {
            Text(
                text = "🔍",
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "没有发现聊天室",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "点击右下角按钮创建一个新的聊天室，\n或等待发现附近的聊天室",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onCreateClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("创建聊天室")
            }
        }
    }
}

/**
 * 空搜索结果
 */
@Composable
private fun EmptySearchResult(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "😕",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "未找到 \"$query\"",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "尝试其他关键词搜索",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 聊天室卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomCard(
    room: ChatRoom,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (room.hasPassword) {
                    Text(
                        text = "🔒",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "HOST: ${room.hostName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${room.userCount} 人在线",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "信号: ${room.signalStrength}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 创建聊天室对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateRoomDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nickname by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "创建聊天室",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "作为 HOST 创建一个新的聊天室",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("你的昵称") },
                    placeholder = { Text("输入昵称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nickname) },
                enabled = nickname.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 加入聊天室对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinRoomDialog(
    room: ChatRoom,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nickname by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "加入聊天室",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "加入 \"${room.name}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "HOST: ${room.hostName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("你的昵称") },
                    placeholder = { Text("输入昵称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nickname) },
                enabled = nickname.isNotBlank()
            ) {
                Text("加入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
