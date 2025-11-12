## 导航架构迁移策略

本文档分析当前导航架构的可迁移性，以及如何设计才能最小化迁移到官方导航时的改动。

---

## 当前架构分析

### 架构层次

```
业务层（几乎不需要改动）
├── RouteHandler（ScreenRouteHandler）
├── 页面组件（DetailScreen, HomeScreen 等）
└── 业务逻辑

抽象层（保持不变）
├── IAppNavigation 接口
├── ScreenRouteHandler 接口
├── RouterRegistry
└── ScreenRouterData

实现层（需要替换）
└── AppNavigation（使用 Decompose）
```

---

## 迁移到官方导航的改动评估

### ✅ 不需要改动的代码（约 90%）

#### 1. **业务层代码**

```kotlin
// ✅ RouteHandler - 完全不需要改动
object ProductDetail : ScreenRouteHandler {
    @Composable
    override fun Content(router: ScreenRouterData, modifier: Modifier) {
        DetailScreen(modifier = modifier)
    }
}

// ✅ 页面组件 - 完全不需要改动
@Composable
fun DetailScreen(modifier: Modifier = Modifier) {
    val navigation = LocalAppNavigation.current
    val product = navigation.state.activeScreen.getParamsSerialize<Product>()
    // ...
}

// ✅ 业务逻辑 - 完全不需要改动
navigation.openScreen(PRODUCT_DETAIL, product)
```

#### 2. **抽象层接口**

```kotlin
// ✅ IAppNavigation 接口 - 保持不变
interface IAppNavigation {
    val state: ScreenRouterState
    fun openScreen(...)
    fun navigateBack()
}

// ✅ ScreenRouteHandler 接口 - 保持不变
interface ScreenRouteHandler {
    val route: String
    @Composable fun Content(router: ScreenRouterData, modifier: Modifier)
}

// ✅ RouterRegistry - 保持不变
object RouterRegistry { ... }
```

#### 3. **路由注册**

```kotlin
// ✅ 路由注册 - 保持不变
fun initializeRoutes() {
    RouterRegistry.registerAll(
        HomeRouteHandler,
        ProductDetailRouteHandler,
    )
}
```

### ⚠️ 需要改动的代码（约 10%）

#### 1. **AppNavigation 实现类**

```kotlin
// ⚠️ 需要替换：从 Decompose 改为官方导航
class AppNavigation(...) : IAppNavigation {
    // 当前：使用 Decompose
    private val navigation = StackNavigation<ScreenRouterData>()
    private val childStack = childStack(...)
    
    // 迁移后：使用官方导航
    // val navController = rememberNavController()
    // NavHost(navController, ...) { ... }
}
```

#### 2. **AppRoot 中的 Render 方法**

```kotlin
// ⚠️ 可能需要调整：Render 方法的实现
override fun Render(...) {
    // 当前：使用 Decompose 的 Children
    Children(stack = childStack, ...) { ... }
    
    // 迁移后：使用官方导航的 NavHost
    // NavHost(navController, ...) { ... }
}
```

#### 3. **ScreenRouterData（可能需要调整）**

```kotlin
// ⚠️ 可能需要调整：适配官方导航的参数传递方式
@Serializable
data class ScreenRouterData(...) {
    // 参数获取方法保持不变
    fun getParamsSerialize<T>(): T? { ... }
}
```

---

## 迁移策略

### 策略1：保持接口不变（推荐）

**核心思想**：只替换实现，不改变接口

```kotlin
// 接口保持不变
interface IAppNavigation {
    val state: ScreenRouterState
    fun openScreen(...)
    fun navigateBack()
}

// 实现类替换
// 当前：AppNavigation（Decompose）
// 迁移后：OfficialNavigation（官方导航）
class OfficialNavigation : IAppNavigation {
    // 使用官方导航 API 实现接口
}
```

**优势**：
- ✅ 业务代码完全不需要改动
- ✅ RouteHandler 不需要改动
- ✅ 页面组件不需要改动
- ✅ 只需要替换一个实现类

### 策略2：适配器模式

**核心思想**：创建一个适配器，将官方导航适配到现有接口

```kotlin
class OfficialNavigationAdapter : IAppNavigation {
    private val navController = rememberNavController()
    
    override fun openScreen(...) {
        // 将接口调用转换为官方导航调用
        navController.navigate(route) { ... }
    }
    
    override val state: ScreenRouterState
        get() = ScreenRouterState(
            canNavigateBack = navController.previousBackStackEntry != null,
            activeScreen = convertToScreenRouterData(navController.currentBackStackEntry)
        )
}
```

**优势**：
- ✅ 完全隔离底层实现
- ✅ 业务代码零改动
- ✅ 可以逐步迁移

---

## 改动量评估

### 改动量对比

