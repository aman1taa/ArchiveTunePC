package com.vaulttune.di

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import com.vaulttune.AppSettings
import com.vaulttune.SearchRepository
import org.koin.dsl.module
import java.util.prefs.Preferences

val appModule =
    module {
        single<Settings> { PreferencesSettings(Preferences.userRoot().node("vaulttune")) }
        single { AppSettings(get()) }
        single { SearchRepository() }
    }
