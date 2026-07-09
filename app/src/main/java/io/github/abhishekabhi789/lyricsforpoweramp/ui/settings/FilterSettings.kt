package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.Disclaimer
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.TextInputWithChips
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.utils.FilterType
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SettingsViewModel

@Composable
private fun FilterSettingsContent(
    modifier: Modifier = Modifier,
    topbar: @Composable (() -> Unit),
    filters: Map<FilterType, List<String>>,
    onFilterChange: (FilterType, List<String>) -> Unit
) {
    SettingsPageLayout(topbar = topbar, modifier = modifier) {
        Disclaimer(
            textContent = AnnotatedString(stringResource(R.string.settings_filter_detailed_description)),
            icon = Icons.Default.Info,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            filters.forEach { (filterType, value) ->
                ElevatedCard {
                    TextInputWithChips(
                        fieldLabel = stringResource(filterType.labelResId),
                        leadingIcon = filterType.icon,
                        chipItems = value,
                        onChipListChange = { onFilterChange(filterType, it) },
                        isRemovable = { true },
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FilterSettings(
    modifier: Modifier = Modifier,
    topbar: @Composable (() -> Unit),
    viewmodel: SettingsViewModel
) {
    val filters by viewmodel.filters.collectAsStateWithLifecycle()
    FilterSettingsContent(
        modifier = modifier,
        topbar = topbar,
        filters = filters,
        onFilterChange = viewmodel::setFilter
    )
}

@Preview(showSystemUi = true)
@Composable
private fun FilterSettingsPreview() {
    var filters by remember {
        mutableStateOf(
            mapOf(
                FilterType.TITLE_FILTER to listOf("Karaoke", "Instrumental", "\\d{3}kbps"),
                FilterType.ARTISTS_FILTER to emptyList(),
                FilterType.ALBUM_FILTER to listOf("(Original Motion Picture Soundtrack)")
            )
        )
    }

    LyricsForPowerAmpTheme {
        FilterSettingsContent(
            topbar = { TopAppBar(title = { Text("Filter Settings") }) },
            filters = filters,
            onFilterChange = { type, list ->
                val newFilters = filters.toMutableMap()
                newFilters[type] = list
                filters = newFilters
            }
        )
    }
}
