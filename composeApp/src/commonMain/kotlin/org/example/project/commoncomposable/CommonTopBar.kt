package org.example.project.commoncomposable

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.navigation.LocalNavController

@Composable
fun CommonTopBarBack(title: String, onBack: (() -> Unit)? = null) {
    CommonTopBar(title = title, showNavigation = true, onBack = onBack)
}

@Composable
fun CommonTopBar(title: String) {
    CommonTopBar(title = title, showNavigation = false, onBack = null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommonTopBar(
    title: String,
    containerColor: Color = Color.Transparent,
    showNavigation: Boolean = true,
    onBack: (() -> Unit)? = null
) {
    val navController = LocalNavController.current
    CenterAlignedTopAppBar(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .height(42.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (showNavigation) {
                IconButton(onClick = onBack ?: {
                    navController.popBackStack()
                    Unit
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }
        },
        actions = {

        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,// 背景色
            titleContentColor = Color.Black,// 标题默认色（可与 Text 内的颜色一致或不同）
            navigationIconContentColor = Color(0xFF424242),
            actionIconContentColor = Color(0xFF424242)
        )
    )
}