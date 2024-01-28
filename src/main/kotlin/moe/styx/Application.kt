package moe.styx

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import moe.styx.db.StyxDBClient
import moe.styx.misc.Changes
import moe.styx.misc.Config
import moe.styx.misc.startParsing
import moe.styx.routes.*
import moe.styx.tasks.startTasks
import moe.styx.types.json
import java.io.File
import kotlin.system.exitProcess

var config: Config = Config(dbIP = "", dbPass = "", dbUser = "")
var changes: Changes = Changes(0, 0)

private var changesFile: File = File("")
private var configFile: File = File("")

fun loadDBConfig() {
    if (System.getProperty("os.name").lowercase().contains("win")) {
        val styxDir = File(System.getenv("APPDATA"), "Styx")
        val apiDir = File(styxDir, "API-v2")
        apiDir.mkdirs()
        configFile = File(apiDir, "config.json")
        changesFile = File(apiDir, "changes.json")
    } else {
        val configDir = File(System.getProperty("user.home"), ".config")
        val styxDir = File(configDir, "Styx")
        val apiDir = File(styxDir, "API-v2")
        apiDir.mkdirs()
        configFile = File(apiDir, "config.json")
        changesFile = File(apiDir, "changes.json")
    }
    if (!configFile.exists()) {
        configFile.writeText(Json(json) { prettyPrint = true }.encodeToString(config))
        println("Please fill in your config.json! Located at: ${configFile.absolutePath}")
        exitProcess(1)
    }

    if (!changesFile.exists()) {
        changesFile.writeText(json.encodeToString(changes))
    }

    config = json.decodeFromString(configFile.readText())
    changes = json.decodeFromString(changesFile.readText())
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
    install(Sessions)
    //install(trafficPlugin)

    routing {
        deviceLogin()
        deviceCreate()
        deviceFirstAuth()
        discordAuth()

        mediaList()
        mediaEntries()
        schedules()
        images()
        categories()
        favourites()
        watch()

        changes()
    }
}
