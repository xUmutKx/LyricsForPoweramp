package io.github.abhishekabhi789.lyricsforpoweramp.ui.editor

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.getCleanedPath
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.EditorViewmodel

@Composable
fun PlaybackControl(modifier: Modifier = Modifier, viewmodel: EditorViewmodel) {
    val context = LocalContext.current
    val playerInitialized by viewmodel.playerInitialized.collectAsStateWithLifecycle()
    val trackDuration by viewmodel.trackDuration.collectAsStateWithLifecycle()
    val playbackPosition by viewmodel.playbackPosition.collectAsStateWithLifecycle()
    val isPlaying by viewmodel.isPlaying.collectAsStateWithLifecycle()
    val filePath by viewmodel.filePath.collectAsStateWithLifecycle() // e.g. primary/Music/Folder/File.mp3
    var askUriAccess by rememberSaveable { mutableStateOf(false) }
    val highLevelFolder: DocumentFile? by remember(filePath) {
        derivedStateOf {
            filePath?.let { path ->
                AppPreference.getSavedUris(context)
                    .find { uri -> path.startsWith(uri.getCleanedPath()) }
                    ?.let { uri -> DocumentFile.fromTreeUri(context, uri) }
            }
        }
    }
    val fileUri by remember(highLevelFolder) {
        derivedStateOf {
            getFileUriFromTreeUri(highLevelFolder?.uri, filePath)
        }
    }
    val hasUriAccess: Boolean? by remember(filePath, askUriAccess) {
        derivedStateOf {
            if (filePath == null) return@derivedStateOf null
            val savedTreeUri = AppPreference.getSavedUris(context)
                .find { uri -> filePath?.startsWith(uri.getCleanedPath()) == true }
            val perms = context.contentResolver.persistedUriPermissions
            perms.any { perm ->
                perm.uri == savedTreeUri && perm.isReadPermission
            }
        }
    }

    LaunchedEffect(hasUriAccess) {
        hasUriAccess?.let { hasAccess ->
            if (hasAccess) {
                fileUri?.let { viewmodel.setTrackUri(it) }
            }
            askUriAccess = !hasAccess
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            Timestamp(duration = playbackPosition)
            Slider(
                enabled = playerInitialized,
                value = playbackPosition.toFloat(),
                onValueChange = { viewmodel.seekTo(it.toInt()) },
                valueRange = 0f..trackDuration.toFloat(),
                modifier = Modifier.weight(1f)
            )
            Timestamp(duration = trackDuration)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val changePlayback = { delta: Int ->
                val newPosition = playbackPosition + delta
                viewmodel.seekTo(newPosition)
            }
            IconButton(onClick = { changePlayback(-10) }, enabled = playerInitialized) {
                Icon(Icons.Default.Replay10, stringResource(R.string.playback_rewind_10s))
            }
            val onPlayToggle = {
                if (hasUriAccess == true) viewmodel.togglePlayback(!isPlaying) else askUriAccess =
                    true
            }
            FilledTonalIconButton(onClick = onPlayToggle, modifier = Modifier) {
                val (icon, label) = if (isPlaying) Icons.Default.Pause to stringResource(R.string.playback_pause_button)
                else Icons.Default.PlayArrow to stringResource(R.string.playback_play_button)
                Icon(icon, label)
            }
            IconButton(onClick = { changePlayback(10) }, enabled = playerInitialized) {
                Icon(Icons.Default.Forward10, stringResource(R.string.playback_forward_10s))
            }
        }
    }

    val pickFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { uri ->
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            AppPreference.saveFolderUri(context, uri)
            getFileUriFromTreeUri(uri, filePath)?.let {
                Log.d("TAG", "PlaybackControl: uri created $it")
                viewmodel.setTrackUri(it)
            }
            askUriAccess = false
        }
    }

    if (filePath != null && askUriAccess) {
        AlertDialog(
            onDismissRequest = { askUriAccess = false },
            confirmButton = {
                TextButton({ pickFolderLauncher.launch(fileUri) }) {
                    Text(stringResource(R.string.grant_access))
                }
            },
            dismissButton = {
                TextButton({ askUriAccess = false }) {
                    Text(stringResource(R.string.dismiss))
                }
            },
            icon = { Icon(Icons.Default.Info, null) },
            title = { Text(stringResource(R.string.folder_access_request_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.folder_access_request_explanation,
                        filePath?.substringBeforeLast("/") ?: ""
                    )
                )
            },
        )
    }
}

@Composable
fun Timestamp(modifier: Modifier = Modifier, duration: Int) {
    val formattedDuration by remember(duration) {
        derivedStateOf {
            buildString {
                append(duration / 60)
                append(":")
                append((duration % 60).let { if (it < 10) "0$it" else it.toString() })
            }
        }
    }
    Text(text = formattedDuration, modifier = modifier)
}

fun getFileUriFromTreeUri(treeUri: Uri?, fullPath: String?): Uri? {
    if (treeUri == null || fullPath == null) return null
    val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
    val normalizedTreePath = treeDocId.replace(":", "/")
    if (!fullPath.startsWith(normalizedTreePath)) {
        return null
    }
    val relativePath = fullPath.removePrefix(normalizedTreePath).trimStart('/')
    val fileDocId = if (relativePath.isEmpty()) {
        treeDocId
    } else {
        "$treeDocId/$relativePath"
    }
    return DocumentsContract.buildDocumentUriUsingTree(treeUri, fileDocId)
}
