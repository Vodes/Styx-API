package moe.styx.misc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.styx.config
import moe.styx.db.getDevices
import moe.styx.getDBClient
import moe.styx.routes.watch.addTraffic
import java.io.File

val watchTrafficRegex =
    "GET \\/watch\\/(?<media>.+)?token=(?<token>.+) HTTP\\/\\d\\.\\d\" (?<code>2\\d+) (?<bytes>\\d+)".toRegex(RegexOption.IGNORE_CASE)

fun startParsing() {
    if (config.nginxWatchLogFile.isBlank() || !File(config.nginxWatchLogFile).exists())
        return

    val parseJob = Job()
    val scope = CoroutineScope(parseJob)
    val parsedLines = mutableListOf<String>()
    val file = File(config.nginxWatchLogFile)
    if (!file.canWrite())
        file.setWritable(true, false)
    file.writeText("")
    scope.launch {
        delay(10000)
        while (true) {
            val lines = file.readLines()
            getDBClient().executeAndClose {
                for (line in lines.filter { !parsedLines.contains(it) }) {
                    parsedLines.add(line)
                    val match = watchTrafficRegex.find(line) ?: continue
                    val device = match.groups["token"]?.value?.let { getDevices(mapOf("watchToken" to it)).firstOrNull() } ?: continue
                    val traffic = match.groups["bytes"]?.value?.toLongOrNull() ?: continue
                    addTraffic(device, traffic)
                }
            }
            if (parsedLines.size > 100 && file.canWrite()) {
                parsedLines.clear()
                file.writeText("")
            }
            delay(30000)
        }
    }
}