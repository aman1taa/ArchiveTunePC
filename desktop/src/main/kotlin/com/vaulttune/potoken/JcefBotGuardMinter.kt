package com.vaulttune.potoken

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import me.friwi.jcefmaven.CefAppBuilder
import org.cef.CefApp
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.handler.CefMessageRouterHandlerAdapter
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import javax.swing.SwingUtilities

/**
 * Mints YouTube PoTokens by running the BotGuard attestation challenge inside an
 * off-screen JCEF (Chromium) browser, replacing the Android WebView implementation.
 */
class JcefBotGuardMinter(
    private val http: BotGuardHttp,
    private val html: String,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var cefApp: CefApp
    private lateinit var client: CefClient
    private lateinit var browser: CefBrowser
    private lateinit var router: CefMessageRouter

    private var pumpTimer: javax.swing.Timer? = null

    private val bootSignal = CompletableDeferred<Unit>()
    private var mintSignal: CompletableDeferred<String>? = null

    private var webPoSignalOutput: String? = null
    private var expiry: Instant = Instant.MIN

    /** Boots the browser, runs the challenge, and waits until the minter is ready. */
    suspend fun initialize() {
        setupBrowser()
        withTimeout(BOOT_TIMEOUT_MS) { bootSignal.await() }
    }

    /** Mints a single content-bound token for [identifier] (a session id or video id). */
    suspend fun mint(identifier: String): String {
        val signal = CompletableDeferred<String>()
        mintSignal = signal
        val quoted = JSONObject.quote(identifier)
        executeJs(
            """
            try {
                obtainPoToken(${stringToJsUint8Array(identifier)}).then(function(u8) {
                    window.__vaulttuneBridge("mintOk:$quoted:" + u8.join(","));
                }).catch(function(e) {
                    window.__vaulttuneBridge("mintErr:$quoted:" + (e + "\n" + (e.stack || '')));
                });
            } catch(e) {
                window.__vaulttuneBridge("mintErr:$quoted:" + (e + "\n" + (e.stack || '')));
            }
            """.trimIndent(),
        )
        return withTimeout(MINT_TIMEOUT_MS) { signal.await() }
    }

    fun close() {
        runCatching {
            SwingUtilities.invokeAndWait {
                runCatching { browser.close(true) }
                runCatching { client.removeMessageRouter(router) }
                runCatching { client.dispose() }
                runCatching { cefApp.dispose() }
            }
        }
    }

    // ── bridge handling ─────────────────────────────────────────────

    private fun handleQuery(request: String, callback: CefQueryCallback) {
        callback.success("")
        println("[minter] bridge <- ${request.take(140)}")
        val command = request.substringBefore(':')
        val arg = request.substringAfter(':', "")
        when (command) {
            CMD_PAGE_LOADED -> scope.launch { runBootStep("pageLoaded") { onPageLoaded() } }
            CMD_BOTGUARD_READY -> scope.launch { runBootStep("botGuardReady") { onBotGuardReady(arg) } }
            CMD_FATAL -> signalFatal(arg)
            CMD_MINTER_READY -> {
                println("[minter] minter ready")
                bootSignal.complete(Unit)
            }
            CMD_MINT_OK -> onMintOk(arg)
            CMD_MINT_ERR -> onMintErr(arg)
        }
    }

    private suspend fun runBootStep(name: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            println("[minter] boot step '$name' failed: $t")
            t.printStackTrace()
            if (!bootSignal.isCompleted) bootSignal.completeExceptionally(t)
        }
    }

    private suspend fun onPageLoaded() {
        println("[minter] fetching challenge page")
        val pageHtml = http.fetch(CHALLENGE_PAGE_URL)
        println("[minter] challenge page fetched (${pageHtml.length} bytes)")
        val page = parsePageChallenge(pageHtml)
        val interpreterUrl =
            page.challenge
                .getJSONObject("interpreterUrl")
                .getString("privateDoNotAccessOrElseTrustedResourceUrlWrappedValue")
                .let { if (it.startsWith("//")) "https:$it" else it }
        println("[minter] fetching interpreter: ${interpreterUrl.take(80)}")
        val interpreter = http.fetch(interpreterUrl)
        println("[minter] interpreter fetched (${interpreter.length} bytes)")
        if (interpreter.isBlank()) throw PoTokenException("Empty BotGuard interpreter")
        page.challenge.put(
            "interpreterJavascript",
            JSONObject().put("privateDoNotAccessOrElseSafeScriptWrappedValue", interpreter),
        )
        executeJs(
            """
            try {
                globalThis.yt = { config_: ${page.configuration} };
                var data = ${page.challenge};
                runBotGuard(data).then(function(r) {
                    this.webPoSignalOutput = r.webPoSignalOutput;
                    window.__vaulttuneBridge("botGuardReady:" + r.botguardResponse);
                }, function(e) {
                    window.__vaulttuneBridge("fatalError:" + (e + "\n" + (e.stack || '')));
                });
            } catch(e) {
                window.__vaulttuneBridge("fatalError:" + (e + "\n" + (e.stack || '')));
            }
            """.trimIndent(),
        )
    }

    private suspend fun onBotGuardReady(botguardResponse: String) {
        println("[minter] botguard ready, requesting integrity token")
        val raw = http.fetch(GENERATE_IT_URL, JSONArray().put(REQUEST_KEY).put(botguardResponse).toString())
        println("[minter] integrity token fetched")
        val (tokenU8, lifetimeSec) = parseIntegrityToken(raw)
        expiry = Instant.now().plusSeconds(lifetimeSec).minus(10, ChronoUnit.MINUTES)
        executeJs(
            """
            try {
                this.integrityToken = $tokenU8;
                createPoTokenMinter(webPoSignalOutput, integrityToken).then(function() {
                    window.__vaulttuneBridge("minterReady");
                }).catch(function(e) {
                    window.__vaulttuneBridge("fatalError:" + (e + "\n" + (e.stack || '')));
                });
            } catch(e) {
                window.__vaulttuneBridge("fatalError:" + (e + "\n" + (e.stack || '')));
            }
            """.trimIndent(),
        )
    }

    private fun onMintOk(arg: String) {
        val csvBytes = arg.substringAfter(':', "")
        mintSignal?.complete(commaSeparatedBytesToBase64(csvBytes))
    }

    private fun onMintErr(arg: String) {
        val error = arg.substringAfter(':', "")
        mintSignal?.completeExceptionally(classifyJsError(error))
    }

    private fun signalFatal(error: String) {
        println("[minter] FATAL: ${error.take(200)}")
        val failure = classifyJsError(error)
        if (!bootSignal.isCompleted) bootSignal.completeExceptionally(failure)
        mintSignal?.completeExceptionally(failure)
    }

    private fun executeJs(code: String) {
        browser.executeJavaScript(code, "https://www.youtube.com/", 0)
    }

    // ── setup ───────────────────────────────────────────────────────

    private fun setupBrowser() {
        SwingUtilities.invokeAndWait {
            val builder = CefAppBuilder()
            builder.setInstallDir(File(System.getProperty("java.io.tmpdir"), "vaulttune-jcef"))
            builder.setProgressHandler { _, _ -> }
            builder.install()
            cefApp = builder.build()

            client = cefApp.createClient()

            client.addDisplayHandler(
                object : CefDisplayHandlerAdapter() {
                    override fun onConsoleMessage(
                        browser: CefBrowser,
                        level: org.cef.CefSettings.LogSeverity,
                        message: String,
                        source: String,
                        line: Int,
                    ): Boolean {
                        println("[cef-console] $message")
                        return false
                    }
                },
            )
            client.addLoadHandler(
                object : CefLoadHandlerAdapter() {
                    override fun onLoadingStateChange(
                        browser: CefBrowser,
                        isLoading: Boolean,
                        canGoBack: Boolean,
                        canGoForward: Boolean,
                    ) {
                        println("[cef-load] isLoading=$isLoading url=${browser.url}")
                    }

                    override fun onLoadError(
                        browser: CefBrowser,
                        frame: CefFrame,
                        errorCode: CefLoadHandler.ErrorCode,
                        errorText: String,
                        failedUrl: String,
                    ) {
                        println("[cef-load-error] $errorCode $errorText url=${failedUrl.take(80)}")
                    }
                },
            )

            router =
                CefMessageRouter.create(
                    CefMessageRouter.CefMessageRouterConfig("cefQuery", "cefQueryCancel"),
                )
            client.addMessageRouter(router)
            router.addHandler(
                object : CefMessageRouterHandlerAdapter() {
                    override fun onQuery(
                        browser: CefBrowser,
                        frame: CefFrame,
                        queryId: Long,
                        request: String,
                        persistent: Boolean,
                        callback: CefQueryCallback,
                    ): Boolean {
                        handleQuery(request, callback)
                        return true
                    }
                },
                true,
            )

            browser = client.createBrowser("about:blank", true, false)
            val patched =
                html.replaceFirst("</script>", "\n$BRIDGE_JS\nwindow.__vaulttuneBridge('pageLoaded');</script>")
            val dataUrl =
                "data:text/html;charset=utf-8;base64," +
                    Base64.getEncoder().encodeToString(patched.toByteArray(Charsets.UTF_8))
            browser.loadURL(dataUrl)

            // Explicitly pump the CEF message loop (the default Swing timer may
            // not run in headless/test contexts).
            pumpTimer =
                javax.swing.Timer(10) {
                    runCatching { cefApp.doMessageLoopWork(0) }
                }.also { it.start() }
        }
    }

    companion object {
        private const val CHALLENGE_PAGE_URL = "https://www.youtube.com/?hl=en"
        private const val GENERATE_IT_URL = "https://www.youtube.com/api/jnn/v1/GenerateIT"
        private const val REQUEST_KEY = "O43z0dpjhgX20SCx4KAo"
        private const val BOOT_TIMEOUT_MS = 90_000L
        private const val MINT_TIMEOUT_MS = 30_000L

        private const val CMD_PAGE_LOADED = "pageLoaded"
        private const val CMD_BOTGUARD_READY = "botGuardReady"
        private const val CMD_FATAL = "fatalError"
        private const val CMD_MINTER_READY = "minterReady"
        private const val CMD_MINT_OK = "mintOk"
        private const val CMD_MINT_ERR = "mintErr"

        private val BRIDGE_JS =
            """
            window.__vaulttuneBridge = function(payload) {
                return new Promise(function(resolve, reject) {
                    window.cefQuery({
                        request: payload,
                        onSuccess: function(response) { resolve(response); },
                        onFailure: function(code, msg) { reject(new Error(msg)); }
                    });
                });
            };
            """.trimIndent()
    }
}
