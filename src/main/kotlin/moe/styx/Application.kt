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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import moe.styx.common.json
import moe.styx.db.DBClient
import moe.styx.misc.Config
import moe.styx.misc.startParsing
import moe.styx.routes.*
import moe.styx.tasks.startTasks
import java.io.File
import kotlin.system.exitProcess

lateinit var config: Config
var secretsFile: File = File("SECRETS")
private var configFile: File = File("")

val isDocker by lazy {
    File("/.dockerenv").exists()
}

val dbClient by lazy {
    DBClient(
        "jdbc:postgresql://${config.dbIP}/Styx",
        "org.postgresql.Driver",
        config.dbUser,
        config.dbPass,
        25
    )
}

fun loadDBConfig() {
    val apiDir = if (System.getProperty("os.name").lowercase().contains("win")) {
        val styxDir = File(System.getenv("APPDATA"), "Styx")
        File(styxDir, "API-v2").also { it.mkdirs() }
    } else if (isDocker) {
        File("/config").also { it.mkdirs() }
    } else {
        val configDir = File(System.getProperty("user.home"), ".config")
        val styxDir = File(configDir, "Styx")
        File(styxDir, "API-v2").also { it.mkdirs() }
    }
    configFile = File(apiDir, "config.json")
    secretsFile = File(apiDir, "SECRETS")
    if (!configFile.exists()) {
        configFile.writeText(
            Json(json) { prettyPrint = true; encodeDefaults = true }.encodeToString(
                Config(
                    dbIP = "",
                    dbUser = "",
                    dbPass = ""
                )
            )
        )
        println("Please fill in your config.json! Located at: ${configFile.absolutePath}")
        exitProcess(1)
    }

    if (!secretsFile.exists() || secretsFile.readText().isBlank()) {
        println("Make sure you have a secrets file in your API directory.\nThis should contain all valid app-secrets for auth. Separated by a newline.")
    }

    config = json.decodeFromString(configFile.readText())
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
