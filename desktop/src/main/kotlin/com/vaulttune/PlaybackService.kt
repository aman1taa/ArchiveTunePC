package com.vaulttune

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiAudioQuality
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiResolutionPriority
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiResolver
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiResolvedStream
import moe.rukamori.archivetune.morideobfuscator.youtubei.YoutubeiStreamRequest
import com.vaulttune.potoken.PlaywrightPoTokenMinter

/**
 * Orchestrates the full playback pipeline: search -> visitorData -> poToken
 * (BotGuard via Playwright) -> youtubei.js stream resolution.
 */
class PlaybackService(
    private val searchRepository: SearchRepository,
    private val resolver: YoutubeiResolver,
    private val minter: PlaywrightPoTokenMinter,
) {
    private var minterReady = false

    suspend fun searchSongs(query: String): List<SongItem> = searchRepository.searchSongs(query)

    suspend fun resolve(song: SongItem): YoutubeiResolvedStream =
        withContext(Dispatchers.IO) {
            if (!minterReady) {
                minter.initialize()
                minterReady = true
            }
            val visitorData = YouTube.visitorData().getOrNull()?.trim()?.takeIf { it.isNotBlank() }
            val sessionToken = minter.mint(visitorData.orEmpty())

            resolver.resolve(
                request =
                    YoutubeiStreamRequest(
                        mediaId = song.id,
                        quality = YoutubeiAudioQuality.AUTO,
                        networkMetered = false,
                        authFingerprint = "",
                        visitorData = visitorData,
                        sessionPoToken = sessionToken,
                    ),
                priority = YoutubeiResolutionPriority.FOREGROUND,
                videoPoTokenProvider = { videoId -> minter.mint(videoId) },
            )
        }
}
