# Component 协程作用域管理指南

## 概述

在 Decompose Component 中管理协程作用域是确保应用稳定性和避免内存泄漏的关键。本文档介绍如何使用 `essenty-lifecycle-coroutines` 库来正确管理协程生命周期。

## 依赖配置

在 `build.gradle.kts` 中添加依赖：

```kotlin
implementation("com.arkivanov.essenty:lifecycle-coroutines:2.5.0")
```

**版本说明：**
- 版本需与 `decompose` 使用的 `essenty` 版本匹配
- Decompose 3.4.0 使用 essenty 2.5.0，因此 `lifecycle-coroutines` 也应使用 2.5.0

## API 说明

### `coroutineScope()` 扩展函数

`coroutineScope()` 是 `LifecycleOwner` 的扩展函数，用于创建与生命周期绑定的协程作用域。

**函数签名：**
```kotlin
fun LifecycleOwner.coroutineScope(
    context: CoroutineContext = Dispatchers.Main.immediateOrFallback,
): CoroutineScope
```

**特点：**
- 返回的 `CoroutineScope` 会在 `Lifecycle` 销毁时自动取消
- 默认使用 `Dispatchers.Main.immediateOrFallback` 作为协程上下文
- `ComponentContext` 实现了 `LifecycleOwner` 接口，可以直接调用

## 标准用法

### 基本用法

```kotlin
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.launch

class DefaultHomeComponent(
    componentContext: ComponentContext
) : HomeComponent, ComponentContext by componentContext {
    
    // 创建与生命周期绑定的协程作用域
    private val componentScope = coroutineScope()
    
    init {
        componentScope.launch {
            // 协程代码
        }
    }
}
```

### 自定义协程上下文

如果需要使用自定义的协程上下文：

```kotlin
import kotlinx.coroutines.Dispatchers

private val componentScope = coroutineScope(Dispatchers.Default)
```

### 在方法中使用

```kotlin
override fun loadData() {
    componentScope.launch {
        // 异步加载数据
        val data = repository.fetchData()
        _state.value = data
    }
}
```

## 工作原理

1. **创建阶段**：调用 `coroutineScope()` 创建一个新的 `CoroutineScope`
2. **绑定生命周期**：内部调用 `lifecycle.doOnDestroy { cancel() }` 绑定生命周期
3. **自动取消**：当 Component 销毁时，`Lifecycle` 触发 `onDestroy` 事件，协程作用域自动取消

## 与 Android ViewModel 对比

| 特性 | Android ViewModel | Decompose Component |
|------|------------------|---------------------|
| 协程作用域 | `viewModelScope` | `coroutineScope()` |
| 生命周期绑定 | 自动 | 自动 |
| 平台支持 | Android only | Multiplatform |
| 使用方式 | `viewModelScope.launch { }` | `componentScope.launch { }` |

## 最佳实践

### ✅ 推荐做法

1. **在 Component 中创建作用域**
   ```kotlin
   private val componentScope = coroutineScope()
   ```

2. **在 init 中启动初始化协程**
   ```kotlin
   init {
       componentScope.launch {
           loadInitialData()
       }
   }
   ```

3. **在方法中使用作用域**
   ```kotlin
   fun refreshData() {
       componentScope.launch {
           // 刷新数据
       }
   }
   ```

### ❌ 避免的做法

1. **不要在每次调用时创建新作用域**
   ```kotlin
   // ❌ 错误：每次都创建新作用域
   fun loadData() {
       coroutineScope().launch { ... }
   }
   ```

2. **不要手动管理生命周期**
   ```kotlin
   // ❌ 错误：手动管理，容易出错
   private val scope = CoroutineScope(SupervisorJob())
   init {
       lifecycle.doOnDestroy { scope.cancel() }
   }
   ```

## 常见问题

### Q: 为什么使用 `coroutineScope()` 而不是手动创建？

**A:** `coroutineScope()` 是官方推荐的标准做法，具有以下优势：
- 代码更简洁
- 自动处理生命周期绑定
- 减少出错可能性
- 符合 Decompose 最佳实践

### Q: 可以在 Compose UI 中使用吗？

**A:** 不可以。`coroutineScope()` 是为 Component 设计的。在 Compose UI 中应使用 `rememberCoroutineScope()`。

### Q: 版本不匹配会怎样？

**A:** 可能导致编译错误或运行时问题。确保 `lifecycle-coroutines` 版本与 `decompose` 使用的 `essenty` 版本匹配。

## 参考资料

- [Essenty GitHub](https://github.com/arkivanov/Essenty)
- [Decompose 官方文档](https://arkivanov.github.io/Decompose/)
- [Kotlin 协程文档](https://kotlinlang.org/docs/coroutines-guide.html)


