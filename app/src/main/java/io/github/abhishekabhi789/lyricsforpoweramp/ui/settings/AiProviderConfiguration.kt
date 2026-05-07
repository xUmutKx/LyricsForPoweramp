package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.airewrite.AiProvider

@Composable
fun AiProviderConfiguration(
    modifier: Modifier = Modifier,
    provider: AiProvider,
    apiKey: String,
    chosenModel: String,
    onModelChange: (String) -> Unit,
    onKeyChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var inputValue by rememberSaveable(apiKey) { mutableStateOf(apiKey) }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val saveInput = {
        focusManager.clearFocus()
        onKeyChange(inputValue)
    }
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(provider.nameRes),
            style = MaterialTheme.typography.labelMedium
        )
        OutlinedTextField(
            value = inputValue,
            onValueChange = { inputValue = it },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions { saveInput() },
            trailingIcon = {
                if (inputValue != apiKey) {
                    IconButton(onClick = saveInput) {
                        Icon(Icons.Default.Done, stringResource(R.string.save))
                    }
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            label = { Text(stringResource(R.string.settings_editor_api_key_input_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { showPassword = it.isFocused })
        var showModelSelectionList by remember { mutableStateOf(false) }
        BasicSettings(label = stringResource(R.string.settings_editor_ai_model_label)) {
            DropdownSettings(
                expanded = showModelSelectionList,
                values = provider.availableModels,
                currentValue = chosenModel,
                onSelection = onModelChange,
                onExpandChanged = { showModelSelectionList = it },
                getLabel = { it }
            )
        }
    }
}
