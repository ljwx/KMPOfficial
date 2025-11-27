package org.example.project.page.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.common.backhandler.BlockBackHandler
import org.example.project.commoncomposable.CommonTopBarBack
import org.example.project.feature.product.ProductViewModel
import org.example.project.log.KSLog
import org.example.project.network.model.ProductSummaryData
import org.koin.compose.viewmodel.koinViewModel

/**
 * DetailScreen - 产品详情页
 * 
 * 最佳实践:根据 ID 加载数据,而非接收完整对象
 * - 保持单一数据源
 * - 确保数据为最新
 * - 符合导航最佳实践
 */
@Composable
fun DetailScreen(
    product: ProductSummaryData,
    viewModel: ProductViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    BlockBackHandler(enabled = true) {
        KSLog.iRouter("点击了返回按钮")
    }
    MaterialTheme {
        Scaffold(topBar = { CommonTopBarBack("详情页", null) }) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .safeContentPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (product != null) {
                    Text("详情内容", fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("商品ID: ${product.id}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("商品名称: ${product.name}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("价格: ¥${product.price}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("描述: ${product.description}", fontSize = 16.sp)
                } else {
                    Text("产品不存在", fontSize = 16.sp)
                }
            }
        }
    }
}