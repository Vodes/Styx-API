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
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import moe.styx.database.Changes
import moe.styx.database.DbConfig
import moe.styx.routes.*
import moe.styx.tasks.startTasks
import java.io.File
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
        val configDir = File(System.getProperty("user.home"), ".config")
        val styxDir = File(configDir, "Styx")
        val apiDir = File(styxDir, "API-v2")
        apiDir.mkdirs()
        configFile = File(apiDir, "dbConfig.json")
        changesFile = File(apiDir, "changes.json")
    }
    if (!configFile.exists()) {
        configFile.writeText(json.encodeToString(dbConfig))
        println("Please fill in your dbconfig.json! Located at: ${configFile.parentFile.absolutePath}")
        exitProcess(1)
    }

    if (!changesFile.exists()) {
        changesFile.writeText(json.encodeToString(changes))
    }

    dbConfig = json.decodeFromString(configFile.readText())
    changes = json.decodeFromString(changesFile.readText())
}

fun updateChanges(media: Long, entry: Long) {
    changes = Changes(media, entry)
    changesFile.writeText(json.encodeToString(changes))
}

//    MediaSchedule("63D8E793-42FC-4954-8493-FEC2F540E725", ScheduleWeekday.WEDNESDAY, 15, 0, 0, 12).save()
//    MediaSchedule("E341CD0C-1624-4142-8A7F-FD1C5AD915C2", ScheduleWeekday.SUNDAY, 4, 0, 0, 0).save()
//    MediaSchedule("B7694A16-1804-4792-9D96-B36C8440E467", ScheduleWeekday.SATURDAY, 17, 30, 0, 0).save()
//    MediaSchedule("F21F989A-ED08-461C-983F-0F983CF9C0B6", ScheduleWeekday.SATURDAY, 18, 30, 0, 0).save()
//    MediaSchedule("46E72D70-10B9-4ECA-BA5D-A4D2E04162A5", ScheduleWeekday.MONDAY, 17, 30, 0, 24).save()
//
//    val list = getAllMediaSchedules()

fun main() {
    loadDBConfig()
    startTasks()

    if (!System.getProperty("os.name").contains("win", true) && changes.media == 0L)
        updateChanges(Clock.System.now().epochSeconds, Clock.System.now().epochSeconds)

    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(Compression) {
        gzip {
            priority = 1.0

        }
    }
    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }
    install(PartialContent) {
        maxRangeCount = 5
    }
    install(ContentNegotiation) {
        json(json)
    }
//    install(WebSockets) {
//        pingPeriod = Duration.ofSeconds(15)
//        timeout = Duration.ofSeconds(15)
//        maxFrameSize = Long.MAX_VALUE
//        masking = false
//    }

    routing {
        deviceLogin()
        deviceCreate()
        deviceFirstAuth()

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
