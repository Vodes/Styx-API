package moe.styx

import kotlinx.serialization.Serializable

@Serializable
data class Media(
    val GUID: String, val name: String, val nameJP: String?, val nameEN: String?, val synopsisEN: String?,
    val synopsisDE: String?, val thumbID: String?, val bannerID: String? = null, val categoryID: String? = null,
    val prequel: String? = null, val sequel: String? = null, val genres: String? = null, val tags: String? = null,
    val metadataMap: String? = null, val isSeries: Int = 1, val added: Long = 0
)

@Serializable
data class Category(val GUID: String, val sort: Int, val isSeries: Int, val isVisible: Int, val name: String)

@Serializable
data class Image(
    val GUID: String, val hasWEBP: Int? = 0, val hasPNG: Int? = 0,
    val hasJPG: Int? = 0, val externalURL: String? = null, val type: Int = 0
)

@Serializable
data class MediaEntry(
    val GUID: String, val mediaID: String, val timestamp: Long, val entryNumber: String,
    val nameEN: String?, val nameDE: String?, val synopsisEN: String?, val synopsisDE: String?,
    val thumbID: String?, val filePath: String, val fileSize: Long, val originalName: String?
)

@Serializable
data class MediaInfo(
    val entryID: String, var videoCodec: String, var videoBitdepth: Int, var videoRes: String,
    var hasEnglishDub: Int, var hasGermanDub: Int, var hasGermanSub: Int
)

@Serializable
data class MediaWatched(val entryID: String, val userID: String, var lastWatched: Long, var progress: Float, var maxProgress: Float)

@Serializable
data class User(
    val GUID: String, var name: String, var discordID: String, val added: Long, var lastLogin: Long,
    var permissions: Int
)

@Serializable
data class Favourite(val mediaID: String, var userID: String, var added: Long)


@Serializable
data class LoginResponse(
    val name: String, val permissions: Int, val accessToken: String, val watchToken: String,
    val tokenExpiry: Long, val refreshToken: String? = null
)

@Serializable
data class CreationResponse(val GUID: String, val code: Int, val expiry: Long)

@Serializable
data class DeviceInfo(
    val type: String, val name: String?, val model: String?, val cpu: String?, val gpu: String?,
    val os: String, val osVersion: String?, var jvm: String?, var jvmVersion: String?
)

@Serializable
data class UnregisteredDevice(val GUID: String, val deviceInfo: DeviceInfo, val codeExpiry: Long, val code: Int)

@Serializable
data class Device(
    var GUID: String, var userID: String, var name: String, var deviceInfo: DeviceInfo,
    var lastUsed: Long, var accessToken: String, var watchToken: String, var refreshToken: String, var tokenExpiry: Long
)

@Serializable
data class ApiResponse(var code: Int, var message: String?, var silent: Boolean = false)