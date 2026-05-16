package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.Disclaimer
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.TextInputWithChips
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SettingsViewModel

@Composable
fun FilterSettings(modifier: Modifier = Modifier, viewmodel: SettingsViewModel) {
    SettingsGroup(
        modifier = modifier,
        title = stringResource(R.string.settings_filter_label),
        icon = Icons.Default.FilterAlt
    ) {
        Disclaimer(
            textContent = AnnotatedString(stringResource(R.string.settings_filter_description)),
            icon = Icons.Default.Info,
            modifier = Modifier.padding(8.dp)
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            modifier = Modifier.padding(8.dp)
        ) {
            val filters by viewmodel.filters.collectAsStateWithLifecycle()
            filters.forEach { (filterType, value) ->
                TextInputWithChips(
                    fieldLabel = stringResource(filterType.labelResId),
                    leadingIcon = filterType.icon,
                    chipItems = value,
                    onChipListChange = { viewmodel.setFilter(filterType, it) },
                )
            }
        }
    }
}
