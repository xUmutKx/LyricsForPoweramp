package io.github.abhishekabhi789.lyricsforpoweramp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.maxmpz.poweramp.player.PowerampAPI
import io.github.abhishekabhi789.lyricsforpoweramp.model.Track

class TrackChangeReceiver(
    private val onTrackChanged: (track: Track) -> Unit,
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            PowerampAPI.ACTION_TRACK_CHANGED -> {
                Log.d(TAG, "onReceive:  track changed $intent")
                val realId = intent.getLongExtra(PowerampAPI.Track.REAL_ID, PowerampAPI.ID_NO_ID)
                val trackName = intent.getStringExtra(PowerampAPI.Track.TITLE) ?: ""
                val artistName = intent.getStringExtra(PowerampAPI.Track.ARTIST) ?: ""
                val albumName = intent.getStringExtra(PowerampAPI.Track.ALBUM) ?: ""
                val filePath = intent.getStringExtra(PowerampAPI.Track.PATH) ?: ""
                val duration = intent.getIntExtra(PowerampAPI.Track.DURATION, -1)
                onTrackChanged(
                    Track(
                        realId = realId,
                        trackName = trackName,
                        artistName = artistName,
                        albumName = albumName,
                        filePath = filePath,
                        duration = duration
                    )
                )
            }


            else -> {
                Log.d(TAG, "onReceive: receiver got another intent ${intent.action}")
            }
        }
    }

    companion object {
        private const val TAG = "TrackChangeReceiver"
        val IntentFilter = IntentFilter().apply {
            addAction(PowerampAPI.ACTION_TRACK_CHANGED)
            addAction(PowerampAPI.ACTION_STATUS_CHANGED)
        }
    }
}
