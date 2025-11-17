package org.example.project.page.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.commoncomposable.CommonTopBarBack
import org.example.project.network.model.ProductSummaryData
import org.example.project.navigation.LocalAppNavigation
import org.example.project.navigation.BackHandler
import org.example.project.navigation.WEBVIEW_TEST
import org.example.project.navigation.openScreen

@Composable
fun DetailScreen(
    component: ProductDetailComponent,
    modifier: Modifier = Modifier,
) {
    val navigation = LocalAppNavigation.current

    // 从 Component 中获取商品信息，而不是从 navigation 中获取
    val product = component.product

    MaterialTheme {
        Scaffold(topBar = { CommonTopBarBack("详情页", null) }) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .safeContentPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("详情内容", fontSize = 24.sp)
                Spacer(modifier = Modifier.height(16.dp))

                if (product != null) {
                    // 直接使用对象，类型安全
                    Text("商品ID: ${product.id}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("商品名称: ${product.name}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("价格: ¥${product.price}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("描述: ${product.description}", fontSize = 16.sp)
                } else {
                    Text("未找到商品信息", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    navigation.navigateBackWithResult("详情页的值")
                }) {
                    Text("返回主页", fontSize = 16.sp)
                }
                Button(onClick = {
                    navigation.openScreen(WEBVIEW_TEST)
                }) {
                    Text("跳转web", fontSize = 16.sp)
                }
            }
        }
    }
}