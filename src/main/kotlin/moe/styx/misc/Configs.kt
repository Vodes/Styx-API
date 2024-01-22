package moe.styx.misc

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val baseURL: String = "https://api.styx.moe",
    val mainSiteBaseURL: String = "https://beta.styx.moe",
    val discordClientID: String = "",
    val discordClientSecret: String = "",
    val nginxWatchLogFile: String = "",
    val dbIP: String,
    val dbUser: String,
    val dbPass: String
)

@Serializable
data class Changes(val media: Long, val entry: Long)