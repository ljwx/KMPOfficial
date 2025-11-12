# Compose 副作用 API 详解

本文档详细解释 Compose 中的副作用（Side Effect）概念以及常用的副作用 API，帮助你理解如何在 Compose 中安全地执行副作用操作。

---

## 1. 什么是副作用（Side Effect）？

**副作用**是指函数执行时除了返回值外，还会影响外部状态或执行外部操作。

### 纯函数 vs 副作用

```kotlin
// ✅ 纯函数：只计算，不改变外部状态
fun add(a: Int, b: Int): Int {
    return a + b  // 只返回结果，不影响外部
}

// ❌ 副作用：改变外部状态或执行外部操作
fun updateDatabase() {
    database.save()  // 改变数据库状态
}

fun startTimer() {
    timer.start()  // 启动外部定时器
}

fun subscribeToEvents() {
    eventBus.subscribe { ... }  // 注册监听器
}
```

---

## 2. Compose 中的副作用问题

在 Compose 中，Composable 函数应该是**纯函数**，但有时需要执行副作用操作。

### 问题：重组导致重复执行

```kotlin
@Composable
fun MyScreen() {
    var count by remember { mutableStateOf(0) }
    
    // ❌ 问题：每次重组都会执行
    println("执行了 $count 次")  // 每次重组都打印
    startTimer()  // 每次重组都启动定时器（内存泄漏！）
    
    Button(onClick = { count++ }) {
        Text("点击: $count")
    }
}
```

**问题**：每次状态变化都会触发重组，导致副作用重复执行，可能造成：
- 资源泄漏（重复创建订阅、定时器）
- 性能问题（重复执行昂贵操作）
- 逻辑错误（重复发送请求）

### 解决方案：使用副作用 API

副作用 API 确保副作用在**正确的时机**执行，且**不会重复执行**。

---

## 3. 常用的副作用 API

### 3.1 `remember` - 记住值

**作用**：在首次组合时创建值，重组时复用同一个实例。

```kotlin
@Composable
fun MyScreen() {
    // 只在首次组合时创建，重组时复用
    val expensiveObject = remember { 
        ExpensiveObject()  // 只创建一次
    }
    
    // 带 key 的 remember：key 变化时重新计算
    val computedValue = remember(key1, key2) {
        computeValue(key1, key2)
    }
}
```

**特点**：
- 首次组合时执行
- 重组时复用（除非 key 变化）
- 组合结束时自动清理

**适用场景**：
- 记住计算结果
- 记住对象实例
- 记住状态

---

### 3.2 `DisposableEffect` - 可清理的副作用

**作用**：执行需要清理的副作用（订阅、监听器等）。

```kotlin
@Composable
fun MyScreen() {
    DisposableEffect(Unit) {
        // 组合时执行
        val subscription = eventBus.subscribe { ... }
        val timer = Timer().apply { start() }
        
        // 离开组合时自动执行清理
        onDispose {
            subscription.unsubscribe()
            timer.stop()
        }
    }
}
```

**特点**：
- 在组合时执行一次
- 在离开组合时自动执行 `onDispose`
- key 变化时：先执行旧的 `onDispose`，再执行新的 lambda

**适用场景**：
- 订阅事件总线
- 注册监听器
- 启动定时器
- 管理资源（文件、网络连接）

**实际应用示例**：

```kotlin
@Composable
fun rememberAppNavigation(): AppNavigation {
    val lifecycle = remember { LifecycleRegistry() }
    
    DisposableEffect(Unit) {
        lifecycle.resume()  // 激活生命周期
        onDispose {
            lifecycle.destroy()  // 清理生命周期
        }
    }
    
    return remember {
        AppNavigation(DefaultComponentContext(lifecycle))
    }
}
```

---

### 3.3 `LaunchedEffect` - 协程副作用

**作用**：在协程中执行副作用，适合异步操作。

```kotlin
@Composable
fun MyScreen(userId: String) {
    var data by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    
    // 当 userId 变化时，重新执行
    LaunchedEffect(userId) {
        loading = true
        try {
            data = fetchUserData(userId)  // 异步获取数据
        } finally {
            loading = false
        }
    }
    
    if (loading) {
        CircularProgressIndicator()
    } else {
        Text(data ?: "无数据")
    }
}
```

**特点**：
- 在协程中执行
- key 变化时自动取消旧协程，启动新协程
- 组合结束时自动取消协程

**适用场景**：
- 网络请求
- 数据库查询
- 异步初始化
- 动画控制

**示例：一次性执行**

```kotlin
@Composable
fun MyScreen() {
    LaunchedEffect(Unit) {  // Unit 作为 key，只执行一次
        // 初始化操作
        initializeApp()
    }
}
```

---

