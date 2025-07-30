package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.translation.Translator
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.Disclaimer
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSettings(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    SettingsGroup(
        title = stringResource(R.string.settings_editor_label),
        icon = Icons.Default.EditNote,
        modifier = modifier
    ) {
        var showApiKeySection by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { showApiKeySection = !showApiKeySection }) {
                Text(
                    stringResource(R.string.settings_editor_api_keys_list),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                ExposedDropdownMenuDefaults.TrailingIcon(showApiKeySection)
            }
        }
        AnimatedVisibility(showApiKeySection) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Disclaimer(
                    textContent = stringResource(R.string.settings_editor_api_keys_disclaimer),
                    icon = Icons.Default.Info,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                TranslatorApiKey(translator = Translator.GEMINI)
            }
        }
        BasicSettings(
            label = stringResource(R.string.settings_timestamp_step_title),
            description = stringResource(R.string.settings_timestamp_step_summary)
        ) {
            val suggstedSteps = listOf(1, 5, 10, 25, 50)
            var expanded by remember { mutableStateOf(false) }
            var savedValue by remember { mutableIntStateOf(AppPreference.getTimestampDelta(context)) }
            DropdownSettings(
                expanded = expanded,
                currentValue = savedValue,
                values = suggstedSteps,
                onExpandChanged = { expanded = it },
                getLabel = {
                    stringResource(
                        R.string.settings_timestamp_step_dropdown_item,
                        it
                    )
                },
                onSelection = {
                    savedValue = it
                    AppPreference.setTimestampDelta(context, it)
                },
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
fun TranslatorApiKey(modifier: Modifier = Modifier, translator: Translator) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var savedApiKey by remember {
        mutableStateOf(AppPreference.getTranslationApiKey(context, translator))
    }
    var inputValue by rememberSaveable { mutableStateOf(savedApiKey) }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val saveInput = {
        AppPreference.setTranslatorApiKey(context, inputValue, translator)
        focusManager.clearFocus()
        savedApiKey = inputValue
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        OutlinedTextField(
            value = inputValue,
            onValueChange = { inputValue = it },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions { saveInput() },
            trailingIcon = {
                if (inputValue != savedApiKey) {
                    IconButton(onClick = saveInput) {
                        Icon(Icons.Default.Done, stringResource(R.string.save))
                    }
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            label = { Text(stringResource(translator.nameRes)) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { showPassword = it.isFocused })
    }
}
