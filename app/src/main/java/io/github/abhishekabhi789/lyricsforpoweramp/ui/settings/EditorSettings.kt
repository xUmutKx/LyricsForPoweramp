package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.translation.Translator
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSettings(modifier: Modifier = Modifier) {
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
            TranslatorApiKey(translator = Translator.GEMINI)
        }
    }
}

@Composable
fun TranslatorApiKey(modifier: Modifier = Modifier, translator: Translator) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var key by remember {
        mutableStateOf(AppPreference.getTranslationApiKey(context, translator))
    }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val onKeyChange = { newKey: String ->
        key = newKey
        AppPreference.setTranslatorApiKey(context, key, translator)
    }
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = key,
            onValueChange = onKeyChange,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions { focusManager.clearFocus() },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            label = { Text(stringResource(translator.nameRes)) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { showPassword = it.isFocused })
    }
}
