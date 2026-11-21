package com.vaulttune

import com.russhwolf.settings.Settings

/**
 * Typed wrapper over a key-value settings store.
 * On desktop/JVM this is backed by java.util.prefs (the Windows registry).
 */
class AppSettings(
    private val settings: Settings,
) {
    var localeGl: String
        get() = settings.getString("locale_gl", "US")
        set(value) = settings.putString("locale_gl", value)

    var localeHl: String
        get() = settings.getString("locale_hl", "en")
        set(value) = settings.putString("locale_hl", value)

    var lastQuery: String
        get() = settings.getString("last_query", "")
        set(value) = settings.putString("last_query", value)
}
