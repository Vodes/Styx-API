package moe.styx.misc

import kotlinx.serialization.Serializable
import moe.styx.isDocker

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
    val mpvFolder: String = if (isDocker) "/mpv" else "",
    val imageDir: String = if (isDocker) "/images" else "",
    val buildDir: String = if (isDocker) "/builds" else "",
    val androidBuildDir: String = if (isDocker) "/android-builds" else ""
)