package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings

import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.Disclaimer
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.TextInputWithChips
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SettingsViewModel

@Composable
fun LyricsProviderSettings(modifier: Modifier = Modifier, viewmodel: SettingsViewModel) {
    SettingsGroup(
        modifier = modifier,
        title = stringResource(R.string.settings_lyrics_providers_label),
        icon = Icons.Default.CloudCircle
    ) {
        val resources = LocalResources.current
        val lrclibInstances by viewmodel.lrclibApiInstances.collectAsStateWithLifecycle()

        BasicSettings(
            label = stringResource(R.string.settings_lyrics_providers_modify_lrclib_api_label),
            control = {})

        Disclaimer(
            textContent = stringResource(R.string.settings_lyrics_providers_modify_lrclib_api_description),
            icon = Icons.Default.Info
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextInputWithChips(
            fieldLabel = stringResource(R.string.settings_lyrics_providers_add_new_lrclib_api_url_label),
            chipItems = lrclibInstances,
            onChipListChange = { viewmodel.updateLrclibInstancesList(it) },
            onValidateInput = { input ->
                when {
                    input.isBlank() -> resources.getString(R.string.settings_lyrics_providers_error_blank)
                    !input.startsWith("https://") -> resources.getString(R.string.settings_lyrics_providers_url_must_start_with)
                    !input.endsWith("/api") -> resources.getString(R.string.settings_lyrics_providers_url_must_end_with)
                    else -> null
                }
            },
        )
        BasicSettings(
            label = stringResource(R.string.settings_lyrics_providers_selected_lrclib_api_label),
            description = stringResource(R.string.settings_lyrics_providers_selected_lrclib_api_description)
        ) { interactionSource ->
            var showListSelection by remember { mutableStateOf(false) }
            val currentValue by viewmodel.selectedLrcLibInstanceUrl.collectAsStateWithLifecycle()
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interaction ->
                    if (interaction is PressInteraction.Release) {
                        if (!showListSelection) showListSelection = true
                    }
                }
            }
            DropdownSettings(
                expanded = showListSelection,
                currentValue = currentValue,
                values = lrclibInstances,
                onSelection = { viewmodel.updateSelectedLrclibUrl(it) },
                onExpandChanged = { if (showListSelection) showListSelection = false },
                getLabel = { it },
            )
        }
    }
}
