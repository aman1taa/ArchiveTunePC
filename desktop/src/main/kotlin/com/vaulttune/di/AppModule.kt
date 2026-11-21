package com.vaulttune.di

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import com.vaulttune.AppSettings
import com.vaulttune.ClasspathYoutubeiHost
import com.vaulttune.SearchRepository
import com.vaulttune.StreamResolver
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiHost
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiNetworkConfiguration
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiResolver
import okhttp3.Dns
import org.koin.dsl.module
import java.io.File
import java.util.prefs.Preferences

val appModule =
    module {
        single<Settings> { PreferencesSettings(Preferences.userRoot().node("vaulttune")) }
        single { AppSettings(get()) }
        single { SearchRepository() }

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
        single { StreamResolver(get()) }
    }
