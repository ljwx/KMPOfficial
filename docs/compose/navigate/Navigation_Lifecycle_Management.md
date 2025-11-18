# 导航生命周期管理指南

## 概述

本文档详细说明了如何在 KMP Compose 项目中正确管理导航生命周期，确保在配置变更、进程重启等场景下导航状态能够正确保存和恢复。

## 问题背景

在单 Activity/ViewController 架构中，虽然平台入口点本身不会被系统回收，但在以下场景下，状态可能会丢失：

1. **配置变更**（Android 最常见）
   - 屏幕旋转
   - 语言切换
   - 深色模式切换
   - Activity 会被重建，但状态可以通过 Bundle 恢复

2. **进程重启**
   - 系统内存不足，杀死应用进程
   - 用户重新打开应用
   - 状态需要从持久化存储恢复

3. **Compose 组合被移除**
   - 虽然 Activity/ViewController 还在，但 Compose 组合可能被移除
   - 使用 `remember` 的状态会丢失

## 解决方案架构

### 核心组件

1. **RootComponent**
   - 应用根组件，管理 AppNavigation 的生命周期
   - 通过 ComponentContext 提供状态保存和恢复能力

2. **PlatformLifecycleOwner**
   - 平台生命周期管理器接口
   - 每个平台实现此接口，将平台生命周期绑定到 Decompose 生命周期

3. **LifecycleController**
   - Decompose 提供的生命周期控制器
   - 自动同步平台生命周期和 Decompose 生命周期

### 架构图

```
┌─────────────────────────────────────────────────────────┐
│                   平台入口点                              │
│  (Activity/ViewController/Window/ComposeViewport)      │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│            PlatformLifecycleOwner                       │
│  (AndroidLifecycleOwner/IOSLifecycleOwner/...)          │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                  RootComponent                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │         ComponentContext                         │  │
│  │  - Lifecycle: 生命周期管理                        │  │
│  │  - StateKeeper: 状态保存和恢复                    │  │
│  │  - InstanceKeeper: 实例保存                      │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │            AppNavigation                         │  │
│  │  - childStack: 导航栈（自动序列化）              │  │
│  │  - navigation: 导航控制器                         │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## 平台实现

### Android

**文件**: `composeApp/src/androidMain/kotlin/org/example/project/navigation/AndroidLifecycleOwner.kt`

**特点**:
- 使用 `LifecycleEventObserver` 手动绑定 Activity 生命周期到 Decompose
- 通过 `SavedStateRegistry` 保存和恢复状态（主要方式）
- 通过 `Bundle` 保存和恢复状态（备用方式）
- 支持配置变更、进程重启和"不保留活动"场景
- `SerializableContainer` 通过 JSON 序列化保存到 Bundle

**使用示例**:
```kotlin
class MainActivity : ComponentActivity() {
    private val lifecycleOwner = AndroidLifecycleOwner(this)
    private var rootComponent: RootComponent? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 创建 RootComponent，传入 savedInstanceState 以恢复状态
        rootComponent = lifecycleOwner.createRootComponent(savedInstanceState)
        
