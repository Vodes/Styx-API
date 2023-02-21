package moe.styx

import kotlinx.serialization.decodeFromString
import moe.styx.database.openStatement

fun getUnregisteredDevices(): MutableList<UnregisteredDevice> {
    val list = mutableListOf<UnregisteredDevice>()
    val (con, stat) = openStatement("SELECT * FROM UnregisteredDevices;")
    val results = stat.executeQuery()
    while (results.next()) {
        list.add(
            UnregisteredDevice(
                results.getString("GUID"), json.decodeFromString(results.getString("deviceInfo")),
                results.getLong("codeExpiry"), results.getInt("code")
            )
        )
    }
    stat.close()
    con.close()
    return list
}

fun getDevices(): MutableList<Device> {
    val list = mutableListOf<Device>()
    val (con, stat) = openStatement("SELECT * FROM UserDevices;")
    val results = stat.executeQuery()
    while (results.next()) {
        list.add(
            Device(
                results.getString("GUID"),
                results.getString("userID"),
                results.getString("deviceName"),
                json.decodeFromString(results.getString("deviceInfo")),
                results.getLong("lastUsed"),
                results.getString("accessToken"),
                results.getString("watchToken"),
                results.getString("refreshToken"),
                results.getLong("tokenExpiry")
            )
        )
    }
    stat.close()
    con.close()
    return list
}

fun getUsers(): List<User> {
    val users = mutableListOf<User>()
    val query = "SELECT * FROM User;"
    val (con, stat) = openStatement(query)
    val rs = stat.executeQuery()
    while (rs.next()) {
        val user = User(
            rs.getString("GUID"), rs.getString("name"),
            rs.getString("discordID"), rs.getLong("added"),
            rs.getLong("lastLogin"), rs.getInt("permissions")
        )
        users.add(user)
    }
    stat.close()
    con.close()
    return users
}

fun getCategories(): List<Category> {
    val categories = mutableListOf<Category>()
    val query = "SELECT * FROM Category ORDER BY isSeries DESC, sort DESC;"
    val (con, stat) = openStatement(query)
    val rs = stat.executeQuery()
    while (rs.next()) {
        val category = Category(
            rs.getString("GUID"), rs.getInt("sort"), rs.getInt("isSeries"),
            rs.getInt("isVisible"), rs.getString("name")
        )
        categories.add(category)
    }
    stat.close()
    con.close()
    return categories
}

fun getMedia(): List<Media> {
    val media = mutableListOf<Media>()
    val query = "SELECT * FROM Media;"
    val (con, stat) = openStatement(query)
    val rs = stat.executeQuery()
    while (rs.next()) {
        val mediaItem = Media(
            rs.getString("GUID"), rs.getString("name"),
            rs.getString("nameJP"), rs.getString("nameEN"),
            rs.getString("synopsisEN"), rs.getString("synopsisDE"),
            rs.getString("thumbID"), rs.getString("bannerID"),
            rs.getString("categoryID"), rs.getString("prequel"),
            rs.getString("sequel"), rs.getString("genres"),
            rs.getString("tags"), rs.getString("metadataMap"),
            rs.getInt("isSeries"), rs.getLong("added")
        )
        media.add(mediaItem)
    }
    stat.close()
    con.close()
    return media
}

fun getMediaEntries(): List<MediaEntry> {
    val mediaEntries = mutableListOf<MediaEntry>()
    val query = "SELECT * FROM MediaEntry;"
    val (con, stat) = openStatement(query)
    val rs = stat.executeQuery()
    while (rs.next()) {
        val mediaEntry = MediaEntry(
            rs.getString("GUID"), rs.getString("mediaID"),
            rs.getLong("timestamp"), rs.getString("entryNumber"),
            rs.getString("nameEN"), rs.getString("nameDE"),
            rs.getString("synopsisEN"), rs.getString("synopsisDE"),
            rs.getString("thumbID"), rs.getString("filePath"),
            rs.getLong("fileSize"), rs.getString("originalName")
        )
        mediaEntries.add(mediaEntry)
    }
    stat.close()
    con.close()
    return mediaEntries
}

fun getMediaInfo(): List<MediaInfo> {
    val mediaInfo = mutableListOf<MediaInfo>()
    val query = "SELECT * FROM MediaInfo;"
    val (con, stat) = openStatement(query)
    val rs = stat.executeQuery()
    while (rs.next()) {
        val media = MediaInfo(
            rs.getString("entryID"),
            rs.getString("videoCodec"),
            rs.getInt("videoBitdepth"),
            rs.getString("videoRes"),
            rs.getInt("hasEnglishDub"),
            rs.getInt("hasGermanDub"),
            rs.getInt("hasGermanSub")
        )
        mediaInfo.add(media)
    }
    stat.close()
    con.close()
    return mediaInfo
}

fun getFavourites(): List<Favourite> {
    val favs = mutableListOf<Favourite>()
    val query = "SELECT * FROM Favourites;"
    val (con, stat) = openStatement(query)
    val rs = stat.executeQuery()
    while (rs.next()) {
        val fav = Favourite(rs.getString("mediaID"), rs.getString("userID"), rs.getLong("added"))
        favs.add(fav)
    }
    stat.close()
    con.close()
    return favs
}

fun getAllImages(): List<Image> {
    val images = mutableListOf<Image>()
    val query = "SELECT * FROM Image;"
    val (con, stat) = openStatement(query)
    val rs = stat.executeQuery()
    while (rs.next()) {
        val image = Image(
            rs.getString("GUID"),
            rs.getInt("hasWEBP"),
            rs.getInt("hasPNG"),
            rs.getInt("hasJPG"),
            rs.getString("externalURL"),
            rs.getInt("type")
        )
        images.add(image)
    }
    stat.close()
    con.close()
    return images
}

fun getAllMediaSchedules(): List<MediaSchedule> {
    val query = "SELECT * FROM MediaSchedule;"
    val (con, stat) = openStatement(query)
    val result = stat.executeQuery()
    val mediaSchedules = mutableListOf<MediaSchedule>()
    while (result.next()) {
        mediaSchedules.add(
            MediaSchedule(
                result.getString("mediaID"),
                ScheduleWeekday.valueOf(result.getString("day")),
                result.getInt("hour"),
                result.getInt("minute"),
                result.getInt("isEstimated"),
                result.getInt("finalEpisodeCount")
            )
        )
    }
    con.close()
    return mediaSchedules
}
