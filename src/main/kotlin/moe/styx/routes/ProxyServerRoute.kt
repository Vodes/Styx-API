package moe.styx.routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import moe.styx.db.tables.ProxyServerTable
import moe.styx.transaction
import org.jetbrains.exposed.sql.selectAll

fun Route.proxyServers() {
    post("/proxy-servers") {
        val form = call.receiveParameters()
        val token = form["token"]
        val (user, device) = checkTokenDeviceUser(token, call)
        if (device == null || user == null)
            return@post
        call.respond(HttpStatusCode.OK, transaction { ProxyServerTable.query { selectAll().toList() } })
    }
}