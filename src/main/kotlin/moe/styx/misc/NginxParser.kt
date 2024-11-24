package moe.styx.misc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.styx.common.config.UnifiedConfig
import moe.styx.common.data.APIState
import moe.styx.common.extension.eqI
import moe.styx.common.util.Log
import moe.styx.db.tables.APIStateTable
import moe.styx.db.tables.DeviceTable
import moe.styx.routes.watch.addTraffic
import moe.styx.transaction
import org.jetbrains.exposed.sql.selectAll
import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

val watchTrafficRegex =
    "\\[(?<timestamp>\\d{2}\\/\\w{3}\\/\\d{4}:\\d{2}:\\d{2}:\\d{2} (?:\\+|\\-)\\d+)\\] \\\"GET \\/watch\\/(?<media>.+?)?token=(?<token>.+?) HTTP\\/\\d\\.\\d\" (?<code>2\\d+) (?<bytes>\\d+)".toRegex(
        RegexOption.IGNORE_CASE
    )

val nginxTimestampFormatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z")

fun startParsing() {
    val nginxLogFile = UnifiedConfig.current.apiConfig.nginxLogFile.ifBlank { null }?.let { File(it) }
    if (nginxLogFile == null || !nginxLogFile.exists()) {
        Log.w { "Nginx Traffic parsing disabled due to no file specified in the config or found!" }
        return
    }

    val parseJob = Job()
    val scope = CoroutineScope(parseJob)
    val parsedLines = mutableListOf<String>()
    scope.launch {
        delay(10000)
        while (true) {
            val apiState = transaction { APIStateTable.getCurrent() ?: APIState(0) }
            val lines = nginxLogFile.readLines().filter { !parsedLines.contains(it) }
            val devices = transaction { DeviceTable.query { selectAll().toList() } }
            for (line in lines) {
                parsedLines.add(line)
                val match = watchTrafficRegex.find(line) ?: continue
                val timestamp = match.groups["timestamp"]?.value?.let {
                    runCatching { OffsetDateTime.parse(it, nginxTimestampFormatter) }.getOrNull()
                }?.toInstant()?.epochSecond ?: continue
                val traffic = match.groups["bytes"]?.value?.toLongOrNull() ?: continue
                val device = match.groups["token"]?.value?.let { devices.find { dev -> dev.watchToken eqI it } } ?: continue

                if (timestamp < apiState.lastUpdatedTraffic)
                    continue

                addTraffic(device, traffic)
            }
            transaction { APIStateTable.setToNow() }
            delay(30000)
        }
    }
}