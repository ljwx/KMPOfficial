package org.example.project.navigation

import androidx.compose.ui.window.Window
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.destroy
import org.example.project.log.KSLog

/**
 * Desktop (JVM) 平台生命周期管理实现
 * 
 * 将 Decompose 的生命周期绑定到 Compose Desktop Window 的生命周期。
 * 确保在窗口关闭、最小化等场景下，导航状态能够正确保存和恢复。
 * 
 * 使用方式：
 * ```kotlin
 * fun main() = application {
 *     val lifecycleOwner = DesktopLifecycleOwner()
 *     var rootComponent: RootComponent? = null
 *     
 *     Window(
 *         onCloseRequest = {
 *             lifecycleOwner.destroyRootComponent()
 *             exitApplication()
 *         },
 *         title = "App"
 *     ) {
 *         if (rootComponent == null) {
 *             rootComponent = lifecycleOwner.createRootComponent()
 *         }
 *         AppRoot(rootComponent = rootComponent!!)
 *     }
 * }
 * ```
 * 
 * 工作原理：
 * 1. Window 创建时创建 RootComponent
 * 2. Window 创建后立即将生命周期设置为 resume（Desktop 窗口创建后通常就是可见的）
 * 3. Window 关闭时销毁 RootComponent
 * 
 * 注意：Desktop 应用通常不需要复杂的状态保存，因为应用关闭时状态可以丢失。
 * 如果需要持久化状态，可以通过 StateKeeper 保存到文件系统。
 */
class DesktopLifecycleOwner(
    private val window: Window
) : PlatformLifecycleOwner {
    
    private var rootComponent: RootComponent? = null
    private var lifecycleRegistry: LifecycleRegistry? = null
    
    /**
     * 创建 RootComponent 实例
     * 
     * @return RootComponent 实例
     */
    override fun createRootComponent(): RootComponent {
        if (rootComponent != null) {
            KSLog.wRouter("RootComponent 已存在，返回现有实例")
            return rootComponent!!
        }
        
        KSLog.iRouter("创建 RootComponent（Desktop）")
        
        // 创建 Decompose 的生命周期组件
        // 注意：StateKeeper 由 DefaultComponentContext 自动创建
        val lifecycle = LifecycleRegistry()
        lifecycleRegistry = lifecycle // 保存引用以便销毁时使用
        
        // 创建 ComponentContext
        val componentContext = DefaultComponentContext(
            lifecycle = lifecycle
        )
        
        // 创建 RootComponent
        rootComponent = RootComponent(componentContext)
        
        // Desktop 窗口创建后通常就是可见的，直接设置为 resume
        lifecycle.resume()
        
        KSLog.iRouter("RootComponent 创建完成（Desktop）")
        
        return rootComponent!!
    }
    
    override fun destroyRootComponent() {
        KSLog.iRouter("销毁 RootComponent（Desktop）")
        
        // 销毁生命周期注册表
        lifecycleRegistry?.destroy()
        lifecycleRegistry = null
        
        rootComponent = null
    }
}

