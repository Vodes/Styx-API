package moe.styx.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import moe.styx.*

fun Route.mediaList() {
    post("/media/list") {
        val form = call.receiveParameters()
        val token = form["token"]
        if (!token.isNullOrBlank()) {
            if (!checkToken(token, call))
                return@post
            call.respond(HttpStatusCode.OK, getMedia())
        } else
            call.respond(HttpStatusCode.BadRequest, ApiResponse(HttpStatusCode.BadRequest.value, "No token was found in your request."))
    }
}

fun Route.mediaEntries() {
    post("/media/entries") {
        val form = call.receiveParameters()
        val token = form["token"]
        if (!token.isNullOrBlank()) {
            if (!checkToken(token, call))
                return@post
            call.respond(HttpStatusCode.OK, getMediaEntries())
        } else
            call.respond(HttpStatusCode.BadRequest, ApiResponse(HttpStatusCode.BadRequest.value, "No token was found in your request."))
    }
}

fun Route.images() {
    post("/media/images") {
        val form = call.receiveParameters()
        val token = form["token"]
        if (!token.isNullOrBlank()) {

            if (!checkToken(token, call))
                return@post

            call.respond(HttpStatusCode.OK, getAllImages())
        } else
            call.respond(HttpStatusCode.BadRequest, ApiResponse(HttpStatusCode.BadRequest.value, "No token was found in your request."))
    }
}

fun Route.schedules() {
    post("/media/schedules") {
        val form = call.receiveParameters()
        val token = form["token"]
        if (!token.isNullOrBlank()) {

            if (!checkToken(token, call))
                return@post

            call.respond(HttpStatusCode.OK, getAllMediaSchedules())
        } else
            call.respond(HttpStatusCode.BadRequest, ApiResponse(HttpStatusCode.BadRequest.value, "No token was found in your request."))
    }
}


fun Route.changes() {
    get("/changes") {
        call.respond(HttpStatusCode.OK, changes)
    }
}

fun Route.categories() {
    post("/media/categories") {
        val form = call.receiveParameters()
        val token = form["token"]
        if (!token.isNullOrBlank()) {
            if (!checkToken(token, call))
                return@post

            call.respond(HttpStatusCode.OK, getCategories())
        } else
            call.respond(HttpStatusCode.BadRequest, ApiResponse(HttpStatusCode.BadRequest.value, "No token was found in your request."))
    }
}

fun Route.favourites() {
    post("/favourites/list") {
        val form = call.receiveParameters()
        val token = form["token"]
        if (!token.isNullOrBlank()) {
            val device = getDevices().find { it.accessToken.equals(token, true) }
            if (device == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse(HttpStatusCode.Unauthorized.value, "No device has been found for this token."))
                return@post
            }

            val user = getUsers().find { it.GUID.equals(device.userID, true) }
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse(HttpStatusCode.Unauthorized.value, "No user relating to this device has been found."))
                return@post
            }

            call.respond(HttpStatusCode.OK, getFavourites().filter { it.userID.equals(user.GUID, true) })
        } else
            call.respond(HttpStatusCode.BadRequest, ApiResponse(HttpStatusCode.BadRequest.value, "No token was found in your request."))
    }
    post("/favourites/add/{media}") {
        val form = call.receiveParameters()
        val token = form["token"]

        if (call.parameters["media"].isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, ApiResponse(400, "No media ID was found in your request."))
            return@post
        }

        if (!token.isNullOrBlank()) {
            val device = getDevices().find { it.accessToken.equals(token, true) }
            if (device == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse(HttpStatusCode.Unauthorized.value, "No device has been found for this token."))
                return@post
            }

            val user = getUsers().find { it.GUID.equals(device.userID, true) }
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse(HttpStatusCode.Unauthorized.value, "No user relating to this device has been found."))
                return@post
            }

            val media = getMedia().find { it.GUID.equals(call.parameters["media"], true) }

            if (media == null) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse(404, "No media with that ID was found."))
                return@post
            }

            if (Favourite(media.GUID, user.GUID, Clock.System.now().epochSeconds).save()) {
                call.respond(HttpStatusCode.OK, ApiResponse(HttpStatusCode.OK.value, "Favourite added."))
            } else {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse(HttpStatusCode.InternalServerError.value, "Failed to add favourite."))
            }
        } else
            call.respond(HttpStatusCode.BadRequest, ApiResponse(HttpStatusCode.BadRequest.value, "No token was found in your request."))
    }

    post("/favourites/delete/{media}") {
        val form = call.receiveParameters()
        val token = form["token"]

        if (call.parameters["media"].isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, ApiResponse(400, "No media ID was found in your request."))
            return@post
        }

        if (!token.isNullOrBlank()) {
            val device = getDevices().find { it.accessToken.equals(token, true) }
            if (device == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse(HttpStatusCode.Unauthorized.value, "No device has been found for this token."))
                return@post
            }

            val user = getUsers().find { it.GUID.equals(device.userID, true) }
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse(HttpStatusCode.Unauthorized.value, "No user relating to this device has been found."))
                return@post
            }

            val media = getMedia().find { it.GUID.equals(call.parameters["media"], true) }

            if (media == null) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse(404, "No media with that ID was found."))
                return@post
            }

            if (Favourite(media.GUID, user.GUID, 0L).delete()) {
                call.respond(HttpStatusCode.OK, ApiResponse(HttpStatusCode.OK.value, "Favourite deleted."))
            } else {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse(HttpStatusCode.InternalServerError.value, "Failed to delete favourite."))
            }
        } else
            call.respond(HttpStatusCode.BadRequest, ApiResponse(HttpStatusCode.BadRequest.value, "No token was found in your request."))
    }
}

suspend fun checkToken(token: String, call: ApplicationCall): Boolean {
    val device = getDevices().find { it.accessToken.equals(token, true) }
    if (device == null) {
        call.respond(HttpStatusCode.Unauthorized, ApiResponse(HttpStatusCode.Unauthorized.value, "No device has been found for this token."))
        return false
    }
    val users = getUsers().filter { it.GUID.equals(device.userID, true) }
    if (users.isEmpty()) {
        call.respond(HttpStatusCode.Unauthorized, ApiResponse(HttpStatusCode.Unauthorized.value, "No user relating to this device has been found."))
        return false
    }
    return true
}