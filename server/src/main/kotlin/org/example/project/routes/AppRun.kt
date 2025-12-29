package org.example.project.routes

import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.example.project.config.ConfigRateLimit

fun Route.appRun() {
    route("/app/run/") {
        rateLimit(RateLimitName(ConfigRateLimit.API)) {
            post {

            }
        }
    }
}