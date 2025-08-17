package io.github.abhishekabhi789.lyricsforpoweramp.ui.editor

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun EditorBottomBar(
    modifier: Modifier = Modifier,
    canUndo: Boolean,
    onUndo: () -> Unit,
    canRedo: Boolean,
    onRedo: () -> Unit,
    canTranslate: Boolean,
    onTranslate: () -> Unit,
    onTimestampChange: (increase: Boolean) -> Unit,
    showTimestampAdjustButtons: Boolean,
    onSyncLine: () -> Unit,
    enablePlayLine: Boolean,
    onPlayLine: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val timestampDeltaCenti = remember { AppPreference.getTimestampDelta(context) }
    val listState = rememberLazyListState()

    BottomAppBar(
        actions = {
            LazyRow(state = listState) {
                item {
                    var undoJob: Job? by remember { mutableStateOf(null) }
                    BottomBarToolButton(
                        icon = Icons.AutoMirrored.Filled.Undo,
                        label = stringResource(R.string.undo),
                        enabled = canUndo,
                        onClick = onUndo,
                        onLongPressChange = { pressed ->
                            if (pressed) {
                                undoJob?.cancel()
                                undoJob = scope.launch {
                                    while (isActive) {
                                        onUndo()
                                        delay(100.milliseconds)
                                    }
                                }
                            } else {
                                undoJob?.cancel()
                                undoJob = null
                            }
                        }
                    )
                }
                item {
                    var redoJob: Job? by remember { mutableStateOf(null) }
                    BottomBarToolButton(
                        icon = Icons.AutoMirrored.Filled.Redo,
                        label = stringResource(R.string.redo),
                        enabled = canRedo,
                        onClick = onRedo,
                        onLongPressChange = { pressed ->
                            if (pressed) {
                                redoJob?.cancel()
                                redoJob = scope.launch {
                                    while (isActive) {
                                        onRedo()
                                        delay(100.milliseconds)
                                    }
                                }
                            } else {
                                redoJob?.cancel()
                                redoJob = null
                            }
                        }
                    )
                }
                item {
                    BottomBarToolButton(
                        icon = Icons.Default.Translate,
                        label = stringResource(R.string.translation_button_description),
                        onClick = onTranslate,
                        enabled = canTranslate
                    )
                }
                item {
                    BottomBarToolButton(
                        icon = Icons.Default.Remove,
                        label = stringResource(
                            R.string.decrease_timestamp_by,
                            timestampDeltaCenti
                        ),
                        onClick = { onTimestampChange(false) },
                        enabled = showTimestampAdjustButtons
                    )
                }
                item {
                    BottomBarToolButton(
                        icon = Icons.Default.Add,
                        label = stringResource(
                            R.string.increase_timestamp_by,
                            timestampDeltaCenti
                        ),
                        onClick = { onTimestampChange(true) },
                        enabled = showTimestampAdjustButtons
                    )
                }
                item {
                    BottomBarToolButton(
                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                        label = stringResource(R.string.play_current_line),
                        onClick = onPlayLine,
                        enabled = enablePlayLine
                    )
                }
            }
        },
        floatingActionButton = {
            SmallFloatingActionButton(onClick = onSyncLine) {
                Icon(
                    Icons.Default.MoreTime,
                    stringResource(R.string.sync_line_button_descr),
                )
            }
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewEditorBottomBar() {
    EditorBottomBar(
        canUndo = true,
        onUndo = {},
        canRedo = true,
        onRedo = {},
        canTranslate = true,
        onTranslate = {},
        onTimestampChange = {},
        showTimestampAdjustButtons = true,
        onSyncLine = {},
        enablePlayLine = true,
        onPlayLine = {},
    )
}
