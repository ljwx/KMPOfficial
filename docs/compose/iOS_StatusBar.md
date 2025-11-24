# iOS 状态栏控制完善说明

## 修改概述

为了让 `StatusBarConfig` 在 iOS 上真正生效，我们实现了一个完整的跨平台状态栏控制方案。

## 实现原理

### 1. Kotlin 层（`StatusBarConfig.ios.kt`）
- 当调用 `StatusBarConfig(style)` 时，保存样式到全局变量 `globalStatusBarStyle`
- 通过 `NSNotificationCenter` 发送通知 `"StatusBarStyleChangedNotification"`
- 提供 `getStatusBarStyle()` 函数供 Swift 层调用

### 2. Swift 层（`ContentView.swift`）
- 创建自定义 `CustomHostingController` 继承自 `UIHostingController`
- 监听来自 Kotlin 的通知
- 重写 `preferredStatusBarStyle` 属性
- 收到通知后调用 `setNeedsStatusBarAppearanceUpdate()` 触发状态栏更新

### 3. 配置层（`Info.plist`）
- 添加 `UIViewControllerBasedStatusBarAppearance = true`
- 启用基于 ViewController 的状态栏样式控制

## 工作流程

```
Compose 页面调用 StatusBarConfig(style)
         ↓
Kotlin 保存样式 + 发送通知
         ↓
Swift CustomHostingController 收到通知
         ↓
调用 getStatusBarStyle() 获取最新样式
         ↓
转换为 UIStatusBarStyle
         ↓
调用 setNeedsStatusBarAppearanceUpdate()
         ↓
iOS 系统更新状态栏显示
```

## 样式映射

| Kotlin 枚举 | iOS UIStatusBarStyle | 效果 |
|------------|---------------------|------|
| `StatusBarStyle.DARK_CONTENT` | `.darkContent` | 深色图标，适合浅色背景 |
| `StatusBarStyle.LIGHT_CONTENT` | `.lightContent` | 浅色图标，适合深色背景 |

## 使用方式

在任何 Composable 函数中调用：

```kotlin
@Composable
fun MyScreen() {
    // 设置深色图标（浅色背景）
    StatusBarConfig(StatusBarStyle.DARK_CONTENT)
    
    // 或设置浅色图标（深色背景）
    StatusBarConfig(StatusBarStyle.LIGHT_CONTENT)
    
    // 你的 UI 代码...
}
```

## 注意事项

1. **DisposableEffect**：`StatusBarConfig` 使用了 `DisposableEffect`，当 Composable 离开组合时不会自动恢复样式。如果需要恢复，可以在父级设置默认样式。

2. **性能**：通知机制非常轻量，不会影响性能。

3. **线程安全**：`NSNotificationCenter` 的调用是线程安全的，可以在任何线程发送通知。

4. **兼容性**：支持 iOS 13.0+（Compose Multiplatform 的最低要求）。

## 测试建议

1. 在 Splash 页面设置 `LIGHT_CONTENT`，检查状态栏是否为白色图标
2. 进入 Home 页面设置 `DARK_CONTENT`，检查状态栏是否变为黑色图标
3. 在不同页面切换，验证状态栏是否正确更新

## 与 Android 的一致性

现在 iOS 和 Android 的状态栏控制逻辑完全一致：
- 都使用 `StatusBarConfig(style)` 函数
- 都支持动态切换
- 都是声明式的（Compose 风格）

这就是 Kotlin Multiplatform 的魅力：**一套 API，多平台实现**！
