package com.vaulttune.di

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import com.vaulttune.AppSettings
import com.vaulttune.AudioPlayer
import com.vaulttune.ClasspathYoutubeiHost
import com.vaulttune.PlaybackService
import com.vaulttune.SearchRepository
import com.vaulttune.VlcjPlayer
import com.vaulttune.YouTubeLoginService
import com.vaulttune.potoken.BotGuardHttp
import com.vaulttune.potoken.PlaywrightPoTokenMinter
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiHost
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiNetworkConfiguration
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiResolver
import okhttp3.Dns
import okhttp3.OkHttpClient
import org.koin.dsl.module
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.prefs.Preferences

private const val USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.3"

val appModule =
    module {
        single<Settings> { PreferencesSettings(Preferences.userRoot().node("vaulttune")) }
        single { AppSettings(get()) }
        single { SearchRepository() }
        single<AudioPlayer> { VlcjPlayer() }

        single<YoutubeiHost> {
            ClasspathYoutubeiHost(
                cacheDirectory = File(System.getProperty("user.home"), ".vaulttune/cache"),
            )
        }
        single {
            YoutubeiResolver(
                host = get(),
                networkConfigurationProvider = {
                    YoutubeiNetworkConfiguration(
                        proxy = null,
                        proxyUsername = null,
                        proxyPassword = null,
                        dns = Dns.SYSTEM,
                    )
                },
            )
        }
        single<BotGuardHttp> {
            BotGuardHttp(
                OkHttpClient
                    .Builder()
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .callTimeout(30, TimeUnit.SECONDS)
                    .build(),
                USER_AGENT,
            )
        }
        single {
            val html =
                PlaywrightPoTokenMinter::class.java.classLoader
                    .getResourceAsStream("po_token.html")!!
                    .bufferedReader()
                    .use { it.readText() }
            val script = html.substringAfter("<script>", "").substringBefore("</script>", "")
            PlaywrightPoTokenMinter(get(), script)
        }
        single { PlaybackService(get(), get(), get()) }
        single { YouTubeLoginService() }
    }
