package moe.styx.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.date.*
import moe.styx.config
import moe.styx.misc.DiscordAPI

fun Route.discordAuth() {
    get("/discord/auth") {
        val params = call.request.queryParameters
        val code = params["code"]
        if (code.isNullOrBlank()) {
            call.respondRedirect(DiscordAPI.buildAuthURL())
            return@get
        }
        DiscordAPI.handleIncomingCode(code, call)
    }
    get("/discord/logout") {
        call.response.cookies.append(
            Cookie(
                "access_token",
                "",
                maxAge = 0,
                expires = GMTDate.START,
                domain = ".styx.moe",
                path = "/",
                extensions = mapOf("SameSite" to "lax")
            )
        )
        call.respondRedirect(config.mainSiteBaseURL)
    }
}