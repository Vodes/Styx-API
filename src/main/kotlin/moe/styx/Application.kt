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
import io.ktor.server.websocket.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import moe.styx.database.Changes
import moe.styx.database.DbConfig
import moe.styx.routes.*
import moe.styx.tasks.startTasks
import java.io.File
import java.time.Duration
import kotlin.system.exitProcess

val json = Json {
    prettyPrint = true
    isLenient = true
    ignoreUnknownKeys = true
}
var dbConfig: DbConfig = DbConfig("", "", "")
var changes: Changes = Changes(0, 0)

private var changesFile: File = File("")
private var configFile: File = File("")

fun loadDBConfig() {
    if (System.getProperty("os.name").lowercase().contains("win")) {
        val styxDir = File(System.getenv("APPDATA"), "Styx")
        val apiDir = File(styxDir, "API-v2")
        apiDir.mkdirs()
        configFile = File(apiDir, "dbConfig.json")
        changesFile = File(apiDir, "changes.json")
    } else {
        val configDir = File(System.getProperty("user.dir"), ".config")
        val styxDir = File(configDir, "Styx")
        val apiDir = File(styxDir, "API-v2")
        apiDir.mkdirs()
        configFile = File(apiDir, "dbConfig.json")
        changesFile = File(apiDir, "changes.json")
    }
    if (!configFile.exists()) {
        configFile.writeText(json.encodeToString(dbConfig), Charsets.UTF_8)
        println("Please fill in your dbconfig.json! Located at: ${configFile.parentFile.absolutePath}")
        exitProcess(1)
    }

    if (!changesFile.exists()) {
        changesFile.writeText(json.encodeToString(changes), Charsets.UTF_8)
    }

    dbConfig = json.decodeFromString(configFile.readText(Charsets.UTF_8))
    changes = json.decodeFromString(changesFile.readText(Charsets.UTF_8))
}

fun updateChanges(media: Long, entry: Long) {
    changes = Changes(media, entry)
    changesFile.writeText(json.encodeToString(changes), Charsets.UTF_8)
}

fun main() {
    loadDBConfig()

    startTasks()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)

}

fun Application.module() {
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(512) // condition
        }
    }
    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }
    install(PartialContent) {
        // Maximum number of ranges that will be accepted from a HTTP request.
        // If the HTTP request specifies more ranges, they will all be merged into a single range.
        maxRangeCount = 20
    }
    install(ContentNegotiation) {
        json(json)
    }
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        deviceLogin()
        deviceCreate()
        deviceFirstAuth()

        mediaList()
        mediaEntries()
        images()
        categories()
        watch()

        changes()
    }
}
