# MultiStateLayout 使用指南

`MultiStateLayout` 是一个功能强大且高度可定制的 Jetpack Compose 组件，旨在简化在 KMP (Kotlin Multiplatform) 项目中处理不同UI状态的复杂性。它提供了一个统一的布局，可以根据当前数据状态显示内容、加载指示器、空状态、错误消息、离线提示或自定义扩展视图。

## 特性

- **状态驱动**: 完全由一个密封接口 `MultiStateLayoutState` 控制，确保UI与状态的强一致性。
- **KMP 兼容**: 位于 `commonMain` 中，可在所有支持的平台（Android, iOS, Desktop, Web）上无缝工作。
- **高度可定制**: 除了内容视图，所有其他状态（加载、空、错误、离线、扩展）的UI都可以通过槽位 API (Slot API) 进行完全替换。
- **内置默认实现**: 为常见状态提供了简洁的默认UI，实现开箱即用。
- **内置重试逻辑**: `Error` 和 `Offline` 状态默认包含重试按钮，并将重试事件回调给调用方。

## `MultiStateLayoutState` 状态模型

UI的显示由 `MultiStateLayoutState` 密封接口驱动，它包含以下几种状态：

- `Content`: 显示主要内容。
- `Loading`: 显示加载指示器，通常在数据获取期间使用。
- `Empty`: 当没有数据可显示时使用。
- `Error(throwable: Throwable)`: 当发生错误时使用，并携带了异常信息。
- `Offline`: 当网络连接不可用时使用。
- `Extension`: 用于不属于以上任何一种情况的自定义状态。

## 如何使用

集成 `MultiStateLayout` 非常简单，通常分为两步：在 ViewModel 中管理状态，以及在 Composable 中使用组件。

### 1. 在 ViewModel 中管理状态

在您的 ViewModel (或任何业务逻辑处理器) 中，维护一个 `StateFlow` 来持有当前的 `MultiStateLayoutState`。根据业务逻辑（如API请求结果）来更新此状态。

```kotlin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.ui.component.MultiStateLayoutState

class MyScreenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<MultiStateLayoutState>(MultiStateLayoutState.Loading)
    val uiState = _uiState.asStateFlow()

    // 假设这是你的内容数据
    private val _data = MutableStateFlow<List<String>>(emptyList())
    val data = _data.asStateFlow()

    fun fetchData() {
        viewModelScope.launch {
            _uiState.value = MultiStateLayoutState.Loading
            try {
                // 模拟网络请求
                val result = repository.fetchSomeData() 
                if (result.isEmpty()) {
                    _uiState.value = MultiStateLayoutState.Empty
                } else {
                    _data.value = result
                    _uiState.value = MultiStateLayoutState.Content
                }
            } catch (e: NoConnectivityException) { // 自定义网络异常
                _uiState.value = MultiStateLayoutState.Offline
            } catch (e: Exception) {
                _uiState.value = MultiStateLayoutState.Error(e)
            }
        }
    }
}
```

### 2. 在 Composable 中使用 `MultiStateLayout`

在你的UI层，观察 ViewModel 中的状态，并将其传递给 `MultiStateLayout`。

```kotlin
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun MyScreen(viewModel: MyScreenViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val data by viewModel.data.collectAsState()

    MultiStateLayout(
        state = uiState,
        onRetry = { viewModel.fetchData() }
    ) {
        // `content` 槽位: 只有在 state 是 Content 时才会显示
        LazyColumn {
            items(data) { item ->
                Text(text = "Item: $item")
            }
        }
    }
}
```

## 自定义状态视图

`MultiStateLayout` 的强大之处在于其可定制性。你可以通过向 `MultiStateLayout` 传入你自己的 Composable 来替换任何状态的默认视图。

```kotlin
@Composable
fun MyCustomizedScreen(viewModel: MyScreenViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    MultiStateLayout(
        state = uiState,
        onRetry = { viewModel.fetchData() },
        
        // 自定义加载视图
        loadingView = { MyCustomLoadingSpinner() },
        
        // 自定义空状态视图
        emptyView = { MyCustomEmptyView("暂无数据，请稍后再试") },
        
        // 自定义错误视图
        errorView = { throwable, onRetry -> 
            MyCustomErrorView(
                errorMessage = "发生错误: ${throwable.message}",
                onRetryClick = onRetry
            )
        }
    ) {
        // 你的内容视图...
    }
}
```

通过这种方式，你可以在整个应用中复用 `MultiStateLayout` 的逻辑，同时为不同页面定制独特的外观和感觉。
