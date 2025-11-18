package org.example.project.navigation

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.destroy
import org.example.project.log.KSLog

/**
 * iOS 平台生命周期管理实现
 * 
 * 将 Decompose 的生命周期绑定到 iOS UIViewController 的生命周期。
 * 确保在视图控制器被系统回收或应用进入后台时，导航状态能够正确保存和恢复。
 * 
 * 注意：iOS 的生命周期管理相对简单，因为 UIViewController 通常不会被系统回收。
 * 但在内存压力下，系统可能会释放视图控制器的视图层次结构。
 * 
 * 使用方式：
 * ```swift
 * class ViewController: UIViewController {
 *     private var lifecycleOwner: IOSLifecycleOwner?
 *     private var rootComponent: RootComponent?
 *     
 *     override func viewDidLoad() {
 *         super.viewDidLoad()
 *         lifecycleOwner = IOSLifecycleOwner()
 *         rootComponent = lifecycleOwner?.createRootComponent()
 *         // 设置 Compose UI
 *     }
 *     
 *     override func viewWillDisappear(_ animated: Bool) {
 *         super.viewWillDisappear(animated)
 *         if isMovingFromParent {
 *             lifecycleOwner?.destroyRootComponent()
 *         }
 *     }
 * }
 * ```
 * 
 * 或者在使用 ComposeUIViewController 时：
 * ```kotlin
 * fun MainViewController() = ComposeUIViewController {
 *     val lifecycleOwner = IOSLifecycleOwner()
 *     val rootComponent = lifecycleOwner.createRootComponent()
 *     AppRoot(rootComponent = rootComponent)
 * }
 * ```
 * 
 * 工作原理：
 * 1. viewDidLoad 时创建 RootComponent
 * 2. 监听 UIViewController 的生命周期方法
 * 3. 在 viewWillDisappear 时保存状态（如果需要）
 * 4. 在 viewDidAppear 时恢复状态（如果需要）
 */
class IOSLifecycleOwner : PlatformLifecycleOwner {
    
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
        
        KSLog.iRouter("创建 RootComponent（iOS）")
        
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
        
        // iOS 中，视图控制器创建时通常处于活跃状态
        lifecycle.resume()
        
        KSLog.iRouter("RootComponent 创建完成（iOS）")
        
        return rootComponent!!
    }
    
    /**
     * 暂停 RootComponent（当视图控制器不可见时调用）
     * 
     * 可以在 UIViewController.viewWillDisappear 中调用。
     */
    fun pause() {
        rootComponent?.lifecycle?.pause()
    }
    
    /**
     * 恢复 RootComponent（当视图控制器可见时调用）
     * 
     * 可以在 UIViewController.viewDidAppear 中调用。
     */
    fun resume() {
        rootComponent?.lifecycle?.resume()
    }
    
    /**
     * 停止 RootComponent（当视图控制器被移除时调用）
     * 
     * 可以在 UIViewController.viewDidDisappear 中调用。
     */
    fun stop() {
        rootComponent?.lifecycle?.stop()
    }
    
    override fun destroyRootComponent() {
        KSLog.iRouter("销毁 RootComponent（iOS）")
        
        // 销毁生命周期注册表
        lifecycleRegistry?.destroy()
        lifecycleRegistry = null
        
        rootComponent = null
    }
}

