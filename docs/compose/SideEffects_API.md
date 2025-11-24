# Compose Side Effects 决策指南

本文档旨在帮助你快速选择正确的 Compose 副作用 API。不要死记硬背 API，而是根据**你的需求**来选择。

---

## 🚀 快速决策树 (Decision Tree)

**Q1: 你想做什么？**

*   **A. 我想在某个状态变化时，执行一段代码（非 UI 渲染）。**
    *   *是异步操作吗？（如网络请求、倒计时）*
        *   ✅ 是 -> **`LaunchedEffect`**
        *   ❌ 否 -> **`SideEffect`** (极少用) 或直接写在 `LaunchedEffect` 里
    *   *需要清理资源吗？（如注册监听器、绑定 Service）*
        *   ✅ 是 -> **`DisposableEffect`**

*   **B. 我想把非 Compose 的状态（如 Flow, LiveData）转为 Compose State。**
    *   *是 Flow/StateFlow 吗？*
        *   ✅ 是 -> **`collectAsState()`** (或 `collectAsStateWithLifecycle`)
    *   *是其他回调/监听器吗？*
        *   ✅ 是 -> **`produceState`**

*   **C. 我想在用户点击按钮（回调）时启动协程。**
    *   ✅ 是 -> **`rememberCoroutineScope`**

*   **D. 我想根据其他 State 计算出一个新 State，且计算很耗时。**
    *   ✅ 是 -> **`derivedStateOf`**

---

## 1. 场景一：进入页面或状态变化时执行 (One-off Actions)

### ✅ `LaunchedEffect`
**场景**：我想在进入页面时请求数据，或者在 `userId` 变化时重新请求。
**特点**：自动在协程中执行，离开页面或 Key 变化时自动取消旧协程。

```kotlin
// 场景：进入页面加载数据
LaunchedEffect(Unit) {
    viewModel.refresh()
}

// 场景：userId 变化时重新搜索
LaunchedEffect(userId) {
    viewModel.search(userId) // 如果 userId 变了，上一次请求会被取消
}
```

### ❌ 避坑指南
*   **不要** 在 `LaunchedEffect` 里写死循环而不挂起（会导致 UI 卡死）。
*   **不要** 在 `LaunchedEffect(Unit)` 里监听 Flow（应该用 `collectAsState`）。

---

## 2. 场景二：需要清理的副作用 (Cleanup Required)

### ✅ `DisposableEffect`
**场景**：我想注册一个广播接收器、绑定一个 Service、或者开始一个需要手动停止的 Timer。
**特点**：必须提供 `onDispose` 代码块，Compose 会在离开页面时自动调用它。

```kotlin
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event -> ... }
    lifecycleOwner.lifecycle.addObserver(observer)

    // 必须有！离开页面时清理
    onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
    }
}
```

### ❌ 避坑指南
*   **不要** 忘记 `onDispose`，否则会内存泄漏。
*   **不要** 在 `onDispose` 里做耗时操作。

---

## 3. 场景三：手动触发协程 (User Actions)

### ✅ `rememberCoroutineScope`
**场景**：我想在 `onClick` 点击事件里启动一个协程（比如弹出一个 Snackbar，或者保存数据）。
**特点**：它给你一个 `Scope`，让你能在非 Composable 环境（如回调函数）里启动协程。

```kotlin
val scope = rememberCoroutineScope()

Button(onClick = {
    // 在点击回调里启动协程
    scope.launch {
        snackbarHostState.showSnackbar("保存成功")
    }
}) { Text("保存") }
```

### ❌ 避坑指南
*   **不要** 把 `scope` 传给 ViewModel（ViewModel 有自己的 `viewModelScope`）。
*   **不要** 用它来替代 `LaunchedEffect` 做页面初始化。

---

## 4. 场景四：状态转换与计算 (State Transformation)

### ✅ `derivedStateOf`
**场景**：我的状态 A 变化非常频繁（如滚动距离），但我只关心它是否超过某个阈值（状态 B）。
**特点**：只有当计算结果真正变化时，才会触发下游重组。

```kotlin
val listState = rememberLazyListState()

// 只有当 showButton 从 true 变 false (或反之) 时，才会触发重组
val showButton by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 0 }
}
```

### ✅ `produceState`
**场景**：我想把一个非 Compose 的数据源（如 Socket 连接、定位回调）转为 State。
**特点**：它是 `LaunchedEffect` + `State` 的语法糖。

```kotlin
@Composable
fun loadNetworkImage(url: String): State<Result<Image>> {
    // 创建一个 State，初始值为 Loading
    return produceState(initialValue = Result.Loading, url) {
        val image = imageLoader.load(url) // 挂起函数
        value = Result.Success(image) // 更新 State
    }
}
```

---

## 5. 总结对照表

| API | 核心用途 | 关键字 | 自动取消/清理? |
| :--- | :--- | :--- | :--- |
| **LaunchedEffect** | 异步操作、网络请求 | `suspend` | ✅ (协程取消) |
| **DisposableEffect** | 绑定/解绑、注册/注销 | `onDispose` | ✅ (执行 onDispose) |
| **rememberCoroutineScope** | 点击事件、回调中启动协程 | `launch` | ✅ (页面销毁时取消) |
| **derivedStateOf** | 过滤高频状态变化 | `State` | N/A |
| **SideEffect** | 每次重组都执行 (极少用) | 非 Compose 状态同步 | ❌ |

---

## 6. ViewModel 生命周期补充说明

在 Compose 中使用 `koinViewModel()` 或 `viewModel()`：

*   **创建**：当 Composable **首次** 进入组合（Composition）时创建。
*   **存活**：只要该 Composable 所在的 **Navigation Route (BackStackEntry)** 还在堆栈中，ViewModel 就一直存活。
*   **重组**：Composable 函数因为状态变化重新执行（Recomposition）时，**不会** 重新创建 ViewModel，而是返回同一个实例。
*   **销毁**：当 Route 从堆栈中弹出（pop）时，ViewModel 触发 `onCleared()` 并销毁。

**结论**：在 `HomeRoute` 参数中声明 `viewModel: VM = koinViewModel()` 是安全的，不会导致重复创建。


