package moe.styx.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import moe.styx.respondStyx
import moe.styx.types.ClientHeartbeat
import moe.styx.types.json

fun Route.heartbeat() {
    post("/heartbeat") {
        val form = call.receiveParameters()
        val token = form["token"]
        val clientHeartbeat = runCatching { json.decodeFromString<ClientHeartbeat>(form["content"]!!) }.getOrNull()
        if (clientHeartbeat == null) {
            call.respondStyx(HttpStatusCode.BadRequest, "No heartbeat was found in your request.")
            return@post
        }
        val (user, device) = checkTokenDeviceUser(token, call)
        if (user == null || device == null)
            return@post
        // TODO: Respond with active users
        call.respondStyx(HttpStatusCode.OK, json.encodeToString(emptyList<String>()), true)
    }
}