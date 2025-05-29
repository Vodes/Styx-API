package moe.styx.routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import moe.styx.common.extension.currentUnixSeconds
import moe.styx.db.tables.UserTable
import moe.styx.misc.NoRefreshMALClient
import moe.styx.respondStyx
import moe.styx.transaction
import kotlin.time.Duration.Companion.minutes

fun Route.userMALRoutes() {
    post("/mal/fetch-token") {
        val form = call.receiveParameters()
        val token = form["token"]
        val (user, device) = checkTokenDeviceUser(token, call)
        if (device == null || user == null)
            return@post

        if (user.malData == null) {
            call.respondStyx(HttpStatusCode.NotFound, "User does not have a MyAnimeList connection.")
            return@post
        }
        val now = currentUnixSeconds()
        val halfHourDuration = 30.minutes.inWholeSeconds
        if (user.malData!!.accessTokenExpiry > (now + halfHourDuration)) {
            call.respond(HttpStatusCode.OK, user.malData!!)
            return@post
        }
        val newData = NoRefreshMALClient.refreshTokenForUser(user)
        if (newData == null) {
            call.respondStyx(HttpStatusCode.InternalServerError, "Failed to refresh access tokens!")
            return@post
        }
        transaction {
            UserTable.upsertItem(user.copy(malData = newData))
        }
        call.respond(HttpStatusCode.OK, newData)
    }
}