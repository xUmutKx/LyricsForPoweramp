package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings

import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.Disclaimer
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.TextInputWithChips
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SettingsViewModel

@Composable
private fun LyricsProviderSettingsContent(
    modifier: Modifier = Modifier,
    topbar: @Composable (() -> Unit),
    lrclibApiInstances: List<String>,
    onLrclibApiInstancesChange: (List<String>) -> Unit,
    selectedLrcLibInstanceUrl: String,
    onSelectedLrcLibInstanceUrlChange: (String) -> Unit
) {
    SettingsPageLayout(topbar = topbar, modifier = modifier) {
        val resources = LocalResources.current
        val defaultApiName = stringResource(R.string.settings_lyrics_providers_default_api_url)
        val formattedApiList by remember(lrclibApiInstances) {
            derivedStateOf {
                lrclibApiInstances.map {
                    if (it == AppPreference.DEFAULT_API_URL) defaultApiName else it
                }
            }
        }
        SettingsGroup {
            Text(
                text = stringResource(R.string.settings_lyrics_providers_modify_lrclib_api_label),
                style = MaterialTheme.typography.titleMedium
            )
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
                    onLrclibApiInstancesChange(list)
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
        }
        BasicSettings(
            label = stringResource(R.string.settings_lyrics_providers_selected_lrclib_api_label),
            description = stringResource(R.string.settings_lyrics_providers_selected_lrclib_api_description)
        ) { interactionSource ->
            var showListSelection by remember { mutableStateOf(false) }
            val selectedValue =
                if (selectedLrcLibInstanceUrl == AppPreference.DEFAULT_API_URL) defaultApiName else selectedLrcLibInstanceUrl
            var ignoreInteractions by remember { mutableStateOf(false) }
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interaction ->
                    if (interaction is PressInteraction.Release) {
                        if (!ignoreInteractions) showListSelection = !showListSelection
                        ignoreInteractions = false
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
                    onSelectedLrcLibInstanceUrlChange(url)
                    showListSelection = false
                },
                onExpandChanged = {
                    ignoreInteractions = true
                    showListSelection = it
                },
                getLabel = { it },
            )
        }
    }
}

@Composable
fun LyricsProviderSettings(
    modifier: Modifier = Modifier,
    topbar: @Composable (() -> Unit),
    viewmodel: SettingsViewModel
) {
    val lrclibInstances by viewmodel.lrclibApiInstances.collectAsStateWithLifecycle()
    val selectedLrcLibInstanceUrl by viewmodel.selectedLrcLibInstanceUrl.collectAsStateWithLifecycle()

    LyricsProviderSettingsContent(
        modifier = modifier,
        topbar = topbar,
        lrclibApiInstances = lrclibInstances,
        onLrclibApiInstancesChange = viewmodel::updateLrclibInstancesList,
        selectedLrcLibInstanceUrl = selectedLrcLibInstanceUrl,
        onSelectedLrcLibInstanceUrlChange = viewmodel::updateSelectedLrclibUrl
    )
}

@Preview(showSystemUi = true)
@Composable
private fun LyricsProviderSettingsPreview() {
    var lrclibInstances by remember { mutableStateOf(listOf(AppPreference.DEFAULT_API_URL)) }
    var selectedLrcLibInstanceUrl by remember { mutableStateOf(AppPreference.DEFAULT_API_URL) }

    LyricsForPowerAmpTheme {
        LyricsProviderSettingsContent(
            topbar = { TopAppBar(title = { Text("Lyrics Provider Settings") }) },
            lrclibApiInstances = lrclibInstances,
            onLrclibApiInstancesChange = { lrclibInstances = it },
            selectedLrcLibInstanceUrl = selectedLrcLibInstanceUrl,
            onSelectedLrcLibInstanceUrlChange = { selectedLrcLibInstanceUrl = it }
        )
    }
}
