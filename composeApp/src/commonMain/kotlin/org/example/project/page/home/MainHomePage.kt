package org.example.project.page.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.compose_multiplatform
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.Greeting
import org.example.project.feature.product.ProductViewModel
import org.example.project.multiplestate.MultiStateLayout
import org.example.project.multiplestate.MultiStateLayoutState
import kotlinx.serialization.json.Json
import org.example.project.navigation.LocalNavController
import org.example.project.network.model.ProductSummaryData
import org.example.project.routes.RouterProductDetail
import org.example.project.routes.RouterPullRefresh
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * 使用 Component 架构的 HomeScreen（推荐）
 * Component 的生命周期独立于 Compose 组合，状态不会因为组合被移除而丢失
 */
sealed interface HomeEvent {
    data class OnProductClick(val product: ProductSummaryData) : HomeEvent
    data object OnPullRefreshClick : HomeEvent
    data object OnShowToastClick : HomeEvent // New event for demo
}

sealed interface HomeEffect {
    data class ShowToast(val message: String) : HomeEffect
}

@Composable
fun MainHomePage(
    modifier: Modifier = Modifier,
    viewModel: ProductViewModel = koinViewModel(),
) {
    val navController = LocalNavController.current
    val products by viewModel.productList.collectAsState()
    val multiState by viewModel.multiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeEffect.ShowToast -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    HomeScreen(
        modifier = modifier,
        products = products,
        multiState = multiState,
        snackbarHostState = snackbarHostState,
        onEvent = { event ->
            when (event) {
                is HomeEvent.OnProductClick -> {
                    val productJson = Json.encodeToString(ProductSummaryData.serializer(), event.product)
                    navController.navigate(RouterProductDetail(productJson))
                }

                HomeEvent.OnPullRefreshClick -> {
                    navController.navigate(RouterPullRefresh)
                }

                HomeEvent.OnShowToastClick -> {
                    viewModel.showToast("This is a Side Effect from ViewModel!")
                }
            }
        },
    )
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    products: List<ProductSummaryData>,
    multiState: MultiStateLayoutState,
    snackbarHostState: SnackbarHostState,
    onEvent: (HomeEvent) -> Unit,
) {
    val greeting = remember { Greeting().greet() }

    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val onRefresh: () -> Unit = {
        isRefreshing = true
        scope.launch {
            delay(3000)
            isRefreshing = false
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        MultiStateLayout(
            state = multiState,
            modifier = Modifier.padding(innerPadding)
        ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                state = pullToRefreshState,
//                indicator = {
//                    // 只有在下拉或刷新时才显示
//                    if (isRefreshing || pullToRefreshState.distanceFraction > 0f) {
//                        Box(
//                            Modifier
//                                .align(Alignment.TopCenter)
//                                .padding(top = 16.dp)
//                                .graphicsLayer {
//                                    // 根据下拉距离改变透明度，避免突兀显示
//                                    alpha = if (isRefreshing) 1f else pullToRefreshState.distanceFraction.coerceIn(0f, 1f)
//                                    // 让 Indicator 跟随手势下滑
//                                    translationY = pullToRefreshState.distanceFraction * 100.dp.toPx()
//                                }
//                        ) {
//                            if (isRefreshing) {
//                                Text("正在刷新...")
//                            } else {
//                                Text(if (pullToRefreshState.distanceFraction > 1f) "释放刷新" else "下拉刷新")
//                            }
//                        }
//                    }
//                }
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
//                        .background(Color.LightGray)
                        .graphicsLayer {
                            // 让内容跟随下拉手势偏移
                            // 100.dp 是一个经验值，对应 Indicator 的大致显示高度
                            translationY = pullToRefreshState.distanceFraction * 100.dp.toPx()
                        },
                    contentAlignment = Alignment.TopCenter
                ) {
                    // 当宽度大于 600dp 时认为是宽屏设备（如 Web 桌面端、平板）
                    val isWideScreen = maxWidth > 600.dp

                    LazyColumn(
                        modifier = Modifier
                            .then(
                                if (isWideScreen) {
                                    Modifier.fillMaxWidth(0.5f)
                                } else {
                                    Modifier.fillMaxWidth()
                                }
                            ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        )
                    ) {
                        // 顶部欢迎信息
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    painter = painterResource(Res.drawable.compose_multiplatform),
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Compose: $greeting", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // 添加下拉刷新示例按钮
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onEvent(HomeEvent.OnPullRefreshClick) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("查看下拉刷新示例")
                                }
                                Button(
                                    onClick = { onEvent(HomeEvent.OnShowToastClick) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("测试 Side Effect (Toast)")
                                }
                            }
                        }

                        items(products) { product ->
                            ProductItem(
                                product = product,
                                onClick = { onEvent(HomeEvent.OnProductClick(product)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductItem(
    product: ProductSummaryData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = product.description,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                text = "¥${product.price}",
                fontSize = 20.sp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}