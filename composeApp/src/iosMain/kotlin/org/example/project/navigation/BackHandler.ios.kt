package org.example.project.navigation

import androidx.compose.runtime.Composable

/**
 * iOS 平台的返回键处理实现
 * 
 * 注意：
 * - iOS 设备通常没有物理返回键，主要使用导航栏的返回按钮
 * - Decompose 已经通过 handleBackButton = true 处理了返回逻辑
 * - 如果需要自定义返回行为，建议在导航栏按钮的 onClick 中处理
 * - 这里提供一个占位实现，保持 API 一致性
 * 
 * 实际使用建议：
 * 在 iOS 上，通过 CommonTopBar 的 onBack 参数来处理返回逻辑
 */
@Composable
actual fun BackHandler(
    enabled: Boolean,
    onBack: () -> Boolean
) {
    // iOS 平台通常使用导航栏返回按钮，而不是物理返回键
    // Decompose 已经处理了返回逻辑，这里提供一个占位实现以保持 API 一致性
    // 实际使用时，建议通过导航栏按钮来处理返回
}

