# 📋 LazyColumn + Paging 快速参考

## 🎯 核心概念(30秒理解)

### LazyColumn 的结构
```
┌─────────────────┐
│   数据区域       │ ← items() 显示数据
├─────────────────┤
│   底部状态区域   │ ← when(append) 显示加载状态
└─────────────────┘
```

### 底部 Loading 的三种状态

| 状态 | 显示什么 | 代码 |
|------|---------|------|
| `Loading` | 🔄 加载中... | `item { CircularProgressIndicator() }` |
| `Error` | ❌ 加载失败 [重试] | `item { ErrorView() }` |
| `NotLoading` | (不显示) | `Unit` |

---

## 💻 代码模板

### 最简版(5 行代码)
```kotlin
LazyColumn {
    items(count = pagingItems.itemCount) { index ->
        ItemView(pagingItems[index])
    }
    if (pagingItems.loadState.append is LoadState.Loading) {
        item { CircularProgressIndicator() }
    }
}
```

### 标准版(推荐)
```kotlin
LazyColumn {
    // 数据
    items(
        count = pagingItems.itemCount,
        key = { index -> pagingItems[index]?.id ?: index }
    ) { index ->
        pagingItems[index]?.let { ItemView(it) }
    }
    
    // 底部状态
    when (pagingItems.loadState.append) {
        is LoadState.Loading -> {
            item { LoadingIndicator() }
        }
        is LoadState.Error -> {
            item { ErrorView(onRetry = { pagingItems.retry() }) }
        }
        else -> Unit
    }
}
```

### 完整版(包含所有状态)
```kotlin
LazyColumn {
    // 1. 首次加载状态
    when (pagingItems.loadState.refresh) {
        is LoadState.Loading -> item { FullScreenLoading() }
        is LoadState.Error -> item { FullScreenError() }
        else -> Unit
    }
    
    // 2. 数据列表
    items(
        count = pagingItems.itemCount,
        key = { index -> pagingItems[index]?.id ?: index }
    ) { index ->
        pagingItems[index]?.let { ItemView(it) }
    }
    
    // 3. 底部加载状态
    when (val state = pagingItems.loadState.append) {
        is LoadState.Loading -> {
            item { BottomLoading() }
        }
        is LoadState.Error -> {
            item { BottomError(onRetry = { pagingItems.retry() }) }
        }
        is LoadState.NotLoading -> {
            if (state.endOfPaginationReached && pagingItems.itemCount > 0) {
                item { Text("没有更多数据了") }
            }
        }
    }
}
```

---

## 🔑 关键点

### 1. 为什么底部状态要放在 items() 后面?
因为 LazyColumn 从上到下渲染,后面的 item 就显示在底部!

### 2. 什么时候触发加载?
当用户滚动到距离底部 `prefetchDistance` 个 item 时,Paging 自动触发。

### 3. 需要手动触发加载吗?
**不需要!** Paging 会自动处理,你只需要根据状态显示 UI。

### 4. 如何重试失败的加载?
调用 `pagingItems.retry()`

### 5. 如何判断是否还有更多数据?
```kotlin
val noMore = pagingItems.loadState.append is LoadState.NotLoading 
    && (pagingItems.loadState.append as LoadState.NotLoading).endOfPaginationReached
```

---

## 📊 LoadState 速查表

### refresh (首次加载/刷新)
- `Loading` → 显示全屏 Loading
- `Error` → 显示全屏错误
- `NotLoading` → 显示数据

### append (底部加载更多)
- `Loading` → 显示底部 Loading ⭐
- `Error` → 显示底部错误和重试
- `NotLoading` → 不显示(或显示"没有更多")

### prepend (顶部加载更早的数据)
- 通常不用,除非你需要双向滚动

---

## 🐛 常见问题

### Q: Loading 一直显示,不消失?
**A:** 检查 PagingSource 的 `nextKey`:
```kotlin
LoadResult.Page(
    data = items,
    nextKey = if (items.isEmpty()) null else page + 1  // ← 没有更多数据时返回 null!
)
```

### Q: 为什么不触发加载?
**A:** 检查 `PagingConfig`:
```kotlin
PagingConfig(
    pageSize = 20,
    prefetchDistance = 5,  // ← 确保这个值合理
    enablePlaceholders = false
)
```

### Q: 如何调试加载状态?
**A:** 打印状态:
```kotlin
LaunchedEffect(pagingItems.loadState) {
    println("Append: ${pagingItems.loadState.append}")
}
```

---

## 🎨 UI 组件示例

### LoadingIndicator
```kotlin
@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Text("加载中...")
        }
    }
}
```

### ErrorView
```kotlin
@Composable
fun ErrorView(onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("加载失败")
            Button(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}
```

---

## 📚 相关文档

- 详细教程: `docs/LAZY_COLUMN_PAGING_GUIDE.md`
- 可视化图解: `docs/BOTTOM_LOADING_VISUAL_GUIDE.md`
- 代码示例: `composeApp/src/commonMain/kotlin/org/example/project/paging/`
  - `SimplePagingUI.kt` - 基础示例
  - `AnnotatedPagingExample.kt` - 带注释的详细示例
  - `BottomLoadingExamples.kt` - 三种复杂度的实现

---

## 🚀 快速开始

1. **创建 PagingSource**
```kotlin
class MyPagingSource : PagingSource<Int, MyItem>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MyItem> {
        val page = params.key ?: 0
        val items = loadFromNetwork(page)
        return LoadResult.Page(
            data = items,
            prevKey = if (page == 0) null else page - 1,
            nextKey = if (items.isEmpty()) null else page + 1
        )
    }
    
    override fun getRefreshKey(state: PagingState<Int, MyItem>): Int? = null
}
```

2. **创建 Pager**
```kotlin
val pager = Pager(
    config = PagingConfig(pageSize = 20),
    pagingSourceFactory = { MyPagingSource() }
).flow
```

3. **在 Compose 中使用**
```kotlin
@Composable
fun MyScreen() {
    val pagingItems = pager.collectAsLazyPagingItems()
    
    LazyColumn {
        items(count = pagingItems.itemCount) { index ->
            ItemView(pagingItems[index])
        }
        
        if (pagingItems.loadState.append is LoadState.Loading) {
            item { CircularProgressIndicator() }
        }
    }
}
```

完成! 🎉
