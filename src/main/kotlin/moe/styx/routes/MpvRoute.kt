package moe.styx.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import moe.styx.config
import java.io.File

fun Route.mpvRoute() {
    get("/mpv") {
        val folder = File(config.mpvFolder)
        val list = folder.listFiles()?.sortedBy { it.name }
        if (!folder.exists() || !folder.isDirectory || list.isNullOrEmpty()) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        call.respond(HttpStatusCode.OK, list.last().name)
    }

    get("/mpv/download") {
        val folder = File(config.mpvFolder)
        val list = folder.listFiles()?.sortedBy { it.name }
        if (!folder.exists() || !folder.isDirectory || list.isNullOrEmpty()) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        call.respondFile(list.last())
    }
}