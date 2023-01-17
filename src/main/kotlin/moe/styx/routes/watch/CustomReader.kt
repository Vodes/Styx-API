package moe.styx.routes.watch

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.http.content.*
import io.ktor.util.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import moe.styx.Device
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import kotlin.coroutines.CoroutineContext

fun File.customReadChannel(
    start: Long = 0,
    endInclusive: Long = -1,
    coroutineContext: CoroutineContext = Dispatchers.IO,
    device: Device
): ByteReadChannel {
    val fileLength = length()
    return CoroutineScope(coroutineContext).writer(CoroutineName("file-reader") + coroutineContext, autoFlush = false) {
        require(start >= 0L) { "start position shouldn't be negative but it is $start" }
        require(endInclusive <= fileLength - 1) {
            "endInclusive points to the position out of the file: file size = $fileLength, endInclusive = $endInclusive"
        }

        @Suppress("BlockingMethodInNonBlockingContext")
        RandomAccessFile(this@customReadChannel, "r").use { file ->
            val fileChannel: FileChannel = file.channel
            if (start > 0) {
                fileChannel.position(start)
            }

            if (endInclusive == -1L) {
                println("Test")
                @Suppress("DEPRECATION")
                channel.writeSuspendSession {
                    while (true) {
                        val buffer = request(1)
                        if (buffer == null) {
                            channel.flush()
                            tryAwait(1)
                            continue
                        }

                        val rc = fileChannel.read(buffer)
                        if (rc == -1) break

                        addTraffic(device, rc.toLong())

                        written(rc)
                    }
                }

                return@use
            }

            var position = start
            channel.writeWhile { buffer ->
                val fileRemaining = endInclusive - position + 1
                val rc = if (fileRemaining < buffer.remaining()) {
                    val l = buffer.limit()
                    buffer.limit(buffer.position() + fileRemaining.toInt())
                    val r = fileChannel.read(buffer)
                    buffer.limit(l)
                    r
                } else {
                    fileChannel.read(buffer)
                }

                addTraffic(device, rc.toLong())

                if (rc > 0) position += rc

                rc != -1 && position <= endInclusive
            }

        }
    }.channel
}

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

    override fun readFrom(): ByteReadChannel = file.customReadChannel(device = device)

    override fun readFrom(range: LongRange): ByteReadChannel = file.customReadChannel(range.start, range.endInclusive, device = device)
}