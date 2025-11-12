## CompositionLocal 使用指南

`CompositionLocal` 是 Jetpack Compose 提供的依赖注入机制，用于在组件树中向下传递值，避免逐层传递参数。本文档介绍其概念、用法、最佳实践以及在本项目中的应用。

---

## 什么是 CompositionLocal？

`CompositionLocal` 是 Compose 的隐式参数传递机制，类似于依赖注入容器。它允许你在组件树的某个位置提供一个值，然后该位置的所有子组件都可以访问这个值，而无需通过函数参数显式传递。

### 核心组件

1. **`CompositionLocal`**：定义需要传递的值类型
2. **`CompositionLocalProvider`**：在组件树中提供值
3. **`.current`**：在子组件中访问值

---

## 项目中的实际应用

### 1. 定义 CompositionLocal

在 `AppNavigation.kt` 中定义了导航相关的 CompositionLocal：

```kotlin
val LocalAppNavigation = staticCompositionLocalOf<IAppNavigation> {
    error("No AppNavigation provided")
}
```

**说明**：
- 使用 `staticCompositionLocalOf` 创建静态 CompositionLocal
- 如果未提供值，访问时会抛出错误
- 类型为 `IAppNavigation` 接口

### 2. 提供值

在 `AppRoot.kt` 中使用 `CompositionLocalProvider` 提供导航实例：

```kotlin
@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    val navigation = rememberAppNavigation()
    CompositionLocalProvider(LocalAppNavigation provides navigation) {
        navigation.Render(modifier = modifier.fillMaxSize(), null) { modifier, router, appNavigation ->
            when (router.router) {
                ROUTER_HOME -> MainHomeContainer()
                PRODUCT_DETAIL -> DetailScreen("test")
            }
        }
    }
}
```

**要点**：
- `rememberAppNavigation()` 创建导航实例
- `CompositionLocalProvider` 包裹需要访问导航的组件树
- 使用 `provides` 关键字提供值

### 3. 在子组件中访问

在 `HomeScreen` 等子组件中通过 `.current` 访问：

```kotlin
@Composable
fun HomeScreen(
    tabTitle: String,
    modifier: Modifier = Modifier,
) {
    val navigation = LocalAppNavigation.current
    
    Button(onClick = {
        navigation.openScreen(PRODUCT_DETAIL)
    }) {
        Text("查看详情")
    }
}
```

**优势**：
- 无需通过函数参数传递 `navigation`
- 任何深度的子组件都可以直接访问
- 代码更简洁，耦合度更低

---

## 为什么使用 CompositionLocal？

### 1. 避免参数传递地狱

**不使用 CompositionLocal**：
```kotlin
@Composable
fun AppRoot() {
    val navigation = rememberAppNavigation()
    MainContainer(navigation = navigation)
}

@Composable
fun MainContainer(navigation: IAppNavigation) {
    HomeScreen(navigation = navigation)
}

@Composable
fun HomeScreen(navigation: IAppNavigation) {
    // 需要导航功能
}
```

**使用 CompositionLocal**：
```kotlin
@Composable
fun AppRoot() {
    val navigation = rememberAppNavigation()
    CompositionLocalProvider(LocalAppNavigation provides navigation) {
        MainContainer() // 无需传递参数
    }
}

@Composable
fun MainContainer() {
    HomeScreen() // 无需传递参数
}

@Composable
fun HomeScreen() {
    val navigation = LocalAppNavigation.current // 直接访问
}
```

### 2. 解耦组件

- 子组件不需要知道 `navigation` 的来源
- 只需知道接口 `IAppNavigation`，符合依赖倒置原则
- 便于测试：可以轻松替换不同的实现

### 3. 作用域控制

`CompositionLocalProvider` 的作用域内，所有子组件都能访问同一个实例：
- 保证全局单例
- 避免重复创建
- 统一管理生命周期

---

## staticCompositionLocalOf vs compositionLocalOf

### 区别对比

| 特性 | `staticCompositionLocalOf` | `compositionLocalOf` |
| --- | --- | --- |
| **重组行为** | 值变化时**不会**触发重组 | 值变化时**会**触发重组 |
| **性能** | 性能更好，无额外开销 | 性能稍差，需要跟踪变化 |
| **适用场景** | 值很少变化或不变 | 值会频繁变化 |
| **状态管理** | 需要手动管理状态订阅 | 自动处理状态变化 |

### 项目中的选择

本项目使用 `staticCompositionLocalOf` 是**正确的选择**，原因：

1. **导航对象本身不变化**：`AppNavigation` 实例在应用生命周期内保持不变
2. **状态通过其他方式管理**：导航状态通过 `Value<ChildStack>` 管理，子组件通过 `navigation.state` 访问
3. **性能优化**：避免不必要的重组，提升性能

### 何时使用 compositionLocalOf

如果导航对象本身会变化（例如切换不同的导航实例），应该使用：

```kotlin
val LocalAppNavigation = compositionLocalOf<IAppNavigation> {
    error("No AppNavigation provided")
}
```

---

## 这是常规用法吗？

**是的**，这是 Compose 的**标准且推荐**的用法。Jetpack Compose 官方库中大量使用了 CompositionLocal：

