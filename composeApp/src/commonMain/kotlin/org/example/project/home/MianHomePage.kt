package org.example.project.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.compose_multiplatform
import kotlinx.serialization.Serializable
import org.example.project.Greeting
import org.example.project.PlatformType
import org.example.project.getPlatform
import org.example.project.log.KSLog
import org.example.project.navigation.LocalAppNavigation
import org.example.project.navigation.PRODUCT_DETAIL
import org.example.project.navigation.openScreen
import org.jetbrains.compose.resources.painterResource

// 示例数据类：商品信息
@Serializable
data class Product(
    val id: Int,
    val name: String,
    val price: Double,
    val description: String
)

@Composable
fun HomeScreen(
    tabTitle: String,
    modifier: Modifier = Modifier,
) {
    val greeting = remember { Greeting().greet() }
    val navigation = LocalAppNavigation.current

    // 模拟商品列表数据（实际项目中可能来自 ViewModel 或数据源）
    val products = remember {
        listOf(
            Product(1, "商品A", 99.99, "这是商品A的描述"),
            Product(2, "商品B", 199.99, "这是商品B的描述"),
            Product(3, "商品C", 299.99, "这是商品C的描述"),
            Product(4, "商品D", 399.99, "这是商品D的描述"),
        )
    }

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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                )
            ) {
                items(products) { product ->
                    ProductItem(
                        product = product,
                        onClick = {
                            navigation.openScreenForResult<Product, String>(
                                router = PRODUCT_DETAIL,
                                params = product,
                                serializer = Product.serializer(),
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

@Composable
private fun ProductItem(
    product: Product,
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