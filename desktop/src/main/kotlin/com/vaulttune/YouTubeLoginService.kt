package com.vaulttune

import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.WaitUntilState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.innertube.YouTube
import java.nio.file.Path

data class AccountIdentity(
    val name: String,
    val email: String,
)

class LoginTimeoutException : Exception("YouTube login timed out")

/**
 * Opens a visible Chromium window at YouTube Music, waits for the user to sign
 * in, then extracts the auth cookies and applies them to the InnerTube client.
 */
class YouTubeLoginService {
    suspend fun login(): Result<AccountIdentity> =
        withContext(Dispatchers.IO) {
            runCatching {
                var playwright: Playwright? = null
                var context: BrowserContext? = null
                try {
                    playwright = Playwright.create()
                    val profile = Path.of(System.getProperty("user.home"), ".vaulttune", "chrome-profile")
                    context =
                        playwright.chromium().launchPersistentContext(
                            profile,
                            BrowserType.LaunchPersistentContextOptions()
                                .setHeadless(false)
                                .setChannel("chrome")
                                .setViewportSize(1280, 800)
                                .setArgs(listOf("--disable-blink-features=AutomationControlled")),
                        )
                    context.addInitScript(
                        "Object.defineProperty(navigator, 'webdriver', { get: () => undefined });",
                    )
                    val page = context.pages().firstOrNull() ?: context.newPage()
                    page.navigate(
                        "https://music.youtube.com/",
                        Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED),
                    )

                    val cookie = waitForLoginCookie(context)
                    YouTube.cookie = cookie
                    val info = YouTube.accountInfo().getOrThrow()
                    AccountIdentity(info.name, info.email.orEmpty())
                } finally {
                    runCatching { context?.close() }
                    runCatching { playwright?.close() }
                }
            }
        }

    private suspend fun waitForLoginCookie(context: BrowserContext): String {
        val deadline = System.currentTimeMillis() + LOGIN_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val cookies = context.cookies("https://music.youtube.com")
            val hasLoginInfo = cookies.any { it.name == "LOGIN_INFO" && it.value.isNotBlank() }
            val hasSapisid = cookies.any { it.name in SAPISID_NAMES && it.value.isNotBlank() }
            if (hasLoginInfo && hasSapisid) {
                return cookies.joinToString("; ") { "${it.name}=${it.value}" }
            }
            delay(2000)
        }
        throw LoginTimeoutException()
    }

    companion object {
        private const val LOGIN_TIMEOUT_MS = 5 * 60 * 1000L
        private val SAPISID_NAMES = setOf("SAPISID", "__Secure-3PAPISID", "__Secure-1PAPISID")
    }
}
