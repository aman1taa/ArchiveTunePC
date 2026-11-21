package com.vaulttune

import kotlinx.coroutines.runBlocking
import moe.rukamori.archivetune.innertube.NetworkGatekeeperControl
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.innertube.models.YouTubeLocale
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiNetworkConfiguration
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiResolver
import okhttp3.Dns
import org.junit.Ignore
import org.junit.Test
import java.io.File

/**
 * Smoke test for full search -> stream resolution on the JVM via youtubei.js.
 *
 * Ignored for now: anonymous playback needs a content-bound poToken, minted by
 * running YouTube's BotGuard challenge in a browser (WebView on Android, JCEF
 * on desktop). The pipeline itself (QuickJS + youtubei.js) is covered by
 * [YoutubeiEngineSmokeTest].
 */
class StreamSmokeTest {
    @Ignore("Blocked on JCEF-based poToken minting")
    @Test
    fun resolvesPlayableUrl() =
        runBlocking {
            NetworkGatekeeperControl.allowConnections()
            YouTube.locale = YouTubeLocale(gl = "US", hl = "en")

            val host =
                ClasspathYoutubeiHost(
                    File(System.getProperty("java.io.tmpdir"), "vaulttune-cache-test"),
                )
            val resolver =
                YoutubeiResolver(host = host) {
                    YoutubeiNetworkConfiguration(
                        proxy = null,
                        proxyUsername = null,
                        proxyPassword = null,
                        dns = Dns.SYSTEM,
                    )
                }

            val songs =
                YouTube
                    .search("daft punk", YouTube.SearchFilter.FILTER_SONG, useAccountContext = false)
                    .getOrThrow()
                    .items
                    .filterIsInstance<SongItem>()

            check(songs.isNotEmpty()) { "Expected search results" }

            val stream = StreamResolver(resolver).resolve(songs.first())

            println("Resolved stream for '${stream.title ?: songs.first().title}':")
            println("  mime=${stream.mimeType} bitrate=${stream.bitrate} codecs=${stream.codecs}")
            println("  url=${stream.url.take(100)}...")

            check(stream.url.startsWith("http")) { "Expected a playable URL, got: ${stream.url.take(80)}" }
        }
}
