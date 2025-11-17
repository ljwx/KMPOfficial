package org.example.project.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import kotlinx.browser.window
import org.w3c.dom.events.Event

/**
 * Web 平台特定的导航历史记录同步
 * 
 * 当导航堆栈发生变化时，更新浏览器历史记录，使浏览器返回按钮能够正常工作
 */
@Composable
actual fun syncPlatformNavigation(navigation: IAppNavigation) {
    val childStack = navigation.getChildStack()
    
    DisposableEffect(Unit) {
        var previousStackSize = childStack.value.items.size
        
        val cancellation = childStack.subscribe { stack ->
            val currentStackSize = stack.items.size
            
            // 当堆栈大小变化时，更新浏览器历史记录
            if (currentStackSize != previousStackSize) {
                if (currentStackSize > previousStackSize) {
                    // 页面入栈（前进），添加历史记录
                    val currentRoute = stack.active.instance.config.router
                    val url = window.location.pathname + "#$currentRoute"
                    window.history.pushState(null, "", url)
                } else {
                    // 页面出栈（后退），更新 URL 但不添加历史记录
                    val currentRoute = stack.active.instance.config.router
                    val url = window.location.pathname + "#$currentRoute"
                    window.history.replaceState(null, "", url)
                }
                previousStackSize = currentStackSize
            }
        }
        
        // 初始化：设置当前 URL
        val initialRoute = childStack.value.active.instance.config.router
        val initialUrl = window.location.pathname + "#$initialRoute"
        if (window.location.hash != "#$initialRoute") {
            window.history.replaceState(null, "", initialUrl)
        }
        
        // 监听浏览器后退/前进按钮
        val popStateHandler: (Event) -> Unit = { _ ->
            // 浏览器后退/前进时，Decompose 的 handleBackButton 会自动处理
            // 这里我们只需要确保 URL 与当前页面匹配
            val hash = window.location.hash.removePrefix("#")
            val currentRoute = childStack.value.active.instance.config.router
            if (hash != currentRoute) {
                // URL 与当前页面不匹配，更新 URL
                val url = window.location.pathname + "#$currentRoute"
                window.history.replaceState(null, "", url)
            }
        }
        
        window.addEventListener("popstate", popStateHandler)
        
        onDispose {
            cancellation.cancel()
            window.removeEventListener("popstate", popStateHandler)
        }
    }
}

