package moe.styx.routes.watch

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.http.content.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.styx.types.Device
import java.io.File
import java.io.IOException

class CustomLocalFileContent(
    val file: File,
    override val contentType: ContentType = ContentType.defaultForFile(file),
    val device: Device
) : OutgoingContent.ReadChannelContent() {

    override val contentLength: Long get() = file.length()

    init {
        if (!file.exists()) {
            throw IOException("No such file ${file.absolutePath}")
        } else {
            val lastModifiedVersion = file.lastModified()
            versions += LastModifiedVersion(lastModifiedVersion)
        }
    }

    override fun readFrom(): ByteReadChannel {
        val channel = file.readChannel()
        listenChannel(channel)
        return channel
    }

    override fun readFrom(range: LongRange): ByteReadChannel {
        val channel = file.readChannel(range.start, range.endInclusive)
        listenChannel(channel)
        return channel
    }

    private fun listenChannel(channel: ByteReadChannel) {
        val listenJob = Job()
        val scope = CoroutineScope(listenJob)
        scope.launch {
            while (!channel.isClosedForRead)
                delay(500L).also { println("Waiting for channel to close...") }
            addTraffic(device, channel.totalBytesRead)
            println("Adding ${channel.totalBytesRead} bytes of traffic to the buffer.")
            listenJob.complete()
        }
    }
}