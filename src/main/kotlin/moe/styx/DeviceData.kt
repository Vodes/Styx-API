package moe.styx

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import moe.styx.database.objectExists
import moe.styx.database.openStatement

@Serializable
data class DeviceInfo(
    val type: String, val name: String?, val model: String?, val cpu: String?, val gpu: String?,
    val os: String, val osVersion: String?, val jvm: String?, val jvmVersion: String?
)

@Serializable
data class UnregisteredDevice(val GUID: String, val deviceInfo: DeviceInfo, val codeExpiry: Long, val code: Int) : IDatabaseObject {

    override fun save(newID: String?): Boolean {
        val edit = objectExists(GUID, "UnregisteredDevices")
        val query: String = if (edit)
            "UPDATE UnregisteredDevices SET GUID=?, deviceInfo=?, codeExpiry=?, code=? WHERE GUID=?;"
        else
            "INSERT INTO UnregisteredDevices (GUID, deviceInfo, codeExpiry, code) VALUES(?, ?, ?, ?);"

        val (con, stat) = openStatement(query)
        stat.setString(1, GUID)
        stat.setString(2, json.encodeToString<DeviceInfo>(deviceInfo))
        stat.setLong(3, codeExpiry)
        stat.setInt(4, code)
        if (edit)
            stat.setString(5, GUID)

        val i = stat.executeUpdate()
        stat.close()
        con.close()
        return i.toBoolean()
    }

    override fun delete(): Boolean {
        val (con, stat) = openStatement("DELETE FROM UnregisteredDevices WHERE GUID=?;")
        stat.setString(1, GUID)
        val i = stat.executeUpdate()
        stat.close()
        con.close()
        return i.toBoolean()
    }
}

@Serializable
data class Device(
    var GUID: String, var userID: String, var name: String, var deviceInfo: DeviceInfo,
    var lastUsed: Long, var accessToken: String, var watchToken: String, var refreshToken: String, var tokenExpiry: Long
) : IDatabaseObject {

    override fun save(newID: String?): Boolean {
        val edit = objectExists(GUID, "UserDevices")
        val query: String = if (edit)
            "UPDATE UserDevices SET GUID=?, userID=?, deviceName=?, deviceInfo=?, lastUsed=?, accessToken=?, watchToken=?, refreshToken=?, tokenExpiry=? WHERE GUID=?;"
        else
            "INSERT INTO UserDevices (GUID, userID, deviceName, deviceInfo, lastUsed, accessToken, watchToken, refreshToken, tokenExpiry) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?);"

        val (con, stat) = openStatement(query)
        stat.setString(1, if (newID.isNullOrBlank()) GUID else newID)
        stat.setString(2, userID)
        stat.setString(3, name)
        stat.setString(4, json.encodeToString<DeviceInfo>(deviceInfo))
        stat.setLong(5, lastUsed)
        stat.setString(6, accessToken)
        stat.setString(7, watchToken)
        stat.setString(8, refreshToken)
        stat.setLong(9, tokenExpiry)
        if (edit)
            stat.setString(10, GUID)

        val i = stat.executeUpdate()
        stat.close()
        con.close()

        if (!newID.isNullOrBlank())
            GUID = newID

        return i.toBoolean()
    }

    override fun delete(): Boolean {
        val (con, stat) = openStatement("DELETE FROM UserDevices WHERE GUID=?;")
        stat.setString(1, GUID)
        val i = stat.executeUpdate()
        stat.close()
        con.close()

        return i.toBoolean()
    }
}

interface IDatabaseObject {
    abstract fun save(newID: String? = null): Boolean
    abstract fun delete(): Boolean
}