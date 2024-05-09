package moe.styx.tasks

import kotlinx.datetime.Clock
import moe.styx.common.extension.eqI
import moe.styx.common.extension.toBoolean
import moe.styx.config
import moe.styx.db.tables.ImageTable
import moe.styx.db.tables.MediaTable
import moe.styx.db.tables.UnregisteredDeviceTable
import moe.styx.routes.watch.checkTrafficBuffers
import moe.styx.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import java.io.File


enum class Tasks(val seconds: Int, val run: () -> Unit, val initialWait: Int = 5) {

    CLEAN_UNREGISTERED(20, { cleanUnregistered() }, 10),

    BACKUP_DATABASE(86400, { println("Wew") }),

    SYNC_TRAFFIC(15, { checkTrafficBuffers() }, 10),

    SPRING_CLEANING(86400, { deleteUnusedData() }, 120)
}

private fun cleanUnregistered() {
    val now = Clock.System.now().epochSeconds
    transaction {
        UnregisteredDeviceTable.deleteWhere { codeExpiry less now }
    }
}

private fun deleteUnusedData() {
    var deletedImages = 0
    transaction {
        val media = MediaTable.query { selectAll().toList() }
        val images = ImageTable.query { selectAll().toList() }

        val unused = images.filter { img -> media.find { it.thumbID eqI img.GUID || it.bannerID eqI img.GUID } == null }
        if (unused.isNotEmpty()) {
            val files = File(config.imageDir).listFiles()
                ?.filter { it.isFile && it.extension.lowercase() in arrayOf("webp", "png", "jpg", "jpeg") }
                ?: emptyList<File>()
            if (files.isNotEmpty()) {
                unused.forEach { img ->
                    val result = ImageTable.deleteWhere { GUID eq img.GUID }.toBoolean()
                    if (result) {
                        val file = files.find { it.nameWithoutExtension eqI img.GUID }
                        if (file != null && file.exists()) {
                            runCatching { file.delete() }
                        }
                        deletedImages++
                    }
                }
            }
        }
    }
    if (deletedImages != 0)
        println("Deleted unused images: $deletedImages")
}

//private fun springCleaning() {
//    getDBClient().executeAndClose {
//        val entries = getEntries()
//        val mediaList = getMedia()
//        val users = getUsers()
//
//        var deletedFavs = 0
//        val favs = getFavourites()
//        for (fav in favs) {
//            val media = mediaList.find { it.GUID eqI fav.mediaID }
//            val user = users.find { it.GUID eqI fav.userID }
//            if (media == null || user == null)
//                delete(fav).also { deletedFavs++ }
//        }
//
//        var deletedWatched = 0
//        val watched = getMediaWatched()
//        for (wat in watched) {
//            val entry = entries.find { it.GUID eqI wat.entryID }
//            val user = users.find { it.GUID eqI wat.userID }
//            if (entry == null || user == null)
//                delete(wat).also { deletedWatched++ }
//        }
//        var deletedImages = 0
//        if (config.imageDir.isNotBlank()) {
//            val files = File(config.imageDir).listFiles()?.filter { it.isFile && it.extension.lowercase() in arrayOf("webp", "png", "jpg") }
//                ?: emptyList<File>()
//            val images = getImages()
//            for (img in images) {
//                val media = mediaList.find { it.thumbID eqI img.GUID || it.bannerID eqI img.GUID }
//                if (media != null)
//                    continue
//                delete(img)
//                val imgFile = files.find { it.nameWithoutExtension eqI img.GUID }
//                if (imgFile != null && imgFile.exists())
//                    imgFile.delete()
//                deletedImages++
//            }
//        }
//
//        if (deletedWatched != 0 || deletedFavs != 0 || deletedImages != 0)
//            println(
//                "Spring Cleaning Complete:\n\n" +
//                        "Deleted MediaWatched entries: $deletedWatched\n" +
//                        "Deleted Favourites: $deletedFavs\n" +
//                        "Deleted unused images: $deletedImages\n"
//            )
//    }
//}