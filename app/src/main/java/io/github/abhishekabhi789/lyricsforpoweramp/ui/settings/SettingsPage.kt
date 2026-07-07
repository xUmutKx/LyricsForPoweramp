package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings

import kotlinx.serialization.Serializable

@Serializable
sealed interface SettingsPage {

    @Serializable
    data object Main : SettingsPage

    @Serializable
    data object Theme : SettingsPage

    @Serializable
    data object Request : SettingsPage

    @Serializable
    data class Storage(val accessRequestedPath: String? = null) : SettingsPage

    @Serializable
    data object LyricsProvider : SettingsPage

    @Serializable
    data object Editor : SettingsPage

    @Serializable
    data object Filter : SettingsPage
}
