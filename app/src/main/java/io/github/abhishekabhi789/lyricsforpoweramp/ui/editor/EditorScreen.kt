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
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.BottomAppBar
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
import androidx.compose.runtime.rememberCoroutineScope
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
import io.github.abhishekabhi789.lyricsforpoweramp.ui.searchresult.ResultBottomSheet
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.EditorViewmodel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(
    modifier: Modifier = Modifier,
    viewmodel: EditorViewmodel,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
    val showTimestampAdjustButtons by remember {
        derivedStateOf {
            getTimeStampsFromRange(textFieldValue.selection, lyricsContent).isNotEmpty()
        }
    }
    val timestampDeltaCenti = remember { AppPreference.getTimestampDelta(context) }
    var saveAsFileEnabled by remember {
        mutableStateOf(AppPreference.getSaveAsFile(context))
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
            BottomAppBar(
                actions = {
                    var undoJob: Job? by remember { mutableStateOf(null) }
                    BottomBarToolButton(
                        icon = Icons.AutoMirrored.Filled.Undo,
                        label = stringResource(R.string.undo),
                        enabled = canUndo,
                        onClick = viewmodel::undo,
                        onLongPressChange = { pressed ->
                            if (pressed) {
                                undoJob?.cancel()
                                undoJob = scope.launch {
                                    while (isActive) {
                                        viewmodel.undo()
                                        delay(100.milliseconds)
                                    }
                                }
                            } else {
                                undoJob?.cancel()
                                undoJob = null
                            }
                        }
                    )
                    var redoJob: Job? by remember { mutableStateOf(null) }
                    BottomBarToolButton(
                        icon = Icons.AutoMirrored.Filled.Redo,
                        label = stringResource(R.string.redo),
                        enabled = canRedo,
                        onClick = viewmodel::redo,
                        onLongPressChange = { pressed ->
                            if (pressed) {
                                redoJob?.cancel()
                                redoJob = scope.launch {
                                    while (isActive) {
                                        viewmodel.redo()
                                        delay(100.milliseconds)
                                    }
                                }
                            } else {
                                redoJob?.cancel()
                                redoJob = null
                            }
                        }
                    )
                    BottomBarToolButton(
                        icon = Icons.Default.Translate,
                        label = stringResource(R.string.translation_button_description),
                        onClick = { showTranslator = true },
                        enabled = textFieldValue.text.isNotBlank()
                    )

                    val offsetTimestamp = { increase: Boolean ->
                        val timestamps =
                            getTimeStampsFromRange(textFieldValue.selection, lyricsContent)
                        val newLyrics = timestamps.fold(lyricsContent) { lyrics, timestamp ->
                            val newTimestamp = if (increase) timestamp.increase(timestampDeltaCenti)
                            else timestamp.decrease(timestampDeltaCenti)
                            lyrics.replace(timestamp.toString(), newTimestamp.toString())
                        }
                        viewmodel.updateInputState(
                            EditorInputState(newLyrics, textFieldValue.selection)
                        )
                    }
                    BottomBarToolButton(
                        icon = Icons.Default.Remove,
                        label = stringResource(R.string.decrease_timestamp_by, timestampDeltaCenti),
                        onClick = { offsetTimestamp(false) },
                        enabled = showTimestampAdjustButtons
                    )
                    BottomBarToolButton(
                        icon = Icons.Default.Add,
                        label = stringResource(R.string.increase_timestamp_by, timestampDeltaCenti),
                        onClick = { offsetTimestamp(true) },
                        enabled = showTimestampAdjustButtons
                    )
                    val syncLine = {
                        val newTimestamp = viewmodel.getCurrentTimestamp()
                        val startIndex = textFieldValue.selection
                            .let { range -> minOf(range.start, range.end) }
                        val firstLineIndex = (
                                lyricsContent
                                    .substring(0, (startIndex - 1).coerceAtLeast(0))
                                    .lines()
                                    .size - 1
                                ).coerceAtLeast(0)

                        val lines = lyricsContent.lines().toMutableList()
                        val timeStampRegex = Regex("(\\[\\d{2}:\\d{2}\\.\\d{2}])")
                        val currentLine = lines.getOrNull(firstLineIndex) ?: ""
                        val updatedLine = if (timeStampRegex.containsMatchIn(currentLine)) {
                            timeStampRegex.replace(currentLine) { newTimestamp.toString() }
                        } else {
                            "$newTimestamp $currentLine"
                        }
                        lines[firstLineIndex] = updatedLine
                        val newLyrics = lines.joinToString("\n")
                        viewmodel.updateInputState(inputState.copy(lyrics = newLyrics))
                    }

                    BottomBarToolButton(
                        icon = Icons.Default.MoreTime,
                        label = stringResource(R.string.sync_line_button_descr),
                        onClick = syncLine,
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { viewmodel.sendLyricsToPoweramp(context) }) {
                        Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save))
                    }
                },
                modifier = Modifier.imePadding()
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            val textColor = MaterialTheme.colorScheme.onSurface
            val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
            val onPrimaryContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
            val errorColor = MaterialTheme.colorScheme.onErrorContainer
            val errorContainerColor = MaterialTheme.colorScheme.errorContainer
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
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
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
            BasicTextField(
                value = textFieldValue,
                onValueChange = {
                    textFieldValue = it
                    if (it.text != lyricsContent) {
                        viewmodel.updateInputState(
                            EditorInputState(it.text, it.selection)
                        )
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
                        text = it.text,
                        primaryContainerColor = primaryContainerColor,
                        onPrimaryContainerColor = onPrimaryContainerColor,
                        textColor = textColor,
                        errorColor = errorColor,
                        errorContainerColor = errorContainerColor
                    )
                },
                cursorBrush = SolidColor(Color.Gray),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .weight(1f)
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
            TranslationBottomSheet(
                viewmodel = viewmodel, onDismiss = { showTranslator = false })
        }
    }
}
