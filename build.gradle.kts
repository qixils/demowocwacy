plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    application
}

group = "dev.qixils.demowocwacy"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("net.dv8tion:JDA:${property("jda.version")}")
    implementation("club.minnced:jda-ktx:${property("jda-ktx.version")}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-hocon:${property("serialization.version")}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:${property("serialization.version")}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${property("serialization.version")}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${property("coroutines.version")}")
    implementation("com.aallam.openai:openai-client:4.0.1")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.18")
    runtimeOnly("io.ktor:ktor-client-okhttp:3.1.2")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("dev.qixils.demowocwacy.Bot")
}