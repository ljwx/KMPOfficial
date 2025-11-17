package org.example.project.statusbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * 全局状态栏样式状态
 */
private var globalStatusBarStyle: StatusBarStyle = StatusBarStyle.DARK_CONTENT

/**
 * iOS 平台的状态栏配置实现
 * 
 * 注意：iOS 的状态栏样式配置相对复杂，需要在 UIViewController 中实现 preferredStatusBarStyle
 * 当前实现通过全局状态保存样式配置，实际的状态栏样式需要通过其他方式设置
 * 
 * 建议方案：
 * 1. 在 Info.plist 中设置 UIViewControllerBasedStatusBarAppearance = false，然后使用全局状态栏样式
 * 2. 或者在 Swift 包装代码中根据全局状态动态设置状态栏样式
 */
@Composable
actual fun StatusBarConfig(style: StatusBarStyle) {
    DisposableEffect(style) {
        globalStatusBarStyle = style
        // iOS 状态栏样式更新需要通过 UIViewController 实现
        // 这里只保存状态，实际更新需要在视图控制器层面处理
        onDispose {
            // 清理时可以恢复默认样式
        }
    }
}

/**
 * 获取当前的状态栏样式（供其他模块使用）
 */
internal fun getStatusBarStyle(): StatusBarStyle {
    return globalStatusBarStyle
}

