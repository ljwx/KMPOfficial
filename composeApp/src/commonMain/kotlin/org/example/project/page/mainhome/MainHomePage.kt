package org.example.project.page.mainhome

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
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
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Icon
import org.example.project.page.mainhome.user.MainTabUserScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.example.project.navigation.isCurrent
import org.example.project.page.mainhome.home.MainTabHomeScreen
import org.example.project.statusbar.StatusBarConfig
import org.example.project.statusbar.StatusBarStyle
import org.example.project.page.mainhome.create.MainTabCreateScreen

private const val tabHome = "main_tab_home"
private const val tabCreate = "main_tab_create"
private const val tabMine = "main_tab_mine"

enum class HomeBottomTab(val label: String, val icon: ImageVector, val route: String) {
    Home("主页", Icons.Filled.Home, tabHome),
    Create("创作", Icons.Filled.Create, tabCreate),
    User("我的", Icons.Filled.Person, tabMine)
}

@Composable
@Preview
fun MainHomePage() {

    StatusBarConfig(StatusBarStyle.DARK_CONTENT)

    val tabNavController = rememberNavController()
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
                                    selected = tabNavController.isCurrent(tab.route),
                                    onClick = {
                                        tabNavController.navigate(tab.route) {
                                            popUpTo(tabNavController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            restoreState = true
                                            launchSingleTop = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            tab.icon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    val contentModifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)

                    NavHost(
                        navController = tabNavController,
                        startDestination = tabHome,
                        modifier = contentModifier,
                        enterTransition = { fadeIn(tween(250)) },
                        exitTransition = { fadeOut(tween(250)) },
                        popEnterTransition = { fadeIn(tween(250)) },
                        popExitTransition = { fadeOut(tween(250)) }
                    ) {
                        composable(tabHome) {
                            MainTabHomeScreen(modifier = contentModifier)
                        }
                        composable(tabCreate) {
                            MainTabCreateScreen()
                        }
                        composable(tabMine) {
                            MainTabUserScreen()
                        }
                    }
                }
            }
        }
    }
}