pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        exclusiveContent {
            forRepository {
                maven { url = uri("https://jitpack.io") }
            }
            filter {
                includeGroup("com.github.TeamNewPipe")
            }
        }
    }
}

rootProject.name = "VaultTuneDesktop"

include(":core")
project(":core").projectDir = file("../core")

include(":lyrics")
project(":lyrics").projectDir = file("../lyrics")

listOf(
    "kugou",
    "lrclib",
    "simpmusic",
    "paxsenix",
    "betterlyrics",
    "unison",
    "youlyplus",
    "lrcred",
    "binimum",
    "megalobiz",
    "musixmatch",
).forEach { name ->
    include(":lyrics:$name")
    project(":lyrics:$name").projectDir = file("../lyrics/$name")
}

include(":morideobfuscator")
project(":morideobfuscator").projectDir = file("../morideobfuscator")
