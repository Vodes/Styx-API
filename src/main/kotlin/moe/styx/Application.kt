package moe.styx

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
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import moe.styx.common.data.Changes
import moe.styx.common.json
import moe.styx.db.StyxDBClient
import moe.styx.misc.Config
import moe.styx.misc.startParsing
import moe.styx.routes.*
import moe.styx.tasks.startTasks
import java.io.File
import kotlin.system.exitProcess

var config: Config = Config(dbIP = "", dbPass = "", dbUser = "")
var changes: Changes = Changes(0, 0)
var changesUpdated = 0L

var changesFile: File = File("")
var secretsFile: File = File("SECRETS")
private var configFile: File = File("")

fun loadDBConfig() {
    if (System.getProperty("os.name").lowercase().contains("win")) {
        val styxDir = File(System.getenv("APPDATA"), "Styx")
        val apiDir = File(styxDir, "API-v2")
        apiDir.mkdirs()
        configFile = File(apiDir, "config.json")
        changesFile = File(styxDir, "changes.json")
        secretsFile = File(apiDir, "SECRETS")
    } else {
        val configDir = File(System.getProperty("user.home"), ".config")
        val styxDir = File(configDir, "Styx")
        val apiDir = File(styxDir, "API-v2")
        apiDir.mkdirs()
        configFile = File(apiDir, "config.json")
        changesFile = File(styxDir, "changes.json")
        secretsFile = File(apiDir, "SECRETS")
    }
    if (!configFile.exists()) {
        configFile.writeText(Json(json) { prettyPrint = true }.encodeToString(config))
        println("Please fill in your config.json! Located at: ${configFile.absolutePath}")
        exitProcess(1)
    }

    if (!secretsFile.exists() || secretsFile.readText().isBlank()) {
        println("Make sure you have a secrets file in your API directory.\nThis should contain all valid app-secrets for auth. Separated by a newline.")
        exitProcess(1)
    }

    if (!changesFile.exists()) {
        changesFile.writeText(json.encodeToString(changes))
    }

    config = json.decodeFromString(configFile.readText())
    changes = json.decodeFromString(changesFile.readText())
    changesUpdated = Clock.System.now().epochSeconds
}

fun updateChanges(media: Long, entry: Long) {
    changes = Changes(media, entry)
    changesFile.writeText(json.encodeToString(changes))
}

fun getDBClient(): StyxDBClient {
    return StyxDBClient(
        "com.mysql.cj.jdbc.Driver",
        "jdbc:mysql://${config.dbIP}/Styx2?" +
                "useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Europe/Berlin",
        config.dbUser,
        config.dbPass
    )
}

fun main() {
    loadDBConfig()
    startTasks()
    startParsing()

    if (!System.getProperty("os.name").contains("win", true) && changes.media == 0L)
        updateChanges(Clock.System.now().epochSeconds, Clock.System.now().epochSeconds)

    embeddedServer(Netty, port = 8081, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(Compression) {
        gzip {
            priority = 1.0
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
    }
}
