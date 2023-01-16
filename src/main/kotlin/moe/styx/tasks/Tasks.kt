package moe.styx.tasks

import kotlinx.datetime.Clock
import moe.styx.delete
import moe.styx.getUnregisteredDevices


enum class Tasks(val seconds: Int, val run: () -> Unit, val initialWait: Int = 0) {

    CLEAN_UNREGISTERED(20, {
        println("Cleaning...")
        cleanUnregistered()
    }, 10),

    BACKUP_DATABASE(86400, {
        println("Wew")
    })
}

private fun cleanUnregistered() {
    val now = Clock.System.now().epochSeconds
    val unregistered = getUnregisteredDevices()

    for (d in unregistered) {
        if (d.codeExpiry < now)
            d.delete()
    }
}