package org.example.project.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import kotlinx.browser.window

/**
 * Web 平台的返回键处理实现
 * 处理 Escape 键和浏览器的后退按钮
 * 
 * 注意：
 * - Web 平台会监听 Escape 键和浏览器后退按钮
 * - 如果 onBack 返回 true，会阻止默认行为
 * - Decompose 已经通过 handleBackButton = true 处理了返回逻辑
 * - 此实现同时支持 JS 和 WASM 目标
 */
@Composable
actual fun BackHandler(
    enabled: Boolean,
    onBack: () -> Boolean
) {
    DisposableEffect(enabled) {
        if (!enabled) {
            return@DisposableEffect onDispose { }
        }

        // 处理 Escape 键
        val escapeKeyHandler: (Event) -> Unit = { event ->
            val keyboardEvent = event as? KeyboardEvent
            if (keyboardEvent?.key == "Escape") {
                val handled = onBack()
                if (handled) {
                    keyboardEvent.preventDefault()
                    keyboardEvent.stopPropagation()
                }
            }
        }

        // 处理浏览器后退按钮
        val popStateHandler: (Event) -> Unit = { _ ->
            val handled = onBack()
            if (handled) {
                // 如果已处理，阻止浏览器默认后退行为
                window.history.pushState(null, "", window.location.href)
            }
        }

        window.addEventListener("keydown", escapeKeyHandler)
        window.addEventListener("popstate", popStateHandler)

        onDispose {
            window.removeEventListener("keydown", escapeKeyHandler)
            window.removeEventListener("popstate", popStateHandler)
        }
    }
}

