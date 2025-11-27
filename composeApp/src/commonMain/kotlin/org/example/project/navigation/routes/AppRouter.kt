package org.example.project.navigation.routes

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

private const val moduleLaunch = "launch_"
private const val moduleAuth = "auth_"
private const val moduleMain = "main_"
private const val moduleProduct = "product_"

private const val RouteSplash = moduleLaunch + "splash"
private const val RouteMainPage = moduleMain + "main_page"
private const val RouteMainTabHome = moduleMain + "main_tab_home"
private const val RouteMainTabCreate = moduleMain + "main_tab_create"
private const val RouteMainTabMine = moduleMain + "main_tab_mine"
private const val RouteProductDetail = moduleProduct + "detail"

@Serializable
@Polymorphic
sealed class BaseRouter(val route: String)

@Serializable
data object RouterSplash : BaseRouter(RouteSplash)

@Serializable
object RouterMainPage : BaseRouter(RouteMainPage)

@Serializable
object RouterPullRefresh : BaseRouter("")

@Serializable
data class RouterProductDetail(val productJson: String) : BaseRouter(RouteProductDetail)

@Serializable
data class Detail(val id: String, val fromSource: String) : BaseRouter("")