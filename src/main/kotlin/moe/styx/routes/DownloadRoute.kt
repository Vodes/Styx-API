package moe.styx.routes

import io.github.z4kn4fein.semver.toVersion
import io.github.z4kn4fein.semver.toVersionOrNull
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import moe.styx.common.extension.eqI
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

        val winMsi = getBuildForPlatform("win", call = call) ?: return@get
        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, winMsi.name).toString()
        )
        call.response.header("Build-Filename", winMsi.name)
        call.respondFile(winMsi)
    }
}

fun Route.download() {
    get("/download/{platform}/{version?}") {
        val variables = call.request.pathVariables

        val (user, device) = checkTokenDeviceUser(call.request.queryParameters["token"], call)
        if (device == null || user == null)
            return@get

        val buildFile = getBuildForPlatform(variables["platform"]!!, variables["version"], call) ?: return@get
        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, buildFile.name)
                .toString()
        )
        call.response.header("Build-Filename", buildFile.name)
        call.respondFile(buildFile)
    }
}

private suspend fun getBuildForPlatform(platform: String, version: String? = null, call: ApplicationCall): File? {
    val buildDir = File(if (platform.contains("android", true)) config.androidBuildDir else config.buildDir)
    if (!buildDir.exists()) {
        call.respondStyx(HttpStatusCode.NotFound, "Could not find any builds on the server.")
        return null
    }
    val requestedVersion = version?.toVersionOrNull(false)

    val versionDir = if (requestedVersion != null) buildDir.listFiles()
        ?.find { it.isDirectory && it.name.toVersionOrNull(false) == requestedVersion }
    else buildDir.listFiles()?.filter { it.isDirectory && it.name.toVersionOrNull(false)?.isPreRelease == false }
        ?.maxBy { it.name.toVersionOrNull(false) ?: "0.0.0".toVersion() }


    if (versionDir == null) {
        call.respondStyx(
            HttpStatusCode.NotFound,
            "Could not find ${if (version.isNullOrBlank()) "latest" else "requested version of"} windows build on the server."
        )
        return null
    }
    when (platform.lowercase()) {
        "win", "windows" -> {
            return versionDir.walkTopDown().find { it.isFile && it.extension eqI "msi" }
                .also { if (it == null) call.respond(HttpStatusCode.NotFound) }
        }

        "rpm", "fedora" -> {
            return versionDir.walkTopDown().find { it.isFile && it.extension eqI "rpm" }
                .also { if (it == null) call.respond(HttpStatusCode.NotFound) }
        }

        "deb", "ubuntu", "debian" -> {
            return versionDir.walkTopDown().find { it.isFile && it.extension eqI "deb" }
                .also { if (it == null) call.respond(HttpStatusCode.NotFound) }
        }

        "android-arm64" -> {
            return versionDir.walkTopDown().find { it.isFile && it.extension eqI "apk" && it.name.contains("arm64") }
                .also { if (it == null) call.respond(HttpStatusCode.NotFound) }
        }

        "android-universal" -> {
            return versionDir.walkTopDown()
                .find { it.isFile && it.extension eqI "apk" && it.name.contains("universal") }
                .also { if (it == null) call.respond(HttpStatusCode.NotFound) }
        }
    }
    call.respondStyx(HttpStatusCode.BadRequest, "Invalid platform requested.")
    return null
}