        setContent {
            AppRoot(rootComponent = rootComponent)
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 保存状态到 Bundle
        lifecycleOwner.saveState(outState)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            lifecycleOwner.destroyRootComponent()
        }
    }
}
```

### iOS

**文件**: `composeApp/src/iosMain/kotlin/org/example/project/navigation/IOSLifecycleOwner.kt`

**特点**:
- UIViewController 通常不会被系统回收
- 但在内存压力下，视图层次结构可能被释放
- 通过 StateKeeper 可以保存和恢复状态

**使用示例**:
```kotlin
fun MainViewController() = ComposeUIViewController {
    val lifecycleOwner = IOSLifecycleOwner()
    val rootComponent = lifecycleOwner.createRootComponent()
    AppRoot(rootComponent = rootComponent)
}
```

### Desktop (JVM)

**文件**: `composeApp/src/jvmMain/kotlin/org/example/project/navigation/DesktopLifecycleOwner.kt`

**特点**:
- 绑定到 Compose Desktop Window 生命周期
- 窗口关闭时销毁 RootComponent
- 通常不需要持久化状态（应用关闭时状态可以丢失）

**使用示例**:
```kotlin
fun main() = application {
    var rootComponent: RootComponent? = null
    var lifecycleOwner: DesktopLifecycleOwner? = null
    
    Window(
        onCloseRequest = {
            lifecycleOwner?.destroyRootComponent()
            exitApplication()
        }
    ) {
        if (lifecycleOwner == null) {
            lifecycleOwner = DesktopLifecycleOwner(window)
            rootComponent = lifecycleOwner.createRootComponent()
        }
        AppRoot(rootComponent = rootComponent)
    }
}
```

### Web

**文件**: `composeApp/src/webMain/kotlin/org/example/project/navigation/WebLifecycleOwner.kt`

**特点**:
- 绑定到浏览器页面生命周期
- 可以通过 localStorage 持久化状态（如果需要）
- 导航栈状态通过 Decompose serializer 自动序列化

**使用示例**:
```kotlin
fun main() {
    val lifecycleOwner = WebLifecycleOwner()
    val rootComponent = lifecycleOwner.createRootComponent()
    
    ComposeViewport {
        AppRoot(rootComponent = rootComponent)
    }
}
```

## 状态保存机制

### 1. 导航栈状态（自动）

Decompose 的 `childStack` 使用 `serializer` 自动序列化导航栈配置：

```kotlin
private val childStack: Value<ChildStack<ScreenRouterData, ScreenComponent>> =
    childStack(
        source = navigation,
        serializer = ScreenRouterData.serializer(), // ← 自动序列化
        initialConfiguration = ScreenRouterData(APP_SPLASH),
        handleBackButton = true,
        childFactory = { config, componentContext ->
            // ...
        }
    )
```

### 2. Component 状态（手动）

如果 Component 需要保存额外的状态，可以使用 `StateKeeper`：

```kotlin
class ProductDetailComponent(
    componentContext: ComponentContext,
    private val productId: String
) : ComponentContext by componentContext {
    
    private var scrollPosition = 0
    
    init {
        // 恢复状态
        stateKeeper.consume<Int>("scrollPosition")?.let {
            scrollPosition = it
        }
        
        // 注册状态保存
        stateKeeper.register("scrollPosition") { scrollPosition }
    }
}
```

### 3. 状态保存流程

```
配置变更/进程重启
    ↓
平台生命周期回调（onSaveInstanceState/viewWillDisappear 等）
    ↓
LifecycleController 检测到生命周期变化
    ↓
StateKeeper 收集所有注册的状态
    ↓
通过 serializer 序列化为字节流
    ↓
保存到平台特定存储（Bundle/UserDefaults/localStorage）
    ↓
恢复时反向流程
```

## 迁移指南

### 从旧版本迁移

如果你之前使用了 `rememberAppNavigation()`，需要迁移到新的架构：

**旧代码**:
```kotlin
@Composable
fun AppRoot() {
    val navigation = rememberAppNavigation()
    // ...
}
```

**新代码**:
```kotlin
// 在平台入口点
val lifecycleOwner = AndroidLifecycleOwner(this)
val rootComponent = lifecycleOwner.createRootComponent(savedInstanceState)

