package moe.styx

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import moe.styx.common.data.ApiResponse
import moe.styx.common.data.DeviceInfo

suspend inline fun ApplicationCall.respondStyx(status: HttpStatusCode, message: String, silent: Boolean = false) {
    response.status(status)
    respond(ApiResponse(status.value, message, silent))
}

fun HttpStatusCode.makeResponse(message: String, silent: Boolean = false): ApiResponse {
    return ApiResponse(this.value, message, silent)
}

fun ApplicationCall.fetchIP(): String {
    var ip = request.origin.remoteAddress
    if (request.headers.contains("X-PROXY-IP")) {
        ip = request.headers["X-PROXY-IP"] ?: ""
    } else if (request.headers.contains("CF-Connecting-IP")) {
        ip = request.headers["CF-Connecting-IP"] ?: ""
    }
    return ip
}

@Serializable
data class IPDeviceEntry(val ip: String, val deviceInfo: DeviceInfo)
