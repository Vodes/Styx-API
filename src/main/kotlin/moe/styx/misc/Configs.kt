package moe.styx.misc

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val baseURL: String = "",
    val mainSiteBaseURL: String = "",
    val discordClientID: String = "",
    val discordClientSecret: String = "",
    val nginxWatchLogFile: String = "",
    val dbIP: String,
    val dbUser: String,
    val dbPass: String,
    val mpvFolder: String = "",
    val imageDir: String = "",
    val buildDir: String = "",
    val androidBuildDir: String = ""
)