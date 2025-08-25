package io.github.abhishekabhi789.lyricsforpoweramp.helpers

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import io.github.abhishekabhi789.lyricsforpoweramp.model.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackHelper(context: Context) {

    private val player = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(), true
        )
        .build()

    // Always interact with the player on the main thread
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _playerInitialized = MutableStateFlow(false)
    val playerInitialized: StateFlow<Boolean> = _playerInitialized.asStateFlow()

    private val _playbackSeconds = MutableStateFlow(0)
    val playbackSeconds: StateFlow<Int> = _playbackSeconds.asStateFlow()

    private val _trackDurationInSeconds = MutableStateFlow(0)
    val trackDurationInSeconds: StateFlow<Int> = _trackDurationInSeconds.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var updateJob: Job? = null

    init {
        player.addListener(object : Player.Listener {

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d(TAG, "onIsPlayingChanged: isPlaying $isPlaying")
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "onPlaybackStateChanged: newState $playbackState")
                _playerInitialized.value = playbackState != Player.STATE_IDLE
                if (playbackState == Player.STATE_READY) updateDuration()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                Log.d(TAG, "onMediaItemTransition: reason $reason, item $mediaItem")
                _playerInitialized.value = false
                updateDuration()
            }

            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                updateDuration()
            }
        })

        startUpdatingTimeFlow()
    }

    private fun updateDuration() {
        _trackDurationInSeconds.value = getDurationMs().div(1000).toInt().coerceAtLeast(0)
    }

    /** Optional: exact duration in milliseconds (0 if unknown) */
    fun getDurationMs(): Long {
        val d = player.duration
        return if (d != C.TIME_UNSET) d else 0L
    }

    fun setTrackUri(trackUri: Uri) {
        scope.launch {
            if (player.currentMediaItem?.localConfiguration?.uri.toString() == trackUri.toString()) return@launch
            _playerInitialized.value = false
            player.setMediaItem(MediaItem.fromUri(trackUri))
            player.prepare()
            // duration may be immediately available for local files
            updateDuration()
            Log.d(TAG, "setTrackUri: track uri set to ${player.currentMediaItem.toString()}")
        }
    }

    /** togglePlayback
     * @param play playWhenReady */
    fun togglePlayback(play: Boolean) {
        scope.launch {
            if (player.currentPosition < player.duration) {
                player.playWhenReady = play
            } else {
                player.seekTo(0)
                player.playWhenReady = true
            }

        }
        Log.d(TAG, "togglePlayback: playWhenReady changed to ${player.playWhenReady}")
    }

    /** Seek to a position.
     * @param ms position in milliseconds */
    fun seekTo(ms: Long) {
        scope.launch { player.seekTo(ms.coerceIn(0L, player.duration)) }
    }

    fun getCurrentTimestamp(): Timestamp {
        return Timestamp.fromMillis(player.currentPosition)
    }

    private fun startUpdatingTimeFlow() {
        // Poll on main to avoid wrong-thread access
        updateJob = scope.launch {
            while (isActive) {
                _playbackSeconds.value = (player.currentPosition / 1000L).toInt().coerceAtLeast(0)
                delay(100L)
            }
        }
    }

    /** Release resources */
    fun destroy() {
        updateJob?.cancel()
        scope.launch {
            player.removeListener(object : Player.Listener {})
            player.release()
        }
    }

    companion object {
        private const val TAG = "PlaybackHelper"
    }
}
