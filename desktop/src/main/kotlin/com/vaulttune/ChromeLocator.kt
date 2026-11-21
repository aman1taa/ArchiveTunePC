package com.vaulttune

import java.nio.file.Files
import java.nio.file.Path

/**
 * Locates a usable Chrome/Chromium executable for Playwright:
 * 1) a Chromium bundled next to the app (for the packaged .exe),
 * 2) the Playwright-managed browser in ~/AppData/Local/ms-playwright,
 * 3) the system Chrome/Edge install.
 */
object ChromeLocator {
    private val SYSTEM_BROWSERS =
        listOf(
            "C:/Program Files/Google/Chrome/Application/chrome.exe",
            "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe",
            "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe",
            "C:/Program Files/Microsoft/Edge/Application/msedge.exe",
        )

    fun findChromiumExecutable(): String? =
        bundledChrome()
            ?: playwrightChrome()
            ?: systemChrome()

    private fun bundledChrome(): String? =
        runCatching {
            val appDir = Path.of(System.getProperty("java.home")).parent
            val browsers = appDir.resolve("browsers")
            if (!Files.isDirectory(browsers)) return@runCatching null
            Files.walk(browsers).use { stream ->
                stream
                    .filter { it.fileName.toString().equals("chrome.exe", ignoreCase = true) }
                    .findFirst()
                    .orElse(null)
            }?.toString()
        }.getOrNull()

    private fun playwrightChrome(): String? =
        runCatching {
            val ms = Path.of(System.getProperty("user.home"), "AppData", "Local", "ms-playwright")
            if (!Files.isDirectory(ms)) return@runCatching null
            Files.walk(ms).use { stream ->
                stream
                    .filter { it.fileName.toString().equals("chrome.exe", ignoreCase = true) }
                    .findFirst()
                    .orElse(null)
            }?.toString()
        }.getOrNull()

    private fun systemChrome(): String? = SYSTEM_BROWSERS.firstOrNull { Files.exists(Path.of(it)) }
}
