package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.airewrite.AiProvider
import io.github.abhishekabhi789.lyricsforpoweramp.utils.makeToast

@Composable
fun AiProviderConfiguration(
    modifier: Modifier = Modifier,
    provider: AiProvider,
    apiKey: String,
    chosenModel: String,
    onModelChange: (String) -> Unit,
    onKeyChange: (String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var inputValue by rememberSaveable(apiKey) { mutableStateOf(apiKey) }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val saveInput = {
        focusManager.clearFocus()
        onKeyChange(inputValue)
    }
    var useCustomModel by remember(chosenModel) { mutableStateOf(chosenModel !in provider.availableModels) }
    val customModelLabel = stringResource(R.string.settings_editor_selected_model_custom_label)
    val availableModels = provider.availableModels + customModelLabel
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(provider.nameRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(4.dp))
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
            leadingIcon = { Icon(Icons.Default.Key, null) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { showPassword = it.isFocused })

        Spacer(Modifier.height(8.dp))

        var showModelSelectionList by remember { mutableStateOf(false) }
        Text(
            text = stringResource(R.string.settings_editor_ai_model_label),
            style = MaterialTheme.typography.titleSmall
        )
        BasicSettings(label = stringResource(R.string.settings_editor_selected_ai_model_label)) {
            val currentModel by remember(useCustomModel, chosenModel) {
                derivedStateOf { if (useCustomModel) customModelLabel else chosenModel }
            }
            DropdownSettings(
                expanded = showModelSelectionList,
                values = availableModels,
                currentValue = currentModel,
                onSelection = {
                    if (it == customModelLabel) useCustomModel = true
                    else {
                        useCustomModel = false
                        onModelChange(it)
                    }
                },
                onExpandChanged = { showModelSelectionList = it },
                getLabel = { it })
        }
        AnimatedVisibility(useCustomModel) {
            val currentModel = if (chosenModel in provider.availableModels) "" else chosenModel
            val inputState = rememberTextFieldState(currentModel)
            val onSaveModel = {
                if (inputState.text.isNotBlank()) {
                    onModelChange(inputState.text.toString())
                    keyboardController?.hide()
                    focusManager.clearFocus()
                } else {
                    context.makeToast(R.string.settings_editor_custom_model_field_empty_warning)
                }
            }
            if (inputState.text.isEmpty()) {
                BackHandler {
                    context.makeToast(R.string.settings_editor_custom_model_field_empty_warning)
                }
            }
            OutlinedTextField(
                state = inputState,
                label = { Text(stringResource(R.string.settings_editor_custom_model_field_label)) },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.None
                ),
                onKeyboardAction = { onSaveModel() },
                inputTransformation = InputTransformation {
                    val specialCharacters = setOf('.', '/', '-', ':')
                    val filtered = asCharSequence()
                        .map { char -> char.lowercaseChar() }
                        .filter { char ->
                            char.isLowerCase() || char.isDigit() || specialCharacters.contains(char)
                        }
                        .joinToString(separator = "")

                    if (filtered != asCharSequence().toString()) {
                        replace(0, length, filtered)
                    }
                },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_network_intelligence), null)
                },
                trailingIcon = {
                    if (inputState.text.toString() != currentModel) {
                        IconButton(onClick = onSaveModel) {
                            Icon(Icons.Default.Done, stringResource(R.string.save))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        HorizontalDivider(Modifier.fillMaxWidth())
    }
}
