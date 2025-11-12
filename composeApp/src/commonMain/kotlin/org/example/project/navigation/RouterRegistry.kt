package org.example.project.navigation

import org.example.project.log.KSLog
import org.example.project.routes.MianHome
import org.example.project.routes.ProductDetail

fun initializeRoutes() {
    RouterRegistry.registerAll(
        MianHome,
        ProductDetail,
    )
}

object RouterRegistry {
    private val handlers = mutableMapOf<String, ScreenRouteHandler>()

    fun register(handler: ScreenRouteHandler) {
        handlers[handler.route] = handler
        KSLog.iRouter("注册路由:" + handler.route)
    }

    fun registerAll(vararg handlers: ScreenRouteHandler) {
        handlers.forEach { register(it) }
    }

    fun getHandler(route: String): ScreenRouteHandler? {
        val handler = handlers[route]
        return handler
    }

    fun isRegistered(route: String): Boolean {
        return handlers.containsKey(route)
    }

    fun getAllRoutes(): Set<String> {
        return handlers.keys.toSet()
    }
}

