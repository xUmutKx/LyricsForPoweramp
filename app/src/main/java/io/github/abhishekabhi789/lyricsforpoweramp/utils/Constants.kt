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
    Dark(R.string.settings_theme_dark_label),
    Amoled(R.string.settings_theme_amoled_label)
}

/**
 * Accent used for the highlights across the app. [Default] leaves the palette the app ships with
 * (and the wallpaper colours on Android 12+) untouched; anything else repaints primary,
 * secondary and tertiary from that one colour.
 */
enum class AccentColor(val labelResId: Int, val light: Long, val dark: Long) {
    Default(R.string.settings_accent_default_label, 0x00000000, 0x00000000),
    Green(R.string.settings_accent_green_label, 0xFF2E7D4F, 0xFF6BD79A),
    Blue(R.string.settings_accent_blue_label, 0xFF1F5FA9, 0xFF8FBCFF),
    Purple(R.string.settings_accent_purple_label, 0xFF6A42A6, 0xFFC4A5F5),
    Pink(R.string.settings_accent_pink_label, 0xFFB03565, 0xFFF7A2C4),
    Orange(R.string.settings_accent_orange_label, 0xFFA5541A, 0xFFFFB27A),
    Red(R.string.settings_accent_red_label, 0xFFA33131, 0xFFFF9E9E),
    Teal(R.string.settings_accent_teal_label, 0xFF11635F, 0xFF74D9D2),
    Amber(R.string.settings_accent_amber_label, 0xFF8A6300, 0xFFF3C664)
}

enum class FilterType(val key: String, val labelResId: Int, val icon: ImageVector) {
    TITLE_FILTER("title_filter", R.string.settings_filter_title_label, Icons.Default.MusicNote),
    ARTISTS_FILTER(
        "artists_filter", R.string.settings_filter_artists_label, Icons.Default.InterpreterMode
    ),
    ALBUM_FILTER("album_filter", R.string.settings_filter_album_label, Icons.Default.Album),
}
