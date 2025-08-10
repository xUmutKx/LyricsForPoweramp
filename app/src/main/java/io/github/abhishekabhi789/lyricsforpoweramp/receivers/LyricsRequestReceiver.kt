package io.github.abhishekabhi789.lyricsforpoweramp.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.maxmpz.poweramp.player.PowerampAPI
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.PowerampApiHelper
import io.github.abhishekabhi789.lyricsforpoweramp.model.Track
import io.github.abhishekabhi789.lyricsforpoweramp.workers.LyricsRequestWorker
import java.util.concurrent.TimeUnit

class LyricsRequestReceiver : BroadcastReceiver() {

    @SuppressLint("NewApi")
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            Log.d(TAG, "onReceive: received intent ${intent.action}")
            when (intent.action) {
                PowerampAPI.Lyrics.ACTION_NEED_LYRICS -> {
                    val realId = intent.getLongExtra(PowerampAPI.Track.REAL_ID, PowerampAPI.NO_ID)
                    val track = PowerampApiHelper.makeTrack(context, intent)
                    val workData = Data.Builder().run {
                        putLong(Track.KEY_REAL_ID, realId)
                        putString(Track.KEY_TRACK_NAME, track?.trackName)
                        putString(Track.KEY_ARTIST_NAME, track?.artistName)
                        putString(Track.KEY_ALBUM_NAME, track?.albumName)
                        putString(Track.KEY_FILE_PATH, track?.filePath.toString())
                        track?.duration?.let { putInt(Track.KEY_DURATION, it) }
                        build()
                    }
                    val workRequest = OneTimeWorkRequestBuilder<LyricsRequestWorker>()
                        .setInputData(workData)
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        ).build()
                    WorkManager.getInstance(context).enqueue(workRequest)
                }
            }
        }
    }


    companion object {
        private const val TAG = "LyricsRequestReceiver"
    }
}
