package org.example.project.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.destroy

/**
 * 平台生命周期管理接口
 * 
 * 用于在不同平台上创建和管理 RootComponent 的生命周期。
 * 每个平台需要实现此接口，将 Decompose 的生命周期绑定到平台原生生命周期。
 * 
 * 生命周期绑定流程：
 * 1. 平台入口点（Activity/ViewController/Window）创建时，调用 createRootComponent()
 * 2. 平台生命周期变化时，调用相应的生命周期方法
 * 3. RootComponent 的 ComponentContext 会自动处理状态保存和恢复
 */
interface PlatformLifecycleOwner {
    /**
     * 创建 RootComponent 实例
     * 
     * 应该在平台入口点（Activity.onCreate、ViewController.viewDidLoad 等）调用。
     * 返回的 RootComponent 应该在平台生命周期内保持存活。
     * 
     * @return RootComponent 实例
     */
    fun createRootComponent(): RootComponent
    
    /**
     * 销毁 RootComponent
     * 
     * 应该在平台入口点销毁时调用（Activity.onDestroy、ViewController.viewDidDisappear 等）。
     * 这会触发 ComponentContext 的销毁流程，清理所有资源。
     */
    fun destroyRootComponent()
}

/**
 * 默认的平台生命周期实现（用于没有原生生命周期绑定的场景）
 * 
 * 这个实现使用独立的 LifecycleRegistry，不绑定到任何平台生命周期。
 * 适用于测试、预览等场景。
 * 
 * 注意：在生产环境中，应该使用平台特定的实现（如 AndroidLifecycleOwner）。
 */
class DefaultPlatformLifecycleOwner : PlatformLifecycleOwner {
    private var rootComponent: RootComponent? = null
    private var lifecycleRegistry: LifecycleRegistry? = null
    
    override fun createRootComponent(): RootComponent {
        if (rootComponent != null) {
            return rootComponent!!
        }
        
        val lifecycle = LifecycleRegistry()
        lifecycleRegistry = lifecycle // 保存引用以便销毁时使用
        
        // 注意：StateKeeper 由 DefaultComponentContext 自动创建
        // childStack 的 serializer 会自动处理状态保存
        val componentContext = DefaultComponentContext(
            lifecycle = lifecycle
        )
        
        rootComponent = RootComponent(componentContext)
        lifecycle.resume()
        
        return rootComponent!!
    }
    
    override fun destroyRootComponent() {
        // 销毁生命周期注册表
        lifecycleRegistry?.destroy()
        lifecycleRegistry = null
        
        rootComponent = null
    }
}