### 官方示例

1. **主题相关**：
   - `LocalContentColor`
   - `LocalTextStyle`
   - `LocalDensity`

2. **布局相关**：
   - `LocalLayoutDirection`
   - `LocalViewConfiguration`

3. **平台相关**：
   - `LocalContext` (Android)
   - `LocalConfiguration`

### 常见使用场景

| 场景 | 示例 | 说明 |
| --- | --- | --- |
| **导航对象** | `LocalAppNavigation` | 全局导航服务 |
| **主题配置** | `LocalTheme` | 颜色、字体等主题信息 |
| **用户信息** | `LocalUser` | 当前登录用户 |
| **权限状态** | `LocalPermissions` | 应用权限状态 |
| **日志服务** | `LocalLogger` | 日志记录器 |
| **网络客户端** | `LocalHttpClient` | HTTP 客户端 |
| **数据库** | `LocalDatabase` | 数据库实例 |

---

## 最佳实践场景

### ✅ 适合使用的场景

1. **全局服务对象**
   - 导航、日志、网络客户端等
   - 生命周期与应用一致
   - 多个组件需要访问

2. **主题和配置**
   - 颜色、字体、密度等
   - 需要在整个应用中保持一致

3. **上下文信息**
   - 用户信息、权限、语言设置等
   - 需要全局访问但很少变化

4. **避免深层传递**
   - 参数需要传递超过 3-4 层
   - 中间组件不需要使用该参数

### ❌ 不适合使用的场景

1. **频繁变化的状态**
   - 应该使用 `State` 或 `ViewModel`
   - 例如：表单输入、计数器等

2. **局部状态**
   - 父子组件间的简单状态传递
   - 直接传递参数更清晰

3. **需要类型安全**
   - 接口比 CompositionLocal 更明确
   - 编译时就能发现错误

---

## 潜在问题和注意事项

### 1. 未提供值的错误

如果忘记使用 `CompositionLocalProvider` 提供值，访问时会抛出错误：

```kotlin
// ❌ 错误：未提供值
@Composable
fun SomeScreen() {
    val navigation = LocalAppNavigation.current // 抛出异常
}

// ✅ 正确：先提供值
@Composable
fun AppRoot() {
    val navigation = rememberAppNavigation()
    CompositionLocalProvider(LocalAppNavigation provides navigation) {
        SomeScreen() // 现在可以安全访问
    }
}
```

### 2. 作用域问题

`CompositionLocal` 只在提供它的 `CompositionLocalProvider` 作用域内有效：

```kotlin
@Composable
fun AppRoot() {
    CompositionLocalProvider(LocalAppNavigation provides navigation1) {
        ScreenA() // 访问 navigation1
        
        CompositionLocalProvider(LocalAppNavigation provides navigation2) {
            ScreenB() // 访问 navigation2（覆盖了 navigation1）
        }
        
        ScreenC() // 访问 navigation1
    }
}
```

### 3. 性能考虑

- 使用 `staticCompositionLocalOf` 时，值变化不会触发重组
- 如果值会变化且需要触发重组，使用 `compositionLocalOf`
- 避免在 `CompositionLocal` 中存储大量数据

---

## 建议改进

### 1. 添加文档注释

```kotlin
/**
 * 应用导航的 CompositionLocal
 * 
 * 在 [AppRoot] 中通过 [CompositionLocalProvider] 提供导航实例。
 * 子组件通过 [LocalAppNavigation.current] 访问导航功能。
 * 
 * 使用 [staticCompositionLocalOf] 因为导航实例在应用生命周期内保持不变，
 * 状态变化通过 [IAppNavigation.state] 管理。
 * 
 * @see AppRoot
 * @see IAppNavigation
 */
val LocalAppNavigation = staticCompositionLocalOf<IAppNavigation> {
    error("No AppNavigation provided. Make sure to wrap your app with CompositionLocalProvider(LocalAppNavigation provides navigation)")
}
```

### 2. 考虑使用 compositionLocalOf（如果需要）

如果导航状态变化需要触发重组，可以考虑：

```kotlin
val LocalAppNavigation = compositionLocalOf<IAppNavigation> {
    error("No AppNavigation provided")
}
```

但需要评估性能影响。

### 3. 提供便捷访问函数

可以添加扩展函数简化访问：

```kotlin
/**
 * 获取当前导航实例的便捷方法
 */
@Composable
fun appNavigation(): IAppNavigation = LocalAppNavigation.current
```

使用：
```kotlin
val navigation = appNavigation() // 比 LocalAppNavigation.current 更简洁
```

---

## 总结

1. **CompositionLocal 是 Compose 的标准依赖注入机制**，适合传递全局服务、主题配置等
2. **本项目中的用法是正确的**，使用 `staticCompositionLocalOf` 符合导航对象的特点
3. **这是常规用法**，官方库和社区都广泛使用
4. **适合场景**：全局服务、主题配置、避免深层参数传递
5. **注意事项**：确保在组件树中提供值，理解作用域，选择合适的类型

通过合理使用 `CompositionLocal`，可以让代码更简洁、解耦更好、维护更容易。

