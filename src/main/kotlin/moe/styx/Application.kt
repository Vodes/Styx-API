package moe.styx

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import moe.styx.common.config.UnifiedConfig
import moe.styx.common.json
import moe.styx.db.DBClient
import moe.styx.misc.startParsing
import moe.styx.routes.*
import moe.styx.tasks.startTasks
import java.io.File

var secretsFile: File = File("SECRETS")

val dbClient by lazy {
    DBClient(
        "jdbc:postgresql://${UnifiedConfig.current.dbConfig.host()}/Styx",
        "org.postgresql.Driver",
        UnifiedConfig.current.dbConfig.user(),
        UnifiedConfig.current.dbConfig.pass(),
        25
    )
}

fun loadDBConfig() {
    secretsFile = File(UnifiedConfig.configFile.parentFile, "SECRETS")
    if (!secretsFile.exists() || secretsFile.readText().isBlank()) {
        println("Make sure you have a secrets file in your API directory.\nThis should contain all valid app-secrets for auth. Separated by a newline.")
    }

    dbClient.transaction { dbClient.createTables() }
}

fun <T> transaction(block: () -> T): T =
    org.jetbrains.exposed.sql.transactions.transaction(dbClient.databaseConnection) {
        block()
    }

fun main() {
    loadDBConfig()
    startTasks()
    startParsing()

    embeddedServer(Netty, port = 8081, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(Compression) {
        gzip {
            priority = 1.0
            matchContentType(ContentType.Text.Any, ContentType.Application.Json)
        }
    }
    install(DefaultHeaders) {
        header("X-Engine", "Ktor")
    }
    install(PartialContent) {
        maxRangeCount = 5
    }
    install(ContentNegotiation) {
        json(json)
    }
    install(AutoHeadResponse)
    install(Sessions)
    //install(trafficPlugin)

    routing {
        deviceLogin()
        deviceCreate()
        deviceFirstAuth()
        discordAuth()

        media()
        favourites()
        watched()
        watch()

        heartbeat()

        changes()
        mpvRoute()
        downloadVersions()
        download()
    }
}
