package org.example.project.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavController

val LocalNavController = compositionLocalOf<NavController> {
    error("No NavController provided. Make sure to wrap your NavHost with CompositionLocalProvider(LocalNavController provides navController)")
}

