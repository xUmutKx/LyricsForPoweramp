package io.github.abhishekabhi789.lyricsforpoweramp.ui.locallyrics

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.BulkLyricsDownloader
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.PowerampPlaybackHelper
import io.github.abhishekabhi789.lyricsforpoweramp.utils.MIN_SEARCH_QUERY_LENGTH
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.LocalLyricsViewModel
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.LocalLyricsViewModel.IndexState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalLyricsScreen(
    viewModel: LocalLyricsViewModel,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val browseEntries by viewModel.browseEntries.collectAsStateWithLifecycle()
    val indexState by viewModel.indexState.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val playFromMatchedLine by viewModel.playFromMatchedLine.collectAsStateWithLifecycle()
    val bulkDownload by viewModel.bulkDownload.collectAsStateWithLifecycle()
    val chooserTitle = stringResource(R.string.local_lyrics_play_with)
    val playbackFailedMessage = stringResource(R.string.local_lyrics_playback_failed)
    var showMenu by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { pickedUri ->
        if (pickedUri != null) {
            context.contentResolver.takePersistableUriPermission(
                pickedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.setFolder(pickedUri)
        }
    }

    val playInPoweramp: (String, Long?) -> Unit = { audioUri, matchedPositionMs ->
        val uri = audioUri.toUri()
        val positionMs = matchedPositionMs.takeIf { playFromMatchedLine }
        val played = PowerampPlaybackHelper.openToPlay(context, uri, positionMs)
        if (!played && !PowerampPlaybackHelper.openWithChooser(context, uri, chooserTitle)) {
            scope.launch { snackbarHostState.showSnackbar(playbackFailedMessage) }
        }
    }

    if (bulkDownload != null) {
        BulkDownloadDialog(
            progress = bulkDownload,
            onCancel = viewModel::cancelBulkDownload,
            onDismiss = viewModel::dismissBulkDownloadResult
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.local_lyrics_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back_action)
                        )
                    }
                },
                actions = {
                    if (indexState !is IndexState.NoFolder) {
                        IconButton(onClick = viewModel::startBulkDownload) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = stringResource(R.string.local_lyrics_download_missing)
                            )
                        }
                        IconButton(onClick = viewModel::rescan) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.local_lyrics_rescan)
                            )
                        }
                    }
                    IconButton(onClick = { folderPicker.launch(null) }) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = stringResource(R.string.local_lyrics_choose_folder)
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.local_lyrics_playback_settings)
                            )
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            Text(
                                text = stringResource(R.string.local_lyrics_playback_settings),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.local_lyrics_play_from_matched_line)) },
                                trailingIcon = {
                                    Switch(
                                        checked = playFromMatchedLine,
                                        onCheckedChange = viewModel::setPlayFromMatchedLine
                                    )
                                },
                                onClick = { viewModel.setPlayFromMatchedLine(!playFromMatchedLine) }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (indexState is IndexState.NoFolder) {
                EmptyFolderState(onPickFolder = { folderPicker.launch(null) })
                return@Column
            }

            OutlinedTextField(
                value = query,
                onValueChange = viewModel::updateQuery,
                label = { Text(stringResource(R.string.local_lyrics_search_label)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            IndexStatus(indexState)

            when {
                isSearching -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

                // nothing typed yet: browse the whole library instead of asking for a query
                query.isBlank() -> when {
                    browseEntries.isEmpty() -> InfoText(stringResource(R.string.local_lyrics_browse_empty))
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items = browseEntries, key = { it.lrcUri }) { entry ->
                            LocalLyricsBrowseItem(
                                entry = entry,
                                onPlay = { entry.audioUri?.let { playInPoweramp(it, null) } }
                            )
                        }
                    }
                }

                query.trim().length < MIN_SEARCH_QUERY_LENGTH -> InfoText(
                    stringResource(R.string.local_lyrics_min_characters, MIN_SEARCH_QUERY_LENGTH)
                )

                results.isEmpty() -> InfoText(stringResource(R.string.local_lyrics_no_matches))

                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items = results, key = { it.entry.lrcUri }) { match ->
                        LocalLyricsItem(
                            match = match,
                            onPlay = { positionMs ->
                                match.entry.audioUri?.let { playInPoweramp(it, positionMs) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BulkDownloadDialog(
    progress: BulkLyricsDownloader.Progress?,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val done = progress as? BulkLyricsDownloader.Progress.Done
    AlertDialog(
        onDismissRequest = { if (done != null) onDismiss() },
        confirmButton = {
            if (done != null) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.local_lyrics_download_close)) }
            } else {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.local_lyrics_download_cancel)) }
            }
        },
        title = { Text(stringResource(R.string.local_lyrics_download_missing)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (progress) {
                    null, is BulkLyricsDownloader.Progress.Scanning -> {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.local_lyrics_download_scanning),
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    is BulkLyricsDownloader.Progress.Downloading -> {
                        val fraction = if (progress.total > 0) {
                            progress.current.toFloat() / progress.total
                        } else 0f
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        Text(
                            text = stringResource(
                                R.string.local_lyrics_download_progress,
                                progress.current + 1,
                                progress.total,
                                progress.trackTitle
                            ),
                            textAlign = TextAlign.Center
                        )
                    }

                    is BulkLyricsDownloader.Progress.Done -> {
                        Text(
                            text = stringResource(
                                R.string.local_lyrics_download_done_summary,
                                progress.downloaded,
                                progress.skipped,
                                progress.failed,
                                progress.total
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun IndexStatus(state: IndexState, modifier: Modifier = Modifier) {
    val text = when (state) {
        is IndexState.Scanning -> stringResource(R.string.local_lyrics_indexing, state.found)
        is IndexState.Ready -> stringResource(R.string.local_lyrics_indexed, state.count)
        IndexState.Failed -> stringResource(R.string.local_lyrics_index_failed)
        IndexState.NoFolder -> return
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun EmptyFolderState(onPickFolder: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.local_lyrics_no_folder_explanation),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onPickFolder) {
                Text(stringResource(R.string.local_lyrics_choose_folder))
            }
        }
    }
}

@Composable
private fun InfoText(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
