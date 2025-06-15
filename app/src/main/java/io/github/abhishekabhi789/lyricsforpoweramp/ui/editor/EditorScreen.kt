package io.github.abhishekabhi789.lyricsforpoweramp.ui.editor

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.activities.SettingsActivity
import io.github.abhishekabhi789.lyricsforpoweramp.ui.searchresult.ResultBottomSheet
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.EditorViewmodel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(
    modifier: Modifier = Modifier,
    viewmodel: EditorViewmodel,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val viewModelLyrics by viewmodel.lyricsContent.collectAsStateWithLifecycle()
    val canUndo by viewmodel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewmodel.canRedo.collectAsStateWithLifecycle()
    val sendLyricsState by viewmodel.sendLyricsState.collectAsState()
    var showTranslator by remember { mutableStateOf(false) }
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
                    IconButton(enabled = canUndo, onClick = viewmodel::undo) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = stringResource(R.string.undo)
                        )
                    }
                    IconButton(enabled = canRedo, onClick = viewmodel::redo) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = stringResource(R.string.redo)
                        )
                    }
                    IconButton(onClick = { showTranslator = true }) {
                        Icon(
                            Icons.Default.Translate,
                            contentDescription = stringResource(R.string.translation_button_description)
                        )
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = {
                        viewmodel.sendLyricsToPoweramp(context)
                    }) {
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
            BasicTextField(
                value = viewModelLyrics,
                onValueChange = viewmodel::setLyrics,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                decorationBox = { innerTextField ->
                    if (viewModelLyrics.isEmpty()) {
                        Text(stringResource(R.string.editor_placeholder), color = Color.Gray)
                    }
                    innerTextField()
                },
                visualTransformation = {
                    TransformedText(
                        transformLyrics(
                            text = viewModelLyrics,
                            primaryContainerColor = primaryContainerColor,
                            onPrimaryContainerColor = onPrimaryContainerColor,
                            textColor = textColor,
                            errorColor = errorColor,
                            errorContainerColor = errorContainerColor
                        ),
                        OffsetMapping.Identity
                    )
                },
                cursorBrush = SolidColor(Color.Gray),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            )
        }
        if (sendLyricsState.progress != 0f) {
            ResultBottomSheet(
                sendLyricsState = sendLyricsState,
                onDismiss = viewmodel::resetSendLyricsState,
                grantAccess = {
                    viewmodel.resetSendLyricsState()
                    val path = viewmodel.filePath.substringBeforeLast(File.separatorChar)
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
                viewmodel = viewmodel,
                onDismiss = { showTranslator = false })
        }
    }
}

fun transformLyrics(
    text: String,
    primaryContainerColor: Color,
    onPrimaryContainerColor: Color,
    textColor: Color,
    errorColor: Color,
    errorContainerColor: Color
): AnnotatedString {
    val timeStampRegex = Regex("\\[[^]]+]")
    return buildAnnotatedString {
        var lastIndex = 0
        for (match in timeStampRegex.findAll(text)) {
            if (match.range.first > lastIndex) {
                withStyle(SpanStyle(color = textColor)) {
                    append(text.substring(lastIndex, match.range.first))
                }
            }
            val timestamp = match.value
            val isValid = isValidTimestamp(timestamp)
            val contentStart = match.range.last + 1
            val nextMatchStart =
                timeStampRegex.find(text, contentStart)?.range?.first ?: text.length
            val lyricsText = text.substring(contentStart, nextMatchStart)
            withStyle(
                ParagraphStyle(
                    textIndent = TextIndent(restLine = 95.sp),
                    lineHeight = TextUnit(1f, TextUnitType.Em)
                )
            ) {
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        background = if (isValid) primaryContainerColor else errorContainerColor,
                        color = if (isValid) onPrimaryContainerColor else errorColor
                    )
                ) {
                    append(timestamp)
                }
                withStyle(SpanStyle(color = textColor)) {
                    append(lyricsText)
                }
            }
            lastIndex = nextMatchStart
        }
        if (lastIndex < text.length) {
            withStyle(SpanStyle(color = textColor)) {
                append(text.substring(lastIndex))
            }
        }
    }
}

fun isValidTimestamp(ts: String): Boolean {
    // Expecting [mm:ss.cc]
    val match = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2})]").matchEntire(ts) ?: return false
    val (mm, ss, cc) = match.destructured

    val minutes = mm.toIntOrNull() ?: return false
    val seconds = ss.toIntOrNull() ?: return false
    val centis = cc.toIntOrNull() ?: return false

    return minutes in 0..99 && seconds in 0..59 && centis in 0..99
}
