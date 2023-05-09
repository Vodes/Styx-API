package moe.styx

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

fun Boolean.toInt() = if (this) 1 else 0
fun Int.toBoolean() = this > 0

suspend inline fun ApplicationCall.respondStyx(status: HttpStatusCode, message: String) {
    response.status(status)
    respond(ApiResponse(status.value, message))
}
