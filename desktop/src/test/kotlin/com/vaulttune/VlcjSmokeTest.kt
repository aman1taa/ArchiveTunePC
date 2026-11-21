package com.vaulttune

import org.junit.Test

/**
 * Smoke test proving that the VLCJ engine loads native libVLC and plays audio.
 * Uses a public test MP3. Requires an audio-capable Windows session.
 */
class VlcjSmokeTest {
    @Test
    fun loadsLibVlcAndPlays() {
        val player = VlcjPlayer() // throws if libVLC cannot be found/loaded
        try {
            val url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
            val started = player.play(url)
            Thread.sleep(3000)
            println("play() returned $started, isPlaying after 3s = ${player.isPlaying}")
            check(started) { "Expected play() to start playback" }
        } finally {
            player.stop()
            player.release()
        }
    }
}
