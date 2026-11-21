import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

group = "com.vaulttune"
version = "0.1.0"

dependencies {
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.coroutines.core)

    implementation(project(":core"))

    implementation(project(":lyrics:kugou"))
    implementation(project(":lyrics:lrclib"))
    implementation(project(":lyrics:simpmusic"))
    implementation(project(":lyrics:paxsenix"))
    implementation(project(":lyrics:betterlyrics"))
    implementation(project(":lyrics:unison"))
    implementation(project(":lyrics:youlyplus"))
    implementation(project(":lyrics:lrcred"))
    implementation(project(":lyrics:binimum"))
    implementation(project(":lyrics:megalobiz"))
    implementation(project(":lyrics:musixmatch"))

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.core)
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
