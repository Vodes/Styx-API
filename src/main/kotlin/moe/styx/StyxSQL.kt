package moe.styx

import kotlinx.serialization.encodeToString
import moe.styx.database.genericDelete
import moe.styx.database.objectExists
import moe.styx.database.objectExistsTwo
import moe.styx.database.openStatement

//TODO: Device SQL Extensions
fun Device.save(newID: String? = null): Boolean {
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
    return false
}

fun Device.delete(): Boolean = genericDelete(GUID, "UserDevices");

//TODO: Unregistered Device SQL Extensions
fun UnregisteredDevice.save(newID: String? = null): Boolean {
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

fun UnregisteredDevice.delete(): Boolean = genericDelete(GUID, "UnregisteredDevices");

fun UnregisteredDevice.toDevice(userID: String, name: String): Device {
    return Device(GUID, userID, name, deviceInfo, -1, "", "", "", -1)
}

// TODO: MediaInfo SQL Extensions
fun MediaInfo.save(newID: String? = null): Boolean {
    val edit = objectExists(entryID, "MediaInfo", "entryID")
    val query: String = if (edit) {
        "UPDATE MediaInfo SET entryID=?, videoCodec=?, videoBitdepth=?, videoRes=?, hasEnglishDub=?, hasGermanDub=?, hasGermanSub=? WHERE entryID=?;"
    } else {
        "INSERT INTO MediaInfo (entryID, videoCodec, videoBitdepth, videoRes, hasEnglishDub, hasGermanDub, hasGermanSub) VALUES(?, ?, ?, ?, ?, ?, ?);"
    }

    val (con, stat) = openStatement(query)
    stat.setString(1, entryID)
    stat.setString(2, videoCodec)
    stat.setInt(3, videoBitdepth)
    stat.setString(4, videoRes)
    stat.setInt(5, hasEnglishDub)
    stat.setInt(6, hasGermanDub)
    stat.setInt(7, hasGermanSub)
    if (edit) {
        stat.setString(8, entryID)
    }

    val i = stat.executeUpdate()
    stat.close()
    con.close()
    return i.toBoolean()
}

fun MediaInfo.delete(): Boolean = genericDelete(entryID, "MediaInfo", "entryID");

// TODO: MediaWatched SQL Extensions
fun MediaWatched.save(newID: String? = null): Boolean {
    val edit = objectExistsTwo("entryID", "userID", entryID, userID, "MediaInfo")
    val query: String = if (edit)
        "UPDATE MediaWatched SET entryID=?, userID=?, lastWatched=?, progress=?, maxProgress=? WHERE entryID=? AND userID=?;"
    else
        "INSERT INTO MediaWatched (entryID, userID, lastWatched, progress, maxProgress) VALUES(?, ?, ?, ?, ?);"

    val (con, stat) = openStatement(query)
    stat.setString(1, entryID)
    stat.setString(2, userID)
    stat.setLong(3, lastWatched)
    stat.setFloat(4, progress)
    stat.setFloat(5, maxProgress)
    if (edit) {
        stat.setString(6, entryID)
        stat.setString(7, userID)
    }

    val i = stat.executeUpdate()
    stat.close()
    con.close()
    return i.toBoolean()
}

fun MediaWatched.delete(): Boolean {
    val (con, stat) = openStatement("DELETE FROM MediaWatched WHERE WHERE entryID=? AND userID=?;")
    stat.setString(1, entryID)
    stat.setString(2, userID)
    val i = stat.executeUpdate()
    stat.close()
    con.close()
    return i.toBoolean()
}

// TODO: Media SQL Extensions
fun Media.save(newID: String? = null): Boolean {
    val edit = objectExists(GUID, "Media", "GUID")
    val query: String = if (edit)
        "UPDATE Media SET GUID=?, name=?, nameJP=?, nameEN=?, synopsisEN=?, synopsisDE=?, thumbID=?, bannerID=?, categoryID=?, prequel=?, sequel=?, genres=?, tags=?, metadataMap=?, isSeries=? WHERE GUID=?;"
    else
        "INSERT INTO Media (GUID, name, nameJP, nameEN, synopsisEN, synopsisDE, thumbID, bannerID, categoryID, prequel, sequel, genres, tags, metadataMap, isSeries) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"

    val (con, stat) = openStatement(query)
    stat.setString(1, if (newID.isNullOrBlank()) GUID else newID)
    stat.setString(2, name)
    stat.setString(3, nameJP)
    stat.setString(4, nameEN)
    stat.setString(5, synopsisEN)
    stat.setString(6, synopsisDE)
    stat.setString(7, thumbID)
    stat.setString(8, bannerID)
    stat.setString(9, categoryID)
    stat.setString(10, prequel)
    stat.setString(11, sequel)
    stat.setString(12, genres)
    stat.setString(13, tags)
    stat.setString(14, metadataMap)
    stat.setInt(15, isSeries)
    if (edit) {
        stat.setString(16, GUID)
    }

    val i = stat.executeUpdate()
    stat.close()
    con.close()
    return i.toBoolean()
}

fun Media.delete(): Boolean = genericDelete(GUID, "Media");

// TODO: MediaEntry SQL Extensions
fun MediaEntry.save(newID: String? = null): Boolean {
    val edit = objectExists(GUID, "MediaEntry")
    val query: String = if (edit)
        "UPDATE MediaEntry SET GUID=?, mediaID=?, timestamp=?, entryNumber=?, nameEN=?, nameDE=?, synopsisEN=?, synopsisDE=?, thumbID=?, filePath=?, fileSize=?, originalName=? WHERE GUID=?;"
    else
        "INSERT INTO MediaEntry (GUID, mediaID, timestamp, entryNumber, nameEN, nameDE, synopsisEN, synopsisDE, thumbID, filePath, fileSize, originalName) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"

    val (con, stat) = openStatement(query)
    stat.setString(1, if (newID.isNullOrBlank()) GUID else newID)
    stat.setString(2, mediaID)
    stat.setLong(3, timestamp)
    stat.setString(4, entryNumber)
    stat.setString(5, nameEN)
    stat.setString(6, nameDE)
    stat.setString(7, synopsisEN)
    stat.setString(8, synopsisDE)
    stat.setString(9, thumbID)
    stat.setString(10, filePath)
    stat.setLong(11, fileSize)
    stat.setString(12, originalName)
    if (edit) {
        stat.setString(13, GUID)
    }

    val i = stat.executeUpdate()
    stat.close()
    con.close()
    return i.toBoolean()
}

fun MediaEntry.delete(): Boolean = genericDelete(GUID, "MediaEntry");

// TODO: Image SQL Extensions
fun Image.save(newID: String? = null): Boolean {
    val edit = objectExists(GUID, "Image")
    val query: String = if (edit)
        "UPDATE Image SET GUID=?, hasWEBP=?, hasPNG=?, hasJPG=?, externalURL=?, type=? WHERE GUID=?;"
    else
        "INSERT INTO Image (GUID, hasWEBP, hasPNG, hasJPG, externalURL, type) VALUES(?, ?, ?, ?, ?, ?);"

    val (con, stat) = openStatement(query)
    stat.setString(1, if (newID.isNullOrBlank()) GUID else newID)
    stat.setInt(2, hasWEBP ?: 0)
    stat.setInt(3, hasPNG ?: 0)
    stat.setInt(4, hasJPG ?: 0)
    stat.setString(5, externalURL)
    stat.setInt(6, type)
    if (edit) {
        stat.setString(7, GUID)
    }

    val i = stat.executeUpdate()
    stat.close()
    con.close()
    return i.toBoolean()
}

fun Image.delete(): Boolean = genericDelete(GUID, "Image");

// TODO: Category SQL Extensions

fun Category.save(newID: String? = null): Boolean {
    val edit = objectExists(GUID, "Category")
    val query: String = if (edit) {
        "UPDATE Category SET GUID=?, sort=?, isSeries=?, isVisible=?, name=? WHERE GUID=?;"
    } else {
        "INSERT INTO Category (GUID, sort, isSeries, isVisible, name) VALUES(?, ?, ?, ?, ?);"
    }

    val (con, stat) = openStatement(query)
    stat.setString(1, if (newID.isNullOrBlank()) GUID else newID)
    stat.setInt(2, sort)
    stat.setInt(3, isSeries)
    stat.setInt(4, isVisible)
    stat.setString(5, name)
    if (edit) {
        stat.setString(6, GUID)
    }

    val i = stat.executeUpdate()
    stat.close()
    con.close()
    return i.toBoolean()
}

fun Category.delete(): Boolean = genericDelete(GUID, "Category");

// TODO: User SQL Extensions

fun User.save(newID: String? = null): Boolean {
    val edit = objectExists(GUID, "User")
    val query: String = if (edit) {
        "UPDATE User SET GUID=?, name=?, discordID=?, added=?, lastLogin=?, permissions=? WHERE GUID=?;"
    } else {
        "INSERT INTO User (GUID, name, discordID, added, lastLogin, permissions) VALUES(?, ?, ?, ?, ?, ?);"
    }

    val (con, stat) = openStatement(query)
    stat.setString(1, if (newID.isNullOrBlank()) GUID else newID)
    stat.setString(2, name)
    stat.setString(3, discordID)
    stat.setLong(4, added)
    stat.setLong(5, lastLogin)
    stat.setInt(6, permissions)
    if (edit) {
        stat.setString(7, GUID)
    }

    val i = stat.executeUpdate()
    stat.close()
    con.close()
    return i.toBoolean()
}

fun User.delete(): Boolean = genericDelete(GUID, "User");
