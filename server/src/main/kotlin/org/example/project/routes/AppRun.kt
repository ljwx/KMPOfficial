package org.example.project.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.example.project.config.ConfigRateLimit
import org.example.project.db.apprun.AppRunService
import org.example.project.network.model.BaseApiResponse
import org.example.project.network.model.ConstResponseCode
import org.example.project.util.DateTimeUtils

fun Route.appRunInfo(appRunService: AppRunService) {
    route("/app/run/") {
        rateLimit(RateLimitName(ConfigRateLimit.API)) {
            get("daily") {

            }
            get("app/{appName}") {
//                call.request.queryParameters[""]
                val appName = call.parameters["appName"]
                if (appName.isNullOrEmpty()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        BaseApiResponse(
                            code = ConstResponseCode.PATH_PARAMS_EXCEPTION,
                            message = "api路径错误",
                            data = null
                        )
                    )
                } else {
                    val today = DateTimeUtils.nowUTC().toLocalDate()
                    val info = appRunService.getAppRunInfo(appName, today).lastOrNull()
                    call.respond(
                        HttpStatusCode.OK,
                        BaseApiResponse(
                            code = 200,
                            message = "成功",
                            data = appRunService.toInfoResponse(info)
                        )
                    )
                }
            }
            post("app/{appName}") {
                val appName = call.parameters["appName"]
                if (appName.isNullOrEmpty()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        BaseApiResponse(
                            code = ConstResponseCode.PATH_PARAMS_EXCEPTION,
                            message = "api路径错误",
                            data = null
                        )
                    )
                } else {
                    appRunService.appRun(appName)
                }
            }
        }
    }
}