package org.example.project.paging

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import org.example.project.common.paging.createCommonPager
import org.example.project.commoncomposable.LazyColumnLoadMore.LazyColumLoadMore
import org.example.project.log.KSLog

/**
 * Paging UI 示例
 * 演示如何在 Compose 中使用 LazyPagingItems
 */

@Composable
fun SimplePagingScreen(
    modifier: Modifier = Modifier
) {
    // 使用 remember 缓存 Flow,避免每次重组都创建新的 Pager
    val pagingDataFlow = remember {
        createCommonPager { pageIndex, pageSize ->
            KSLog.iNet("加载第 $pageIndex 页, pageSize: $pageSize")
            // 生成该页的数据
            List(pageSize) { itemIndex ->
                val itemId = pageIndex * pageSize + itemIndex
                TestPagingItem(
                    id = itemId,
                    title = "Item #$itemId",
                    description = "This is the description for item $itemId"
                )
            }
        }
    }

    val pagingItems = pagingDataFlow.collectAsLazyPagingItems()

    SimplePagingList(
        pagingItems = pagingItems,
        modifier = modifier
    )
}

@Composable
fun SimplePagingList(
    pagingItems: LazyPagingItems<TestPagingItem>,
    modifier: Modifier = Modifier
) {
    LazyColumLoadMore(
        modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        pagingItems
    ) {
        ItemCard(item = it)
    }
//    LazyColumn(
//        modifier = modifier.fillMaxSize(),
//        contentPadding = PaddingValues(16.dp),
//        verticalArrangement = Arrangement.spacedBy(8.dp)
//    ) {
//        // 显示加载状态
//        when (pagingItems.loadState.refresh) {
//            is LoadState.Loading -> {
//                item {
//                    Box(
//                        modifier = Modifier.fillMaxWidth().padding(16.dp),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        CircularProgressIndicator()
//                    }
//                }
//            }
//
//            is LoadState.Error -> {
//                val error = (pagingItems.loadState.refresh as LoadState.Error).error
//                item {
//                    ErrorItem(
//                        message = error.message ?: "Unknown error",
//                        onRetry = { pagingItems.retry() }
//                    )
//                }
//            }
//
//            else -> Unit
//        }
//
//        // 显示数据列表
//        items(
//            count = pagingItems.itemCount,
//            key = { index ->
//                val item = pagingItems.itemSnapshotList.getOrNull(index)
//                item?.id ?: index
//            }
//        ) { index ->
//            val item = pagingItems[index]
//            if (item != null) {
//                ItemCard(item = item)
//            } else {
//                // 占位符
//                ItemPlaceholder()
//            }
//        }
//
//        // 显示加载更多状态
//        when (pagingItems.loadState.append) {
//            is LoadState.Loading -> {
//                item {
//                    Box(
//                        modifier = Modifier.fillMaxWidth().padding(16.dp),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        CircularProgressIndicator()
//                    }
//                }
//            }
//
//            is LoadState.Error -> {
//                val error = (pagingItems.loadState.append as LoadState.Error).error
//                item {
//                    ErrorItem(
//                        message = error.message ?: "Unknown error",
//                        onRetry = { pagingItems.retry() }
//                    )
//                }
//            }
//
//            else -> Unit
//        }
//    }
}

@Composable
private fun ItemCard(
    item: TestPagingItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ItemPlaceholder(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp).align(Alignment.Center)
            )
        }
    }
}
