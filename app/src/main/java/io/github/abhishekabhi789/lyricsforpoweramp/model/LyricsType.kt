package io.github.abhishekabhi789.lyricsforpoweramp.model

import io.github.abhishekabhi789.lyricsforpoweramp.R

enum class LyricsType(
    val shortLabelResId: Int,
    val longLabelResId: Int,
    val iconRes: Int
) {
    PLAIN(R.string.plain_lyrics_short, R.string.plain_lyrics, R.drawable.ic_plain_lyrics),
    SYNCED(R.string.synced_lyrics_short, R.string.synced_lyrics, R.drawable.ic_synced_lyrics),
    INSTRUMENTAL(
        R.string.settings_mark_instrumental_tracks,
        R.string.settings_mark_instrumental_tracks,
        R.drawable.app_icon//won't be used
    )
}
