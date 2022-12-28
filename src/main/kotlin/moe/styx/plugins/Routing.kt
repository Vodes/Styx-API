package moe.styx.plugins

import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.resources.Resources
import io.ktor.resources.*
import kotlinx.serialization.Serializable
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*

fun Application.configureRouting() {
    install(Resources)

    routing {
        get("/") {
            call.respondText("Hello World!")
        }
    }
}

@Serializable
@Resource("/articles")
class Articles(val sort: String? = "new")
