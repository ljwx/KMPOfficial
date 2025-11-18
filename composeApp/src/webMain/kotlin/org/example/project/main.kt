package org.example.project

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import org.example.project.navigation.AppRoot
import org.example.project.navigation.WebLifecycleOwner

/**
 * Web 应用入口
 * 
 * 职责：
 * 1. 创建和管理 RootComponent 的生命周期
 * 2. 监听浏览器页面生命周期事件
 * 
 * 生命周期管理：
 * - 页面加载时：创建 RootComponent
 * - 页面卸载时：销毁 RootComponent
 * 
 * 状态保存：
 * - Web 平台的状态保存通常使用浏览器历史记录 API
 * - 可以通过 StateKeeper 保存到 localStorage（如果需要）
 * - 导航栈状态会通过 Decompose 的 serializer 自动序列化
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // 创建 Web 生命周期管理器
    val lifecycleOwner = WebLifecycleOwner()
    
    // 创建 RootComponent
    val rootComponent = lifecycleOwner.createRootComponent()
    
    ComposeViewport {
        // 传入 RootComponent，确保导航状态能够正确管理
        AppRoot(rootComponent = rootComponent)
    }
}