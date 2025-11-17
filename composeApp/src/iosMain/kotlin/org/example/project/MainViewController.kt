package org.example.project

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import org.example.project.navigation.AppRoot

/**
 * iOS 主视图控制器
 * 
 * 注意：状态栏样式通过 StatusBarConfig Composable 在 AppRoot 中配置
 * iOS 的状态栏样式需要在 Info.plist 中设置 UIViewControllerBasedStatusBarAppearance = false
 * 或者通过 UIViewController 的 preferredStatusBarStyle 方法设置
 */
@OptIn(ExperimentalForeignApi::class)
fun MainViewController() = ComposeUIViewController { AppRoot() }