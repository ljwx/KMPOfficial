# PullRefresh 下拉刷新使用指南

`PullRefresh` 是一个跨平台的下拉刷新组件，API 设计与 Android Material 3 的 `pullRefresh` 保持一致，确保未来官方提供跨平台支持时可以无缝切换。

## 特性

- **API 兼容**: 与 Android Material 3 的 `pullRefresh` API 完全兼容
- **跨平台支持**: 支持 Android、iOS、Desktop、Web 等所有 Compose Multiplatform 平台
- **流畅动画**: 提供流畅的下拉和刷新动画
- **易于集成**: 简单的 API，易于集成到现有代码中

## API 概览

主要 API 包括：

1. **`PullRefreshState`**: 下拉刷新状态类
2. **`rememberPullRefreshState(refreshing, onRefresh, refreshThresholdDp, maxDragDistanceDp)`**: 创建并记住 PullRefreshState，支持自定义刷新阈值和最大下拉距离
3. **`Modifier.pullRefresh(state)`**: 下拉刷新修饰符
4. **`PullRefreshIndicator()`**: 下拉刷新指示器组件

## 基本使用

### 方式一：使用 PullRefreshBox（推荐）

`PullRefreshBox` 是一个便捷的容器组件，集成了下拉刷新功能：

```kotlin
@Composable
fun MyRefreshableScreen() {
    var refreshing by remember { mutableStateOf(false) }
    
    PullRefreshBox(
        refreshing = refreshing,
        onRefresh = {
            refreshing = true
            // 执行刷新逻辑
            viewModel.refreshData()
            refreshing = false
        }
    ) {
        LazyColumn {
            items(data) { item ->
                Text("Item: $item")
            }
        }
    }
}
```

### 方式二：手动组合（更灵活）

如果需要更多控制，可以手动组合使用：

```kotlin
@Composable
fun MyRefreshableScreen() {
    var refreshing by remember { mutableStateOf(false) }
    val state = rememberPullRefreshState(refreshing) {
        refreshing = true
        // 执行刷新逻辑
        viewModel.refreshData()
        refreshing = false
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(state)
    ) {
        LazyColumn {
            items(data) { item ->
                Text("Item: $item")
            }
        }
        
        PullRefreshIndicator(
            refreshing = refreshing,
            state = state,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
```

## 与 ViewModel 集成

```kotlin
class MyViewModel : ViewModel() {
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()
    
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // 执行刷新逻辑
                repository.refreshData()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}

@Composable
fun MyScreen(viewModel: MyViewModel) {
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    PullRefreshBox(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refresh() }
    ) {
        // 内容
    }
}
```

## 自定义指示器

可以自定义刷新指示器的外观。`indicator` lambda 接收 `PullRefreshState`，你可以利用它来控制自定义指示器的显示、位置和动画。

```kotlin
PullRefreshBox(
    refreshing = refreshing,
    onRefresh = { /* ... */ },
    indicator = { state ->
        // state.progress, state.isRefreshing, state.getIndicatorOffset() 都可以用来驱动动画
        val offset = with(LocalDensity.current) { state.getIndicatorOffset().toDp() }
        
        // 自定义指示器
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = offset) // 使用 state 提供的偏移量
        ) {
            if (refreshing || state.progress > 0f) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    // 根据刷新状态或下拉进度显示
                    progress = if (refreshing) null else state.progress
                )
            }
        }
    }
) {
    // 内容
}
```

## 与 MultiStateLayout 结合使用

下拉刷新可以与 `MultiStateLayout` 结合使用：

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    PullRefreshBox(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refresh() }
    ) {
        MultiStateLayout(
            state = uiState,
            onRetry = { viewModel.refresh() }
        ) {
            LazyColumn {
                items(data) { item ->
                    Text("Item: $item")
                }
            }
        }
    }
}
```

## API 兼容性说明

本组件的 API 设计与 Android Material 3 的 `pullRefresh` 保持一致：

- `PullRefreshState` 对应 `androidx.compose.material3.PullRefreshState`
- `rememberPullRefreshState()` 对应 `androidx.compose.material3.rememberPullRefreshState()`
- `Modifier.pullRefresh()` 对应 `androidx.compose.material3.pullRefresh()`
- `PullRefreshIndicator()` 对应 `androidx.compose.material3.PullRefreshIndicator()`

当 Android 官方提供跨平台的 `pullRefresh` 支持时，只需要：

1. 将导入从 `org.example.project.pullrefresh` 改为 `androidx.compose.material3`
2. 代码无需修改，即可无缝切换

## 注意事项

1. **刷新状态管理**: 确保在刷新完成后将 `refreshing` 设置为 `false`，否则指示器会一直显示
2. **异步操作**: `onRefresh` 回调中的异步操作应该使用协程处理
3. **性能**: 下拉刷新会触发重组，确保刷新逻辑不会阻塞 UI 线程

## 示例代码

完整示例请参考项目中的示例页面。

