package com.vaulttune

/** Minimal audio playback surface, independent of the underlying engine. */
interface AudioPlayer {
    val isPlaying: Boolean

    fun play(url: String): Boolean

    fun pause()

    fun stop()

    fun release()
}
