package moe.styx.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import moe.styx.db.getDevices
import moe.styx.db.getUsers
import moe.styx.db.save
import moe.styx.getDBClient
import moe.styx.respondStyx
import moe.styx.types.*
import java.util.*
import kotlin.random.Random

fun Route.deviceLogin() {
    post("/login") {
        val form = call.receiveParameters()
        val token = form["token"]
        val (user, device) = checkTokenDeviceUser(token, call, login = true)
        if (device == null || user == null)
            return@post
        call.respond(HttpStatusCode.OK, createLoginResponse(device, user))
    }
}

fun Route.deviceCreate() {
    post("/device/create") {
        val form = call.receiveParameters()
        val info = form["info"]
        val deviceInfo: DeviceInfo
        if (!info.isNullOrBlank()) {
            try {
                deviceInfo = json.decodeFromString<DeviceInfo>(info)
            } catch (ex: Exception) {
                call.respondStyx(HttpStatusCode.BadRequest, "Invalid DeviceInfo object has been sent!")
                return@post
            }
        } else {
            call.respondStyx(HttpStatusCode.BadRequest, "Please include a device identifying field.")
            return@post
        }

        val unregisteredDevice = UnregisteredDevice(
            UUID.randomUUID().toString().uppercase(), deviceInfo,
            Clock.System.now().epochSeconds + 70, Random.nextInt(100000, 999999)
        )

        if (getDBClient().executeGet { save(unregisteredDevice) }) {
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

        val device = getDBClient().executeGet { getDevices(mapOf("GUID" to token)).firstOrNull() }

        if (device == null) {
            call.respondStyx(HttpStatusCode.Unauthorized, "No device found for this token.")
        } else {
            if (device.refreshToken.isNotBlank()) {
                call.respondStyx(HttpStatusCode.Forbidden, "This device has already been registered.")
                return@post
            }
            val users = getDBClient().executeGet { getUsers(mapOf("GUID" to device.userID)) }
            if (users.isEmpty()) {
                call.respondStyx(HttpStatusCode.Unauthorized, "No user relating to this device has been found.")
                return@post
            }
            call.respond(HttpStatusCode.OK, createLoginResponse(device, users[0], true))
        }
    }
}

private fun createLoginResponse(device: Device, user: User, first: Boolean = false): LoginResponse {
    device.accessToken = UUID.randomUUID().toString().uppercase()
    device.watchToken = UUID.randomUUID().toString().uppercase()
    val now = Clock.System.now()
    device.lastUsed = now.epochSeconds
    device.tokenExpiry = now.plus(24, DateTimeUnit.HOUR).epochSeconds
    if (first)
        device.refreshToken = UUID.randomUUID().toString().uppercase()

    val dbClient = getDBClient()
    dbClient.save(device)
    user.lastLogin = now.epochSeconds
    dbClient.executeAndClose { dbClient.save(user) }

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

