package com.vaulttune

import kotlinx.coroutines.runBlocking
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiNetworkConfiguration
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiResolver
import okhttp3.Dns
import org.junit.Test
import java.io.File

/**
 * Proves the JVM port of `morideobfuscator`: native QuickJS loads and the
 * youtubei.js bundle + polyfills are evaluated from the classpath.
 */
class YoutubeiEngineSmokeTest {
    @Test
    fun engineInitializes() =
        runBlocking {
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

            // Throws if QuickJS fails to load or the JS bundle is missing/mismatched.
            resolver.preWarm()
            println("youtubei.js engine initialized on JVM")

            resolver.invalidateSessions()
        }
}
