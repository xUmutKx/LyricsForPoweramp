package io.github.abhishekabhi789.lyricsforpoweramp.ui.editor

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.airewrite.RequestState

@Composable
fun PromptBox(
    modifier: Modifier = Modifier,
    requestState: RequestState,
    onPromptChange: () -> Unit,//to clear any previous error
    onPrompt: (String) -> Unit
) {
    var prompt by rememberSaveable { mutableStateOf("") }
    val isProcessing = requestState is RequestState.Processing
    val isError = requestState is RequestState.Failure
    val interactionSource = remember { MutableInteractionSource() }

    val infiniteTransition = rememberInfiniteTransition(label = "hourglass animation")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
        label = "pulse animation"
    )

    LaunchedEffect(prompt) { onPromptChange() }

    BasicTextField(
        value = prompt,
        onValueChange = { prompt = it },
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        enabled = !isProcessing,
        interactionSource = interactionSource,
        maxLines = 10,
        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = prompt,
                innerTextField = innerTextField,
                enabled = !isProcessing,
                singleLine = false,
                isError = isError,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                label = {
                    Text(stringResource(R.string.ai_rewrite_field_label))
                },
                placeholder = { Text(stringResource(R.string.ai_rewrite_field_placeholder)) },
                supportingText = {
                    if (isError) {
                        Text(
                            text = requestState.errorMessage ?: stringResource(R.string.ai_rewrite_failed),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (prompt.isNotEmpty() && !isProcessing) {
                            IconButton(onClick = { prompt = "" }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = stringResource(
                                        R.string.clear_field,
                                        stringResource(R.string.ai_rewrite_field_label)
                                    )
                                )
                            }
                        }

                        FilledTonalIconButton(
                            enabled = !isProcessing,
                            onClick = { onPrompt(prompt) }) {
                            if (isProcessing) {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = stringResource(R.string.ai_rewrite_processing_state_label),
                                    modifier = Modifier.graphicsLayer {
                                        rotationZ = rotation
                                    }
                                )
                            } else {
                                Icon(
                                    Icons.Default.AutoFixHigh,
                                    contentDescription = stringResource(R.string.ai_rewrite_send_prompt_button_label)
                                )
                            }
                        }
                    }
                },
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = !isProcessing,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = OutlinedTextFieldDefaults.colors(),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.graphicsLayer {
                            if (isProcessing) alpha = animatedAlpha
                        }
                    )
                }
            )
        }
    )
}
