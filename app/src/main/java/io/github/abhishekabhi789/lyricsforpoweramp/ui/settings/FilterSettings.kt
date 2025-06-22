package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InterpreterMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.Disclaimer
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference.FilterType

@Composable
fun FilterSettings(modifier: Modifier = Modifier) {
    SettingsGroup(
        modifier = modifier,
        title = stringResource(R.string.settings_filter_label),
        icon = Icons.Default.FilterAlt
    ) {
        Disclaimer(
            textContent = stringResource(R.string.settings_filter_description),
            icon = Icons.Default.Info,
            modifier = Modifier.padding(8.dp)
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            modifier = Modifier.padding(8.dp)
        ) {
            FilterField(filterType = FilterType.TITLE_FILTER, icon = Icons.Default.MusicNote)
            FilterField(
                filterType = FilterType.ARTISTS_FILTER,
                icon = Icons.Default.InterpreterMode
            )
            FilterField(filterType = FilterType.ALBUM_FILTER, icon = Icons.Default.Album)
        }
    }
}
