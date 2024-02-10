package moe.styx.misc

import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import moe.styx.common.http.httpClient
import moe.styx.common.json
import moe.styx.config

object DiscordAPI {
    fun getUserFromToken(token: String): DiscordUser? = runBlocking {
        if (token.isBlank())
            return@runBlocking null

        val response = httpClient.get("https://discord.com/api/users/@me") {
            accept(ContentType.Application.Json)
            bearerAuth(token)
            userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
        }

        if (response.status != HttpStatusCode.OK)
            return@runBlocking null
        return@runBlocking json.decodeFromString(response.bodyAsText())
    }

    suspend fun handleIncomingCode(code: String, call: ApplicationCall) {
        val response = httpClient.submitForm("https://discord.com/api/oauth2/token", formParameters = parameters {
            append("grant_type", "authorization_code")
            append("client_id", config.discordClientID)
            append("client_secret", config.discordClientSecret)
            append("redirect_uri", "${config.baseURL}/discord/auth")
            append("code", code)
        }) {
            method = HttpMethod.Post
            contentType(ContentType.Application.FormUrlEncoded)
            accept(ContentType.Application.Json)
        }
        if (response.status != HttpStatusCode.OK) {
            call.respond("Invalid auth code!")
            return
        }
        val data = json.decodeFromString<DiscordTokenResponse>(response.bodyAsText())
        call.response.cookies.append(
            Cookie(
                "access_token",
                data.accessToken,
                maxAge = data.expiresIn,
                domain = ".styx.moe",
                path = "/",
                extensions = mapOf("SameSite" to "lax")
            )
        )
        call.respondRedirect(config.mainSiteBaseURL)
    }

    fun buildAuthURL(): String {
        val builder = URLBuilder("https://discord.com/api/oauth2/authorize")
        builder.parameters.append("client_id", config.discordClientID)
        builder.parameters.append("redirect_uri", "${config.baseURL}/discord/auth")
        builder.parameters.append("response_type", "code")
        builder.parameters.append("scope", "identify guilds")
        return builder.buildString()
    }
}

@Serializable
data class DiscordUser(
    val id: String,
    val username: String,
    @SerialName("global_name")
    val globalName: String
)

@Serializable
data class DiscordTokenResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("token_type")
    val tokenType: String,
    @SerialName("expires_in")
    val expiresIn: Int,
    @SerialName("refresh_token")
    val refreshToken: String
)