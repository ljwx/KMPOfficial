package org.example.project.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Android 平台的返回键处理实现
 * 使用 androidx.activity.compose.BackHandler 来拦截系统返回键
 * 
 * 工作原理：
 * - Android 的 BackHandler 一旦回调被调用，就会阻止默认返回行为
 * - 由于项目使用 Decompose 导航且设置了 handleBackButton = true，Decompose 会自动处理返回键
 * - 使用此 BackHandler 可以拦截返回键并执行自定义逻辑
 * 
 * 使用建议：
 * - 如果需要在返回前执行某些操作（如显示确认对话框、保存数据等），在 onBack 中处理
 * - 如果需要执行默认返回，在 onBack 中调用 navigation.navigateBack()，然后返回 true
 * - 如果不需要返回（如显示对话框），在 onBack 中返回 true，不调用 navigation.navigateBack()
 * - 返回 true 表示已处理返回键，阻止默认行为；返回 false 理论上不应该使用（因为会阻止默认行为）
 */
@Composable
actual fun BackHandler(
    enabled: Boolean,
    onBack: () -> Boolean
) {
    BackHandler(enabled = enabled) {
        // 调用 onBack，如果返回 true 表示已处理，BackHandler 会阻止默认行为
        // 注意：Android 的 BackHandler 一旦回调被调用就会阻止默认行为
        // 所以建议在 onBack 中总是处理返回逻辑，如果需要默认返回，则调用 navigation.navigateBack()
        onBack()
    }
}

