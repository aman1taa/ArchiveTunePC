package com.vaulttune.potoken

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Test
import java.util.concurrent.TimeUnit

/** End-to-end test of the Playwright-based BotGuard poToken minting. */
class PlaywrightPoTokenMinterTest {
    @Test
    fun mintsToken() =
        runBlocking {
            val html =
                javaClass.classLoader
                    .getResourceAsStream("po_token.html")!!
                    .bufferedReader()
                    .use { it.readText() }
            val script = html.substringAfter("<script>", "").substringBefore("</script>", "")

            val client =
                OkHttpClient
                    .Builder()
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .callTimeout(30, TimeUnit.SECONDS)
                    .build()
            val http = BotGuardHttp(client, USER_AGENT)

            val minter = PlaywrightPoTokenMinter(http, script)
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
