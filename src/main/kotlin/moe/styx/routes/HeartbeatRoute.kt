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
import moe.styx.db.tables.ActiveUserTable
import moe.styx.respondStyx
import moe.styx.transaction
import org.jetbrains.exposed.sql.selectAll

fun Route.heartbeat() {
    post("/heartbeat") {
        val form = call.receiveParameters()
        val token = form["token"]

        val clientHeartbeat = call.receiveGenericContent<ClientHeartbeat>(form) ?: return@post
        val (user, device) = checkTokenDeviceUser(token, call)
        if (user == null || device == null)
            return@post

        val activeUsers = transaction {
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
            ActiveUserTable.upsertItem(thisUser)
            ActiveUserTable.query { selectAll().toList() }.filter { (it.lastPing ?: -1) > (now - 45) }
        }
        call.respondStyx(HttpStatusCode.OK, json.encodeToString(activeUsers), true)
    }
}