### 3.4 `SideEffect` - 每次重组都执行

**作用**：在每次重组时执行副作用（但 Compose 会优化）。

```kotlin
@Composable
fun MyScreen(title: String) {
    SideEffect {
        // 每次重组都可能执行
        document.title = title  // 更新网页标题
        analytics.track("screen_view", title)
    }
    
    Text(title)
}
```

**特点**：
- 每次重组都可能执行
- 没有清理机制
- Compose 会优化执行时机

**适用场景**：
- 更新外部状态（如网页标题）
- 发送分析事件
- 更新系统 UI（状态栏颜色）

**注意**：谨慎使用，确保副作用是轻量级的。

---

### 3.5 `rememberCoroutineScope` - 记住协程作用域

**作用**：记住一个协程作用域，用于事件驱动的协程启动。

```kotlin
@Composable
fun MyScreen() {
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<String?>(null) }
    
    Button(onClick = {
        // 在点击事件中启动协程
        scope.launch {
            data = fetchData()
        }
    }) {
        Text("加载数据")
    }
    
    Text(data ?: "未加载")
}
```

**特点**：
- 首次组合时创建
- 组合结束时自动取消所有协程
- 适合事件驱动的异步操作

**适用场景**：
- 按钮点击后的异步操作
- 用户交互触发的网络请求
- 动画控制

---

## 4. 副作用 API 对比表

| API | 执行时机 | 清理机制 | 协程支持 | 适用场景 |
|-----|---------|---------|---------|---------|
| `remember` | 首次组合（key 变化时重新计算） | 自动（组合结束时） | ❌ | 记住值/对象 |
| `DisposableEffect` | 组合时（key 变化时重新执行） | `onDispose` | ❌ | 订阅、监听器、资源 |
| `LaunchedEffect` | key 变化时 | 自动取消协程 | ✅ | 异步操作、网络请求 |
| `SideEffect` | 每次重组 | 无 | ❌ | 更新外部状态 |
| `rememberCoroutineScope` | 首次组合 | 自动取消协程 | ✅ | 事件驱动的协程 |

---

## 5. 副作用 API 的执行规则

### 5.1 Key 参数的作用

副作用 API 通常接受一个或多个 key 参数，用于控制何时重新执行：

```kotlin
// key = Unit，永远不变，只执行一次
DisposableEffect(Unit) { ... }

// key = userId，userId 变化时重新执行
LaunchedEffect(userId) {
    loadUserData(userId)
}

// 多个 key
LaunchedEffect(userId, filterType) {
    loadFilteredData(userId, filterType)
}

// 没有 key（仅 SideEffect）
SideEffect { ... }  // 每次重组都执行
```

### 5.2 执行流程

#### `DisposableEffect` 的执行流程：

```
首次组合：
├─ 执行 lambda
└─ 注册 onDispose 回调

重组（key 不变）：
└─ 不执行任何操作

重组（key 变化）：
├─ 执行旧的 onDispose
└─ 执行新的 lambda，注册新的 onDispose

离开组合：
└─ 执行 onDispose
```

#### `LaunchedEffect` 的执行流程：

```
首次组合：
└─ 启动协程

重组（key 不变）：
└─ 不执行任何操作

重组（key 变化）：
├─ 取消旧协程
└─ 启动新协程

离开组合：
└─ 取消协程
```

---

## 6. 常见错误和正确做法

### 错误示例 1：直接在 Composable 中执行副作用

```kotlin
@Composable
fun BadExample() {
    var count by remember { mutableStateOf(0) }
    
    // ❌ 错误：每次重组都执行
    val timer = Timer()
    timer.start()
    
    // ❌ 错误：每次重组都订阅
    eventBus.subscribe { ... }
    
    Button(onClick = { count++ }) {
        Text("Count: $count")
    }
}
```

**问题**：
- 每次重组都创建新的 Timer 和订阅
- 旧的 Timer 和订阅没有被清理
- 导致内存泄漏

### 正确示例 1：使用 `DisposableEffect`

```kotlin
@Composable
fun GoodExample() {
    var count by remember { mutableStateOf(0) }
    
    // ✅ 正确：使用 DisposableEffect
    DisposableEffect(Unit) {
        val timer = Timer()
        timer.start()
        onDispose {
            timer.stop()  // 清理
        }
    }
    
    // ✅ 正确：使用 DisposableEffect
    DisposableEffect(Unit) {
        val subscription = eventBus.subscribe { ... }
        onDispose {
            subscription.unsubscribe()  // 清理
        }
    }
    
    Button(onClick = { count++ }) {
        Text("Count: $count")
    }
}
```

### 错误示例 2：在 Composable 中直接启动协程

