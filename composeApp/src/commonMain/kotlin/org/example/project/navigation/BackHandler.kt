package org.example.project.navigation

import androidx.compose.runtime.Composable

/**
 * 跨平台返回键处理工具
 * 
 * 这个工具提供了跨平台的返回键处理能力，可以在页面级别自定义返回键的行为。
 * 
 * **使用场景**：
 * - 在返回前显示确认对话框（如有未保存的更改）
 * - 在返回前保存数据
 * - 在返回前执行清理操作
 * - 阻止返回（如显示重要提示）
 * 
 * **使用示例**：
 * 
 * 示例1：显示确认对话框
 * ```kotlin
 * @Composable
 * fun EditScreen() {
 *     val navigation = LocalAppNavigation.current
 *     var showConfirmDialog by remember { mutableStateOf(false) }
 *     var hasUnsavedChanges by remember { mutableStateOf(true) }
 *     
 *     // 自定义返回键行为
 *     BackHandler(enabled = true) {
 *         if (hasUnsavedChanges) {
 *             showConfirmDialog = true
 *             true // 返回 true 表示已处理，阻止默认返回行为
 *         } else {
 *             navigation.navigateBack()
 *             true // 执行返回后，返回 true 表示已处理
 *         }
 *     }
 *     
 *     // 确认对话框
 *     if (showConfirmDialog) {
 *         AlertDialog(
 *             onDismissRequest = { showConfirmDialog = false },
 *             title = { Text("确认返回") },
 *             text = { Text("有未保存的更改，确定要返回吗？") },
 *             confirmButton = {
 *                 TextButton(onClick = {
 *                     showConfirmDialog = false
 *                     navigation.navigateBack()
 *                 }) {
 *                     Text("确定")
 *                 }
 *             },
 *             dismissButton = {
 *                 TextButton(onClick = { showConfirmDialog = false }) {
 *                     Text("取消")
 *                 }
 *             }
 *         )
 *     }
 *     
 *     // ... 其他 UI
 * }
 * ```
 * 
 * 示例2：简单返回
 * ```kotlin
 * @Composable
 * fun DetailScreen() {
 *     val navigation = LocalAppNavigation.current
 *     
 *     BackHandler {
 *         // 执行返回
 *         navigation.navigateBack()
 *         true // 返回 true 表示已处理
 *     }
 *     
 *     // ... 其他 UI
 * }
 * ```
 * 
 * **平台说明**：
 * - **Android**: 使用 `androidx.activity.compose.BackHandler` 拦截系统返回键 ✅
 * - **iOS**: iOS 设备通常没有物理返回键，主要使用导航栏返回按钮。此工具提供占位实现以保持 API 一致性
 * - **Desktop**: 使用 `Modifier.onKeyEvent` 监听 Escape 键 ✅
 * - **Web**: 监听 Escape 键和浏览器后退按钮 ✅
 * 
 * **重要提示**：
 * - 由于项目使用 Decompose 导航且设置了 `handleBackButton = true`，Decompose 会自动处理返回键
 * - 使用此工具可以拦截返回键并执行自定义逻辑
 * - 在 Android 上，一旦 BackHandler 的回调被调用，就会阻止默认返回行为
 * - 建议在 `onBack` 中总是处理返回逻辑，如果需要默认返回，则调用 `navigation.navigateBack()`
 * - 返回 `true` 表示已处理返回键，阻止默认行为
 * 
 * @param enabled 是否启用返回键处理，默认为 true。设置为 false 时，返回键会使用默认行为
 * @param onBack 返回键被按下时的回调函数。返回 `true` 表示已处理（阻止默认行为），返回 `false` 理论上不应该使用
 */
@Composable
expect fun BackHandler(
    enabled: Boolean = true,
    onBack: () -> Boolean
)

