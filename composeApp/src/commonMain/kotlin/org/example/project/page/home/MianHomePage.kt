package org.example.project.page.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.compose_multiplatform
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.Greeting
import org.example.project.PlatformType
import org.example.project.getPlatform
import org.example.project.log.KSLog
import org.example.project.multiplestate.MultiStateLayout
import org.example.project.multiplestate.MultiStateLayoutState
import org.example.project.navigation.LocalAppNavigation
import org.example.project.navigation.PRODUCT_DETAIL
import org.example.project.network.api.ProductApiService
import org.example.project.network.model.ProductSummaryData
import org.jetbrains.compose.resources.painterResource

@Composable
fun HomeScreen(
    tabTitle: String,
    modifier: Modifier = Modifier,
) {
    val greeting = remember { Greeting().greet() }
    val navigation = LocalAppNavigation.current

    var products by remember { mutableStateOf(emptyList<ProductSummaryData>()) }
    val apiService = remember { ProductApiService() }
    var multiState by remember { mutableStateOf<MultiStateLayoutState>(MultiStateLayoutState.Loading) }

    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch {
            apiService.getProducts().onSuccess {
                products = it
                multiState = MultiStateLayoutState.Content
            }.onFailure {
//                KSLog.iNet("请求失败了："+it)
            }
        }
    }
    MultiStateLayout(state = multiState) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .safeContentPadding(),
        ) {
            // 顶部欢迎信息
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

            // 商品列表 - 根据平台设置不同的宽度
            val isWeb = getPlatform().isPlatform(PlatformType.PLATFORM_WEB)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = if (isWeb) Alignment.Center else Alignment.TopStart
            ) {
                LazyColumn(
                    modifier = Modifier
                        .then(
                            if (isWeb) {
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
                    items(products) { product ->
                        ProductItem(
                            product = product,
                            onClick = {
                                navigation.openScreenForResult<ProductSummaryData, String>(
                                    router = PRODUCT_DETAIL,
                                    params = product,
                                    serializer = ProductSummaryData.serializer(),
                                    onResult = {
                                        KSLog.iRouter("跳转页面后返回的结果:$it")
                                    }
                                )
                            }
                        )
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