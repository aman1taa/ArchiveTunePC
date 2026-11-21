import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.4.21"
    id("org.jetbrains.compose") version "1.13.0-rc01"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21"
}

group = "com.vaulttune"
version = "0.1.0"

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.compose.ui:ui:1.13.0-rc01")
    implementation("org.jetbrains.compose.foundation:foundation:1.13.0-rc01")
    implementation("org.jetbrains.compose.material3:material3:1.13.0-rc01")
}

kotlin {
    jvmToolchain(21)
}

compose.desktop {
    application {
        mainClass = "com.vaulttune.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "VaultTune"
            packageVersion = "0.1.0"
            description = "VaultTune — desktop port of ArchiveTune"
            vendor = "VaultTune"
        }
    }
}
