package  io.github.abhishekabhi789.lyricsforpoweramp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.abhishekabhi789.lyricsforpoweramp.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextInputWithChips(
    modifier: Modifier = Modifier,
    fieldLabel: String,
    leadingIcon: ImageVector? = null,
    chipItems: List<String>,
    onChipListChange: (List<String>) -> Unit,
    onValidateInput: (input: String) -> String? = { null }
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var showClearWarningDialog: Boolean by rememberSaveable { mutableStateOf(false) }
    val inputState = rememberTextFieldState()
    var isFocused by rememberSaveable { mutableStateOf(false) }
    val sizeScale by animateFloatAsState(
        targetValue = if (isFocused) 1.025f else 1f,
        label = "searchButtonAnimation"
    )
    val color = MaterialTheme.colorScheme.let { if (isFocused) it.primary else it.outline }
    val onListChange = { item: String, add: Boolean ->
        val newList = if (add) chipItems + item else chipItems - item
        onChipListChange(newList)
    }
    var inputErrorLabel: String? by rememberSaveable { mutableStateOf(null) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .scale(sizeScale)
            .defaultMinSize(TextFieldDefaults.MinWidth, TextFieldDefaults.MinHeight)
            .border(border = BorderStroke(1.dp, color), shape = OutlinedTextFieldDefaults.shape)
            .clickable { focusRequester.requestFocus() }
            .onFocusChanged { state -> isFocused = state.hasFocus }
    ) {
        leadingIcon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = color,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            chipItems.forEach { chipText ->
                AssistChip(
                    label = {
                        Text(
                            text = chipText,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    onClick = { inputState.setTextAndPlaceCursorAtEnd(chipText) },
                    trailingIcon = {
                        IconButton(
                            onClick = { onListChange(chipText, false) },
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(
                                    R.string.clear_field,
                                    chipText
                                )
                            )
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors()
                        .copy(
                            labelColor = MaterialTheme.colorScheme.secondary,
                            trailingIconContentColor = color,
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                )
            }
            TextField(
                state = inputState,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                onKeyboardAction = {
                    if (inputState.text.isNotEmpty()) {
                        val input = inputState.text.toString()
                        inputErrorLabel = onValidateInput(input)
                        if (inputErrorLabel == null) {
                            onListChange(inputState.text.toString(), true)
                            inputState.clearText()
                        }
                    } else {
                        keyboardController?.hide()
                        focusRequester.freeFocus()
                        isFocused = false
                    }
                },
                isError = inputErrorLabel != null,
                supportingText = {
                    inputErrorLabel?.let {
                        Text(it)
                    }
                },
                lineLimits = TextFieldLineLimits.SingleLine,
                label = { Text(text = fieldLabel) },
                colors = TextFieldDefaults.colors()
                    .copy(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    ),
                modifier = Modifier
                    .widthIn(min = 20.dp)
                    .focusRequester(focusRequester)
                    .semantics { stateDescription = "Input new $fieldLabel" }
            )
        }
        if (chipItems.isNotEmpty() || inputState.text.isNotEmpty()) {
            IconButton(
                onClick = {
                    if (inputState.text.isNotEmpty()) {
                        inputState.clearText()
                    } else showClearWarningDialog = true
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(id = R.string.clear_field, fieldLabel),
                    tint = color
                )
            }
        }

    }
    if (showClearWarningDialog) {
        ConfirmationDialog(
            title = stringResource(id = R.string.clear_field_title),
            description = stringResource(R.string.input_clear_confirmation_message, fieldLabel),
            onConfirm = { onChipListChange(emptyList()) },
            onDismiss = { showClearWarningDialog = false }
        )
    }
}

@Preview(showSystemUi = true)
@Composable
fun PreviewTextInputWithChips() {
    TextInputWithChips(
        fieldLabel = "Test Input",
        leadingIcon = Icons.Default.BugReport,
        chipItems = emptyList(),// mutableListOf("Hello", "World"),
        onChipListChange = {},
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
