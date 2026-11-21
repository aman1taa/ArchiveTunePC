package com.vaulttune

import kotlinx.coroutines.runBlocking
import moe.rukamori.archivetune.innertube.NetworkGatekeeperControl
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.innertube.models.YouTubeLocale
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiAudioQuality
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiNetworkConfiguration
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiResolutionPriority
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiResolver
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiStreamRequest
import okhttp3.OkHttpClient
import okhttp3.Dns
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit
import com.vaulttune.potoken.BotGuardHttp
import com.vaulttune.potoken.PlaywrightPoTokenMinter

/**
 * Full end-to-end: search -> poToken (Playwright) -> resolve a playable stream URL.
 */
class StreamResolutionTest {
    @Test
    fun resolvesPlayableStream() =
        runBlocking {
            NetworkGatekeeperControl.allowConnections()
            YouTube.locale = YouTubeLocale(gl = "US", hl = "en")

            val songs =
                YouTube
                    .search("daft punk", YouTube.SearchFilter.FILTER_SONG, useAccountContext = false)
                    .getOrThrow().items.filterIsInstance<SongItem>()
            check(songs.isNotEmpty())
            val song = songs.first()
            println("song: ${song.title} (${song.id})")

            val visitorData = YouTube.visitorData().getOrNull()?.trim()?.takeIf { it.isNotBlank() }
            println("visitorData: ${visitorData?.take(24)}...")

            val html =
                javaClass.classLoader
                    .getResourceAsStream("po_token.html")!!
                    .bufferedReader()
                    .use { it.readText() }
            val script = html.substringAfter("<script>", "").substringBefore("</script>", "")
            val http =
                BotGuardHttp(
                    OkHttpClient
                        .Builder()
                        .followRedirects(false)
                        .followSslRedirects(false)
                        .callTimeout(30, TimeUnit.SECONDS)
                        .build(),
                    USER_AGENT,
                )

            val minter = PlaywrightPoTokenMinter(http, script)
            val host = ClasspathYoutubeiHost(File(System.getProperty("java.io.tmpdir"), "vaulttune-cache-test"))
            val resolver =
                YoutubeiResolver(host = host) {
                    YoutubeiNetworkConfiguration(proxy = null, proxyUsername = null, proxyPassword = null, dns = Dns.SYSTEM)
                }

            try {
                minter.initialize()
                val sessionToken = visitorData?.let { minter.mint(it) }
                println("sessionToken: ${sessionToken?.take(24)}...")

                val stream =
                    resolver.resolve(
                        request =
                            YoutubeiStreamRequest(
                                mediaId = song.id,
                                quality = YoutubeiAudioQuality.AUTO,
                                networkMetered = false,
                                authFingerprint = "",
                                visitorData = visitorData,
                                sessionPoToken = sessionToken,
                            ),
                        priority = YoutubeiResolutionPriority.FOREGROUND,
                        videoPoTokenProvider = { videoId -> minter.mint(videoId) },
                    )

                println("resolved: mime=${stream.mimeType} codecs=${stream.codecs} bitrate=${stream.bitrate}")
                println("url=${stream.url.take(120)}")

                check(stream.url.startsWith("http")) { "Expected a playable URL" }
            } finally {
                minter.close()
            }
        }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.3"
    }
}
