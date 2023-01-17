package moe.styx.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.serialization.decodeFromString
import moe.styx.*
import java.util.*
import kotlin.random.Random

public fun Route.deviceLogin() {
    post("/login") {
        val form = call.receiveParameters()
        val token = form["token"]
        if (!token.isNullOrBlank()) {
            val device = getDevices().find { it.refreshToken.equals(token, true) }
            if (device == null) {
                call.respond(
                    HttpStatusCode.Unauthorized, ApiResponse(401, "No device has been found for this token.")
                )
                return@post
            }
            val users = getUsers().filter { it.GUID.equals(device.userID, true) }
            if (users.isEmpty()) {
                call.respond(
                    HttpStatusCode.Unauthorized, ApiResponse(401, "No user relating to this device has been found.")
                )
                return@post
            }
            call.respond(HttpStatusCode.OK, createLoginResponse(device, users[0]))
        } else
            call.respond(
                HttpStatusCode.BadRequest, ApiResponse(400, "No token was found in your request.")
            )
    }
}

public fun Route.deviceCreate() {
    post("/device/create") {
        val form = call.receiveParameters()
        val info = form["info"]
        val deviceInfo: DeviceInfo
        if (!info.isNullOrBlank()) {
            try {
                deviceInfo = json.decodeFromString<DeviceInfo>(info)
            } catch (ex: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest, ApiResponse(400, "Invalid DeviceInfo object has been sent!")
                )
                return@post
            }
        } else {
            call.respond(
                HttpStatusCode.BadRequest, ApiResponse(400, "Please include a device identifying field.")
            )
            return@post
        }

        val unregisteredDevice = UnregisteredDevice(
            UUID.randomUUID().toString().uppercase(), deviceInfo,
            Clock.System.now().epochSeconds + 70, Random.nextInt(100000, 999999)
        )

        if (unregisteredDevice.save()) {
            call.respond(HttpStatusCode.OK, CreationResponse(unregisteredDevice.GUID, unregisteredDevice.code, unregisteredDevice.codeExpiry))
        } else {
            call.respond(
                HttpStatusCode.InternalServerError, ApiResponse(500, "Something went wrong trying to connect to the database!")
            )
        }
    }
}

public fun Route.deviceFirstAuth() {
    post("/device/firstAuth") {
        val form = call.receiveParameters()
        val token = form["token"]

        if (token.isNullOrBlank()) {
            call.respond(
                HttpStatusCode.BadRequest, ApiResponse(400, "No token was found in your request.")
            )
            return@post
        }

        val device = getDevices().find { it.GUID.equals(token, true) }

        if (device == null) {
            call.respond(
                HttpStatusCode.Unauthorized, ApiResponse(401, "No device found for this token.", true)
            )
        } else {
            if (device.refreshToken.isNotBlank()) {
                call.respond(
                    HttpStatusCode.Forbidden, ApiResponse(403, "This device has already been registered.")
                )
                return@post
            }
            val users = getUsers().filter { it.GUID.equals(device.userID, true) }
            if (users.isEmpty()) {
                call.respond(
                    HttpStatusCode.Unauthorized, ApiResponse(401, "No user relating to this device has been found.")
                )
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

    device.save()
    user.lastLogin = now.epochSeconds
    user.save()

    return LoginResponse(user.name, 0, device.accessToken, device.watchToken, device.tokenExpiry, if (first) device.refreshToken else null)
}

