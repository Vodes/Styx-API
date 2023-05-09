package moe.styx.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import moe.styx.*

fun Route.mediaList() {
    post("/media/list") {
        val form = call.receiveParameters()
        val token = form["token"]
        if (!checkToken(token, call)) return@post

        call.respond(HttpStatusCode.OK, getMedia())
    }
}

fun Route.mediaEntries() {
    post("/media/entries") {
        val form = call.receiveParameters()
        val token = form["token"]
        if (!checkToken(token, call)) return@post

        call.respond(HttpStatusCode.OK, getMediaEntries())
    }
}

fun Route.images() {
    post("/media/images") {
        val form = call.receiveParameters()
        val token = form["token"]
        if (!checkToken(token, call)) return@post

        call.respond(HttpStatusCode.OK, getAllImages())
    }
}

fun Route.schedules() {
    post("/media/schedules") {
        val form = call.receiveParameters()
        val token = form["token"]
        if (!checkToken(token, call)) return@post

        call.respond(HttpStatusCode.OK, getAllMediaSchedules())
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
        if (!checkToken(token, call)) return@post

        call.respond(HttpStatusCode.OK, getCategories())
    }
}

fun Route.favourites() {
    post("/favourites/list") {
        val form = call.receiveParameters()
        val token = form["token"]
        val user = checkTokenUser(token, call) ?: return@post
        call.respond(HttpStatusCode.OK, getFavourites().filter { it.userID.equals(user.GUID, true) })
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

    post("/favourites/sync") {
        val form = call.receiveParameters()
        val token = form["token"]
        val user = checkTokenUser(token, call) ?: return@post

        val content = form["content"]
        if (content.isNullOrBlank()) {
            call.respondStyx(HttpStatusCode.BadRequest, "No content entry was found in the form.")
            return@post
        }
        try {
            val favourites: List<Favourite> = json.decodeFromString(content)
            val current = getFavourites().filter { it.userID.equals(user.GUID, true) }
            current.forEach { it.delete() }
            favourites.forEach {
                it.userID = user.GUID
                if (it.added <= 0)
                    it.added = Clock.System.now().epochSeconds
                it.save()
            }
        } catch (decodeEx: SerializationException) {
            call.respondStyx(HttpStatusCode.BadRequest, "Invalid json content for this endpoint was received.")
            decodeEx.printStackTrace()
        } catch (ex: Exception) {
            call.respondStyx(HttpStatusCode.InternalServerError, "An Error occurred on the server side.\nPlease contact an admin.")
            ex.printStackTrace()
        }
    }
}

suspend fun checkToken(token: String?, call: ApplicationCall): Boolean {
    return checkTokenUser(token, call) != null
}

suspend fun checkTokenUser(token: String?, call: ApplicationCall): User? {
    if (token == null) {
        call.respondStyx(HttpStatusCode.BadRequest, "No token was found in your request.")
        return null
    }

    val device = getDevices().find { it.accessToken.equals(token, true) }
    if (device == null) {
        call.respondStyx(HttpStatusCode.Unauthorized, "No device has been found for this token.")
        return null
    }

    val user = getUsers().find { it.GUID.equals(device.userID, true) }
    if (user == null) {
        call.respondStyx(HttpStatusCode.Unauthorized, "No user relating to this device has been found.")
        return null
    }
    return user
}