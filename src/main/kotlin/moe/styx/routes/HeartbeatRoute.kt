package moe.styx.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import moe.styx.common.data.ActiveUser
import moe.styx.common.data.ClientHeartbeat
import moe.styx.common.json
import moe.styx.db.getActiveUsers
import moe.styx.db.save
import moe.styx.getDBClient
import moe.styx.respondStyx

fun Route.heartbeat() {
    post("/heartbeat") {
        val form = call.receiveParameters()
        val token = form["token"]

        val clientHeartbeat = call.receiveGenericContent<ClientHeartbeat>(form) ?: return@post
        val (user, device) = checkTokenDeviceUser(token, call)
        if (user == null || device == null)
            return@post

        val activeUsers = getDBClient().executeGet {
            val now = Clock.System.now().epochSeconds
            val thisUser =
                ActiveUser(
                    user,
                    device.GUID,
                    device.deviceInfo.type,
                    now,
                    clientHeartbeat.mediaActivity?.takeIf { it.mediaEntry.isNotBlank() },
                    clientHeartbeat.listeningTo
                )
            save(thisUser)
            return@executeGet getActiveUsers().filter { (it.lastPing ?: -1) > (now - 60) }
        }
        call.respondStyx(HttpStatusCode.OK, json.encodeToString(activeUsers), true)
    }
}