package moe.styx.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import moe.styx.config
import moe.styx.respondStyx
import java.io.File

fun Route.downloadVersions() {
    get("/download/desktop") {
        val params = call.request.queryParameters
        val token = params["token"]

        val (user, device) = checkTokenDeviceUser(token, call)
        if (device == null || user == null)
            return@get

        val buildDir = File(config.buildDir)
        if (config.buildDir.isBlank() || !buildDir.exists() || !buildDir.isDirectory) {
            call.respondStyx(HttpStatusCode.NotFound, "Could not find any builds on the server.")
            return@get
        }
        val latest = buildDir.listFiles()?.filter { it.isDirectory }?.maxBy { it.name }
        val winMsi = latest?.walkTopDown()?.find { it.name.endsWith(".msi") }
        if (winMsi == null || !winMsi.exists()) {
            call.respondStyx(HttpStatusCode.NotFound, "Could not find latest windows build on the server.")
            return@get
        }
        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, winMsi.name).toString()
        )
        call.respondFile(winMsi)
    }
}