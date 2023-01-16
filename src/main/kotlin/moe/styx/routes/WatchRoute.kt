package moe.styx.routes

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import moe.styx.*
import moe.styx.routes.watch.CustomLocalFileContent
import java.io.File

fun Route.watch() {
    get("/watch") {
        val params = call.request.queryParameters
        val token = params["token"]

        if (params["media"].isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, ApiResponse(400, "No media ID was found in your request."))
            return@get
        }

        if (!token.isNullOrBlank()) {
            val device = getDevices().find { it.watchToken.equals(token, true) }
            if (device == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse(401, "No device has been found for this token."))
                return@get
            }

            val user = getUsers().find { it.GUID.equals(device.userID, true) }
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse(401, "No user relating to this device has been found."))
                return@get
            }

            val entries = getMediaEntries().filter { it.GUID.equals(params["media"], true) }
            if (entries.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, ApiResponse(404, "No media has been found for this ID."))
                return@get
            }

            val entry = File("E:\\Encoding Stuff\\# Doing\\Made in Abyss S2 (BD)\\premux\\Made in Abyss - S0201 v3 (premux).mkv")

            if (!entry.exists()) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse(500, "The file for this media could not be found."))
                return@get
            }
            call.customRespondFile(entry, device, user)
        } else
            call.respond(HttpStatusCode.BadRequest, ApiResponse(400, "No token was found in your request."))
    }
}

suspend fun ApplicationCall.customRespondFile(file: File, device: Device, user: User, configure: OutgoingContent.() -> Unit = {}) {
    val message = CustomLocalFileContent(file, device = device, user = user).apply(configure)
    respond(message)
}