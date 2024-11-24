package moe.styx.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import moe.styx.common.config.UnifiedConfig
import java.io.File

fun Route.mpvRoute() {
    get("/mpv") {
        val latest = latestMpvBundle()
        if (latest == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        call.respond(HttpStatusCode.OK, latest.name)
    }

    get("/mpv/download") {
        val latest = latestMpvBundle()
        if (latest == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        call.respondFile(latest)
    }
}

private fun latestMpvBundle(): File? {
    val folder = File(UnifiedConfig.current.base.mpvDir())
    val list = folder.listFiles()?.sortedBy { it.name }
    if (!folder.exists() || !folder.isDirectory || list.isNullOrEmpty()) {
        return null
    }
    return list.last()
}