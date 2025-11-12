package org.example.project.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Desktop (JVM) 平台的返回键处理实现
 * 
 * 注意：
 * - Desktop 平台使用 Escape 键作为返回键
 * - 使用 Compose Desktop 的键盘事件处理机制
 * - 通过 Modifier.onKeyEvent 监听 Escape 键
 * - 注意：此实现需要在 Window 级别才能正常工作，或者需要元素获取焦点
 * 
 * 限制：
 * - Compose Desktop 的键盘事件需要元素获取焦点才能接收
 * - 如果页面中有其他可聚焦元素（如按钮、输入框），焦点会被它们获取
 * - 建议在 Window 级别处理键盘事件，或者确保此 Box 能够获取焦点
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun BackHandler(
    enabled: Boolean,
    onBack: () -> Boolean
) {
    if (enabled) {
        // 使用一个不可见的 Box 来捕获键盘事件
        // 注意：在 Compose Desktop 中，键盘事件需要元素获取焦点才能接收
        // 如果页面中有其他可聚焦元素，可能需要使用 Window 级别的键盘事件处理
        Box(
            modifier = Modifier
                .onKeyEvent { event ->
                    // 监听 Escape 键按下事件
                    if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                        val handled = onBack()
                        handled // 返回 true 表示已处理，阻止事件继续传播
                    } else {
                        false // 其他键不处理，让事件继续传播
                    }
                }
        ) {
            // 空的 Box，只用于捕获键盘事件
        }
    }
}

