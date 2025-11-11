package org.example.project

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jdcr.kmplog.KLog
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.compose_multiplatform
import kotlinx.coroutines.launch
import org.example.project.commoncomposable.CommonTopBar
import org.example.project.log.ConstLogTag
import org.example.project.navigation.AppNavigation.State.ScreenState
import org.example.project.navigation.rememberAppNavigation
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private enum class BottomTab(val label: String, val icon: ImageVector) {
    Home("主页", Icons.Filled.Home),
    Profile("个人", Icons.Filled.Person),
    Settings("设置", Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    KLog.d(ConstLogTag.UI_INFO, "启动")
    MaterialTheme {
        // 创建跨平台可复用的导航控制器（内部基于 Decompose 实现）
        val navigation = rememberAppNavigation()
        var selectedTab by remember { mutableStateOf(BottomTab.Home) }
        val navState = navigation.state

        navigation.Render(
            modifier = Modifier.fillMaxSize(),
            home = { modifier ->
                // 首页拥有自己的 Scaffold，使 topBar/bottomBar 与内容同步参与动画
                HomeContainer(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onOpenDetail = navigation::openDetail,
                    modifier = modifier
                )
            },
            detail = { modifier, detail ->
                DetailScreen(
                    message = detail.message,
                    modifier = modifier.fillMaxSize(),
                    onBack = navigation::navigateBack
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContainer(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    onOpenDetail: (title: String, message: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val topBarTitle = selectedTab.label

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(drawerContent = {
        ModalDrawerSheet(modifier = Modifier.width(100.dp).pointerInput(Unit) {
            detectHorizontalDragGestures { change, dragAmount ->
                KLog.d(ConstLogTag.UI_INFO, "dragAmount: $dragAmount")
                if (dragAmount < 0) {
                    change.consume()
                    scope.launch { drawerState.close() }
                }
            }
        }) {
            Text("按钮")
        }
    }, drawerState = drawerState, gesturesEnabled = false) {

        Box(Modifier.fillMaxSize().pointerInput(Unit) {
            detectHorizontalDragGestures(onDragStart = { offset ->
                val edgeWidthPx = 60.dp.toPx()
                if (offset.x > edgeWidthPx) {
                    return@detectHorizontalDragGestures
                }
                scope.launch { drawerState.open() }
            }, onHorizontalDrag = { _, _ ->

            })
        }) {
            Scaffold(
                modifier = modifier,
                topBar = {
                    CommonTopBar(
                        title = topBarTitle,
                        onBack = null
                    )
                },
                bottomBar = {
                    NavigationBar {
                        BottomTab.values().forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { onTabSelected(tab) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                val contentModifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)

                when (selectedTab) {
                    BottomTab.Home -> HomeScreen(
                        tabTitle = selectedTab.label,
                        modifier = contentModifier,
                        onNavigateDetail = onOpenDetail
                    )

                    BottomTab.Profile -> ProfileScreen(modifier = contentModifier)

                    BottomTab.Settings -> SettingsScreen(modifier = contentModifier)
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    tabTitle: String,
    modifier: Modifier = Modifier,
    onNavigateDetail: (title: String, message: String) -> Unit
) {
    val greeting = remember { Greeting().greet() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeContentPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.compose_multiplatform),
            contentDescription = null,
            modifier = Modifier.size(160.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Compose: $greeting")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onNavigateDetail(tabTitle, greeting) }) {
            Text("查看详情", fontSize = 16.sp)
        }
    }
}

@Composable
private fun ProfileScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .safeContentPadding(),
        contentAlignment = Alignment.Center
    ) {
        Text("个人中心内容")
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .safeContentPadding(),
        contentAlignment = Alignment.Center
    ) {
        Text("设置页面内容")
    }
}

@Composable
private fun DetailScreen(
    message: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeContentPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("详情内容")
        Spacer(modifier = Modifier.height(16.dp))
        Text(message)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("返回主页", fontSize = 16.sp)
        }
    }
}