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
import moe.styx.common.util.Log
import moe.styx.db.DBClient
import moe.styx.misc.startParsing
import moe.styx.routes.*
import moe.styx.tasks.startTasks
import pw.vodes.zstd.zstd
import java.io.File

var secretsFile: File = File("SECRETS")

val dbClient by lazy {
    DBClient(
        "jdbc:postgresql://${UnifiedConfig.current.dbConfig.host()}/Styx",
        "org.postgresql.Driver",
        UnifiedConfig.current.dbConfig.user(),
        UnifiedConfig.current.dbConfig.pass(),
        20
    )
}

fun loadDBConfig() {
    secretsFile = File(UnifiedConfig.configFile.parentFile, "SECRETS")
    if (!secretsFile.exists() || secretsFile.readText().isBlank()) {
        println("Make sure you have a secrets file in your API directory.\nThis should contain all valid app-secrets for auth. Separated by a newline.")
    }

    dbClient.transaction { dbClient.createTables() }
    Log.debugEnabled = UnifiedConfig.current.debug()
}

fun <T> transaction(block: () -> T): T =
    org.jetbrains.exposed.sql.transactions.transaction(dbClient.databaseConnection) {
        block()
    }

fun main() {
    loadDBConfig()
    startTasks()
    startParsing()

    embeddedServer(
        Netty,
        host = UnifiedConfig.current.apiConfig.serveHost(),
        port = UnifiedConfig.current.apiConfig.servePort,
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    install(Compression) {
        gzip {
            priority = 0.5
            matchContentType(ContentType.Text.Any, ContentType.Application.Json)
        }
        zstd {
            priority = 1.0
            minimumSize(2048)
            matchContentType(ContentType.Text.Any, ContentType.Application.Json, ContentType.Application.Xml)
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
        proxyServers()
    }
}