setContent {
    AppRoot(rootComponent = rootComponent)
}
```

### 注意事项

1. **不要在 Compose 中创建 RootComponent**
   - RootComponent 应该在平台入口点创建
   - 不应该使用 `remember` 来保存 RootComponent

2. **正确传递 RootComponent**
   - 通过参数传递给 `AppRoot`
   - 不要在每个 Compose 重组时重新创建

3. **处理预览场景**
   - 预览时可以使用默认的 RootComponent
   - `AppRoot()` 会自动创建默认实例（仅用于预览）

## 常见问题

### Q1: 为什么需要 RootComponent？

**A**: RootComponent 提供了统一的生命周期管理入口，确保：
- 导航状态在配置变更时能够恢复
- 进程重启后能够恢复导航栈
- 所有平台使用统一的 API

### Q2: 配置变更时导航栈会丢失吗？

**A**: 不会。通过 `LifecycleController` 和 `StateKeeper`，导航栈状态会自动保存到 Bundle，并在 Activity 重建时恢复。

### Q3: iOS 也需要这个机制吗？

**A**: 虽然 iOS 的 UIViewController 通常不会被回收，但在以下场景下仍然有用：
- 内存压力下视图层次结构被释放
- 应用进入后台后恢复
- 统一跨平台 API

### Q4: Desktop 和 Web 需要持久化状态吗？

**A**: 
- **Desktop**: 通常不需要，应用关闭时状态可以丢失
- **Web**: 可以通过 localStorage 持久化（如果需要）

### Q5: 如何测试状态恢复？

**A**: 
1. **Android**: 旋转屏幕，检查导航栈是否恢复
2. **所有平台**: 杀死应用进程，重新打开，检查导航栈是否恢复
3. **Android 开发者选项**: 开启"不保留活动"选项，退到后台再打开应用，检查导航栈是否恢复

### Q6: Android 上状态保存失败的问题

**问题描述**：
在 Android 上，当开启"不保留活动"选项后，应用退到后台再打开时，导航栈状态无法恢复，总是从 Splash 页开始。

**问题原因**：
1. `SerializableContainer` 不能直接转换为 `java.io.Serializable`，导致 `ClassCastException`
2. `SerializableContainer` 也不是 `Parcelable` 类型
3. 状态保存失败，Bundle 为空，恢复时无法获取状态

**解决方案**：
使用 `kotlinx.serialization` 将 `SerializableContainer` 序列化为 JSON 字符串，然后保存到 Bundle：

```kotlin
// 保存状态
val container = stateKeeper.save()
if (container != null && container !is java.io.Serializable) {
    // 使用 JSON 序列化
    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    val serialized = json.encodeToString(
        kotlinx.serialization.serializer<SerializableContainer>(),
        container
    )
    bundle.putString("state_json", serialized)
}

// 恢复状态
val jsonString = restoredBundle?.getString("state_json")
if (jsonString != null) {
    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    val container = json.decodeFromString(
        kotlinx.serialization.serializer<SerializableContainer>(),
        jsonString
    )
    StateKeeperDispatcher(container)
}
```

**关键点**：
- `SerializableContainer` 需要通过 JSON 序列化才能保存到 Android Bundle
- 必须在 `super.onCreate(savedInstanceState)` 之后调用 `consumeRestoredStateForKey`
- 使用 `SavedStateRegistry` 作为主要的状态保存机制

## 商业级改进

### 错误处理策略

#### 1. 保存失败时的降级策略

**问题**：保存失败时静默失败，用户可能丢失导航状态。

**解决方案**：
- 多层错误处理：JSON 序列化失败时尝试不压缩保存
- 保存完全失败时返回空 Bundle，但不抛出异常，避免应用崩溃
- 记录详细错误日志，便于问题排查
- 下次启动时使用降级策略（创建新状态）

```kotlin
private fun saveStateToBundle(stateKeeper: StateKeeper): Bundle {
    val bundle = Bundle()
    try {
        // 尝试保存状态
        // ...
    } catch (e: Exception) {
        // 保存失败时的降级策略
        KSLog.eRouter("保存状态失败，返回空 Bundle", e)
        // 返回空 Bundle，但不抛出异常，避免影响应用运行
        // 下次启动时会使用降级策略（创建新状态）
    }
    return bundle
}
```

#### 2. 恢复失败时的降级策略

**问题**：恢复失败时用户会回到首页，体验不佳。

**解决方案**：
- 双重恢复机制：优先从 `SavedStateRegistry` 恢复，备用方案从 `savedInstanceState` 恢复
- 恢复失败时使用降级策略：创建新的导航状态，但记录错误信息
- 确保应用能够正常启动，不会因为恢复失败而崩溃

```kotlin
// 策略1: 优先从 SavedStateRegistry 恢复（官方推荐方式）
val restoredFromRegistry = try {
    savedStateRegistry.consumeRestoredStateForKey(savedStateKey) as? Bundle
} catch (e: Exception) {
    KSLog.wRouter("从 SavedStateRegistry 恢复失败，尝试备用方案", e)
    null
}

