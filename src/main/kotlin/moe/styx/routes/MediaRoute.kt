package moe.styx.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.serialization.SerializationException
import moe.styx.changes
import moe.styx.db.*
import moe.styx.getDBClient
import moe.styx.respondStyx
import moe.styx.types.*
import java.io.File

fun Route.mediaList() {
    post("/media/list") {
        val form = call.receiveParameters()
        val token = form["token"]
        if (!checkToken(token, call)) return@post

        call.respond(HttpStatusCode.OK, getDBClient().executeGet { getMedia() })
    }
}

fun Route.mediaEntries() {
    post("/media/entries") {
        val form = call.receiveParameters()
        val token = form["token"]
        if (!checkToken(token, call)) return@post
        
        call.respond(HttpStatusCode.OK, getDBClient().executeGet { getEntries() })
    }
}

fun Route.images() {
    post("/media/images") {
        val form = call.receiveParameters()
        val token = form["token"]
        if (!checkToken(token, call)) return@post

        call.respond(HttpStatusCode.OK, getDBClient().executeGet { getImages() })
    }
}

fun Route.schedules() {
    post("/media/schedules") {
        val form = call.receiveParameters()
        val token = form["token"]
        if (!checkToken(token, call)) return@post

        call.respond(HttpStatusCode.OK, getDBClient().executeGet { getSchedules() })
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

        call.respond(HttpStatusCode.OK, getDBClient().executeGet { getCategories() })
    }
}

fun Route.favourites() {
    post("/favourites/list") {
        val form = call.receiveParameters()
        val token = form["token"]
        val user = checkTokenUser(token, call) ?: return@post
        call.respond(HttpStatusCode.OK, getDBClient().executeGet { getFavourites() }.filter { it.userID.equals(user.GUID, true) })
    }

    post("/favourites/add/{media}") {
        val form = call.receiveParameters()
        val token = form["token"]

        val user = checkTokenUser(token, call) ?: return@post
        val media = checkMedia(call.parameters["media"], call) ?: return@post
        val favourite = Favourite(media.GUID, user.GUID, Clock.System.now().epochSeconds)
        if (getDBClient().executeGet { save(favourite) }) {
            call.respondStyx(HttpStatusCode.OK, "Favourite added.")
        } else {
            call.respondStyx(HttpStatusCode.InternalServerError, "Failed to add favourite.")
        }
    }

    post("/favourites/delete/{media}") {
        val form = call.receiveParameters()
        val token = form["token"]

        val user = checkTokenUser(token, call) ?: return@post
        val media = checkMedia(call.parameters["media"], call) ?: return@post

        if (getDBClient().executeGet { delete(Favourite(media.GUID, user.GUID, 0L)) }) {
            call.respondStyx(HttpStatusCode.OK, "Favourite deleted.")
        } else {
            call.respondStyx(HttpStatusCode.InternalServerError, "Failed to delete favourite.")
        }
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
        val dbClient = getDBClient()
        try {
            val favourites: List<Favourite> = json.decodeFromString(content)
            val current = dbClient.getFavourites().filter { it.userID.equals(user.GUID, true) }
            current.forEach { dbClient.delete(it) }
            favourites.forEach {
                it.userID = user.GUID
                if (it.added <= 0)
                    it.added = Clock.System.now().epochSeconds
                dbClient.save(it)
            }
        } catch (decodeEx: SerializationException) {
            call.respondStyx(HttpStatusCode.BadRequest, "Invalid json content for this endpoint was received.")
            decodeEx.printStackTrace()
        } catch (ex: Exception) {
            call.respondStyx(HttpStatusCode.InternalServerError, "An Error occurred on the server side.\nPlease contact an admin.")
            ex.printStackTrace()
        } finally {
            dbClient.closeConnection()
        }
    }
}

suspend fun checkMedia(id: String?, call: ApplicationCall): Media? {
    if (id.isNullOrBlank()) {
        call.respondStyx(HttpStatusCode.BadRequest, "No media ID was found in your request.")
        return null
    }
    val media = getDBClient().executeGet { getMedia(mapOf("GUID" to id)).firstOrNull() }

    if (media == null)
        call.respondStyx(HttpStatusCode.NotFound, "No media with that ID was found.")

    return media
}

suspend fun checkMediaEntry(id: String?, call: ApplicationCall): MediaEntry? {
    if (id.isNullOrBlank()) {
        call.respondStyx(HttpStatusCode.BadRequest, "No entry ID was found in your request.")
        return null
    }
    val entry = getDBClient().executeGet { getEntries(mapOf("GUID" to id)).firstOrNull() }

    if (entry == null)
        call.respondStyx(HttpStatusCode.NotFound, "No entry with that ID was found.")

    if (entry != null) {
        val entryFile = File(
            if (System.getProperty("os.name").contains("win", true))
                "E:\\Encoding Stuff\\# Doing\\KKS\\premux\\Kekkai Sensen - S01E02 (premux) [BC46BD78].mkv"
            else
                entry.filePath
        )
        if (!entryFile.exists()) {
            call.respondStyx(HttpStatusCode.InternalServerError, "The file for this media entry was not found.")
            return null
        }
    }
    return entry
}

suspend fun checkToken(token: String?, call: ApplicationCall): Boolean {
    return checkTokenUser(token, call) != null
}

suspend fun checkTokenUser(token: String?, call: ApplicationCall): User? {
    return checkTokenDeviceUser(token, call).first
}

suspend fun checkTokenDeviceUser(token: String?, call: ApplicationCall, login: Boolean = false): Pair<User?, Device?> {
    if (token == null)
        call.respondStyx(HttpStatusCode.BadRequest, "No token was found in your request.").also { return Pair(null, null) }
    val dbClient = getDBClient()
    val device = dbClient.getDevices().find { if (!login) it.accessToken.equals(token, true) else it.refreshToken.equals(token, true) }
    if (device == null)
        call.respondStyx(HttpStatusCode.Unauthorized, "No device has been found for this token.").also { return Pair(null, null) }

    val user = dbClient.getUsers().find { it.GUID.equals(device!!.userID, true) }
    if (user == null)
        call.respondStyx(HttpStatusCode.Unauthorized, "No user relating to this device has been found.")

    dbClient.closeConnection()
    return Pair(user, device)
}
