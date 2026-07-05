package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings

import kotlinx.serialization.Serializable

@Serializable
sealed interface SettingsCategory {

    @Serializable
    data object Main : SettingsCategory

    @Serializable
    data object Theme : SettingsCategory

    @Serializable
    data object Request : SettingsCategory

    @Serializable
    data class Storage(val accessRequestedPath: String? = null) : SettingsCategory

    @Serializable
    data object LyricsProvider : SettingsCategory

    @Serializable
    data object Editor : SettingsCategory

    @Serializable
    data object Filter : SettingsCategory
}
