package io.github.abhishekabhi789.lyricsforpoweramp.ui.editor

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.activities.EditorActivity.Companion.TAG
import io.github.abhishekabhi789.lyricsforpoweramp.ui.utils.FolderAccessState
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.EditorViewmodel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackControl(
    modifier: Modifier = Modifier,
    viewmodel: EditorViewmodel,
    folderAccessState: FolderAccessState
) {
    val tooltipPositionProvider = TooltipDefaults.rememberTooltipPositionProvider(
        TooltipAnchorPosition.Above
    )
    val playerInitialized by viewmodel.playerInitialized.collectAsStateWithLifecycle()
    val trackDuration by viewmodel.trackDuration.collectAsStateWithLifecycle()
    val playbackPosition by viewmodel.playbackPosition.collectAsStateWithLifecycle()
    val isPlaying by viewmodel.isPlaying.collectAsStateWithLifecycle()
    val filePath by viewmodel.filePath.collectAsStateWithLifecycle()
    LaunchedEffect(folderAccessState.hasPermission) {
        if (folderAccessState.hasPermission) {
            folderAccessState.getChildUri(filePath)?.let {
                viewmodel.setTrackUri(it)
            } ?: run {
                Log.e(TAG, "PlaybackControl: failed to get track uri")
            }
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
                onValueChange = { viewmodel.seekTo(it.toLong().times(100L)) },
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
                val newPositionCenti = playbackPosition.toLong().plus(delta).times(100)
                viewmodel.seekTo(newPositionCenti)
            }
            TooltipBox(
                state = rememberTooltipState(),
                positionProvider = tooltipPositionProvider,
                focusable = false,
                tooltip = { PlainTooltip { Text(stringResource(R.string.playback_rewind_10s)) } }
            ) {
                IconButton(onClick = { changePlayback(-10) }, enabled = playerInitialized) {
                    Icon(Icons.Default.Replay10, stringResource(R.string.playback_rewind_10s))
                }
            }
            val onPlayToggle = {
                if (folderAccessState.hasPermission)
                    viewmodel.togglePlayback(!isPlaying) else folderAccessState.requestAccess()
            }
            FilledTonalIconButton(onClick = onPlayToggle, modifier = Modifier) {
                val (icon, label) = if (isPlaying) Icons.Default.Pause to stringResource(R.string.playback_pause_button)
                else Icons.Default.PlayArrow to stringResource(R.string.playback_play_button)
                Icon(icon, label)
            }
            TooltipBox(
                state = rememberTooltipState(),
                positionProvider = tooltipPositionProvider,
                focusable = false,
                tooltip = { PlainTooltip { Text(stringResource(R.string.playback_forward_10s)) } }
            ) {
                IconButton(onClick = { changePlayback(10) }, enabled = playerInitialized) {
                    Icon(Icons.Default.Forward10, stringResource(R.string.playback_forward_10s))
                }
            }
        }
    }
}

@Composable
fun Timestamp(modifier: Modifier = Modifier, duration: Int) {
    val formattedDuration by remember(duration) {
        derivedStateOf {
            if (duration >= 0) buildString {
                append(duration / 60)
                append(":")
                append((duration % 60).let { if (it < 10) "0$it" else it.toString() })
            } else "--:--"
        }
    }
    Text(text = formattedDuration, modifier = modifier)
}