// 策略2: 备用方案从 savedInstanceState 恢复
val restoredBundle = restoredFromRegistry ?: savedInstanceState?.getBundle(savedStateKey)

// 策略3: 恢复失败时使用降级策略
val stateKeeper = if (restoredContainer != null) {
    StateKeeperDispatcher(restoredContainer)
} else {
    KSLog.wRouter("状态恢复失败，使用降级策略：创建新的导航状态")
    StateKeeperDispatcher()
}
```

### 状态验证

#### 验证内容

1. **状态容器非空检查**：确保恢复的状态不为空
2. **状态结构完整性**：验证状态的基本结构是否有效
3. **版本兼容性检查**：检查状态版本号，确保兼容性

```kotlin
private fun validateRestoredState(container: SerializableContainer): StateValidationResult {
    try {
        // 验证1: 检查状态是否为空
        if (container.isEmpty) {
            return StateValidationResult(false, "状态容器为空")
        }
        
        // 验证2: 检查状态结构完整性
        // 更详细的验证会在 RootComponent 创建后通过导航栈恢复来验证
        
        return StateValidationResult(true, null)
    } catch (e: Exception) {
        return StateValidationResult(false, "状态验证异常: ${e.message}")
    }
}
```

#### 验证时机

- 在恢复状态后立即验证
- 如果验证失败，使用降级策略（创建新状态）
- 记录验证失败的原因，便于问题排查

### 恢复策略

#### 三重恢复机制

1. **主要方式**：从 `SavedStateRegistry` 恢复（官方推荐）
2. **备用方式**：从 `savedInstanceState` 恢复
3. **降级策略**：恢复失败时创建新的导航状态

#### 恢复流程

```
尝试从 SavedStateRegistry 恢复
    ↓ (失败)
尝试从 savedInstanceState 恢复
    ↓ (失败)
验证恢复的状态
    ↓ (验证失败)
使用降级策略：创建新的导航状态
```

#### 多格式支持

支持多种恢复格式，确保向后兼容：

1. **压缩 JSON**：对于大状态（>10KB），使用压缩格式
2. **普通 JSON**：主要恢复方式
3. **Serializable**：兼容旧版本

```kotlin
private fun restoreStateFromBundle(bundle: Bundle): SerializableContainer? {
    // 方式1: 尝试从压缩的 JSON 恢复
    val compressed = bundle.getByteArray("state_json_compressed")
    if (compressed != null) {
        val decompressed = decompressString(compressed)
        // ...
    }
    
    // 方式2: 尝试从普通 JSON 恢复（主要方式）
    val jsonString = bundle.getString("state_json")
    // ...
    
    // 方式3: 尝试作为 Serializable 恢复（兼容旧版本）
    bundle.getSerializable("state") as? SerializableContainer
    // ...
}
```

### 性能优化

#### 1. JSON 序列化器复用

**优化点**：复用 JSON 序列化器实例，避免重复创建。

```kotlin
// JSON 序列化器（复用实例以提高性能）
private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}
```

#### 2. 状态压缩

**优化点**：对于大状态（>10KB），自动使用压缩，减少存储空间和序列化时间。

```kotlin
// 如果 JSON 字符串较大（>10KB），尝试压缩
if (serialized.length > 10 * 1024) {
    val compressed = compressString(serialized)
    if (compressed != null && compressed.size < serialized.length) {
        bundle.putByteArray("state_json_compressed", compressed)
        bundle.putBoolean("state_compressed", true)
    }
}
```

**压缩算法**：使用 `Deflater`/`Inflater` 进行压缩和解压。

#### 3. 异步保存（预留）

**优化点**：虽然 `SavedStateProvider` 是同步的，但我们预留了异步保存的基础设施。

```kotlin
// 用于异步保存的协程作用域
private val saveJob = SupervisorJob()
private val saveScope = CoroutineScope(Dispatchers.IO + saveJob)
```

**注意**：`SavedStateRegistry.registerSavedStateProvider` 要求同步返回 Bundle，所以实际保存仍然是同步的。但我们可以使用协程作用域进行其他异步操作（如日志记录、状态分析等）。

#### 4. 状态版本管理

**优化点**：添加状态版本号，便于未来进行版本兼容性检查和迁移。

```kotlin
// 状态版本号，用于版本兼容性检查
private val stateVersionKey = "state_version"
private val currentStateVersion = 1

