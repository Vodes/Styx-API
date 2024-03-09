package moe.styx.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import moe.styx.respondStyx
import java.io.File

fun Route.watch() {
    get("/watch/{media}") {
        val params = call.request.queryParameters
        val token = params["token"]

        val (user, device) = checkTokenDeviceUser(token, call, watch = true)
        if (device == null || user == null)
            return@get

        val entry = checkMediaEntry(call.parameters["media"], call) ?: return@get
        val file = File(entry.filePath)
        if (!file.exists()) {
            call.respondStyx(HttpStatusCode.NotFound, "Could not find file on the server.")
            return@get
        }
        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, file.name).toString()
        )
        call.respondFile(file)
    }
}