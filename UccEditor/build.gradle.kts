plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    application
}

application {
    mainClass.set("org.ucceditor.MainKt")
}

group = "org.ucceditor"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.11.0")

    // Kotlin Serialization (편집 패턴/명세 직렬화)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    // JetBrains Koog - AI Agent Framework
    implementation("ai.koog:koog-agents:1.0.0-preview7")
    implementation("ai.koog:koog-ktor:1.0.0-beta-preview7")

    // Test
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
