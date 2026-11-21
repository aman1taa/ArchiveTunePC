package com.vaulttune.potoken

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.WaitUntilState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

/**
 * Mints YouTube PoTokens by running the BotGuard attestation challenge inside an
 * external (Playwright-driven) Chromium browser.
 */
class PlaywrightPoTokenMinter(
    private val http: BotGuardHttp,
    private val script: String,
) {
    private lateinit var playwright: Playwright
    private lateinit var browser: Browser
    private lateinit var page: Page

    suspend fun initialize() =
        withContext(Dispatchers.IO) {
            withTimeout(INIT_TIMEOUT_MS) {
                println("[pw] launching chromium")
                playwright = Playwright.create()
                browser = playwright.chromium().launch(BrowserType.LaunchOptions().setHeadless(true))
                page =
                    browser
                        .newContext(
                            Browser.NewContextOptions()
                                .setUserAgent(USER_AGENT)
                                .setViewportSize(1280, 720),
                        ).newPage()
                page.setDefaultTimeout(30_000.0)

                println("[pw] navigating to youtube.com")
                page.navigate(
                    "https://www.youtube.com/?hl=en",
                    Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED),
                )
                println("[pw] injecting botguard script")
                page.evaluate(script)

                println("[pw] fetching challenge page")
                val pageHtml = http.fetch(CHALLENGE_PAGE_URL)
                println("[pw] challenge page fetched (${pageHtml.length} bytes)")
                val challenge = parsePageChallenge(pageHtml)
                val interpreterUrl =
                    challenge.challenge
                        .getJSONObject("interpreterUrl")
                        .getString("privateDoNotAccessOrElseTrustedResourceUrlWrappedValue")
                        .let { if (it.startsWith("//")) "https:$it" else it }
                println("[pw] fetching interpreter: ${interpreterUrl.take(80)}")
                val interpreter = http.fetch(interpreterUrl)
                println("[pw] interpreter fetched (${interpreter.length} bytes)")
                if (interpreter.isBlank()) throw PoTokenException("Empty BotGuard interpreter")
                challenge.challenge.put(
                    "interpreterJavascript",
                    JSONObject().put("privateDoNotAccessOrElseSafeScriptWrappedValue", interpreter),
                )

                println("[pw] running botguard challenge")
                val botguardResponse =
                    page.evaluate(
                        """
                        (function() {
                            globalThis.yt = { config_: ${challenge.configuration} };
                            var data = ${challenge.challenge};
                            return runBotGuard(data).then(function(r) {
                                window.__w = r.webPoSignalOutput;
                                return r.botguardResponse;
                            });
                        })();
                        """.trimIndent(),
                    ) as String
                println("[pw] botguard responded (${botguardResponse.length} chars)")

                println("[pw] requesting integrity token")
                val (tokenU8, _) =
                    parseIntegrityToken(
                        http.fetch(GENERATE_IT_URL, JSONArray().put(REQUEST_KEY).put(botguardResponse).toString()),
                    )
                println("[pw] integrity token parsed")

                println("[pw] creating poToken minter")
                page.evaluate("createPoTokenMinter(window.__w, $tokenU8)")
                println("[pw] minter ready")
            }
        }

    suspend fun mint(identifier: String): String =
        withContext(Dispatchers.IO) {
            withTimeout(MINT_TIMEOUT_MS) {
                val csv =
                    page.evaluate(
                        "obtainPoToken(${stringToJsUint8Array(identifier)}).then(function(u8) { return Array.from(u8).join(','); })",
                    ) as String
                commaSeparatedBytesToBase64(csv)
            }
        }

    suspend fun close() =
        withContext(Dispatchers.IO) {
            runCatching { browser.close() }
            runCatching { playwright.close() }
        }

    companion object {
        private const val CHALLENGE_PAGE_URL = "https://www.youtube.com/?hl=en"
        private const val GENERATE_IT_URL = "https://www.youtube.com/api/jnn/v1/GenerateIT"
        private const val REQUEST_KEY = "O43z0dpjhgX20SCx4KAo"
        private const val INIT_TIMEOUT_MS = 120_000L
        private const val MINT_TIMEOUT_MS = 30_000L
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.3"
    }
}
