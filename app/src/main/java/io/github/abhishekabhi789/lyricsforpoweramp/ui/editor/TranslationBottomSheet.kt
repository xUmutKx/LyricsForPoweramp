package io.github.abhishekabhi789.lyricsforpoweramp.ui.editor

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val services = remember(viewmodel.translators.size) { viewmodel.translators }
    val chosenTranslator by viewmodel.chosenTranslator.collectAsStateWithLifecycle()
    val targetLanguage by viewmodel.targetLanguage.collectAsStateWithLifecycle()
    val supportedLanguageState by viewmodel.supportedLanguageState.collectAsStateWithLifecycle()
    val translatorState by viewmodel.translatorState.collectAsStateWithLifecycle()
    val languages: List<String> by remember(supportedLanguageState) {
        derivedStateOf {
            (supportedLanguageState as? RequestState.Success<*>)?.response as? List<String>
                ?: emptyList()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = modifier.padding(16.dp)) {
            Text(stringResource(R.string.translation_button_label))
            LazyRow(modifier = Modifier.fillMaxWidth()) {
                items(items = services, key = { it.nameRes }) {
                    val isConfigured = remember { it.isConfigured(context) }
                    AssistChip(
                        enabled = isConfigured,
                        onClick = { viewmodel.setChosenTranslator(it) },
                        leadingIcon = {
                            if (chosenTranslator == it) Icon(Icons.Default.Check, null)
                        },
                        label = { Text(stringResource(it.nameRes)) })
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
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
