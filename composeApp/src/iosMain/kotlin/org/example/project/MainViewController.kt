package org.example.project

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import org.example.project.navigation.AppRoot
import org.example.project.navigation.IOSLifecycleOwner

/**
 * iOS 主视图控制器
 * 
 * 职责：
 * 1. 创建和管理 RootComponent 的生命周期
 * 2. 确保导航状态在视图控制器生命周期内保持
 * 
 * 生命周期管理：
 * - viewDidLoad: 创建 RootComponent
 * - viewWillDisappear: 暂停 RootComponent（如果需要）
 * - viewDidAppear: 恢复 RootComponent（如果需要）
 * - viewDidDisappear: 停止 RootComponent（如果需要）
 * 
 * 注意：
 * - 状态栏样式通过 StatusBarConfig Composable 在 AppRoot 中配置
 * - iOS 的状态栏样式需要在 Info.plist 中设置 UIViewControllerBasedStatusBarAppearance = false
 * - 或者通过 UIViewController 的 preferredStatusBarStyle 方法设置
 * 
 * iOS 特殊说明：
 * - iOS 的 UIViewController 通常不会被系统回收
 * - 但在内存压力下，视图层次结构可能会被释放
 * - RootComponent 通过 StateKeeper 可以保存和恢复状态
 */
@OptIn(ExperimentalForeignApi::class)
fun MainViewController() = ComposeUIViewController {
    // 创建 iOS 生命周期管理器
    val lifecycleOwner = IOSLifecycleOwner()
    
    // 创建 RootComponent
    val rootComponent = lifecycleOwner.createRootComponent()
    
    // 传入 RootComponent，确保导航状态能够正确管理
    AppRoot(rootComponent = rootComponent)
}