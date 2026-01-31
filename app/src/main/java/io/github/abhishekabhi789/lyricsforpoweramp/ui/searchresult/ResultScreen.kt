package io.github.abhishekabhi789.lyricsforpoweramp.ui.searchresult

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.activities.EditorActivity
import io.github.abhishekabhi789.lyricsforpoweramp.activities.SearchResultActivity.Companion.TAG
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.Track
import io.github.abhishekabhi789.lyricsforpoweramp.ui.utils.rememberFolderAccess
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SearchResultViewmodel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    modifier: Modifier = Modifier,
    viewmodel: SearchResultViewmodel,
    onNavigateUp: () -> Unit,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val result by viewmodel.searchResults.collectAsState()
    val isLaunchedFromPoweramp by remember { derivedStateOf { viewmodel.powerampId != null } }
    val sendLyricsState by viewmodel.lyricsSavingState.collectAsState()
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val tagLibSession by viewmodel.tagLibSession.collectAsStateWithLifecycle()
    val filePath by viewmodel.filePath.collectAsStateWithLifecycle()
    val permissionState = rememberFolderAccess(filePath.substringBeforeLast("/"))
    var lyricsForTagEdit: Lyrics? by remember { mutableStateOf(null) }
    val preferredLyricsType by viewmodel.preferredLyricsType.collectAsStateWithLifecycle()
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.result_topbar_title),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back_action),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(400.dp),
            verticalItemSpacing = 8.dp,
            contentPadding = paddingValues,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.consumeWindowInsets(paddingValues)
        ) {
            items(items = result) { lyrics ->
                LyricItem(
                    lyrics = lyrics,
                    isLaunchedFromPowerAmp = isLaunchedFromPoweramp,
                    preferredLyricsType = preferredLyricsType,
                    onLyricChosen = { lyricsType ->
                        showBottomSheet = true
                        scope.launch {
                            viewmodel.sendLyricsToPoweramp(
                                lyrics = lyrics,
                                lyricsType = lyricsType,
                            )
                        }
                    },
                    onEditLyrics = { lyricsType ->
                        try {
                            val intent = Intent(context, EditorActivity::class.java).apply {
                                putExtra(Track.KEY_REAL_ID, viewmodel.powerampId)
                                putExtra(Track.KEY_FILE_PATH, filePath)
                                putExtra(EditorActivity.KEY_LYRICS_TYPE, lyricsType.name)
                                putParcelableArrayListExtra(
                                    Track.KEY_LYRICS, arrayListOf(lyrics)
                                )
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    onFixMetadata = {
                        lyricsForTagEdit = lyrics
                        if (permissionState.hasPermission) {
                            Log.d(TAG, "ResultScreen: permission already granted")
                            viewmodel.prepareTaglibSession(filePath)
                        } else {
                            Log.w(TAG, "ResultScreen: permission needed")
                            permissionState.requestAccess {
                                viewmodel.prepareTaglibSession(filePath)
                            }
                        }
                    }
                )
            }
        }

        if (showBottomSheet) {
            ResultBottomSheet(
                lyricsSavingState = sendLyricsState,
                onDismiss = {
                    showBottomSheet = false
                    viewmodel.clearResultState()
                },
                grantAccess = {
                    permissionState.requestAccess {
                        viewmodel.retrySend()
                    }
                },
                onFinish = onFinish
            )
        }
    }
    tagLibSession?.let { session ->
        lyricsForTagEdit?.let { lyrics ->
            BoxWithConstraints {
                FixMetadataDialog(
                    taglibSession = session,
                    lyrics = lyrics,
                    onDismiss = { viewmodel.prepareTaglibSession(null) },
                    modifier = Modifier.heightIn(
                        min = maxHeight.times(0.3f),
                        max = maxHeight.times(0.8f)
                    )
                )
            }
        }
    }
}
