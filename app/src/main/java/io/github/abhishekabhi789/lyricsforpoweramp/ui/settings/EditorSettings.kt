package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings

import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.airewrite.AiProvider
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.Disclaimer
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorSettingsContent(
    modifier: Modifier = Modifier,
    topbar: @Composable (() -> Unit),
    timestampDelta: Int,
    onTimestampDeltaChange: (Int) -> Unit,
    getAiProviderApiKey: suspend (AiProvider) -> String?,
    onAiProviderApiKeyChange: (AiProvider, String) -> Unit,
    getAiProviderModelFlow: (AiProvider) -> kotlinx.coroutines.flow.StateFlow<String>,
    onAiProviderModelChange: (AiProvider, String) -> Unit
) {
    SettingsPageLayout(topbar = topbar, modifier = modifier) {
        SettingsGroup {
            Text(
                text = stringResource(R.string.settings_editor_ai_provider_settings_label),
                style = MaterialTheme.typography.labelLarge
            )
            Disclaimer(
                textContent = AnnotatedString(stringResource(R.string.settings_editor_api_keys_disclaimer)),
                icon = Icons.Default.Info,
            )
        }
        AiProvider.entries.forEach { provider ->
            val providerName = stringResource(provider.nameRes)
            SettingsGroup {
                Text(text = providerName, style = MaterialTheme.typography.titleMedium)
                val (configUrl, modelsUrl) = when (provider) {
                    AiProvider.GEMINI -> "https://ai.google.dev/gemini-api/docs" to "https://ai.google.dev/gemini-api/docs/models"
                    AiProvider.OPENROUTER -> "https://openrouter.ai/" to "https://openrouter.ai/models?output_modalities=text"
                }
                Disclaimer(
                    textContent = AnnotatedString.fromHtml(
                        stringResource(
                            R.string.settings_editor_api_setup_instructions_html,
                            configUrl, providerName, modelsUrl
                        )
                    ),
                    icon = Icons.Default.Info,
                )

                val apiKey: String by produceState(initialValue = "", key1 = provider) {
                    value = withContext(Dispatchers.IO) {
                        getAiProviderApiKey(provider) ?: ""
                    }
                }
                val chosenModel by getAiProviderModelFlow(provider)
                    .collectAsStateWithLifecycle()

                AiProviderConfiguration(
                    provider = provider,
                    apiKey = apiKey,
                    chosenModel = chosenModel,
                    onModelChange = { onAiProviderModelChange(provider, it) },
                    onKeyChange = { onAiProviderApiKeyChange(provider, it) },
                )
            }
        }
        HorizontalDivider()
        BasicSettings(
            label = stringResource(R.string.settings_timestamp_step_title),
            description = stringResource(R.string.settings_timestamp_step_summary)
        ) { interactionSource ->
            val suggestedSteps = listOf(1, 5, 10, 25, 50)
            var expanded by remember { mutableStateOf(false) }
            var ignoreInteractions by remember { mutableStateOf(false) }
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collectLatest { interaction ->
                    if (interaction is PressInteraction.Release) {
                        if (!ignoreInteractions) expanded = !expanded
                        ignoreInteractions = false
                    }
                }
            }
            DropdownSettings(
                expanded = expanded,
                currentValue = timestampDelta,
                values = suggestedSteps,
                onExpandChanged = { ignoreInteractions = true; expanded = it },
                getLabel = { stringResource(R.string.settings_timestamp_step_dropdown_item, it) },
                onSelection = { onTimestampDeltaChange(it); expanded = false },
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
fun EditorSettings(
    modifier: Modifier = Modifier,
    topbar: @Composable (() -> Unit),
    viewmodel: SettingsViewModel
) {
    val timestampDelta by viewmodel.timestampDelta.collectAsStateWithLifecycle()

    EditorSettingsContent(
        modifier = modifier,
        topbar = topbar,
        timestampDelta = timestampDelta,
        onTimestampDeltaChange = viewmodel::setTimestampDelta,
        getAiProviderApiKey = viewmodel::getAiProviderApiKey,
        onAiProviderApiKeyChange = viewmodel::setAiProviderApiKey,
        getAiProviderModelFlow = viewmodel::getAiProviderModelFlow,
        onAiProviderModelChange = viewmodel::setAiProviderModel
    )
}

@Preview(showSystemUi = true)
@Composable
private fun EditorSettingsPreview() {
    var timestampDelta by remember { mutableIntStateOf(10) }
    val models = remember {
        mutableMapOf(
            AiProvider.GEMINI to kotlinx.coroutines.flow.MutableStateFlow("gemini-1.5-flash"),
            AiProvider.OPENROUTER to kotlinx.coroutines.flow.MutableStateFlow("google/gemini-2.0-flash-exp:free")
        )
    }

    LyricsForPowerAmpTheme {
        EditorSettingsContent(
            topbar = { TopAppBar(title = { Text("Editor Settings") }) },
            timestampDelta = timestampDelta,
            onTimestampDeltaChange = { timestampDelta = it },
            getAiProviderApiKey = { "demo_key" },
            onAiProviderApiKeyChange = { _, _ -> },
            getAiProviderModelFlow = { models[it]!! },
            onAiProviderModelChange = { provider, model -> models[provider]?.value = model }
        )
    }
}
