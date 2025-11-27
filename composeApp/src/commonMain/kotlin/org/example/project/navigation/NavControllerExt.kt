package org.example.project.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
inline fun NavController.isCurrent(route: String): Boolean {
    return this.currentBackStackEntryAsState().value?.destination?.hierarchy?.any { it.route == route } == true
}