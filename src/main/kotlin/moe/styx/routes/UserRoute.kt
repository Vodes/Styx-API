package moe.styx.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import moe.styx.*
import java.util.*
import kotlin.random.Random

public fun Route.deviceLogin() {
    post("/login") {
        val form = call.receiveParameters()
        val token = form["token"]
        if (!token.isNullOrBlank()) {
            val response = LoginResponse("Hurensohn", 0, "", "", 56212356)
            call.respond(HttpStatusCode.OK, response)
        } else
            call.respond(HttpStatusCode.BadRequest, "No token was found in your request.")
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
                call.respond(HttpStatusCode.BadRequest, "Invalid DeviceInfo object has been sent!")
                return@post
            }
        } else {
            call.respond(HttpStatusCode.BadRequest, "Please include a device identifying field.")
            return@post
        }

        val unregisteredDevice = UnregisteredDevice(
            UUID.randomUUID().toString().uppercase(), deviceInfo,
            Clock.System.now().epochSeconds + 70, Random.nextInt(100000, 999999)
        )

        if (unregisteredDevice.save()) {
            call.respond(HttpStatusCode.OK, CreationResponse(unregisteredDevice.code, unregisteredDevice.codeExpiry))
        } else {
            call.respond(HttpStatusCode.InternalServerError, "Something went wrong trying to connect to the database!")
        }
    }
}
