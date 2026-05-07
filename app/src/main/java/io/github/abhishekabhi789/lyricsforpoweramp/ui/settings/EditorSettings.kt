package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.airewrite.AiProvider
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.Disclaimer
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSettings(modifier: Modifier = Modifier, viewmodel: SettingsViewModel) {
    SettingsGroup(
        title = stringResource(R.string.settings_editor_label),
        icon = Icons.Default.EditNote,
        modifier = modifier
    ) {
        var showAiProvidersSettings by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { showAiProvidersSettings = !showAiProvidersSettings }) {
                Text(
                    text = stringResource(R.string.settings_editor_ai_provider_settings_label),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                ExposedDropdownMenuDefaults.TrailingIcon(showAiProvidersSettings)
            }
        }
        AnimatedVisibility(showAiProvidersSettings) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Disclaimer(
                    textContent = stringResource(R.string.settings_editor_api_keys_disclaimer),
                    icon = Icons.Default.Info,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                AiProvider.entries.forEach { provider ->
                    val apiKey: String by produceState(initialValue = "", key1 = provider) {
                        value = withContext(Dispatchers.IO) {
                            viewmodel.getAiProviderApiKey(provider) ?: ""
                        }
                    }
                    val chosenProvider by viewmodel.getAiProviderModelFlow(provider)
                        .collectAsStateWithLifecycle()

                    AiProviderConfiguration(
                        provider = provider,
                        apiKey = apiKey,
                        chosenModel = chosenProvider,
                        onModelChange = { viewmodel.setAiProviderModel(provider, it) },
                        onKeyChange = { viewmodel.setAiProviderApiKey(provider, it) },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
        BasicSettings(
            label = stringResource(R.string.settings_timestamp_step_title),
            description = stringResource(R.string.settings_timestamp_step_summary)
        ) {
            val suggestedSteps = listOf(1, 5, 10, 25, 50)
            var expanded by remember { mutableStateOf(false) }
            val savedValue by viewmodel.timestampDelta.collectAsStateWithLifecycle()
            DropdownSettings(
                expanded = expanded,
                currentValue = savedValue,
                values = suggestedSteps,
                onExpandChanged = { expanded = it },
                getLabel = { stringResource(R.string.settings_timestamp_step_dropdown_item, it) },
                onSelection = { viewmodel.setTimestampDelta(it) },
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
