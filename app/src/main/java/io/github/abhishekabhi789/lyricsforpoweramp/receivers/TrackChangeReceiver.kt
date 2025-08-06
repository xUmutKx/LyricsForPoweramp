package io.github.abhishekabhi789.lyricsforpoweramp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.maxmpz.poweramp.player.PowerampAPI

class TrackChangeReceiver(
    private val onTrackChanged: (realId: Long?, duration: Int?) -> Unit,
    private val onProgressUpdate: (paused: Boolean, position: Int?) -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            PowerampAPI.ACTION_TRACK_CHANGED -> {
                Log.d(TAG, "onReceive:  track changed $intent")
                val realId = intent.getLongExtra(PowerampAPI.Track.REAL_ID, PowerampAPI.NO_ID)
                val duration = intent.getIntExtra(PowerampAPI.Track.DURATION, -1)
                Log.d(TAG, "onReceive: new realId $realId | new duration $duration")
                onTrackChanged(
                    realId.takeIf { it != PowerampAPI.NO_ID }, duration.takeIf { it >= 0 })
            }

            PowerampAPI.ACTION_STATUS_CHANGED -> {
                Log.d(TAG, "onReceive:  status changed $intent")
                val paused = intent.getBooleanExtra(PowerampAPI.EXTRA_PAUSED, true)
                val position = intent.getIntExtra(PowerampAPI.EXTRA_POSITION, -1)
                Log.d(TAG, "onReceive: paused $paused | position $position")
                onProgressUpdate(paused, position.takeIf { it >= 0 })
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
