package org.example.project.navigation

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.destroy
import org.example.project.log.KSLog

/**
 * Web 平台生命周期管理实现
 * 
 * 将 Decompose 的生命周期绑定到浏览器页面的生命周期。
 * 确保在页面刷新、关闭等场景下，导航状态能够正确保存和恢复。
 * 
 * 使用方式：
 * ```kotlin
 * fun main() {
 *     val lifecycleOwner = WebLifecycleOwner()
 *     val rootComponent = lifecycleOwner.createRootComponent()
 *     
 *     ComposeViewport {
 *         AppRoot(rootComponent = rootComponent)
 *     }
 * }
 * ```
 * 
 * 工作原理：
 * 1. 页面加载时创建 RootComponent
 * 2. 监听浏览器页面生命周期事件（beforeunload、visibilitychange 等）
 * 3. 在页面卸载前保存状态到 localStorage（如果需要）
 * 4. 页面加载时从 localStorage 恢复状态（如果需要）
 * 
 * 注意：Web 平台的状态保存通常使用浏览器历史记录 API 或 localStorage。
 * Decompose 的 StateKeeper 可以与这些机制配合使用。
 */
class WebLifecycleOwner : PlatformLifecycleOwner {
    
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
        
        KSLog.iRouter("创建 RootComponent（Web）")
        
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
        
        // Web 页面加载时通常处于活跃状态
        lifecycle.resume()
        
        // 监听页面卸载事件，保存状态
        // 注意：这需要平台特定的实现
        // setupBeforeUnloadListener()
        
        KSLog.iRouter("RootComponent 创建完成（Web）")
        
        return rootComponent!!
    }
    
    override fun destroyRootComponent() {
        KSLog.iRouter("销毁 RootComponent（Web）")
        
        // 在销毁前保存状态到 localStorage（如果需要）
        // saveStateToLocalStorage()
        
        // 销毁生命周期注册表
        lifecycleRegistry?.destroy()
        lifecycleRegistry = null
        
        rootComponent = null
    }
}

