package moe.styx.tasks

import kotlinx.datetime.Clock
import moe.styx.changes
import moe.styx.changesFile
import moe.styx.changesUpdated
import moe.styx.db.*
import moe.styx.getDBClient
import moe.styx.routes.watch.checkTrafficBuffers
import moe.styx.types.eqI
import moe.styx.types.json


enum class Tasks(val seconds: Int, val run: () -> Unit, val initialWait: Int = 5) {

    CLEAN_UNREGISTERED(20, { cleanUnregistered() }, 10),

    BACKUP_DATABASE(86400, { println("Wew") }),

    SYNC_TRAFFIC(15, { checkTrafficBuffers() }, 10),

    UPDATE_CHANGES(5, { updateChanges() }),

    SPRING_CLEANING(86400, { springCleaning() }, 300)
}

private fun cleanUnregistered() {
    getDBClient().executeAndClose {
        val now = Clock.System.now().epochSeconds
        val unregistered = getUnregisteredDevices()
        unregistered.filter { it.codeExpiry < now }.forEach { delete(it) }
    }
}

private fun updateChanges() {
    if (changesFile.exists()) {
        if (changesFile.lastModified() > changesUpdated) {
            changes = json.decodeFromString(changesFile.readText())
            changesUpdated = Clock.System.now().epochSeconds
        }
    }
}

private fun springCleaning() {
    getDBClient().executeAndClose {
        val entries = getEntries()
        val mediaList = getMedia()
        val users = getUsers()

        var deletedFavs = 0
        val favs = getFavourites()
        for (fav in favs) {
            val media = mediaList.find { it.GUID eqI fav.mediaID }
            val user = users.find { it.GUID eqI fav.userID }
            if (media == null || user == null)
                delete(fav).also { deletedFavs++ }
        }

        var deletedWatched = 0
        val watched = getMediaWatched()
        for (wat in watched) {
            val entry = entries.find { it.GUID eqI wat.entryID }
            val user = users.find { it.GUID eqI wat.userID }
            if (entry == null || user == null)
                delete(wat).also { deletedWatched++ }
        }

        if (deletedWatched != 0 || deletedFavs != 0)
            println(
                "Spring Cleaning Complete:\n\n" +
                        "Deleted MediaWatched entries: $deletedWatched\n" +
                        "Deleted Favourites: $deletedFavs\n"
            )
    }
}