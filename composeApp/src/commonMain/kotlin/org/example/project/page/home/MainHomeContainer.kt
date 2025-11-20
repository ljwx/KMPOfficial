package org.example.project.page.home

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.jdcr.kmplog.KLog
import kotlinx.coroutines.launch
import org.example.project.log.ConstLogTag
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import org.example.project.page.settings.SettingsScreen
import org.example.project.page.user.ProfileScreen
import androidx.compose.runtime.remember

enum class HomeBottomTab(val label: String, val icon: ImageVector) {
    Home("主页", Icons.Filled.Home),
    Profile("个人", Icons.Filled.Person),
    Settings("设置", Icons.Filled.Settings)
}

@Composable
@Preview
fun MainHomeContainer() {

    var selectedTab by remember { mutableStateOf(HomeBottomTab.Home) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    MaterialTheme {

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
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            HomeBottomTab.values().forEach { tab ->
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
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
                        HomeBottomTab.Home -> HomeScreen()

                        HomeBottomTab.Profile -> ProfileScreen(modifier = contentModifier)

                        HomeBottomTab.Settings -> SettingsScreen(modifier = contentModifier)
                    }
                }
            }
        }
    }
}