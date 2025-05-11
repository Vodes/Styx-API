package moe.styx.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import moe.styx.common.data.*
import moe.styx.common.extension.toBoolean
import moe.styx.common.json
import moe.styx.db.tables.*
import moe.styx.respondStyx
import moe.styx.transaction
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import java.io.File

fun Route.media() {
    post("/media/list") {
        val form = call.receiveParameters()
        val token = form["token"]
        if (!checkToken(token, call)) return@post

        call.respond(HttpStatusCode.OK, transaction { MediaTable.query { selectAll().toList() } })
    }

    post("/media/entries") {
        val form = call.receiveParameters()
        val token = form["token"]
        if (!checkToken(token, call)) return@post

        call.respond(HttpStatusCode.OK, transaction { MediaEntryTable.query { selectAll().toList() } })
    }

    post("/media/info") {
        val form = call.receiveParameters()
        val token = form["token"]
        if (!checkToken(token, call)) return@post

        call.respond(HttpStatusCode.OK, transaction { MediaInfoTable.query { selectAll().toList() } })
    }

    post("/media/images") {
        val form = call.receiveParameters()
        val token = form["token"]
        if (!checkToken(token, call)) return@post

        call.respond(HttpStatusCode.OK, transaction { ImageTable.query { selectAll().toList() } })
    }

    post("/media/schedules") {
        val form = call.receiveParameters()
        val token = form["token"]
        if (!checkToken(token, call)) return@post

        call.respond(HttpStatusCode.OK, transaction { MediaScheduleTable.query { selectAll().toList() } })
    }

    post("/media/categories") {
        val form = call.receiveParameters()
        val token = form["token"]
        if (!checkToken(token, call)) return@post

        call.respond(
            HttpStatusCode.OK,
            transaction { CategoryTable.query { selectAll().orderBy(isSeries to SortOrder.DESC, sort to SortOrder.DESC).toList() } })
    }
}

fun Route.changes() {
    get("/changes") {
        call.respond(HttpStatusCode.OK, transaction { ChangesTable.getCurrent() } ?: Changes(0, 0))
    }
}

fun Route.favourites() {
    post("/favourites/list") {
        val form = call.receiveParameters()
        val token = form["token"]
        val user = checkTokenUser(token, call) ?: return@post
        call.respond(HttpStatusCode.OK, transaction { FavouriteTable.query { selectAll().where { userID eq user.GUID }.toList() } })
    }

    post("/favourites/add") {
        val form = call.receiveParameters()
        val token = form["token"]

        val user = checkTokenUser(token, call) ?: return@post
        val fav = call.receiveGenericContent<Favourite>(form) ?: return@post

        val result = transaction {
            FavouriteTable.upsertItem(fav.copy(userID = user.GUID))
        }.insertedCount.toBoolean()

        if (result) {
            call.respondStyx(HttpStatusCode.OK, "Favourite added.")
        } else {
            call.respondStyx(HttpStatusCode.InternalServerError, "Failed to add favourite.")
        }
    }

    post("/favourites/delete") {
        val form = call.receiveParameters()
        val token = form["token"]

        val user = checkTokenUser(token, call) ?: return@post
        val fav = call.receiveGenericContent<Favourite>(form) ?: return@post

        val result = transaction { FavouriteTable.deleteWhere { userID eq user.GUID and (mediaID eq fav.mediaID) } }.toBoolean()

        if (result) {
            call.respondStyx(HttpStatusCode.OK, "Favourite deleted.")
        } else {
            call.respondStyx(HttpStatusCode.InternalServerError, "Failed to delete favourite.")
        }
    }

    post("/favourites/sync") {
        val form = call.receiveParameters()
        val token = form["token"]
        val user = checkTokenUser(token, call) ?: return@post
        val queuedChanges = call.receiveGenericContent<QueuedFavChanges>(form) ?: return@post
        transaction {
            queuedChanges.toAdd.forEach { FavouriteTable.upsertItem(it.copy(userID = user.GUID)) }
            queuedChanges.toRemove.forEach { fav -> FavouriteTable.deleteWhere { userID eq user.GUID and (mediaID eq fav.mediaID) } }
        }
        call.respondStyx(HttpStatusCode.OK, "")
    }
}

