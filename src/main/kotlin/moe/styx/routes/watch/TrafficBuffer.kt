package moe.styx.routes.watch

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import moe.styx.Device
import moe.styx.database.openStatement
import moe.styx.toBoolean

/* TODO: Make a class like DeviceTrafficBuffer containing a Device, a bytes Long
    and a "last updated" variable (just current unix time)
*/

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

private fun exists(buffer: DeviceTrafficBuffer, now: LocalDateTime): Boolean {
    val (con, stat) = openStatement("SELECT * FROM DeviceTraffic WHERE deviceID=? AND year=? AND month=? AND day=?;")
    stat.setString(1, buffer.device.GUID)
    stat.setInt(2, now.year)
    stat.setInt(3, now.monthNumber)
    stat.setInt(4, now.dayOfMonth)
    val results = stat.executeQuery()
    val exists = results.next()
    stat.close()
    con.close()
    return exists
}

fun syncTrafficToDB(buffer: DeviceTrafficBuffer): Boolean {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val update = exists(buffer, now)

    val query = if (update) "UPDATE DeviceTraffic SET bytes = bytes + ${buffer.bytes} WHERE deviceID=? AND year=? AND month=? AND day=?;" else
        "INSERT INTO DeviceTraffic (deviceID, year, month, day, bytes) VALUES(?, ?, ?, ?, ?);"

    val (con, stat) = openStatement(query)
    stat.setString(1, buffer.device.GUID)
    stat.setInt(2, now.year)
    stat.setInt(3, now.monthNumber)
    stat.setInt(4, now.dayOfMonth)
    if (!update)
        stat.setLong(5, buffer.bytes)

    val i = stat.executeUpdate()
    stat.close()
    con.close()
    return i.toBoolean()
}