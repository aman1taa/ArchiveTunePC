package com.vaulttune

import kotlinx.coroutines.runBlocking
import moe.rukamori.archivetune.innertube.NetworkGatekeeperControl
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.innertube.models.YouTubeLocale
import org.junit.Test

/**
 * Smoke test proving that the `core` (InnerTube) module runs on the JVM.
 * Requires network access to YouTube Music.
 */
class CoreSmokeTest {
    @Test
    fun searchReturnsSongs() =
        runBlocking {
            NetworkGatekeeperControl.allowConnections()
            YouTube.locale = YouTubeLocale(gl = "US", hl = "en")
            val result = YouTube.search("daft punk", YouTube.SearchFilter.FILTER_SONG, useAccountContext = false)
            val songs = result.getOrThrow().items.filterIsInstance<SongItem>()

            println("Search returned ${songs.size} songs")
            songs.take(5).forEach { song ->
                println(" - ${song.title} — ${song.artists.joinToString(", ") { it.name }}")
            }

            check(songs.isNotEmpty()) { "Expected non-empty search results" }
        }
}
