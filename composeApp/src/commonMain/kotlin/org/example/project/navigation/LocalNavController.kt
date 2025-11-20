package org.example.project.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavController

/**
 * NavController 的 CompositionLocal
 * 
 * 用于在 Compose 组件树中传递 NavController，避免在每个 Screen 中都传递参数。
 * 
 * 使用方式：
 * 1. 在 AppRootPage 中使用 CompositionLocalProvider 提供 navController
 * 2. 在子组件中使用 LocalNavController.current 访问
 * 
 * 示例：
 * ```kotlin
 * // 提供
 * CompositionLocalProvider(LocalNavController provides navController) {
 *     NavHost(...) { ... }
 * }
 * 
 * // 使用
 * val navController = LocalNavController.current
 * navController.navigate(RouterMainHome)
 * ```
 */
val LocalNavController = compositionLocalOf<NavController> {
    error("No NavController provided. Make sure to wrap your NavHost with CompositionLocalProvider(LocalNavController provides navController)")
}

