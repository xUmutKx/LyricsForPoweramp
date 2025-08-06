package io.github.abhishekabhi789.lyricsforpoweramp.model

import com.maxmpz.poweramp.player.PowerampAPI

data class PlaybackState(
    val trackId: Long = PowerampAPI.NO_ID,
    val paused: Boolean = false,
    val position: Int = 0,
    val duration: Int = 0
)
