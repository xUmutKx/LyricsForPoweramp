package io.github.abhishekabhi789.lyricsforpoweramp.ui.main

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.activities.EditorActivity
import io.github.abhishekabhi789.lyricsforpoweramp.activities.SearchResultActivity
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.Track
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.MainActivityViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun AppMain(modifier: Modifier = Modifier, viewModel: MainActivityViewModel) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val focusRemoverModifier = Modifier.clickable(
        interactionSource = interactionSource, indication = null
    ) {
        focusManager.clearFocus()
    }
    val inputState by viewModel.inputState.collectAsStateWithLifecycle()
    val hasRealId by remember { derivedStateOf { inputState.queryTrack.realId != null } }
    val launchedEditor = {
        val intent = Intent(context, EditorActivity::class.java).apply {
            putExtra(Track.KEY_REAL_ID, inputState.queryTrack.realId)
            putExtra(
                Track.KEY_FILE_PATH, inputState.queryTrack.filePath
            )
            putParcelableArrayListExtra(
                Track.KEY_LYRICS,
                arrayListOf(Lyrics.makeDummyLyricsForTrack(inputState.queryTrack))
            )
        }
        context.startActivity(intent)
    }
    LaunchedEffect(snackbarHostState) {
        keyboardController.let { keyboard ->
            if (snackbarHostState.currentSnackbarData == null) keyboard?.show() else keyboard?.hide()
        }
    }
    LaunchedEffect(viewModel.searchErrorFlow) {
        viewModel.searchErrorFlow.collectLatest { errMsg ->
            keyboardController?.hide()
            scope.launch {
                when (snackbarHostState.showSnackbar(
                    message = resources.getString(errMsg.errMsgResId),
                    withDismissAction = true
                )) {
                    SnackbarResult.Dismissed -> keyboardController?.show()
                    else -> {}
                }
            }
        }
    }
    LaunchedEffect(viewModel.searchResultKeyFlow) {
        viewModel.searchResultKeyFlow.collectLatest { key ->
            val intent = Intent(context, SearchResultActivity::class.java).apply {
                putExtra(SearchResultActivity.KEY_RESULT_DATA, key)
            }
            context.startActivity(intent)
        }
    }
    Scaffold(
        topBar = { TopBar() },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (hasRealId) {
                FloatingActionButton(onClick = launchedEditor) {
                    Icon(
                        Icons.Default.EditNote,
                        contentDescription = stringResource(R.string.add_custom_lyrics)
                    )
                }
            }
        },
        modifier = modifier.then(focusRemoverModifier)
    ) { paddingValues ->
        SearchUi(
            viewModel = viewModel,
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        )
    }
}
