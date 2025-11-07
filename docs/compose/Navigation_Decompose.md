## Decompose 库使用指南

本文聚焦如何在 Compose Multiplatform 项目中使用 **Decompose** 导航库，帮助你理解其概念、主要模块与常用 API。

---

### 1. Decompose 是什么？

- **定位**：Kotlin Multiplatform 的导航与组件化框架，专注在逻辑组件（Component）与路由（Router）的组合。  
- **平台覆盖**：Android、iOS、Desktop、Web。任何部署 Compose 的平台都可以复用相同的导航逻辑。  
- **核心理念**：将业务逻辑放在可复用的 `Component` 中，通过 `ComponentContext` 提供生命周期、状态保存、导航等能力。

官方文档：[https://arkivanov.github.io/Decompose/](https://arkivanov.github.io/Decompose/)

---

### 2. 基础概念

| 概念 | 说明 |
| --- | --- |
| `ComponentContext` | Decompose 的基础环境，封装生命周期 (Lifecycle)、状态保存 (StateKeeper)、实例储存 (InstanceKeeper) 等能力。 |
| `StackNavigation<C>` | 堆栈式导航控制器，配合 `childStack` 创建多级返回栈。 |
| `childStack` | 根据导航事件生成 `ChildStack`（包含当前、历史页面）。可与 Compose 集成渲染。 |
| `Value<T>` | Decompose 的可观察数据容器，常用 `subscribe` 或 Compose 扩展将其转换成 state。 |
| `LifecycleController` | 管理原生平台的生命周期（Android Activity/Fragment、Desktop Window 等）。 |

---

### 3. 常用模块与依赖

以 Compose Multiplatform 1.9.x、Kotlin 2.x 为例：

```kotlin
// build.gradle.kts (commonMain)
implementation("com.arkivanov.decompose:decompose:3.3.0")
implementation("com.arkivanov.decompose:extensions-compose:3.3.0") // Compose 集成
implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3") // 3.x 使用 serialization
```

此外需要启用 `kotlinx-serialization` 插件，以便自动生成状态序列化代码。

---

### 4. 创建导航堆栈

```kotlin
private val navigation = StackNavigation<Screen>()

private val childStack: Value<ChildStack<Screen, Screen>> = childStack(
    source = navigation,
    serializer = Screen.serializer(),  // 需要 kotlinx.serialization
    initialConfiguration = Screen.Home,
    handleBackButton = true,
    childFactory = ::createChild
)
```

要点：
1. `Screen` 必须标记 `@Serializable`（Decompose 3.x 默认采用 serialization 保存配置）。  
2. `childFactory` 将 `Screen` 实例转换成实际渲染的子组件或界面。

---

### 5. 在 Compose 中渲染堆栈

两种方式：

1. **使用官方 Compose 扩展**
   ```kotlin
   Children(stack = childStack) { child ->
       when (val screen = child.instance) {
           Screen.Home -> HomePage()
           is Screen.Detail -> DetailPage(screen.id)
       }
   }
   ```

2. **手动订阅 Value**
   ```kotlin
   val childStackState by childStack.subscribeAsState()
   val active = childStackState.active.instance
   ```

在多平台场景推荐第一种，Compose 扩展已经处理了动画、生命周期等细节。

---

### 6. 触发导航事件

`StackNavigation` 提供一组常用方法：

| 方法 | 作用 |
| --- | --- |
| `push(config)` | 入栈新页面。 |
| `pop()` | 返回上一页。 |
| `replaceCurrent(config)` | 替换当前页面。 |
| `popToFirst()` | 返回到栈底。 |
| `navigate { stack -> ... }` | 自定义操作堆栈。 |

示例：
```kotlin
fun openDetail(id: String) {
    navigation.push(Screen.Detail(id))
}

fun onBackPressed() {
    navigation.pop()
}
```

---

### 7. 组合业务组件

在更完整的架构中，可为每个界面封装 `Component`：

```kotlin
class RootComponent(componentContext: ComponentContext) : ComponentContext by componentContext {
    private val navigation = StackNavigation<Config>()
    val childStack = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Home,
        childFactory = ::createChild
    )

    private fun createChild(config: Config, componentContext: ComponentContext): Child = when (config) {
        Config.Home -> Child.Home(HomeComponent(componentContext, ::onDetailRequested))
        is Config.Detail -> Child.Detail(DetailComponent(componentContext, config.id))
    }
}
```

在 Compose 层只需观察 `childStack`，根据 `Child` 类型调用对应组件的 `Content()`。

---

### 8. 生命周期与状态保存

- `ComponentContext` 自动提供 `Lifecycle`；可在 Android Activity/Fragment 或 Desktop Window 中使用 `LifecycleController` 绑定。  
- Decompose 默认使用 `StateKeeper` 保存序列化数据，配合 `serializer` 自动恢复。  
- 若组件需要持久任务（如协程），可使用 `InstanceKeeper` 或配合 `Essenty` 的 `CoroutineScope`。

---

### 9. 与其他技术对比

| 特性 | Decompose | Voyager | Precompose |
| --- | --- | --- | --- |
| 目标平台 | Multiplatform | Multiplatform | Multiplatform |
| 维护活跃度 | 高（JetBrains 合作） | 高（社区） | 正常 |
| 架构思想 | Component + Router | Navigator + Screen | Router + ViewModel |
| 序列化方式 | kotlinx.serialization | Parcelable/Serializable | Parcelable/Serializable |
| 适合场景 | 复杂、长期演进、需要多端统一 | 快速上手、API 简洁 | API 与官方 Navigation 类似 |

若项目需要深度定制、后续可能迁移到官方导航，Decompose 是目前最稳妥的方案之一。

---

### 10. 下一步建议

1. 跟随官方文档完成 [Hello World 示例](https://arkivanov.github.io/Decompose/quick-start/)。  
2. 熟悉 `Component` 抽象与多子导航模式（`ChildSlot`、`ChildPages`）。  
3. 若集成 Android TOP-level 组件，可参考 `JetpackComponentContext` 模块实现与 Activity/Fragment 的互操作。  
4. 关注发行说明：3.4.x 引入了 Predictive Back 新动画、Web 导航等特性，升级时需验证兼容性。

通过以上步骤，你就可以在自己的 Compose Multiplatform 项目中使用 Decompose 打造跨平台的导航结构。遇到具体问题时，建议同时参考官方文档与社区示例获取更深入的指导。

