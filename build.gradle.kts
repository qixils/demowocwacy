plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${property("coroutines.version")}")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}