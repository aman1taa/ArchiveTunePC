package com.vaulttune

import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiHost
import java.io.File
import java.io.InputStream

/**
 * JVM implementation of [YoutubeiHost]: loads the youtubei.js assets from the
 * classpath and uses a plain directory for the disk cache.
 */
class ClasspathYoutubeiHost(
    private val cacheDirectory: File,
    private val classLoader: ClassLoader = ClasspathYoutubeiHost::class.java.classLoader,
) : YoutubeiHost {
    override fun openAsset(name: String): InputStream =
        classLoader.getResourceAsStream(name)
            ?: error("youtubei asset not found on classpath: $name")

    override val cacheDir: File
        get() = cacheDirectory.apply { mkdirs() }
}
