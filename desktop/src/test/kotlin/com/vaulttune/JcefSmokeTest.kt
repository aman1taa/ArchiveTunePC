package com.vaulttune

import me.friwi.jcefmaven.CefAppBuilder
import org.junit.Test
import java.io.File

/**
 * Smoke test proving JCEF (embedded Chromium) downloads, initializes, and
 * creates an off-screen browser on this machine.
 */
class JcefSmokeTest {
    @Test
    fun chromiumLoads() {
        val installDir = File(System.getProperty("java.io.tmpdir"), "vaulttune-jcef")
        val builder = CefAppBuilder()
        builder.setInstallDir(installDir)
        builder.setProgressHandler { _, _ -> }
        builder.install() // downloads the ~150 MB Chromium bundle on first run

        val app = builder.build()
        val client = app.createClient()
        val browser = client.createBrowser("about:blank", true, false) // off-screen rendering

        Thread.sleep(3000)
        println("Chromium browser created (id=${browser.identifier})")

        browser.close(true)
        client.dispose()
        app.dispose()
    }
}
