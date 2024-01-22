package moe.styx.tasks

import kotlinx.datetime.Clock
import moe.styx.db.delete
import moe.styx.db.getUnregisteredDevices
import moe.styx.getDBClient
import moe.styx.routes.watch.checkTrafficBuffers


enum class Tasks(val seconds: Int, val run: () -> Unit, val initialWait: Int = 0) {

    CLEAN_UNREGISTERED(20, { cleanUnregistered() }, 10),

    BACKUP_DATABASE(86400, { println("Wew") }),

    SYNC_TRAFFIC(15, { checkTrafficBuffers() }, 10)
}

private fun cleanUnregistered() {
    getDBClient().executeAndClose {
        val now = Clock.System.now().epochSeconds
        val unregistered = getUnregisteredDevices()
        unregistered.filter { it.codeExpiry < now }.forEach { delete(it) }
    }
}