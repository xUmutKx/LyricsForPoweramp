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
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
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
    val indexState by viewModel.indexState.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val chooserTitle = stringResource(R.string.local_lyrics_play_with)
    val playbackFailedMessage = stringResource(R.string.local_lyrics_playback_failed)

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

    val playInPoweramp: (String, Long?) -> Unit = { audioUri, positionMs ->
        val uri = audioUri.toUri()
        val played = PowerampPlaybackHelper.openToPlay(context, uri, positionMs)
        if (!played && !PowerampPlaybackHelper.openWithChooser(context, uri, chooserTitle)) {
            scope.launch { snackbarHostState.showSnackbar(playbackFailedMessage) }
        }
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
