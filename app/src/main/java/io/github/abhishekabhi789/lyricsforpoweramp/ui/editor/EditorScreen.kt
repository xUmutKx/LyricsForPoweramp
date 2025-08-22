package io.github.abhishekabhi789.lyricsforpoweramp.ui.editor

import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.activities.EditorActivity.Companion.TAG
import io.github.abhishekabhi789.lyricsforpoweramp.model.EditorInputState
import io.github.abhishekabhi789.lyricsforpoweramp.model.Timestamp
import io.github.abhishekabhi789.lyricsforpoweramp.ui.searchresult.ResultBottomSheet
import io.github.abhishekabhi789.lyricsforpoweramp.ui.utils.rememberFolderAccess
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.EditorViewmodel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(
    modifier: Modifier = Modifier, viewmodel: EditorViewmodel, onFinish: () -> Unit
) {
    val timeStampRegex = rememberSaveable { Regex("(\\[\\d{2}:\\d{2}\\.\\d{2}])") }
    val context = LocalContext.current
    val defaultFontSize = LocalTextStyle.current.fontSize.value
    val inputState by viewmodel.inputState.collectAsStateWithLifecycle()
    val canUndo by viewmodel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewmodel.canRedo.collectAsStateWithLifecycle()
    val sendLyricsState by viewmodel.sendLyricsState.collectAsState()
    val filePath by viewmodel.filePath.collectAsStateWithLifecycle()
    val isPlaying by viewmodel.isPlaying.collectAsStateWithLifecycle()
    var showTranslator by rememberSaveable { mutableStateOf(false) }
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(inputState.lyrics, inputState.selection))
    }
    val lyricsContent by remember(inputState) { derivedStateOf { inputState.lyrics } }
    val lyricsLines by remember(lyricsContent) { derivedStateOf { lyricsContent.lines() } }
    val timestampDeltaCenti = remember { AppPreference.getTimestampDelta(context) }
    var fontSize by rememberSaveable {
        mutableFloatStateOf(AppPreference.getEditorFontSize(context) ?: defaultFontSize)
    }
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
        }, bottomBar = {
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
                timestamps.firstOrNull()?.let { timestamp ->
                    viewmodel.seekTo(timestamp.toSeconds())
                }
                if (!isPlaying) viewmodel.togglePlayback(true)
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
                onPlayLine = { timestampOnSelection?.toSeconds()?.let { viewmodel.seekTo(it) } })
        }, floatingActionButton = {
            FloatingActionButton(onClick = { viewmodel.sendLyricsToPoweramp(context) }) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = stringResource(R.string.save),
                    tint = LocalContentColor.current.copy(alpha = if (lyricsContent.isBlank()) 0.4f else 1f)
                )
            }
        }, modifier = modifier
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
                if (inputState.lyrics != textFieldValue.text || inputState.selection != textFieldValue.selection) {
                    textFieldValue = TextFieldValue(
                        inputState.lyrics, selection = inputState.selection
                    )
                }
            }
            var fileUri: Uri? by rememberSaveable(filePath) { mutableStateOf(null) }
            val folderAccessState = rememberFolderAccess(filePath)
            LaunchedEffect(folderAccessState) {
                if (!folderAccessState.hasPermission) {
                    folderAccessState.requestAccess()
                }
            }
            LaunchedEffect(folderAccessState.hasPermission) {
                if (folderAccessState.hasPermission) {
                    for (extension in listOf("lrc", "txt")) {
                        val lyricsPathId = filePath.replaceAfterLast(".", extension)
                        val childUri = folderAccessState.getChildUri(lyricsPathId)
                        if (childUri != null) {
                            fileUri = childUri
                            break
                        }
                    }
                }
            }

            val lyricsText by produceState<String?>(null, fileUri) {
                value = fileUri?.let { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)
                            ?.bufferedReader()
                            ?.use { it.readText() }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to read lyrics", e)
                        null
                    }
                }
            }
            lyricsText?.let { lyrics ->
                EditorSuggestions(
                    visible = lyrics.isNotBlank() && lyricsContent.isEmpty(),
                    actionLabel = stringResource(R.string.open),
                    onAction = { viewmodel.updateInputState(EditorInputState(lyrics)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.load_saved_lyrics_suggestion),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            EditorSuggestions(
                visible = lyricsContent.isNotEmpty() && !saveAsFileEnabled,
                actionLabel = stringResource(R.string.enable),
                onAction = {
                    AppPreference.setSaveAsFile(context, true)
                    saveAsFileEnabled = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.save_as_file_not_enabled_warning),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }


            val playbackPosition by viewmodel.playbackPosition.collectAsStateWithLifecycle()
            val indexesOfCurrentLines: IntArray by remember(playbackPosition) {
                derivedStateOf {
                    val lastTimestamp = lyricsLines.asSequence()
                        .flatMap { line ->
                            timeStampRegex.findAll(line)
                                .mapNotNull { match -> Timestamp.fromString(match.value) }
                        }
                        .filter { it.toSeconds() <= playbackPosition }
                        .lastOrNull()

                    if (lastTimestamp != null) {
                        lyricsLines.mapIndexedNotNull { index, line ->
                            if (line.contains(lastTimestamp.toString())) index else null
                        }.toIntArray()
                    } else {
                        intArrayOf()
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
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = TextUnit(fontSize, TextUnitType.Sp)
                ),
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
                        currentPlayingLines = indexesOfCurrentLines,
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
                    .weight(1f) //needed to show playback control
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            val newSize = (fontSize * (zoom)).coerceIn(9f, 30f)
                            if (newSize != fontSize) {
                                fontSize = newSize
                                AppPreference.setEditorFontSize(context, newSize)
                            }
                        }
                    },
            )
            PlaybackControl(viewmodel = viewmodel, folderAccessState = folderAccessState)
        }
        LaunchedEffect(fontSize) {
            Log.d(TAG, "EditorScreen: fontsize $fontSize")
        }
        if (sendLyricsState.progress != 0f) {
            val path = filePath.substringBeforeLast(File.separatorChar)
            val pathAccess = rememberFolderAccess(path)
            ResultBottomSheet(
                sendLyricsState = sendLyricsState,
                onDismiss = viewmodel::resetSendLyricsState,
                grantAccess = {
                    pathAccess.requestAccess {
                        viewmodel.sendLyricsToPoweramp(context)
                    }
                },
                onFinish = onFinish
            )
        }
        if (showTranslator) {
            TranslationBottomSheet(
                viewmodel = viewmodel,
                onDismiss = { showTranslator = false })
        }
    }
}

@Composable
fun EditorSuggestions(
    modifier: Modifier = Modifier,
    visible: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    content: @Composable (ColumnScope.() -> Unit)
) {
    AnimatedVisibility(visible = visible, modifier = modifier) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                content.invoke(this)
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}
