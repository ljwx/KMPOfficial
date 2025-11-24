package org.example.project.statusbar

import androidx.compose.runtime.Composable

/**
 * 状态栏样式枚举
 */
enum class StatusBarStyle {
    /** 深色图标和文字 - 适用于浅色背景 */
    DARK_CONTENT,
    
    /** 浅色图标和文字 - 适用于深色背景 */
    LIGHT_CONTENT
}

@Composable
expect fun StatusBarConfig(style: StatusBarStyle = StatusBarStyle.DARK_CONTENT)

