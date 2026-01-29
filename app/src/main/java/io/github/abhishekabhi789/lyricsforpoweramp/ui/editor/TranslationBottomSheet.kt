package io.github.abhishekabhi789.lyricsforpoweramp.ui.editor

import android.widget.Toast
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.translation.RequestState
import io.github.abhishekabhi789.lyricsforpoweramp.ui.settings.BasicSettings
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.EditorViewmodel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationBottomSheet(
    modifier: Modifier = Modifier,
    viewmodel: EditorViewmodel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val tooltipPositionProvider = TooltipDefaults.rememberTooltipPositionProvider(
        TooltipAnchorPosition.Above
    )
    val services = remember(viewmodel.translators.size) { viewmodel.translators }
    val chosenTranslator by viewmodel.chosenTranslator.collectAsStateWithLifecycle()
    val targetLanguage by viewmodel.targetLanguage.collectAsStateWithLifecycle()
    val supportedLanguageState by viewmodel.supportedLanguageState.collectAsStateWithLifecycle()
    val translatorState by viewmodel.translatorState.collectAsStateWithLifecycle()
    val languages: List<String> by remember(supportedLanguageState) {
        derivedStateOf {
            if (supportedLanguageState is RequestState.Success<*>) {
                val response =
                    (supportedLanguageState as RequestState.Success<*>).response as List<*>
                response.map { it.toString() }
            } else emptyList()
        }
    }
    LaunchedEffect(Unit) {
        viewmodel.fetchSupportedLanguages()
    }

    LaunchedEffect(translatorState) {
        if (translatorState is RequestState.Failure) {
            val failure = translatorState as RequestState.Failure
            failure.errorMessage.let { errMsg ->
                val text = errMsg ?: context.getString(R.string.translation_failed)
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = modifier.padding(16.dp)) {
            Text(stringResource(R.string.translation_button_label))
            LazyRow(modifier = Modifier.fillMaxWidth()) {
                items(items = services, key = { it.nameRes }) { translator ->
                    val isConfigured =
                        remember(translator) { viewmodel.isTranslatorConfigured(translator) }
                    val tooltipState = rememberTooltipState()
                    TooltipBox(
                        state = tooltipState,
                        positionProvider = tooltipPositionProvider,
                        focusable = false,
                        tooltip = {
                            if (!isConfigured) {
                                PlainTooltip {
                                    Text(
                                        stringResource(
                                            R.string.translation_service_not_configured,
                                            stringResource(translator.nameRes)
                                        )
                                    )
                                }
                            }
                        }
                    ) {
                        AssistChip(
                            enabled = isConfigured,
                            onClick = { viewmodel.setChosenTranslator(translator) },
                            leadingIcon = {
                                if (chosenTranslator == translator) Icon(Icons.Default.Check, null)
                            },
                            label = { Text(stringResource(translator.nameRes)) },
                            trailingIcon = {
                                if (!isConfigured) {
                                    Icon(
                                        Icons.Default.Error,
                                        stringResource(R.string.error),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            })
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.supported_language_choose_title),
                    modifier = Modifier.weight(1f)
                )
                var showLanguageSelection by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = showLanguageSelection,
                    onExpandedChange = { showLanguageSelection = it },
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Column(modifier = Modifier.width(IntrinsicSize.Min)) {
                        var width by remember { mutableIntStateOf(0) }
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .wrapContentWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .onPlaced { width = it.size.width }
                                .padding(horizontal = 8.dp)
                        ) {
                            val label = targetLanguage ?: when (supportedLanguageState) {
                                is RequestState.Failure -> stringResource(R.string.error)
                                RequestState.Idle, is RequestState.Success<*> -> stringResource(R.string.change)
                                RequestState.Processing -> stringResource(R.string.loading)
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.End,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.padding(4.dp))
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showLanguageSelection)
                        }
                        if (supportedLanguageState == RequestState.Processing) {
                            LinearProgressIndicator(modifier = Modifier.requiredWidth(with(density) { width.toDp() }))
                        }
                    }
                    ExposedDropdownMenu(
                        expanded = showLanguageSelection,
                        onDismissRequest = { showLanguageSelection = false },
                        modifier = Modifier.width(IntrinsicSize.Max)
                    ) {
                        languages.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(text = language) },
                                colors = MenuDefaults.itemColors()
                                    .copy(
                                        textColor = if (language == targetLanguage)
                                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    ),
                                onClick = {
                                    viewmodel.setTargetLanguage(language)
                                    showLanguageSelection = false
                                },
                            )
                        }
                    }
                }
            }
            Row {
                val replaceOriginal by viewmodel.replaceOriginalWithTranslation.collectAsStateWithLifecycle()
                BasicSettings(
                    label = stringResource(R.string.editor_settings_lyrics_replace_original_title),
                    description = stringResource(R.string.editor_settings_lyrics_replace_original_description)
                ) { interactionSource ->
                    LaunchedEffect(interactionSource) {
                        interactionSource.interactions.collect { interaction ->
                            if (interaction is PressInteraction.Release) {
                                viewmodel.setReplaceLyrics(!replaceOriginal)
                            }
                        }
                    }
                    Switch(
                        checked = replaceOriginal,
                        onCheckedChange = { viewmodel.setReplaceLyrics(it) },
                        enabled = translatorState == RequestState.Idle
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                when (translatorState) {
                    RequestState.Processing -> {
                        CircularProgressIndicator()
                    }

                    RequestState.Idle, is RequestState.Failure -> {
                        val showTranslateButton by remember(languages) {
                            derivedStateOf { !targetLanguage.isNullOrBlank() }
                        }
                        Button(enabled = showTranslateButton, onClick = viewmodel::translate) {
                            Text(stringResource(R.string.translation_button_label))
                        }
                    }

                    is RequestState.Success<*> -> {
                        Button(onClick = onDismiss) {
                            Text(stringResource(R.string.close))
                        }
                    }
                }
            }
        }
    }
}
