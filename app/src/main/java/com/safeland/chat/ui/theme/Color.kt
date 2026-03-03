package com.safeland.chat.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Telegram风格配色方案
 * 主色：#29A58A（Telegram绿）
 */

// 主色调
val TelegramGreen = Color(0xFF29A58A)
val TelegramGreenDark = Color(0xFF1E8B73)
val TelegramGreenLight = Color(0xFF4DBFA3)

// 浅色主题
object LightColors {
    val Primary = TelegramGreen
    val OnPrimary = Color.White
    val PrimaryContainer = TelegramGreenLight
    val OnPrimaryContainer = Color(0xFF00382C)

    val Secondary = Color(0xFF4A635C)
    val OnSecondary = Color.White
    val SecondaryContainer = Color(0xFFCDE8DF)
    val OnSecondaryContainer = Color(0xFF06201A)

    val Tertiary = Color(0xFF426277)
    val OnTertiary = Color.White
    val TertiaryContainer = Color(0xFFC6E7FF)
    val OnTertiaryContainer = Color(0xFF001E2D)

    val Background = Color(0xFFFFFFFF)
    val OnBackground = Color(0xFF191C1B)

    val Surface = Color(0xFFF5F5F5)
    val OnSurface = Color(0xFF191C1B)
    val SurfaceVariant = Color(0xFFDBE5E0)
    val OnSurfaceVariant = Color(0xFF3F4945)

    val Outline = Color(0xFF6F7975)
    val OutlineVariant = Color(0xFFBFC9C4)

    // 消息气泡颜色
    val MyMessageBubble = TelegramGreen
    val MyMessageText = Color.White
    val OtherMessageBubble = Color.White
    val OtherMessageText = Color(0xFF191C1B)

    // 状态颜色
    val Online = Color(0xFF4CAF50)
    val Offline = Color(0xFF9E9E9E)
    val Error = Color(0xFFB3261E)
}

// 深色主题
object DarkColors {
    val Primary = TelegramGreenLight
    val OnPrimary = Color(0xFF00382C)
    val PrimaryContainer = Color(0xFF005140)
    val OnPrimaryContainer = Color(0xFF6FFBC9)

    val Secondary = Color(0xFFB1CCC3)
    val OnSecondary = Color(0xFF1C352F)
    val SecondaryContainer = Color(0xFF334B45)
    val OnSecondaryContainer = Color(0xFFCDE8DF)

    val Tertiary = Color(0xFFA9CBE3)
    val OnTertiary = Color(0xFF0F3447)
    val TertiaryContainer = Color(0xFF2A4A5E)
    val OnTertiaryContainer = Color(0xFFC6E7FF)

    val Background = Color(0xFF0F1416)
    val OnBackground = Color(0xFFE1E3E1)

    val Surface = Color(0xFF1F2C34)  // Telegram深色背景
    val OnSurface = Color(0xFFE1E3E1)
    val SurfaceVariant = Color(0xFF3F4945)
    val OnSurfaceVariant = Color(0xFFBFC9C4)

    val Outline = Color(0xFF89938E)
    val OutlineVariant = Color(0xFF3F4945)

    // 消息气泡颜色
    val MyMessageBubble = Color(0xFF005C4B)  // Telegram深色模式绿色
    val MyMessageText = Color(0xFFE1E3E1)
    val OtherMessageBubble = Color(0xFF1F2C34)
    val OtherMessageText = Color(0xFFE1E3E1)

    // 状态颜色
    val Online = Color(0xFF81C784)
    val Offline = Color(0xFF757575)
    val Error = Color(0xFFF2B8B5)
}

// 通用颜色
val BlueCheckmark = Color(0xFF4FC3F7)  // 已读蓝色双勾
val GrayCheckmark = Color(0xFF9E9E9E)  // 未读灰色双勾
