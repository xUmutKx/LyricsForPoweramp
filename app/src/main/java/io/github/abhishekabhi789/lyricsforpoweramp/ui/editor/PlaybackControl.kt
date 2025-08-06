package io.github.abhishekabhi789.lyricsforpoweramp.ui.editor

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxmpz.poweramp.player.PowerampAPI
import com.maxmpz.poweramp.player.PowerampAPIHelper
import com.maxmpz.poweramp.player.RemoteTrackTime
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.activities.EditorActivity.Companion.TAG
import io.github.abhishekabhi789.lyricsforpoweramp.receivers.TrackChangeReceiver
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.EditorViewmodel

@Composable
fun PlaybackControl(modifier: Modifier = Modifier, viewmodel: EditorViewmodel) {
    val context = LocalContext.current
    val playerState by viewmodel.playbackState.collectAsStateWithLifecycle()
    val trackId by viewmodel.powerampId.collectAsStateWithLifecycle()
    val sameSongIsPlaying by remember(trackId, playerState.trackId) {
        derivedStateOf { trackId == playerState.trackId }
    }
    val remoteTrackTime = remember(Unit) { RemoteTrackTime(context) }
    DisposableEffect(Unit) {
        val timeChangeListener = object : RemoteTrackTime.TrackTimeListener {
            override fun onTrackDurationChanged(duration: Int) {
                viewmodel.updateNowPlayingTrack(duration = duration)
            }

            override fun onTrackPositionChanged(position: Int) {
                viewmodel.updateNowPlayingTrack(position = position)
            }
        }
        remoteTrackTime.setTrackTimeListener(timeChangeListener)
        val receiver = TrackChangeReceiver(
            onTrackChanged = { realId, duration ->
                Log.d(TAG, "PlaybackControl: track changed realId $realId duration $duration")
                // when track changed, no staus update will be sent, so need to reset the position here.
                viewmodel.updateNowPlayingTrack(trackId = realId, duration = duration, position = 0)
            },
            onProgressUpdate = { paused, position ->
                Log.d(TAG, "PlaybackControl: paused $paused position $position")
                position?.let { remoteTrackTime.updateTrackPosition(it) }
                viewmodel.updateNowPlayingTrack(paused = paused)//timeChangeListener will update position
            }
        )
        remoteTrackTime.registerAndLoadStatus()
        ContextCompat.registerReceiver(
            context,
            receiver,
            TrackChangeReceiver.IntentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
        Log.d(TAG, "LibraryScreen: observation started")
        onDispose {
            Log.d(TAG, "LibraryScreen: disposing observers")
            context.unregisterReceiver(receiver)
            remoteTrackTime.setTrackTimeListener(null)
            remoteTrackTime.unregister()
        }
    }
    LaunchedEffect(sameSongIsPlaying, playerState.paused) {
        val updateProgress = sameSongIsPlaying && !playerState.paused
        if (updateProgress) remoteTrackTime.startSongProgress() else remoteTrackTime.stopSongProgress()
        Log.d(TAG, "PlaybackControl: monitor $updateProgress")
    }

    val togglePlayback = { play: Boolean ->
        val command =
            if (!sameSongIsPlaying) PowerampAPI.Commands.OPEN_TO_PLAY else {
                if (play) PowerampAPI.Commands.PAUSE else PowerampAPI.Commands.PLAY
            }
        val commandIntent = Intent(PowerampAPI.ACTION_API_COMMAND).apply {
            putExtra(PowerampAPI.EXTRA_COMMAND, command)
            if (!sameSongIsPlaying) {
                val uri = Uri.withAppendedPath(PowerampAPI.ROOT_URI, "files/$trackId")
                setData(uri)
            }
            setPackage(PowerampAPIHelper.getPowerampPackageName(context))
        }
        PowerampAPIHelper.sendPAIntent(context, commandIntent, true)
    }
    val onPositionChange = { timeInSeconds: Int ->
        remoteTrackTime.updateTrackPosition(timeInSeconds)
        val commandIntent = Intent(PowerampAPI.ACTION_API_COMMAND).apply {
            putExtra(PowerampAPI.EXTRA_COMMAND, PowerampAPI.Commands.SEEK)
            putExtra(PowerampAPI.Track.POSITION, timeInSeconds)
            setPackage(PowerampAPIHelper.getPowerampPackageName(context))
        }
        PowerampAPIHelper.sendPAIntent(context, commandIntent, true)
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
            Timestamp(duration = playerState.position)
            Slider(
                enabled = sameSongIsPlaying,
                value = playerState.position.toFloat(),
                onValueChange = { onPositionChange(it.toInt()) },
                valueRange = 0f..playerState.duration.toFloat(),
                modifier = Modifier.weight(1f)
            )
            Timestamp(duration = playerState.duration)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val changePlayback = { delta: Int ->
                val newPosition = playerState.position + delta
                onPositionChange(newPosition)
            }
            IconButton(onClick = { changePlayback(-10) }, enabled = sameSongIsPlaying) {
                Icon(Icons.Default.Replay10, stringResource(R.string.playback_rewind_10s))
            }
            FilledTonalIconButton(
                onClick = { togglePlayback(!playerState.paused) },
                modifier = Modifier.padding()
            ) {
                val (icon, label) = if (sameSongIsPlaying && !playerState.paused)
                    Icons.Default.Pause to stringResource(R.string.playback_pause_button)
                else Icons.Default.PlayArrow to stringResource(R.string.playback_play_button)
                Icon(icon, label)
            }
            IconButton(onClick = { changePlayback(10) }, enabled = sameSongIsPlaying) {
                Icon(Icons.Default.Forward10, stringResource(R.string.playback_forward_10s))
            }
        }
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
