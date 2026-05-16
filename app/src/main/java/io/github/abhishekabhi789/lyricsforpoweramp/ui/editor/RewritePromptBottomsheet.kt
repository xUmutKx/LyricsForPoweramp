package io.github.abhishekabhi789.lyricsforpoweramp.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.airewrite.RequestState
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.EditorViewmodel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewritePromptBottomsheet(
    modifier: Modifier = Modifier,
    viewmodel: EditorViewmodel,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val tooltipPositionProvider = TooltipDefaults.rememberTooltipPositionProvider(
        TooltipAnchorPosition.Above
    )
    val aiProviders by viewmodel.aiProviders.collectAsStateWithLifecycle()
    val chosenAiProvider by viewmodel.chosenAiProvider.collectAsStateWithLifecycle()
    val promptRequestState by viewmodel.aiRewriteState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        //clearing promptRequestState from viewmodel, when exiting composition
        onDispose { viewmodel.resetAiWriteState() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = modifier.padding(16.dp)) {
            Text(stringResource(R.string.ai_rewrite_label))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                aiProviders.forEach { (aiProvider, apiKey) ->
                    item(key = aiProvider.key) {
                        val tooltipState = rememberTooltipState()
                        val isConfigured = !apiKey.isNullOrBlank()
                        val isSelected = chosenAiProvider == aiProvider
                        TooltipBox(
                            state = tooltipState,
                            positionProvider = tooltipPositionProvider,
                            focusable = false,
                            tooltip = {
                                if (!isConfigured) {
                                    PlainTooltip {
                                        Text(
                                            stringResource(
                                                R.string.ai_provider_not_configured,
                                                stringResource(aiProvider.nameRes)
                                            )
                                        )
                                    }
                                }
                            }
                        ) {
                            InputChip(
                                onClick = {
                                    if (isConfigured) viewmodel.setChosenAiProvider(aiProvider)
                                    else scope.launch { tooltipState.show() }
                                },
                                label = { Text(stringResource(aiProvider.nameRes)) },
                                selected = isSelected,
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(Icons.Default.Done, null)
                                    }
                                },
                                trailingIcon = {
                                    if (!isConfigured) {
                                        Icon(
                                            Icons.Default.Error,
                                            stringResource(R.string.error),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
            if (promptRequestState == RequestState.Processing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            PromptBox(
                requestState = promptRequestState,
                onPromptChange = {
                    if (promptRequestState is RequestState.Failure) {
                        viewmodel.resetAiWriteState()
                    }
                },
                onPrompt = { prompt -> viewmodel.rewriteWithAi(prompt) }
            )
        }
    }
}
