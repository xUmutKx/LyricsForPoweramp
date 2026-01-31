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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.activities.EditorActivity.Companion.TAG
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.TaglibHelper
import io.github.abhishekabhi789.lyricsforpoweramp.model.EditorInputState
import io.github.abhishekabhi789.lyricsforpoweramp.model.Timestamp
import io.github.abhishekabhi789.lyricsforpoweramp.ui.searchresult.ResultBottomSheet
import io.github.abhishekabhi789.lyricsforpoweramp.ui.utils.rememberFolderAccess
import io.github.abhishekabhi789.lyricsforpoweramp.utils.makeToast
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.EditorViewmodel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(modifier: Modifier = Modifier, viewmodel: EditorViewmodel, onFinish: () -> Unit) {
    val timeStampRegex = rememberSaveable { Regex("(\\[\\d{2}:\\d{2}\\.\\d{2}])") }
    val context = LocalContext.current
    val taglibHelper = remember { TaglibHelper(context) }
    val defaultFontSize = LocalTextStyle.current.fontSize.value
    val inputState by viewmodel.inputState.collectAsStateWithLifecycle()
    val canUndo by viewmodel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewmodel.canRedo.collectAsStateWithLifecycle()
    val lyricsSavingState by viewmodel.lyricsSavingState.collectAsState()
    val filePath by viewmodel.filePath.collectAsStateWithLifecycle()
    val isPlaying by viewmodel.isPlaying.collectAsStateWithLifecycle()
    var showTranslator by rememberSaveable { mutableStateOf(false) }
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(inputState.lyrics, inputState.selection))
    }
    val lyricsContent by remember(inputState) { derivedStateOf { inputState.lyrics } }
    val lyricsLines by remember(lyricsContent) { derivedStateOf { lyricsContent.lines() } }
    val timestampDeltaCenti by viewmodel.timestampDelta.collectAsStateWithLifecycle()
    val fontSize by viewmodel.editorFontSize.collectAsStateWithLifecycle()

    val selectionLineIndexes by remember(textFieldValue) {
        derivedStateOf { getLineIndexesForSelection(textFieldValue) }
    }
    val linesInSelection by remember(selectionLineIndexes) {
        derivedStateOf {
            if (lyricsLines.isEmpty()) emptyList()
            else {
                val safeStart = selectionLineIndexes.first.coerceIn(0, lyricsLines.lastIndex)
                val safeEnd = selectionLineIndexes.last.coerceIn(0, lyricsLines.lastIndex)
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
                    viewmodel.seekTo(timestamp.toTotalCentiseconds())
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
                val nextLineIndex = firstLineIndex + 1
                val newCursorPosition = if (nextLineIndex < lines.size) {
                    val newPosition = lines.take(nextLineIndex).sumOf { it.length + 1 }
                    timeStampRegex.findAll(lines[nextLineIndex]).lastOrNull()?.let {
                        newPosition + it.range.last + 1
                    } ?: newPosition
                } else newLyrics.length
                viewmodel.updateInputState(
                    EditorInputState(newLyrics, TextRange(newCursorPosition))
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
                timestampDelta = timestampDeltaCenti,
                showTimestampAdjustButtons = timestampOnSelection != null,
                onTimestampChange = offsetTimestamp,
                onSyncLine = onSyncLine,
                enablePlayLine = timestampOnSelection != null,
                onPlayLine = {
                    timestampOnSelection?.toTotalCentiseconds()?.let {
                        viewmodel.seekTo(it)
                        viewmodel.togglePlayback(true)
                    }
                })
        }, floatingActionButton = {
            FloatingActionButton(onClick = { viewmodel.saveLyrics() }) {
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

            val lyricsFromFile by produceState<String?>(null, fileUri) {
                value = fileUri?.let { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()
                            ?.use { it.readText().replace("\r\n", "\n") }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to read lyrics", e)
                        null
                    }
                }
            }
            val embeddedLyrics by produceState<String?>(null, filePath) {
                taglibHelper.getTaglibSession(filePath, onError = {
                    Log.e(TAG, "EditorScreen: failed to get lyrics tag info")
                })?.use { session ->
                    value = session.getLyricsTag()
                }
            }
            var showLyricsSourceSelection by remember { mutableStateOf(false) }

            if (showLyricsSourceSelection) {
                LyricsStorageSelection(
                    title = stringResource(R.string.editor_lyrics_source_dialog_title),
                    description = stringResource(R.string.editor_lyrics_source_dialog_description),
                    onDismiss = { showLyricsSourceSelection = false },
                    content = {
                        TextButton(
                            onClick = {
                                viewmodel.updateInputState(EditorInputState(lyricsFromFile ?: ""))
                                context.makeToast(R.string.editor_loaded_from_lyrics_file)
                                showLyricsSourceSelection = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.editor_load_from_lyrics_file)) }
                        TextButton(
                            onClick = {
                                viewmodel.updateInputState(EditorInputState(embeddedLyrics ?: ""))
                                context.makeToast(R.string.editor_loaded_from_embedded)
                                showLyricsSourceSelection = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.editor_load_embedded_lyrics)) }
                    }
                )
            }
            EditorSuggestions(
                visible = lyricsContent.isEmpty() && (!lyricsFromFile.isNullOrBlank() || !embeddedLyrics.isNullOrBlank()),
                actionLabel = stringResource(R.string.open),
                onAction = {
                    when {
                        !lyricsFromFile.isNullOrBlank() && !embeddedLyrics.isNullOrBlank() ->
                            showLyricsSourceSelection = true

                        !lyricsFromFile.isNullOrBlank() -> {
                            viewmodel.updateInputState(EditorInputState(lyricsFromFile ?: ""))
                            context.makeToast(R.string.editor_loaded_from_lyrics_file)
                        }

                        !embeddedLyrics.isNullOrBlank() -> {
                            viewmodel.updateInputState(EditorInputState(embeddedLyrics ?: ""))
                            context.makeToast(R.string.editor_loaded_from_embedded)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.load_saved_lyrics_suggestion),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            val saveAsFileEnabled by viewmodel.saveAsFileEnabled.collectAsStateWithLifecycle()
            val embedIntoFiles by viewmodel.embedLyrics.collectAsStateWithLifecycle()
            var showLyricsStorageSelection by rememberSaveable { mutableStateOf(false) }
            if (showLyricsStorageSelection) {
                LyricsStorageSelection(
                    title = stringResource(R.string.editor_saving_methods_dialog_title),
                    description = stringResource(R.string.editor_saving_methods_dialog_description),
                    onDismiss = { showLyricsStorageSelection = false },
                    content = {
                        LyricsStorage(
                            label = stringResource(R.string.settings_save_as_file_label),
                            checked = saveAsFileEnabled,
                            onCheckChange = { viewmodel.setSaveAsFile(it) }
                        )
                        LyricsStorage(
                            label = stringResource(R.string.settings_embed_into_song_file_label),
                            checked = embedIntoFiles,
                            onCheckChange = { viewmodel.setEmbedLyrics(it) }
                        )
                    }
                )
            }
            EditorSuggestions(
                visible = lyricsContent.isNotEmpty() && !saveAsFileEnabled && !embedIntoFiles,
                actionLabel = stringResource(R.string.enable),
                onAction = { showLyricsStorageSelection = true },
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
                    fontSize = TextUnit(fontSize ?: defaultFontSize, TextUnitType.Sp)
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
                            val newSize =
                                (fontSize ?: defaultFontSize).times(zoom).coerceIn(9f, 30f)
                            if (newSize != fontSize) {
                                viewmodel.setEditorFontSize(newSize)
                            }
                        }
                    },
            )
            PlaybackControl(viewmodel = viewmodel, folderAccessState = folderAccessState)
        }

        if (lyricsSavingState.progress != 0f) {
            val path = filePath.substringBeforeLast(File.separatorChar)
            val pathAccess = rememberFolderAccess(path)
            ResultBottomSheet(
                lyricsSavingState = lyricsSavingState,
                onDismiss = viewmodel::resetLyricsSavingState,
                grantAccess = {
                    pathAccess.requestAccess {
                        viewmodel.saveLyrics()
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
