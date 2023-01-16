package moe.styx.routes.watch

import moe.styx.Device

/* TODO: Make a class like DeviceTrafficBuffer containing a Device, a bytes Long
    and a "last updated" variable (just current unix time)
*/

private var deviceList = mutableListOf<Pair<Device, Long>>()

fun containsDevice(device: Device): Pair<Device, Long>? {
    for (d in deviceList) {
        if (d.first.GUID == device.GUID)
            return d
    }

    return null
}

fun addTraffic(device: Device, bytes: Long) {
    val pair = containsDevice(device)

    if (pair == null) {
        deviceList.add(Pair(device, bytes))
    } else {
        deviceList.set(deviceList.indexOf(pair), Pair(device, pair.second + bytes))
    }

    for (d in deviceList) {
        println("${d.first.name}: ${d.second}")
    }
}

fun syncTrafficToDB(): Boolean {

    return false
}