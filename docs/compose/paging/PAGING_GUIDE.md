# AndroidX Paging 在 Compose Multiplatform 中的使用指南

## 📦 依赖配置

已在项目中配置了官方 AndroidX Paging 3.4.0-alpha03,支持 Kotlin Multiplatform。

### gradle/libs.versions.toml
```toml
[versions]
paging = "3.4.0-alpha03"

[libraries]
paging-common = { module = "androidx.paging:paging-common", version.ref = "paging" }
paging-compose = { module = "androidx.paging:paging-compose", version.ref = "paging" }
```

### composeApp/build.gradle.kts
```kotlin
commonMain.dependencies {
    implementation(libs.paging.common)
    implementation(libs.paging.compose)
}
```

## 🎯 核心概念

### 1. PagingSource
负责加载分页数据的数据源。

```kotlin
class MyPagingSource : PagingSource<Int, MyItem>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MyItem> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        
        return try {
            // 加载数据(从网络、数据库等)
            val items = loadItemsFromNetwork(page, pageSize)
            
            LoadResult.Page(
                data = items,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (items.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
    
    override fun getRefreshKey(state: PagingState<Int, MyItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
```

### 2. Pager
创建 PagingData 流。

```kotlin
fun createMyPager(): Flow<PagingData<MyItem>> {
    return Pager(
        config = PagingConfig(
            pageSize = 20,              // 每页加载的数量
            enablePlaceholders = false,  // 是否启用占位符
            initialLoadSize = 20,        // 初始加载数量
            prefetchDistance = 5         // 预加载距离
        ),
        pagingSourceFactory = { MyPagingSource() }
    ).flow
}
```

### 3. 在 Compose 中使用

```kotlin
@Composable
fun MyPagingScreen() {
    val pagingItems = createMyPager().collectAsLazyPagingItems()
    
    LazyColumn {
        // 处理刷新状态
        when (pagingItems.loadState.refresh) {
            is LoadState.Loading -> {
                item { LoadingIndicator() }
            }
            is LoadState.Error -> {
                item { ErrorView(onRetry = { pagingItems.retry() }) }
            }
            else -> Unit
        }
        
        // 显示数据
        items(
            count = pagingItems.itemCount,
            key = { index -> pagingItems[index]?.id ?: index }
        ) { index ->
            val item = pagingItems[index]
            if (item != null) {
                ItemView(item)
            }
        }
        
        // 处理加载更多状态
        when (pagingItems.loadState.append) {
            is LoadState.Loading -> {
                item { LoadingMoreIndicator() }
            }
            is LoadState.Error -> {
                item { LoadMoreErrorView(onRetry = { pagingItems.retry() }) }
            }
            else -> Unit
        }
    }
}
```

## 🔄 与 ViewModel 集成

```kotlin
class MyViewModel : ViewModel() {
    val pagingDataFlow: Flow<PagingData<MyItem>> = Pager(
        config = PagingConfig(pageSize = 20),
        pagingSourceFactory = { MyPagingSource() }
    ).flow.cachedIn(viewModelScope)  // 缓存 PagingData
}

@Composable
fun MyScreen(viewModel: MyViewModel = koinViewModel()) {
    val pagingItems = viewModel.pagingDataFlow.collectAsLazyPagingItems()
    // ... UI 代码
}
```

## 📝 示例代码

项目中已包含完整示例:
- `SimplePagingExample.kt` - PagingSource 和 Pager 创建示例
- `SimplePagingUI.kt` - Compose UI 集成示例

使用方式:
```kotlin
@Composable
fun App() {
    SimplePagingScreen()
}
```

## 🆚 与 Cash App Paging 的区别

| 特性 | Cash App Paging | AndroidX Paging 3.4+ |
|------|----------------|---------------------|
| 维护状态 | ❌ 已废弃 | ✅ 官方维护 |
| KMP 支持 | 有限 | 完整支持 |
| 模块名称 | `app.cash.paging` | `androidx.paging` |
| Compose 支持 | `paging-compose-common` | `paging-compose` |

## ⚠️ 注意事项

1. **版本要求**: AndroidX Paging 3.4.0+ 才支持 Kotlin Multiplatform
2. **不要混用**: 不要同时使用 Cash App 和 AndroidX 的 Paging 库
3. **缓存**: 在 ViewModel 中使用 `cachedIn(viewModelScope)` 来缓存 PagingData
4. **Key**: 为 items 提供稳定的 key 以优化性能

## 🚀 最佳实践

1. **错误处理**: 总是处理 `LoadState.Error` 状态
2. **重试机制**: 提供重试按钮给用户
3. **占位符**: 根据需求决定是否启用占位符
4. **预加载**: 合理设置 `prefetchDistance` 提升用户体验
5. **缓存**: 使用 `cachedIn()` 避免配置更改时重新加载数据

## 📚 参考资源

- [AndroidX Paging 官方文档](https://developer.android.com/topic/libraries/architecture/paging/v3-overview)
- [Paging 3 KMP 支持公告](https://android-developers.googleblog.com/2024/08/paging-3-multiplatform-support.html)
