package moe.styx.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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