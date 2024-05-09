package moe.styx.routes.watch

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import moe.styx.common.data.Device
import moe.styx.common.extension.toBoolean
import moe.styx.db.tables.DeviceTrafficTable
import moe.styx.transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.upsert

private var deviceList = mutableListOf<DeviceTrafficBuffer>()

data class DeviceTrafficBuffer(val device: Device, var bytes: Long, var lastUpdated: Long)

fun containsDevice(device: Device): DeviceTrafficBuffer? {
    for (d in deviceList) {
        if (d.device.GUID == device.GUID)
            return d
    }

    return null
}

fun addTraffic(device: Device, bytes: Long) {
    val now = Clock.System.now().epochSeconds
    val buf = containsDevice(device)
    if (buf == null) {
        deviceList.add(DeviceTrafficBuffer(device, bytes, now))
    } else {
        buf.bytes += bytes
    }
}

fun checkTrafficBuffers() {
    val now = Clock.System.now().epochSeconds
    try {
        for (d in deviceList) {
            if (d.lastUpdated + 10 < now) {
                if (syncTrafficToDB(d))
                    d.bytes = 0
            }
        }
    } catch (_: Exception) {
    }
}

fun syncTrafficToDB(buffer: DeviceTrafficBuffer): Boolean {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return transaction {
        val existing =
            DeviceTrafficTable.select(DeviceTrafficTable.bytes)
                .where {
                    DeviceTrafficTable.deviceID eq buffer.device.GUID and
                            (DeviceTrafficTable.year eq now.year) and
                            (DeviceTrafficTable.month eq now.monthNumber) and
                            (DeviceTrafficTable.day eq now.dayOfMonth)
                }.toList().firstOrNull()

        DeviceTrafficTable.upsert {
            it[deviceID] = buffer.device.GUID
            it[year] = now.year
            it[month] = now.monthNumber
            it[day] = now.dayOfMonth
            it[bytes] = existing?.let { it[bytes] + buffer.bytes } ?: buffer.bytes
        }
    }.insertedCount.toBoolean()
}