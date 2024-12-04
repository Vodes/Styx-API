package moe.styx.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.serialization.encodeToString
import moe.styx.*
import moe.styx.common.config.UnifiedConfig
import moe.styx.common.data.*
import moe.styx.common.extension.eqI
import moe.styx.common.extension.toBoolean
import moe.styx.common.json
import moe.styx.common.util.launchGlobal
import moe.styx.db.tables.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import java.util.*
import kotlin.random.Random

fun Route.deviceLogin() {
    post("/login") {
        val form = call.receiveParameters()
        val token = form["token"]
        val deviceInfo = call.receiveGenericContent<DeviceInfo>(form) ?: return@post
        val (user, device) = checkTokenDeviceUser(token, call, login = true)
        if (device == null || user == null)
            return@post

        val secretCheckResult = checkSecret(deviceInfo, call)
        if (!secretCheckResult)
            return@post

        launchGlobal {
            transaction {
                var ip = call.request.origin.remoteAddress
                if (call.request.headers.contains("X-PROXY-IP")) {
                    ip = call.request.headers["X-PROXY-IP"] ?: ""
                } else if (call.request.headers.contains("CF-Connecting-IP")) {
                    ip = call.request.headers["CF-Connecting-IP"] ?: ""
                }
                LogTable.upsertItem(
                    Log(
                        user.GUID,
                        device.GUID,
                        LogType.LOGIN,
                        json.encodeToString(IPDeviceEntry(ip, deviceInfo.copy(appSecret = ""))),
                        Clock.System.now().epochSeconds
                    )
                )
            }
        }
        call.respond(HttpStatusCode.OK, createLoginResponse(device, user))
    }

    post("/logout") {
        val form = call.receiveParameters()
        val token = form["token"]
        val (user, device) = checkTokenDeviceUser(token, call)
        if (device == null || user == null)
            return@post

        val activeUsers = transaction { ActiveUserTable.query { selectAll().toList() } }

        launchGlobal {
            transaction {
                val active = activeUsers.find { it.deviceID eqI device.GUID }
                if (active != null)
                    ActiveUserTable.deleteWhere { deviceID eq device.GUID }

                var ip = call.request.origin.remoteAddress
                if (call.request.headers.contains("X-PROXY-IP")) {
                    ip = call.request.headers["X-PROXY-IP"] ?: ""
                } else if (call.request.headers.contains("CF-Connecting-IP")) {
                    ip = call.request.headers["CF-Connecting-IP"] ?: ""
                }
                LogTable.upsertItem(Log(user.GUID, device.GUID, LogType.LOGOUT, "IP: $ip", Clock.System.now().epochSeconds))
            }
        }
        call.respond(HttpStatusCode.OK)
    }
}

fun Route.deviceCreate() {
    post("/device/create") {
        val form = call.receiveParameters()
        val deviceInfo = call.receiveGenericContent<DeviceInfo>(form, "info") ?: return@post

        val secretCheckResult = checkSecret(deviceInfo, call)
        if (!secretCheckResult)
            return@post

        val unregisteredDevice = UnregisteredDevice(
            UUID.randomUUID().toString().uppercase(), deviceInfo.copy(appSecret = ""),
            Clock.System.now().epochSeconds + 70, Random.nextInt(100000, 999999)
        )

        if (transaction { UnregisteredDeviceTable.upsertItem(unregisteredDevice) }.insertedCount.toBoolean()) {
            call.respond(HttpStatusCode.OK, CreationResponse(unregisteredDevice.GUID, unregisteredDevice.code, unregisteredDevice.codeExpiry))
        } else {
            call.respondStyx(HttpStatusCode.InternalServerError, "Something went wrong trying to connect to the database!")
        }
    }
}

fun Route.deviceFirstAuth() {
    post("/device/firstAuth") {
        val form = call.receiveParameters()
        val token = form["token"]

        if (token.isNullOrBlank()) {
            call.respondStyx(HttpStatusCode.BadRequest, "No token was found in your request.")
            return@post
        }
        val device = transaction { DeviceTable.query { selectAll().where { GUID eq token }.toList() }.firstOrNull() }

        if (device == null) {
            call.respondStyx(HttpStatusCode.Unauthorized, "No device found for this token.")
        } else {
            if (device.refreshToken.isNotBlank()) {
                call.respondStyx(HttpStatusCode.Forbidden, "This device has already been registered.")
                return@post
            }
            val user = transaction { UserTable.query { selectAll().where { GUID eq device.userID }.toList() } }.firstOrNull()
            if (user == null) {
                call.respondStyx(HttpStatusCode.Unauthorized, "No user relating to this device has been found.")
                return@post
            }
            call.respond(HttpStatusCode.OK, createLoginResponse(device, user, true))
        }
    }
}

private suspend fun checkSecret(deviceInfo: DeviceInfo, call: ApplicationCall): Boolean {
    if (!UnifiedConfig.current.apiConfig.enableSecretsCheck) {
        return true
    }
    val secret = secretsFile.readLines().filter { it.trim().isNotBlank() }.find { it eqI deviceInfo.appSecret }
    if (secret == null) {
        call.respondStyx(HttpStatusCode.FailedDependency, "Invalid app secret.")
        return false
    }
    return true
}

private fun createLoginResponse(device: Device, user: User, first: Boolean = false): LoginResponse {
    device.accessToken = UUID.randomUUID().toString().uppercase()
    device.watchToken = UUID.randomUUID().toString().uppercase()
    val now = Clock.System.now()
    device.lastUsed = now.epochSeconds
    device.tokenExpiry = now.plus(24, DateTimeUnit.HOUR).epochSeconds
    if (first)
        device.refreshToken = UUID.randomUUID().toString().uppercase()

    dbClient.transaction {
        DeviceTable.upsertItem(device)
        UserTable.upsertItem(user.copy(lastLogin = now.epochSeconds))
    }

    return LoginResponse(
        user.name,
        user.GUID,
        user.permissions,
        device.accessToken,
        device.watchToken,
        device.tokenExpiry,
        if (first) device.refreshToken else null
    )
}

