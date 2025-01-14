val ktorVersion: String by project
val logbackVersion: String by project
val brotliVersion: String by project

plugins {
    kotlin("jvm") version "2.1.0"
    id("io.ktor.plugin") version "3.0.3"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
}

group = "moe.styx"
version = "0.3.5"

application {
    mainClass.set("moe.styx.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven("https://repo.styx.moe/releases")
    maven("https://repo.styx.moe/snapshots")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("moe.styx:styx-db:0.4.6")

    // Ktor
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-resources:$ktorVersion")
    implementation("io.ktor:ktor-server-compression-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-partial-content-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auto-head-response:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-network-tls-jvm:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // Misc
    implementation("io.github.z4kn4fein:semver:2.0.0")
    implementation("org.postgresql:postgresql:42.7.4")

    // Brotli
//    implementation("com.aayushatharva.brotli4j:brotli4j:$brotliVersion")
//    runtimeOnly("com.aayushatharva.brotli4j:native-windows-x86_64:$brotliVersion")
//    runtimeOnly("com.aayushatharva.brotli4j:native-windows-aarch64:$brotliVersion")
//    runtimeOnly("com.aayushatharva.brotli4j:native-linux-x86_64:$brotliVersion")
//    runtimeOnly("com.aayushatharva.brotli4j:native-linux-aarch64:$brotliVersion")
}

kotlin {
    jvmToolchain(21)
}

tasks.register("shadow-ci") {
    dependsOn("buildFatJar")
    doLast {
        val buildDir = File(projectDir, "build")
        buildDir.walk().find { it.extension == "jar" && it.name.contains("-all") }?.copyTo(File(projectDir, "app.jar"))
    }
}