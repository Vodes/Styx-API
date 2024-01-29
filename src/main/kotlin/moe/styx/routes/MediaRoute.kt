package moe.styx.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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
        call.respond(HttpStatusCode.OK, getDBClient().executeGet { getFavourites(mapOf("userID" to user.GUID)) })
    }

    post("/favourites/add") {
        val form = call.receiveParameters()
        val token = form["token"]

        val user = checkTokenUser(token, call) ?: return@post
        val fav = call.receiveGenericContent<Favourite>() ?: return@post
        
        if (getDBClient().executeGet { save(fav.copy(userID = user.GUID)) }) {
            call.respondStyx(HttpStatusCode.OK, "Favourite added.")
        } else {
            call.respondStyx(HttpStatusCode.InternalServerError, "Failed to add favourite.")
        }
    }

    post("/favourites/delete") {
        val form = call.receiveParameters()
        val token = form["token"]

        val user = checkTokenUser(token, call) ?: return@post
        val fav = call.receiveGenericContent<Favourite>() ?: return@post

        if (getDBClient().executeGet { delete(fav.copy(userID = user.GUID)) }) {
            call.respondStyx(HttpStatusCode.OK, "Favourite deleted.")
        } else {
            call.respondStyx(HttpStatusCode.InternalServerError, "Failed to delete favourite.")
        }
    }

    post("/favourites/sync") {
        val form = call.receiveParameters()
        val token = form["token"]
        val user = checkTokenUser(token, call) ?: return@post
        val queuedChanges = call.receiveGenericContent<QueuedFavChanges>() ?: return@post
        getDBClient().executeAndClose {
            queuedChanges.toAdd.forEach { save(it.copy(userID = user.GUID)) }
            queuedChanges.toRemove.forEach { delete(it.copy(userID = user.GUID)) }
        }
        call.respondStyx(HttpStatusCode.OK, "")
    }
}

fun Route.watched() {
    post("/watched/list") {
        val form = call.receiveParameters()
        val token = form["token"]
        val user = checkTokenUser(token, call) ?: return@post
        call.respond(HttpStatusCode.OK, getDBClient().executeGet { getMediaWatched(mapOf("userID" to user.GUID)) })
    }

    post("/watched/add") {
        val form = call.receiveParameters()
        val token = form["token"]
        val user = checkTokenUser(token, call) ?: return@post
        val mediaWatched = call.receiveGenericContent<MediaWatched>() ?: return@post
        if (getDBClient().executeGet {
                val existing = getMediaWatched(mapOf("entryID" to mediaWatched.entryID, "userID" to user.GUID)).firstOrNull()
                val existingProgress = existing?.maxProgress ?: 0F
                return@executeGet save(
                    mediaWatched.copy(
                        userID = user.GUID,
                        maxProgress = if (existingProgress > mediaWatched.maxProgress) existingProgress else mediaWatched.maxProgress
                    )
                )
            })
            call.respond(HttpStatusCode.OK)
        else
            call.respondStyx(HttpStatusCode.InternalServerError, "Failed to save watched entry!")
    }
    post("/watched/delete") {
        val form = call.receiveParameters()
        val token = form["token"]
        val user = checkTokenUser(token, call) ?: return@post
        val mediaWatched = call.receiveGenericContent<MediaWatched>() ?: return@post
        if (getDBClient().executeGet { delete(mediaWatched.copy(userID = user.GUID)) })
            call.respond(HttpStatusCode.OK)
        else
            call.respondStyx(HttpStatusCode.InternalServerError, "Failed to delete watched entry!")
    }
    post("/watched/sync") {
        val form = call.receiveParameters()
        val token = form["token"]
        val user = checkTokenUser(token, call) ?: return@post
        val queuedChanges = call.receiveGenericContent<QueuedWatchedChanges>() ?: return@post
        getDBClient().executeAndClose {
            queuedChanges.toUpdate.forEach {
                var entry = it.copy(userID = user.GUID)
                val existing = getMediaWatched(mapOf("entryID" to entry.entryID, "userID" to entry.userID)).firstOrNull()
                if (existing != null && existing.maxProgress > it.maxProgress)
                    entry = it.copy(maxProgress = existing.maxProgress)
                save(entry)
            }
            queuedChanges.toRemove.forEach {
                val entry = it.copy(userID = user.GUID)
                delete(entry)
            }
        }
        call.respondStyx(HttpStatusCode.OK, "")
    }
}

suspend inline fun <reified T> ApplicationCall.receiveGenericContent(): T? {
    return runCatching {
        json.decodeFromString<T>(receiveParameters()["content"]!!)
    }.onFailure {
        respondStyx(HttpStatusCode.BadRequest, "Could not find valid form entry for the '${T::class.simpleName}' type.")
    }.getOrNull()
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
    var entry = getDBClient().executeGet { getEntries(mapOf("GUID" to id)).firstOrNull() }

    if (entry == null)
        call.respondStyx(HttpStatusCode.NotFound, "No entry with that ID was found.")

    if (entry != null) {
        if (System.getProperty("os.name").contains("win", true))
            entry = entry.copy(filePath = "D:\\Compings\\Jujutsu Kaisen 0 (2021).mkv")
        val entryFile = File(entry.filePath)
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

suspend fun checkTokenDeviceUser(token: String?, call: ApplicationCall, login: Boolean = false, watch: Boolean = false): Pair<User?, Device?> {
    if (token == null)
        call.respondStyx(HttpStatusCode.BadRequest, "No token was found in your request.").also { return Pair(null, null) }
    val dbClient = getDBClient()
    val device = dbClient.getDevices().find {
        if (!login)
            if (watch)
                it.watchToken.equals(token, true)
            else
                it.accessToken.equals(token, true)
        else
            it.refreshToken.equals(token, true)
    }
    if (device == null)
        call.respondStyx(HttpStatusCode.Unauthorized, "No device has been found for this token.").also {
            dbClient.closeConnection()
            return Pair(null, null)
        }

    val user = dbClient.getUsers().find { it.GUID.equals(device!!.userID, true) }
    if (user == null)
        call.respondStyx(HttpStatusCode.Unauthorized, "No user relating to this device has been found.")

    dbClient.closeConnection()
    return Pair(user, device)
}
