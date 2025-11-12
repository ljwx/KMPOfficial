package org.example.project.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.home.Product
import org.example.project.navigation.LocalAppNavigation
import org.example.project.navigation.BackHandler

@Composable
fun DetailScreen(
    modifier: Modifier = Modifier,
) {
    val navigation = LocalAppNavigation.current
    
    val product = navigation.getCurrentActiveInstance().getParamsSerialize<Product>()

    // 跨平台返回键处理示例
    // 在 Android 上会拦截系统返回键，在其他平台上提供统一的 API
    BackHandler(enabled = true) {
        navigation.navigateBack()
        // 返回 true 表示已处理返回键，阻止默认行为
        true
    }

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
    }
}