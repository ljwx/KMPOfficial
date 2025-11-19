package org.example.project.page

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.commoncomposable.CommonTopBar
import org.example.project.commoncomposable.CommonTopBarBack
import org.example.project.pullrefresh.PullRefreshBox
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 下拉刷新示例页面
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun PullRefreshExamplePage() {
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // 用于生成唯一 ID 的计数器
    var idCounter = remember { 0 }
    
    // 示例数据
    val items = remember {
        mutableStateListOf(
            NewsItem("${idCounter++}", "新闻标题 1", "这是新闻内容的简介，展示一些描述信息...", "2分钟前"),
            NewsItem("${idCounter++}", "新闻标题 2", "这是新闻内容的简介，展示一些描述信息...", "5分钟前"),
            NewsItem("${idCounter++}", "新闻标题 3", "这是新闻内容的简介，展示一些描述信息...", "10分钟前"),
            NewsItem("${idCounter++}", "新闻标题 4", "这是新闻内容的简介，展示一些描述信息...", "15分钟前"),
            NewsItem("${idCounter++}", "新闻标题 5", "这是新闻内容的简介，展示一些描述信息...", "20分钟前"),
            NewsItem("${idCounter++}", "新闻标题 6", "这是新闻内容的简介，展示一些描述信息...", "30分钟前"),
            NewsItem("${idCounter++}", "新闻标题 7", "这是新闻内容的简介，展示一些描述信息...", "45分钟前"),
            NewsItem("${idCounter++}", "新闻标题 8", "这是新闻内容的简介，展示一些描述信息...", "1小时前"),
        )
    }

    Scaffold(
        topBar = {
            CommonTopBarBack("下拉刷新示例")
        }
    ) { paddingValues ->
        PullRefreshBox(
            refreshing = refreshing,
            enableContentOffset = true,  // 启用内容跟随
            onRefresh = {
                // 如果已经在刷新中，忽略此次刷新请求
                if (refreshing) return@PullRefreshBox
                
                // 开始刷新
                refreshing = true
                
                // 模拟网络请求
                scope.launch {
                    delay(2000) // 模拟2秒的网络延迟
                    
                    // 添加新数据到列表顶部（使用唯一 ID）
                    val newItem = NewsItem(
                        id = "${idCounter++}",  // 唯一 ID
                        title = "【新】新闻标题 ${Random.nextInt(100, 999)}",
                        description = "这是刚刚刷新加载的新内容，时间戳：${Clock.System.now().toEpochMilliseconds()}",
                        time = "刚刚"
                    )
                    items.add(0, newItem)
                    
                    // 结束刷新（这一步很重要！）
                    refreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 方案 1：使用 Column + verticalScroll（适合固定内容）
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        state = scrollState,
                        enabled = true,
                        reverseScrolling = false
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card {
                    Text(
                        text = "👆 试试下拉刷新\n向下拖动可以触发刷新",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Card {
                    Text(
                        text = "这是固定内容 1",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Card {
                    Text(
                        text = "这是固定内容 2",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Card {
                    Text(
                        text = "这是固定内容 3",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                // 添加更多内容，确保可以滚动
                repeat(10) { index ->
                    Card {
                        Text(
                            text = "内容项 ${index + 4}",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
//            LazyColumn(
//                modifier = Modifier.fillMaxSize().padding(top = 300.dp),
//                contentPadding = PaddingValues(16.dp),
//                verticalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                // 顶部提示
//                item {
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .background(
//                                color = MaterialTheme.colorScheme.primaryContainer,
//                                shape = RoundedCornerShape(8.dp)
//                            )
//                            .padding(16.dp)
//                    ) {
//                        Column {
//                            Text(
//                                text = "👆 试试下拉刷新",
//                                fontSize = 16.sp,
//                                fontWeight = FontWeight.Bold,
//                                color = MaterialTheme.colorScheme.onPrimaryContainer
//                            )
//                            Text(
//                                text = "向下拖动列表可以触发刷新，会在顶部添加新的内容",
//                                fontSize = 12.sp,
//                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
//                                modifier = Modifier.padding(top = 4.dp)
//                            )
//                        }
//                    }
//                }
//
//                // 新闻列表（使用唯一 ID 作为 key）
//                items(items, key = { it.id }) { item ->
//                    NewsCard(item)
//                }
//
//                // 底部提示
//                item {
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(vertical = 16.dp),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Text(
//                            text = "已加载 ${items.size} 条内容",
//                            fontSize = 12.sp,
//                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
//                        )
//                    }
//                }
//            }
        }
    }
}

/**
 * 新闻卡片组件
 */
@Composable
private fun NewsCard(item: NewsItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题
            Text(
                text = item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // 描述
            Text(
                text = item.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )
            
            // 时间
            Text(
                text = item.time,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * 新闻数据类
 */
private data class NewsItem(
    val id: String,  // 添加唯一 ID
    val title: String,
    val description: String,
    val time: String
)

