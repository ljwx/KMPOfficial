package org.example.project.navigation

import org.example.project.routes.AppSplash
import org.example.project.routes.MainHome
import org.example.project.routes.ProductDetail
import org.example.project.routes.TestWebView
import org.example.project.routes.TestPullRefresh

private const val FEATURE_LAUNCH = "launch_"
private const val FEATURE_USER = "user_"
private const val FEATURE_PRODUCT = "product_"
private const val FEATURE_WEVIEW = "webview_"
private const val FEATURE_TEST = "test_"

const val APP_SPLASH = FEATURE_LAUNCH + "splash"
const val MAIN_HOME = FEATURE_LAUNCH + "home"
const val USER_INFO = FEATURE_USER + "info"
const val PRODUCT_DETAIL = FEATURE_PRODUCT + "detail"
const val WEBVIEW_TEST = FEATURE_WEVIEW + "test"
const val PULL_REFRESH_TEST = FEATURE_TEST + "pullrefresh"

val RouterMap = mapOf(
    APP_SPLASH to AppSplash,
    MAIN_HOME to MainHome,
    PRODUCT_DETAIL to ProductDetail,
    WEBVIEW_TEST to TestWebView,
    PULL_REFRESH_TEST to TestPullRefresh,
)