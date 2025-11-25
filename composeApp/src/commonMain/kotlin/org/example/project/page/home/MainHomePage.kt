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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.compose_multiplatform
import kotlinx.serialization.json.Json
import org.example.project.Greeting
import org.example.project.commoncomposable.CommonPageContainer
import org.example.project.commoncomposable.CommonTopBar
import org.example.project.commoncomposable.PageContainerConfig
import org.example.project.feature.product.ProductViewModel
import org.example.project.navigation.LocalNavController
import org.example.project.network.model.ProductSummaryData
import org.example.project.routes.RouterMainHome
import org.example.project.routes.RouterProductDetail
import org.example.project.routes.RouterPullRefresh
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

sealed interface HomeEvent {
    data class OnProductClick(val product: ProductSummaryData) : HomeEvent
    data object OnNavigateToPullRefreshDemo : HomeEvent
    data object OnShowToastDemo : HomeEvent
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

    Scaffold(
        modifier = modifier,
        topBar = { CommonTopBar(title = "测试") },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        CommonPageContainer(
            modifier = Modifier.padding(innerPadding), // 👈 显示区域由 Scaffold 的 innerPadding 定义
            viewModel = viewModel,
            config = PageContainerConfig.AlsoPull,
            router = RouterMainHome
        ) {
            HomeScreen(
                products = products,
                onEvent = { event ->
                    when (event) {
                        is HomeEvent.OnProductClick -> {
                            val productJson =
                                Json.encodeToString(ProductSummaryData.serializer(), event.product)
                            navController.navigate(RouterProductDetail(productJson))
                        }

                        HomeEvent.OnNavigateToPullRefreshDemo -> {
                            navController.navigate(RouterPullRefresh)
                        }

                        HomeEvent.OnShowToastDemo -> {
                            viewModel.showToast("This is a side effect from ViewModel!")
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    products: List<ProductSummaryData>,
    onEvent: (HomeEvent) -> Unit,
) {
    val greeting = remember { Greeting().greet() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
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

            // Demo Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onEvent(HomeEvent.OnNavigateToPullRefreshDemo) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View PullRefresh Demo")
                    }
                    Button(
                        onClick = { onEvent(HomeEvent.OnShowToastDemo) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test Side Effect (Toast)")
                    }
                }
            }

            // Product List
            items(products) { product ->
                ProductItem(
                    product = product,
                    onClick = { onEvent(HomeEvent.OnProductClick(product)) }
                )
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