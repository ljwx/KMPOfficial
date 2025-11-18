package org.example.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.example.project.navigation.AppRoot
import org.example.project.navigation.DesktopLifecycleOwner

/**
 * Desktop (JVM) 应用入口
 * 
 * 职责：
 * 1. 创建和管理 RootComponent 的生命周期
 * 2. 将 Window 生命周期绑定到 Decompose 生命周期
 * 
 * 生命周期管理：
 * - Window 创建时：创建 RootComponent
 * - Window 关闭时：销毁 RootComponent
 * 
 * 状态保存：
 * - Desktop 应用通常不需要持久化状态（应用关闭时状态可以丢失）
 * - 如果需要持久化，可以通过 StateKeeper 保存到文件系统
 */
fun main() = application {
    var rootComponent: org.example.project.navigation.RootComponent? = null
    var lifecycleOwner: DesktopLifecycleOwner? = null
    
    Window(
        onCloseRequest = {
            // 销毁 RootComponent
            lifecycleOwner?.destroyRootComponent()
            rootComponent = null
            lifecycleOwner = null
            exitApplication()
        },
        title = "KotlinProject",
    ) {
        // 创建 Desktop 生命周期管理器
        if (lifecycleOwner == null) {
            lifecycleOwner = DesktopLifecycleOwner(window)
            rootComponent = lifecycleOwner.createRootComponent()
        }
        
        // 传入 RootComponent，确保导航状态能够正确管理
        AppRoot(rootComponent = rootComponent)
    }
}