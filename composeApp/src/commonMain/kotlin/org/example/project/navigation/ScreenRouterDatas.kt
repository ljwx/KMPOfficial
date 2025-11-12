package org.example.project.navigation

import kotlinx.serialization.Serializable

private const val FEATURE_LAUNCH = "launch_"
private const val FEATURE_USER = "user_"
private const val FEATURE_PRODUCT = "product_"

internal const val ROUTER_HOME = FEATURE_LAUNCH + "home"
const val USER_INFO = FEATURE_USER + "info"
const val PRODUCT_DETAIL = FEATURE_PRODUCT + "detail"

internal val routerMap: Map<String, ScreenRouterData> = mapOf(
    ROUTER_HOME to ScreenRouterData(ROUTER_HOME, launchMode = LaunchMode.SINGLE_TASK),
    USER_INFO to ScreenRouterData(USER_INFO),
    PRODUCT_DETAIL to ScreenRouterData(PRODUCT_DETAIL)
)