| 组件 | 当前架构 | 迁移后 | 改动量 |
| --- | --- | --- | --- |
| **RouteHandler** | 不变 | 不变 | 0% |
| **页面组件** | 不变 | 不变 | 0% |
| **业务逻辑** | 不变 | 不变 | 0% |
| **IAppNavigation 接口** | 不变 | 不变 | 0% |
| **ScreenRouteHandler 接口** | 不变 | 不变 | 0% |
| **RouterRegistry** | 不变 | 不变 | 0% |
| **AppNavigation 实现** | Decompose | 官方导航 | 100% |
| **AppRoot** | 少量调整 | 少量调整 | ~20% |
| **ScreenRouterData** | 可能需要调整 | 可能需要调整 | ~10% |

**总体改动量：约 5-10%**

---

## 如何进一步降低迁移成本

### 1. 进一步抽象 Render 方法

当前 `Render` 方法依赖 Decompose 的 `Children`，可以进一步抽象：

```kotlin
interface IAppNavigation {
    // 更抽象的 Render 方法
    @Composable
    fun Render(
        modifier: Modifier,
        content: @Composable (ScreenRouterData) -> Unit
    )
}

// 实现类负责具体的渲染逻辑
class AppNavigation : IAppNavigation {
    override fun Render(...) {
        // Decompose 实现
        Children(...) { child ->
            content(child.instance)
        }
    }
}

// 迁移后
class OfficialNavigation : IAppNavigation {
    override fun Render(...) {
        // 官方导航实现
        NavHost(...) {
            composable(route) {
                content(getCurrentRouteData())
            }
        }
    }
}
```

### 2. 统一参数传递方式

保持 `ScreenRouterData` 和参数获取方法不变：

```kotlin
// 保持不变
data class ScreenRouterData(
    val router: String,
    val paramsJson: String? = null,
    val params: Map<String, String>? = null
) {
    fun getParamsSerialize<T>(): T? { ... }
    fun getParamsMap<T>(key: String, default: T): T { ... }
}
```

### 3. 使用工厂模式

```kotlin
// 创建导航实例的工厂
object NavigationFactory {
    @Composable
    fun createNavigation(): IAppNavigation {
        // 根据配置选择实现
        return when (NavigationConfig.provider) {
            NavigationProvider.DECOMPOSE -> rememberAppNavigation()
            NavigationProvider.OFFICIAL -> rememberOfficialNavigation()
        }
    }
}
```

---

## 迁移步骤（如果官方导航发布）

### 步骤1：创建新的实现类

```kotlin
// 创建官方导航的实现
class OfficialAppNavigation : IAppNavigation {
    // 实现接口方法
}
```

### 步骤2：替换创建方法

```kotlin
// 修改 AppRoot
@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    // 从 Decompose 改为官方导航
    val navigation = rememberOfficialNavigation()  // 替换这里
    CompositionLocalProvider(LocalAppNavigation provides navigation) {
        // 其他代码保持不变
    }
}
```

### 步骤3：调整 Render 实现

```kotlin
// 在 OfficialAppNavigation 中实现 Render
override fun Render(...) {
    // 使用官方导航的 NavHost
    NavHost(...) { ... }
}
```

### 步骤4：测试和验证

- ✅ 测试所有路由跳转
- ✅ 测试参数传递
- ✅ 测试返回功能
- ✅ 测试启动模式

---

## 当前架构的优势

### 1. **抽象层设计良好**

- `IAppNavigation` 接口完全抽象了导航功能
- 业务代码只依赖接口，不依赖具体实现

### 2. **路由注册机制**

- `RouterRegistry` 和 `ScreenRouteHandler` 完全独立
- 不依赖底层导航库

### 3. **参数传递抽象**

- `ScreenRouterData` 和参数获取方法独立
- 可以适配任何导航库的参数传递方式

### 4. **CompositionLocal 使用**

- 通过 `LocalAppNavigation` 访问导航
- 不依赖具体的导航实例类型

---

## 总结

### 改动量评估

| 评估项 | 结果 |
| --- | --- |
| **业务代码改动** | 0% - 完全不需要改动 |
| **RouteHandler 改动** | 0% - 完全不需要改动 |
| **接口改动** | 0% - 保持不变 |
| **实现层改动** | 100% - 需要替换实现类 |
| **总体改动量** | **约 5-10%** |

### 优势

1. ✅ **抽象层设计优秀**：接口完全抽象了导航功能
2. ✅ **业务代码隔离**：业务代码不依赖具体实现
3. ✅ **迁移成本低**：只需要替换实现类
4. ✅ **渐进式迁移**：可以逐步迁移，不影响现有功能

### 建议

1. **保持当前架构**：抽象层设计已经很好了
2. **如果官方导航发布**：
   - 创建新的实现类 `OfficialAppNavigation`
   - 实现 `IAppNavigation` 接口
   - 替换 `rememberAppNavigation()` 为 `rememberOfficialNavigation()`
   - 其他代码保持不变

**结论：当前架构设计良好，迁移到官方导航时改动很小（约 5-10%），主要是替换实现层，业务代码几乎不需要改动。**

