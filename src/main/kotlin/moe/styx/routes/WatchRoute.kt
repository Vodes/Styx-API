package moe.styx.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import moe.styx.ApiResponse
import moe.styx.getDevices
import moe.styx.getMediaEntries
import moe.styx.getUsers
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

            val users = getUsers().filter { it.GUID.equals(device.userID, true) }
            if (users.isEmpty()) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse(401, "No user relating to this device has been found."))
                return@get
            }

            val entries = getMediaEntries().filter { it.GUID.equals(params["media"], true) }
            if (entries.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, ApiResponse(404, "No media has been found for this ID."))
                return@get
            }

            val entry = File(entries[0].filePath)

            if (!entry.exists()) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse(500, "The file for this media could not be found."))
                return@get
            }

            call.respondFile(entry)
        } else
            call.respond(HttpStatusCode.BadRequest, ApiResponse(400, "No token was found in your request."))
    }
}