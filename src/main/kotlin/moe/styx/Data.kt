package moe.styx

import kotlinx.serialization.Serializable

@Serializable
data class Media(
    val GUID: String, val name: String, val nameJP: String?, val nameEN: String?, val synopsisEN: String?,
    val synopsisDE: String?, val thumbID: String?, val bannerID: String? = null, val categoryID: String? = null,
    val prequel: String? = null, val sequel: String? = null, val genres: String? = null, val tags: String? = null,
    val metadataMap: String? = null, val isSeries: Int = 1
)

@Serializable
data class Category(val GUID: String, val sort: Int, val isSeries: Int, val isVisible: Int, val name: String)

@Serializable
data class Image(
    val GUID: String, val hasWEBP: Int? = 0, val hasPNG: Int? = 0,
    val hasJPG: Int? = 0, val externalURL: String?, val type: Int = 0
)

@Serializable
data class MediaEntry(
    val GUID: String, val mediaID: String, val timestamp: Long, val entryNumber: String,
    val nameEN: String?, val nameDE: String?, val synopsisEN: String?, val synopsisDE: String?,
    val thumbID: String?, val filePath: String, val fileSize: Long, val originalName: String?
)

@Serializable
data class MediaInfo(
    val entryID: String, val videoCodec: String, val videoBitdepth: Int, val videoRes: String,
    val hasEnglishDub: Int, val hasGermanDub: Int, val hasGermanSub: Int
)

@Serializable
data class MediaWatched(val entryID: String, val userID: String, val lastWatched: Long, val progress: Float)

@Serializable
data class User(
    val GUID: String, val name: String, val discordID: String, val added: Long, val lastLogin: Long,
    val permissions: Int
)

@Serializable
data class LoginResponse(
    val name: String, val permissions: Int, val accessToken: String, val watchToken: String,
    val tokenExpiry: Long
)

@Serializable
data class CreationResponse(val code: Int, val expiry: Long)