# 下拉刷新组件使用指南

## 设计思路

下拉刷新组件采用**抽屉式设计**：

- **Indicator 位置**：初始隐藏在标题栏下方（-56dp），不占用布局空间
- **下拉效果**：Indicator 和内容同步向下移动，Indicator 从标题栏下被"拉出来"
- **刷新状态**：两者保持在下拉位置，Indicator 旋转显示刷新中
- **刷新结束**：两者同步 400ms 平滑上移，Indicator 被"推回"标题栏下

### 核心特性

1. **递增阻尼**：下拉距离越大，阻力越大（越拉越难）
2. **同步移动**：Indicator 和内容移动相同距离
3. **平滑动画**：所有状态变化都有平滑过渡
4. **不占空间**：Indicator 浮在上层，不影响内容布局

## 基本使用

### 方式一：PullRefreshBox（推荐）

```kotlin
@Composable
fun MyScreen() {
    var refreshing by remember { mutableStateOf(false) }
    
    PullRefreshBox(
        refreshing = refreshing,
        onRefresh = {
            refreshing = true
            // 执行刷新逻辑
            scope.launch {
                delay(2000)
                refreshing = false
            }
        }
    ) {
        LazyColumn {
            items(data) { item ->
                ItemCard(item)
            }
        }
    }
}
```

### 方式二：手动组合

```kotlin
@Composable
fun MyScreen() {
    var refreshing by remember { mutableStateOf(false) }
    val state = rememberPullRefreshState(refreshing) {
        refreshing = true
        // 刷新逻辑
        refreshing = false
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(state)
    ) {
        // 内容
        LazyColumn { /* ... */ }
        
        // Indicator
        PullRefreshIndicator(
            refreshing = refreshing,
            state = state,
            modifier = Modifier.offset(y = state.getIndicatorOffset().toDp() - 56.dp)
        )
    }
}
```

## API 说明

### PullRefreshBox

```kotlin
@Composable
fun PullRefreshBox(
    refreshing: Boolean,              // 是否正在刷新
    onRefresh: () -> Unit,            // 刷新回调
    modifier: Modifier = Modifier,    // 容器修饰符
    enableContentOffset: Boolean = true,  // 是否启用内容跟随
    indicator: @Composable (PullRefreshState) -> Unit = { ... },  // 自定义指示器
    content: @Composable () -> Unit   // 内容
)
```

**参数说明**：

- `refreshing`：刷新状态，刷新完成后必须设置为 `false`
- `onRefresh`：刷新回调，通常在这里执行网络请求
- `enableContentOffset`：是否让内容跟随下拉移动（默认 `true`）
- `indicator`：自定义指示器，默认使用 `PullRefreshIndicator`

### PullRefreshIndicator

```kotlin
@Composable
fun PullRefreshIndicator(
    refreshing: Boolean,
    state: PullRefreshState,
    modifier: Modifier = Modifier,
    scale: Boolean = true,           // 是否根据进度缩放
    contentColor: Color = MaterialTheme.colorScheme.primary
)
```

### rememberPullRefreshState

```kotlin
@Composable
fun rememberPullRefreshState(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    refreshThresholdDp: Dp = 60.dp,   // 刷新阈值
    maxDragDistanceDp: Dp = 120.dp   // 最大下拉距离
): PullRefreshState
```

**参数说明**：

- `refreshThresholdDp`：下拉超过此距离松手会触发刷新
- `maxDragDistanceDp`：最大可下拉距离

## 与 ViewModel 集成

```kotlin
class MyViewModel : ViewModel() {
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()
    
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.fetchData()
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

## 自定义配置

### 调整下拉距离

```kotlin
val state = rememberPullRefreshState(
    refreshing = refreshing,
    onRefresh = { /* ... */ },
    refreshThresholdDp = 80.dp,   // 提高刷新阈值
    maxDragDistanceDp = 150.dp   // 增加最大下拉距离
)
```

### 禁用内容跟随

```kotlin
PullRefreshBox(
    refreshing = refreshing,
    onRefresh = { /* ... */ },
    enableContentOffset = false  // 只有 Indicator 移动
) {
    // 内容
}
```

### 自定义指示器

```kotlin
PullRefreshBox(
    refreshing = refreshing,
    onRefresh = { /* ... */ },
    indicator = { state ->
        // 自定义指示器
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .offset(y = state.getIndicatorOffset().toDp() - 56.dp)
        ) {
            Text("自定义指示器")
        }
    }
) {
    // 内容
}
```

## 注意事项

1. **必须正确管理 refreshing 状态**：刷新完成后必须设置为 `false`
2. **使用协程处理异步操作**：`onRefresh` 中的异步操作应使用协程
3. **需要可滚动内容**：必须配合 `LazyColumn`、`LazyRow` 或 `Column.verticalScroll()` 使用
4. **刷新中不能再次下拉**：组件会自动阻止刷新中的下拉操作

## 动画参数

- **触发刷新**：200ms 缓动到刷新位置
- **未达阈值回弹**：250ms 平滑回弹
- **刷新结束**：400ms 柔和上移（使用贝塞尔曲线）

## 技术实现

- 使用 `NestedScrollConnection` 拦截滚动事件
- 递增阻尼算法：`dampingFactor = 0.5 * (1 - progress)^1.8`
- Indicator 初始位置：-56dp（被标题栏遮住）
- 内容与 Indicator 同步移动相同距离

