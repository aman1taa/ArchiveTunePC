package com.vaulttune.potoken

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * End-to-end test of the JCEF-based BotGuard poToken minting.
 *
 * Ignored for now: the JCEF (embedded Chromium) browser does not load pages in a
 * headless Gradle test on this machine (OSR needs a render handler, non-OSR
 * crashes the JVM). The minting code is structurally complete; the browser layer
 * needs to run inside the real desktop app (visible window) or via an external
 * Chromium (e.g. Playwright).
 */
class JcefBotGuardMinterTest {
    @Ignore("JCEF browser does not load headlessly; needs a window or external Chromium")
    @Test
    fun mintsToken() =
        runBlocking {
            val html =
                javaClass.classLoader
                    .getResourceAsStream("po_token.html")!!
                    .bufferedReader()
                    .use { it.readText() }

            val client =
                OkHttpClient
                    .Builder()
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .callTimeout(30, TimeUnit.SECONDS)
                    .build()
            val http = BotGuardHttp(client, USER_AGENT)

            val minter = JcefBotGuardMinter(http, html)
            try {
                minter.initialize()
                println("Minter ready")

                val token = minter.mint("test-session-id-123")
                println("Minted token (${token.length} chars): ${token.take(48)}...")
                check(token.isNotBlank() && token.length > 50) { "Expected a non-trivial token" }
            } finally {
                minter.close()
            }
        }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.3"
    }
}
