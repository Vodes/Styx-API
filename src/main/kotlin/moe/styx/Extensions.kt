package moe.styx

import io.ktor.client.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import moe.styx.types.ApiResponse
import moe.styx.types.json

suspend inline fun ApplicationCall.respondStyx(status: HttpStatusCode, message: String, silent: Boolean = false) {
    response.status(status)
    respond(ApiResponse(status.value, message, silent))
}

fun HttpStatusCode.makeResponse(message: String, silent: Boolean = false): ApiResponse {
    return ApiResponse(this.value, message, silent)
}

val httpClient = HttpClient {
    install(ContentNegotiation) { json }
    install(ContentEncoding)
    install(HttpCookies)
}
