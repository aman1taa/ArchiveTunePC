package com.vaulttune

import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiAudioQuality
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiResolutionPriority
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiResolver
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiResolvedStream
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiStreamRequest

/**
 * Resolves a search result into a playable audio stream via youtubei.js.
 */
class StreamResolver(
    private val resolver: YoutubeiResolver,
) {
    suspend fun resolve(song: SongItem): YoutubeiResolvedStream =
        resolver.resolve(
            request =
                YoutubeiStreamRequest(
                    mediaId = song.id,
                    quality = YoutubeiAudioQuality.AUTO,
                    networkMetered = false,
                    authFingerprint = "",
                ),
            priority = YoutubeiResolutionPriority.FOREGROUND,
        )
}
