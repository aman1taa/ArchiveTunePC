package com.vaulttune

import com.sun.jna.NativeLibrary
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import java.nio.file.Files
import java.nio.file.Path

/**
 * [AudioPlayer] backed by VLCJ (libVLC).
 *
 * Locates the native libVLC either from a system property (`-Dvlc.path=...`),
 * a portable copy in `~/.tools/libvlc`, or a standard VLC install.
 */
class VlcjPlayer : AudioPlayer {
    private val factory: MediaPlayerFactory
    private val mediaPlayer: MediaPlayer

    init {
        val vlcDir = locateLibVlc()
        if (vlcDir != null) {
            NativeLibrary.addSearchPath("libvlc", vlcDir.toString())
            NativeLibrary.addSearchPath("libvlccore", vlcDir.toString())
        }
        // libVLC auto-locates its plugins/ directory relative to libvlc.dll.
        factory = MediaPlayerFactory()
        mediaPlayer = factory.mediaPlayers().newMediaPlayer()
    }

    override val isPlaying: Boolean
        get() = mediaPlayer.status().isPlaying()

    override fun play(url: String): Boolean = mediaPlayer.media().play(url)

    override fun pause() {
        mediaPlayer.controls().pause()
    }

    override fun stop() {
        mediaPlayer.controls().stop()
    }

    override fun release() {
        mediaPlayer.release()
        factory.release()
    }

    private fun locateLibVlc(): Path? {
        val candidates =
            buildList {
                System.getProperty("vlc.path")?.let { add(Path.of(it)) }
                add(Path.of(System.getProperty("user.home"), ".tools", "libvlc"))
                add(Path.of("C:/Program Files/VideoLAN/VLC"))
            }
        return candidates.firstOrNull { Files.exists(it.resolve("libvlc.dll")) }
    }
}
