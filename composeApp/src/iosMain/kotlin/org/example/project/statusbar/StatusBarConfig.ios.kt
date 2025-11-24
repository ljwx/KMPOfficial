package org.example.project.statusbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNotification
import platform.darwin.NSObject

/**
 * 全局状态栏样式状态
 */
private var globalStatusBarStyle: StatusBarStyle = StatusBarStyle.DARK_CONTENT

/**
 * 状态栏样式变化通知名称
 */
const val STATUS_BAR_STYLE_CHANGED_NOTIFICATION = "StatusBarStyleChangedNotification"

/**
 * iOS 平台的状态栏配置实现
 * 
 * 通过 NotificationCenter 发送通知，触发 UIViewController 更新状态栏样式
 */
@Composable
actual fun StatusBarConfig(style: StatusBarStyle) {
    DisposableEffect(style) {
        val previousStyle = globalStatusBarStyle
        globalStatusBarStyle = style
        
        // 发送通知，告知 UIViewController 状态栏样式已改变
        NSNotificationCenter.defaultCenter.postNotificationName(
            STATUS_BAR_STYLE_CHANGED_NOTIFICATION,
            `object` = null
        )
        
        onDispose {
            // 可选：恢复之前的样式
        }
    }
}

/**
 * 获取当前的状态栏样式（供 Swift 层使用）
 */
fun getStatusBarStyle(): StatusBarStyle {
    return globalStatusBarStyle
}
