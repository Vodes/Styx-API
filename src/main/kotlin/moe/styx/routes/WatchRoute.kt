package moe.styx.routes

import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import moe.styx.routes.watch.CustomLocalFileContent
import moe.styx.types.Device
import java.io.File

fun Route.watch() {
    get("/watch/{media}") {
        val params = call.request.queryParameters
        val token = params["token"]

        val (user, device) = checkTokenDeviceUser(token, call)
        if (device == null || user == null)
            return@get

        val entry = checkMediaEntry(call.parameters["media"], call) ?: return@get

        call.customRespondFile(File(entry.filePath), device)
    }
}

suspend fun ApplicationCall.customRespondFile(file: File, device: Device, configure: OutgoingContent.() -> Unit = {}) {
    val message = CustomLocalFileContent(file, device = device).apply(configure)
    respond(message)
}