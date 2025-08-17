package io.github.abhishekabhi789.lyricsforpoweramp.ui.editor

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.activities.SettingsActivity
import io.github.abhishekabhi789.lyricsforpoweramp.model.EditorInputState
import io.github.abhishekabhi789.lyricsforpoweramp.model.Timestamp
import io.github.abhishekabhi789.lyricsforpoweramp.ui.searchresult.ResultBottomSheet
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.EditorViewmodel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(
    modifier: Modifier = Modifier,
    viewmodel: EditorViewmodel,
    onFinish: () -> Unit
) {
    val timeStampRegex = remember { Regex("(\\[\\d{2}:\\d{2}\\.\\d{2}])") }
    val context = LocalContext.current
    val inputState by viewmodel.inputState.collectAsStateWithLifecycle()
    val canUndo by viewmodel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewmodel.canRedo.collectAsStateWithLifecycle()
    val sendLyricsState by viewmodel.sendLyricsState.collectAsState()
    val filePath by viewmodel.filePath.collectAsStateWithLifecycle()
    var showTranslator by remember { mutableStateOf(false) }
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(inputState.lyrics, inputState.selection))
    }
    val lyricsContent by remember(inputState) { derivedStateOf { inputState.lyrics } }
    val lyricsLines by remember(lyricsContent) { derivedStateOf { lyricsContent.lines() } }
    val timestampDeltaCenti = remember { AppPreference.getTimestampDelta(context) }
    var saveAsFileEnabled by remember {
        mutableStateOf(AppPreference.getSaveAsFile(context))
    }
    val selectionLineIndexes by remember(textFieldValue) {
        derivedStateOf { getLineIndexesForSelection(textFieldValue) }
    }
    val linesInSelection by remember(selectionLineIndexes) {
        derivedStateOf {
            if (lyricsLines.isEmpty()) emptyList()
            else {
                val safeStart = selectionLineIndexes.start.coerceIn(0, lyricsLines.lastIndex)
                val safeEnd = selectionLineIndexes.endInclusive.coerceIn(0, lyricsLines.lastIndex)
                lyricsLines.slice(safeStart..safeEnd)
            }
        }
    }
    BackHandler { onFinish() }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back_action)
                        )
                    }
                },
                title = { Text(stringResource(R.string.title_activity_editor)) },
            )
        },
        bottomBar = {
            val offsetTimestamp = { increase: Boolean ->
                val timestamps = linesInSelection.flatMap { line ->
                    timeStampRegex.findAll(line).mapNotNull { match ->
                        Timestamp.fromString(match.value)
                    }
                }
                val newLyrics = timestamps.fold(lyricsContent) { lyrics, timestamp ->
                    val newTimestamp = if (increase) timestamp.increase(timestampDeltaCenti)
                    else timestamp.decrease(timestampDeltaCenti)
                    lyrics.replace(timestamp.toString(), newTimestamp.toString())
                }
                viewmodel.updateInputState(
                    EditorInputState.fromTextFieldValue(textFieldValue.copy(text = newLyrics))
                )
            }
            val onSyncLine = {
                val newTimestamp = viewmodel.getCurrentTimestamp()
                val lines = lyricsLines.toMutableList()
                val firstLineIndex = selectionLineIndexes.first
                val updatedLine = lyricsLines[firstLineIndex].let { line ->
                    if (timeStampRegex.containsMatchIn(line)) {
                        timeStampRegex.replace(line) { newTimestamp.toString() }
                    } else {
                        "$newTimestamp $line"
                    }
                }
                lines[firstLineIndex] = updatedLine
                val newLyrics = lines.joinToString("\n")
                viewmodel.updateInputState(
                    EditorInputState.fromTextFieldValue(textFieldValue.copy(text = newLyrics))
                )
            }
            val timestampOnSelection by remember(linesInSelection) {
                derivedStateOf {
                    timeStampRegex.find(linesInSelection.joinToString())?.let {
                        Timestamp.fromString(it.value)
                    }
                }
            }

            EditorBottomBar(
                canUndo = canUndo,
                onUndo = viewmodel::undo,
                canRedo = canRedo,
                onRedo = viewmodel::redo,
                canTranslate = textFieldValue.text.isNotBlank(),
                onTranslate = { showTranslator = true },
                showTimestampAdjustButtons = timestampOnSelection != null,
                onTimestampChange = offsetTimestamp,
                onSyncLine = onSyncLine,
                enablePlayLine = timestampOnSelection != null,
                onPlayLine = { timestampOnSelection?.toSeconds()?.let { viewmodel.seekTo(it) } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewmodel.sendLyricsToPoweramp(context) }) {
                Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save))
            }
        },
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            val textColor = MaterialTheme.colorScheme.onSurface
            val selectionContainerColor = MaterialTheme.colorScheme.primaryContainer
            val onSelectionContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
            val timestampContainerColor = MaterialTheme.colorScheme.secondaryContainer
            val onTimestampContainerColor = MaterialTheme.colorScheme.onSecondaryContainer
            val errorContainer = MaterialTheme.colorScheme.errorContainer
            val onErrorContainer = MaterialTheme.colorScheme.onErrorContainer
            LaunchedEffect(inputState) {
                if (inputState.lyrics != textFieldValue.text ||
                    inputState.selection != textFieldValue.selection
                ) {
                    textFieldValue = TextFieldValue(
                        inputState.lyrics,
                        selection = inputState.selection
                    )
                }
            }
            AnimatedVisibility(!saveAsFileEnabled) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = stringResource(R.string.save_as_file_not_enabled_warning),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Button(onClick = {
                            AppPreference.setSaveAsFile(context, true)
                            saveAsFileEnabled = true
                        }) {
                            Text(stringResource(R.string.enable))
                        }
                    }
                }
            }
            val playbackPosition by viewmodel.playbackPosition.collectAsStateWithLifecycle()
            val indexOfCurrentLine by remember(playbackPosition) {
                derivedStateOf {
                    lyricsLines.indexOfLast { line ->
                        timeStampRegex.findAll(line).mapNotNull { match ->
                            Timestamp.fromString(match.value)
                        }.any { timestamp -> timestamp.toSeconds() <= playbackPosition }
                    }
                }
            }

            BasicTextField(
                value = textFieldValue,
                onValueChange = {
                    textFieldValue = it
                    if (it.text != lyricsContent) {
                        viewmodel.updateInputState(EditorInputState.fromTextFieldValue(it))
                    }
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                decorationBox = { innerTextField ->
                    if (lyricsContent.isEmpty()) {
                        Text(stringResource(R.string.editor_placeholder), color = Color.Gray)
                    }
                    innerTextField()
                },
                visualTransformation = {
                    transformLyrics(
                        text = it,
                        selectionLineIndexes = selectionLineIndexes,
                        currentPlayingLine = indexOfCurrentLine,
                        textColor = textColor,
                        selectionContainerColor = selectionContainerColor,
                        onSelectionContainerColor = onSelectionContainerColor,
                        timestampContainerColor = timestampContainerColor,
                        onTimestampContainerColor = onTimestampContainerColor,
                        errorContainer = errorContainer,
                        onErrorContainer = onErrorContainer,
                    )
                },
                cursorBrush = SolidColor(Color.Gray),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
                    .weight(1f)//needed to show playback control
            )
            PlaybackControl(viewmodel = viewmodel)
        }
        if (sendLyricsState.progress != 0f) {
            ResultBottomSheet(
                sendLyricsState = sendLyricsState,
                onDismiss = viewmodel::resetSendLyricsState,
                grantAccess = {
                    viewmodel.resetSendLyricsState()
                    val path = filePath?.substringBeforeLast(File.separatorChar)
                    Intent(context, SettingsActivity::class.java).apply {
                        setAction(SettingsActivity.Companion.OPEN_SETTINGS_ACTION)
                        putExtra(SettingsActivity.EXTRA_REQUIRED_PATH, path)
                    }.let { context.startActivity(it) }
                },
                onFinish = onFinish
            )
        }
        if (showTranslator) {
            TranslationBottomSheet(viewmodel = viewmodel, onDismiss = { showTranslator = false })
        }
    }
}
