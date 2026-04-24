package io.github.abhishekabhi789.lyricsforpoweramp.ui.editor

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.airewrite.RequestState
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.EditorViewmodel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewritePromptBottomsheet(
    modifier: Modifier = Modifier,
    viewmodel: EditorViewmodel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val tooltipPositionProvider = TooltipDefaults.rememberTooltipPositionProvider(
        TooltipAnchorPosition.Above
    )
    val aiProviders by viewmodel.aiProviders.collectAsStateWithLifecycle()
    val chosenAiProvider by viewmodel.chosenAiProvider.collectAsStateWithLifecycle()
    val promptRequestState by viewmodel.aiRewriteState.collectAsStateWithLifecycle()

    LaunchedEffect(promptRequestState) {
        if (promptRequestState is RequestState.Failure) {
            val failure = promptRequestState as RequestState.Failure
            failure.errorMessage.let { errMsg ->
                val text = errMsg ?: resources.getString(R.string.ai_rewrite_failed)
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = modifier.padding(16.dp)) {
            Text(stringResource(R.string.ai_rewrite_label))
            LazyRow(modifier = Modifier.fillMaxWidth()) {
                aiProviders.forEach { (translator, apiKey) ->
                    item(key = translator.key) {
                        val tooltipState = rememberTooltipState()
                        val isConfigured = !apiKey.isNullOrBlank()
                        TooltipBox(
                            state = tooltipState,
                            positionProvider = tooltipPositionProvider,
                            focusable = false,
                            tooltip = {
                                if (isConfigured) {
                                    PlainTooltip {
                                        Text(
                                            stringResource(
                                                R.string.ai_provider_not_configured,
                                                stringResource(translator.nameRes)
                                            )
                                        )
                                    }
                                }
                            }
                        ) {
                            AssistChip(
                                enabled = isConfigured,
                                onClick = { viewmodel.setChosenAiProvider(translator) },
                                leadingIcon = {
                                    if (chosenAiProvider == translator) Icon(
                                        Icons.Default.Check,
                                        null
                                    )
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
            }
            if (promptRequestState == RequestState.Processing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            PromptBox(
                requestState = promptRequestState,
                onPrompt = { prompt -> viewmodel.rewriteWithAi(prompt) }
            )
        }
    }
}
