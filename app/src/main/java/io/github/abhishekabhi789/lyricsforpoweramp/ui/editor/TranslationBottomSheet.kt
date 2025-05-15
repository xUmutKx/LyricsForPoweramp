package io.github.abhishekabhi789.lyricsforpoweramp.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.translation.Translator
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.EditorViewmodel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationBottomSheet(
    modifier: Modifier = Modifier,
    viewmodel: EditorViewmodel,
    onDismiss: () -> Unit
) {
    val services by remember { mutableStateOf(viewmodel.translators) }
    val chosenTranslator by viewmodel.chosenTranslator.collectAsStateWithLifecycle()
    val languages by viewmodel.supportedLanguages.collectAsStateWithLifecycle()
    val targetLanguage by viewmodel.targetLanguage.collectAsStateWithLifecycle()
    val translatorRunning by viewmodel.translatorRunning.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewmodel.setChosenTranslator(Translator.GEMINI)
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = modifier.padding(16.dp)) {
            Text(stringResource(R.string.translation_button_label))
            LazyRow(modifier = Modifier.fillMaxWidth()) {
                items(items = services, key = { it.nameRes }) {
                    AssistChip(
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
                    onExpandedChange = { showLanguageSelection = it }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .wrapContentWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    ) {
                        val label = targetLanguage ?: if (languages.isNullOrEmpty())
                            stringResource(R.string.loading) else stringResource(R.string.change)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showLanguageSelection)
                    }
                    ExposedDropdownMenu(
                        expanded = showLanguageSelection,
                        onDismissRequest = { showLanguageSelection = false },
                        modifier = Modifier.width(IntrinsicSize.Max)
                    ) {
                        languages?.forEach { language ->
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
                if (translatorRunning) {
                    CircularProgressIndicator()
                } else {
                    val showTranslateButton by remember(languages) {
                        derivedStateOf { !targetLanguage.isNullOrBlank() }
                    }
                    Button(enabled = showTranslateButton, onClick = viewmodel::translate) {
                        Text(stringResource(R.string.translation_button_label))
                    }
                }
            }
        }
    }
}