```kotlin
@Composable
fun BadExample(userId: String) {
    var data by remember { mutableStateOf<String?>(null) }
    
    // ❌ 错误：每次重组都启动协程
    GlobalScope.launch {
        data = fetchData(userId)
    }
    
    Text(data ?: "加载中...")
}
```

**问题**：
- 每次重组都启动新协程
- 旧协程没有被取消
- 可能导致竞态条件

### 正确示例 2：使用 `LaunchedEffect`

```kotlin
@Composable
fun GoodExample(userId: String) {
    var data by remember { mutableStateOf<String?>(null) }
    
    // ✅ 正确：使用 LaunchedEffect
    LaunchedEffect(userId) {
        data = fetchData(userId)  // userId 变化时自动重新执行
    }
    
    Text(data ?: "加载中...")
}
```

---

## 7. 实际应用场景

### 场景 1：管理生命周期

```kotlin
@Composable
fun rememberAppNavigation(): AppNavigation {
    val lifecycle = remember { LifecycleRegistry() }
    
    DisposableEffect(Unit) {
        lifecycle.resume()  // 激活生命周期
        onDispose {
            lifecycle.destroy()  // 清理生命周期
        }
    }
    
    return remember {
        AppNavigation(DefaultComponentContext(lifecycle))
    }
}
```

### 场景 2：订阅数据流

```kotlin
@Composable
fun DataScreen() {
    var data by remember { mutableStateOf<Data?>(null) }
    
    DisposableEffect(Unit) {
        val subscription = dataFlow.collect { newData ->
            data = newData
        }
        onDispose {
            subscription.cancel()
        }
    }
    
    Text(data?.toString() ?: "无数据")
}
```

### 场景 3：网络请求

```kotlin
@Composable
fun UserProfile(userId: String) {
    var user by remember { mutableStateOf<User?>(null) }
    var loading by remember { mutableStateOf(false) }
    
    LaunchedEffect(userId) {
        loading = true
        try {
            user = api.getUser(userId)
        } catch (e: Exception) {
            // 处理错误
        } finally {
            loading = false
        }
    }
    
    if (loading) {
        CircularProgressIndicator()
    } else {
        Text(user?.name ?: "用户不存在")
    }
}
```

### 场景 4：事件驱动的操作

```kotlin
@Composable
fun SearchScreen() {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Result>>(emptyList()) }
    
    Button(onClick = {
        scope.launch {
            results = searchApi.search(query)
        }
    }) {
        Text("搜索")
    }
    
    LazyColumn {
        items(results) { result ->
            Text(result.title)
        }
    }
}
```

---

## 8. 最佳实践

### 8.1 选择合适的 API

- **需要清理资源** → 使用 `DisposableEffect`
- **异步操作** → 使用 `LaunchedEffect`
- **记住值** → 使用 `remember`
- **事件驱动** → 使用 `rememberCoroutineScope`
- **更新外部状态** → 使用 `SideEffect`（谨慎使用）

### 8.2 正确使用 Key

```kotlin
// ✅ 好：明确指定 key
LaunchedEffect(userId) { ... }

// ❌ 不好：没有 key（每次都执行）
LaunchedEffect { ... }

// ✅ 好：一次性执行使用 Unit
LaunchedEffect(Unit) { ... }
```

### 8.3 总是清理资源

```kotlin
// ✅ 好：总是提供清理逻辑
DisposableEffect(Unit) {
    val resource = acquireResource()
    onDispose {
        releaseResource(resource)
    }
}

// ❌ 不好：忘记清理
DisposableEffect(Unit) {
    acquireResource()  // 资源泄漏！
}
```

### 8.4 避免在副作用中直接修改状态

```kotlin
// ❌ 不好：在副作用中直接修改状态
LaunchedEffect(Unit) {
    count++  // 可能导致无限重组
}

// ✅ 好：使用回调或事件
LaunchedEffect(Unit) {
    eventBus.collect { event ->
        onEvent(event)  // 通过回调处理
    }
}
```

---

## 9. 总结

Compose 的副作用 API 提供了安全、可控的方式来执行副作用操作：

1. **`remember`**：记住值，避免重复计算
2. **`DisposableEffect`**：管理需要清理的资源
3. **`LaunchedEffect`**：执行异步操作
4. **`SideEffect`**：更新外部状态
5. **`rememberCoroutineScope`**：事件驱动的协程

**核心原则**：
- 副作用应该在正确的时机执行
- 副作用应该可以被清理
- 副作用不应该导致无限重组

通过正确使用这些 API，可以确保 Compose 应用的性能和稳定性。

---

## 10. 参考资源

- [Compose 官方文档 - 副作用](https://developer.android.com/jetpack/compose/side-effects)
- [Decompose 文档 - 生命周期管理](https://arkivanov.github.io/Decompose/lifecycle/overview/)