fun Route.watched() {
    post("/watched/list") {
        val form = call.receiveParameters()
        val token = form["token"]
        val user = checkTokenUser(token, call) ?: return@post
        call.respond(HttpStatusCode.OK, transaction { MediaWatchedTable.query { selectAll().where { userID eq user.GUID }.toList() } })
    }

    post("/watched/add") {
        val form = call.receiveParameters()
        val token = form["token"]
        val user = checkTokenUser(token, call) ?: return@post
        val mediaWatched = call.receiveGenericContent<MediaWatched>(form) ?: return@post
        val existing =
            transaction {
                MediaWatchedTable.query { selectAll().where { userID eq user.GUID and (entryID eq mediaWatched.entryID) }.toList() }.firstOrNull()
            }
        val existingProgress = existing?.maxProgress ?: 0F
        val trans = transaction {
            MediaWatchedTable.upsertItem(
                mediaWatched.copy(
                    userID = user.GUID,
                    maxProgress = if (existingProgress > mediaWatched.maxProgress) existingProgress else mediaWatched.maxProgress
                )
            )
        }
        if (trans.insertedCount.toBoolean())
            call.respondStyx(HttpStatusCode.OK, "")
        else
            call.respondStyx(HttpStatusCode.InternalServerError, "Failed to save watched entry!")
    }
    post("/watched/delete") {
        val form = call.receiveParameters()
        val token = form["token"]
        val user = checkTokenUser(token, call) ?: return@post
        val mediaWatched = call.receiveGenericContent<MediaWatched>(form) ?: return@post

        if (transaction { MediaWatchedTable.deleteWhere { userID eq user.GUID and (entryID eq mediaWatched.entryID) } }.toBoolean())
            call.respondStyx(HttpStatusCode.OK, "")
        else
            call.respondStyx(HttpStatusCode.InternalServerError, "Failed to delete watched entry!")
    }
    post("/watched/sync") {
        val form = call.receiveParameters()
        val token = form["token"]
        val user = checkTokenUser(token, call) ?: return@post
        val queuedChanges = call.receiveGenericContent<QueuedWatchedChanges>(form) ?: return@post

        queuedChanges.toUpdate.forEach { watched ->
            val existing =
                transaction {
                    MediaWatchedTable.query { selectAll().where { userID eq user.GUID and (entryID eq watched.entryID) }.toList() }.firstOrNull()
                }
            val existingProgress = existing?.maxProgress ?: 0F
            transaction {
                MediaWatchedTable.upsertItem(
                    watched.copy(
                        userID = user.GUID,
                        maxProgress = if (existingProgress > watched.maxProgress) existingProgress else watched.maxProgress
                    )
                )
            }
        }
        transaction {
            queuedChanges.toRemove.forEach { mediaWatched -> MediaWatchedTable.deleteWhere { userID eq user.GUID and (entryID eq mediaWatched.entryID) } }
        }
        call.respondStyx(HttpStatusCode.OK, "")
    }
}

fun Route.userMediaPrefs() {
    post("/media/preferences/update") {
        val form = call.receiveParameters()
        val token = form["token"]

        val user = checkTokenUser(token, call) ?: return@post
        val prefs = call.receiveGenericContent<UserMediaPreferences>(form) ?: return@post

        val result = transaction {
            UserMediaPreferencesTable.upsertItem(prefs.copy(userID = user.GUID))
        }.insertedCount.toBoolean()

        if (result) {
            call.respondStyx(HttpStatusCode.OK, "Preferences added.")
        } else {
            call.respondStyx(HttpStatusCode.InternalServerError, "Failed to save/update preferences.")
        }
    }
    post("/media/preferences/list") {
        val form = call.receiveParameters()
        val token = form["token"]
        val user = checkTokenUser(token, call) ?: return@post
        call.respond(HttpStatusCode.OK, transaction { UserMediaPreferencesTable.query { selectAll().where { userID eq user.GUID }.toList() } })
    }
}

suspend inline fun <reified T> ApplicationCall.receiveGenericContent(form: Parameters, formParam: String = "content"): T? {
    return runCatching {
        json.decodeFromString<T>(form[formParam]!!)
    }.onFailure {
        respondStyx(HttpStatusCode.BadRequest, "Could not find valid form entry for the '${T::class.simpleName}' type.")
    }.getOrNull()
}

suspend fun checkMedia(id: String?, call: ApplicationCall): Media? {
    if (id.isNullOrBlank()) {
        call.respondStyx(HttpStatusCode.BadRequest, "No media ID was found in your request.")
        return null
    }
    val media = transaction { MediaTable.query { selectAll().where { GUID eq id }.toList() }.firstOrNull() }

    if (media == null)
        call.respondStyx(HttpStatusCode.NotFound, "No media with that ID was found.")

    return media
}

suspend fun checkMediaEntry(id: String?, call: ApplicationCall): MediaEntry? {
    if (id.isNullOrBlank()) {
        call.respondStyx(HttpStatusCode.BadRequest, "No entry ID was found in your request.")
        return null
    }
    var entry = transaction { MediaEntryTable.query { selectAll().where { GUID eq id }.toList() }.firstOrNull() }

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
    if (token.isNullOrBlank())
        call.respondStyx(HttpStatusCode.BadRequest, "No token was found in your request.").also { return null to null }
    val device = transaction { DeviceTable.query { selectAll().toList() } }.find {
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
            return null to null
        }

    val user = transaction { UserTable.query { selectAll().where { GUID eq device!!.userID }.toList() } }.firstOrNull()
    if (user == null)
        call.respondStyx(HttpStatusCode.Unauthorized, "No user relating to this device has been found.")

    if ((user?.permissions ?: 0) < 0) {
        call.respondStyx(HttpStatusCode.Forbidden, "You are banned.")
        return null to null
    }

    return user to device
}
