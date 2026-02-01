package io.github.abhishekabhi789.lyricsforpoweramp.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.InterpreterMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.abhishekabhi789.lyricsforpoweramp.R

enum class AppTheme(val labelResId: Int) {
    Auto(R.string.settings_theme_auto_label),
    Light(R.string.settings_theme_light_label),
    Dark(R.string.settings_theme_dark_label)
}

enum class FilterType(val key: String, val labelResId: Int, val icon: ImageVector) {
    TITLE_FILTER("title_filter", R.string.settings_filter_title_label, Icons.Default.MusicNote),
    ARTISTS_FILTER(
        "artists_filter", R.string.settings_filter_artists_label, Icons.Default.InterpreterMode
    ),
    ALBUM_FILTER("album_filter", R.string.settings_filter_album_label, Icons.Default.Album),
}