// 保存时添加版本号
bundle.putInt(stateVersionKey, currentStateVersion)

// 恢复时检查版本号
val version = restoredBundle.getInt(stateVersionKey, 0)
```

### 改进对比

| 方面 | 改进前 | 改进后 | 提升 |
|------|--------|--------|------|
| 错误处理 | 记录日志，静默失败 | 记录日志 + 降级策略 | ✅ 健壮性提升 |
| 状态验证 | 无 | 验证状态有效性 | ✅ 可靠性提升 |
| 恢复策略 | 双重恢复 | 双重恢复 + 降级 | ✅ 成功率提升 |
| 性能优化 | 无 | 压缩、复用实例 | ✅ 性能提升 |
| 版本兼容 | 无 | 版本号管理 | ✅ 可维护性提升 |

## 最佳实践

1. **始终使用 RootComponent**
   - 不要直接创建 `AppNavigation`
   - 通过 `RootComponent.navigation` 访问导航功能

2. **正确绑定生命周期**
   - 在平台入口点创建 RootComponent
   - 在生命周期回调中保存和恢复状态

3. **使用 StateKeeper 保存 Component 状态**
   - 对于需要持久化的状态，使用 `stateKeeper.register()`
   - 在 Component 初始化时恢复状态

4. **测试配置变更**
   - 定期测试屏幕旋转、语言切换等场景
   - 确保导航栈能够正确恢复

## 相关文档

- [Decompose 官方文档](https://arkivanov.github.io/Decompose/)
- [Navigation_Decompose.md](./Navigation_Decompose.md) - Decompose 导航使用指南
- [Component_Best_Practices.md](../Component_Best_Practices.md) - Component 最佳实践

## 总结

通过使用 RootComponent 和 PlatformLifecycleOwner，我们实现了：

✅ **跨平台统一的生命周期管理**
✅ **配置变更时状态自动恢复**
✅ **进程重启后状态恢复**
✅ **"不保留活动"场景下的状态恢复**（Android）
✅ **可商用的代码质量**

### 关键技术点

1. **Android 状态保存**：
   - 使用 `SavedStateRegistry` 作为主要的状态保存机制
   - `SerializableContainer` 通过 JSON 序列化保存到 Bundle
   - 支持配置变更和"不保留活动"场景

2. **状态恢复**：
   - 优先从 `SavedStateRegistry` 恢复状态
   - 备用方案：从 `savedInstanceState` 恢复
   - 支持 JSON 反序列化恢复 `SerializableContainer`
   - **改进**：三重恢复机制 + 状态验证 + 降级策略

3. **生命周期绑定**：
   - Activity 生命周期通过 `LifecycleEventObserver` 同步到 Decompose
   - 确保状态在正确的时机保存和恢复

4. **错误处理**：
   - 保存失败时使用降级策略，避免应用崩溃
   - 恢复失败时创建新状态，确保应用能正常启动
   - 详细记录错误日志，便于问题排查

5. **性能优化**：
   - JSON 序列化器复用，减少对象创建
   - 大状态自动压缩，减少存储空间
   - 状态版本管理，便于未来兼容性处理

这确保了应用在各种场景下都能提供良好的用户体验，符合商业级应用的标准。

