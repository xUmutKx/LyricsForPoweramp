package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings

import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.Disclaimer
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.TextInputWithChips
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SettingsViewModel

@Composable
fun LyricsProviderSettings(
    modifier: Modifier = Modifier,
    viewmodel: SettingsViewModel
) {
    SettingsPage(modifier = modifier) {
        val resources = LocalResources.current
        val defaultApiName = stringResource(R.string.settings_lyrics_providers_default_api_url)
        val lrclibInstances by viewmodel.lrclibApiInstances.collectAsStateWithLifecycle()
        val formattedApiList by remember(lrclibInstances) {
            derivedStateOf {
                lrclibInstances.map {
                    if (it == AppPreference.DEFAULT_API_URL) defaultApiName else it
                }
            }
        }
        BasicSettings(
            label = stringResource(R.string.settings_lyrics_providers_modify_lrclib_api_label),
            control = {})

        Disclaimer(
            textContent = AnnotatedString(stringResource(R.string.settings_lyrics_providers_modify_lrclib_api_description)),
            icon = Icons.Default.Info
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextInputWithChips(
            fieldLabel = stringResource(R.string.settings_lyrics_providers_add_new_lrclib_api_url_label),
            chipItems = formattedApiList,
            onChipListChange = { newList ->
                val list =
                    newList.map { if (it == defaultApiName) AppPreference.DEFAULT_API_URL else it }
                viewmodel.updateLrclibInstancesList(list)
            },
            onValidateInput = { input ->
                when {
                    input.isBlank() -> resources.getString(R.string.settings_lyrics_providers_error_blank)
                    !input.startsWith("https://") -> resources.getString(R.string.settings_lyrics_providers_url_must_start_with)
                    !input.endsWith("/api") -> resources.getString(R.string.settings_lyrics_providers_url_must_end_with)
                    else -> null
                }
            },
            isRemovable = { item ->
                item != defaultApiName && item !in AppPreference.DEFAULT_LRCLIB_API_URLS
            }
        )
        BasicSettings(
            label = stringResource(R.string.settings_lyrics_providers_selected_lrclib_api_label),
            description = stringResource(R.string.settings_lyrics_providers_selected_lrclib_api_description)
        ) { interactionSource ->
            var showListSelection by remember { mutableStateOf(false) }
            val currentValue by viewmodel.selectedLrcLibInstanceUrl.collectAsStateWithLifecycle()
            val selectedValue =
                if (currentValue == AppPreference.DEFAULT_API_URL) defaultApiName else currentValue
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interaction ->
                    if (interaction is PressInteraction.Release) {
                        if (!showListSelection) showListSelection = true
                    }
                }
            }
            DropdownSettings(
                expanded = showListSelection,
                currentValue = selectedValue,
                values = formattedApiList,
                onSelection = { selected ->
                    val url =
                        if (selected == defaultApiName) AppPreference.DEFAULT_API_URL else selected
                    viewmodel.updateSelectedLrclibUrl(url)
                },
                onExpandChanged = { if (showListSelection) showListSelection = false },
                getLabel = { it },
            )
        }
    }
}
