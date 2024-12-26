package moe.styx.misc

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import moe.styx.common.config.UnifiedConfig
import moe.styx.common.data.DeviceInfo
import moe.styx.common.data.User
import moe.styx.common.http.httpClient
import moe.styx.common.json
import moe.styx.common.util.launchThreaded

private val noDefaultsNoNullsJson = Json(json) {
    encodeDefaults = false
    explicitNulls = false
}

private val jsonNoDefaultsPretty = Json(json) {
    encodeDefaults = false
    prettyPrint = true
}


@Serializable
data class DiscordMessage(
    val content: String? = null,
    val embeds: List<DiscordEmbed> = emptyList(),
    @SerialName("username") val webhookUsername: String? = null,
    @SerialName("avatar_url") val webhookAvatarUrl: String? = null,
)

@Serializable
data class DiscordEmbed(val title: String? = null, val description: String? = null, val author: DiscordEmbedAuthor? = null)

@Serializable
data class DiscordEmbedAuthor(val name: String, val url: String? = null, @SerialName("icon_url") val iconURL: String? = null)

suspend fun sendEmbedToUrl(url: String, message: DiscordMessage) {
    httpClient.post(url) {
        contentType(ContentType.Application.Json)
        setBody(noDefaultsNoNullsJson.encodeToString(message))
    }
}

fun logNewDevice(ip: String, user: User, deviceInfo: DeviceInfo) = launchThreaded {
    val logWebhook = UnifiedConfig.current.discord.logChannelWebhookURL()
    if (logWebhook.isBlank())
        return@launchThreaded
    sendEmbedToUrl(
        logWebhook, DiscordMessage(
            embeds = listOf(
                DiscordEmbed(
                    title = "New device registered",
                    description = """
                        IP: `$ip`
                        For user: `${user.name}`
                        DeviceInfo: 
                        ```json
                            ${jsonNoDefaultsPretty.encodeToString(deviceInfo)}
                        ```
                    """.trimIndent()
                )
            )
        )
    )
}

fun logDeviceChanges(ip: String, user: User, oldDevice: DeviceInfo, newDevice: DeviceInfo) = launchThreaded {
    val logWebhook = UnifiedConfig.current.discord.logChannelWebhookURL()
    if (logWebhook.isBlank())
        return@launchThreaded
    sendEmbedToUrl(
        logWebhook, DiscordMessage(
            embeds = listOf(
                DiscordEmbed(
                    title = "Hardware changed!",
                    description = """
                        IP: `$ip`
                        For user: `${user.name}`
                        Previously: 
                        ```json
                        ${jsonNoDefaultsPretty.encodeToString(oldDevice)}
                        ```
                        Now: 
                        ```json
                        ${jsonNoDefaultsPretty.encodeToString(newDevice)}
                        ```
                    """.trimIndent()
                )
            )
        )
    )
}