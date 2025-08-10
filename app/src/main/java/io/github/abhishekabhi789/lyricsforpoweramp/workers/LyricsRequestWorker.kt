package io.github.abhishekabhi789.lyricsforpoweramp.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.maxmpz.poweramp.player.PowerampAPI
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.activities.SettingsActivity
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.LrclibApiHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.NotificationHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.PowerampApiHelper.sendLyrics
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.RequestHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.StorageHelper
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.model.Track
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

class LyricsRequestWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val mContext = applicationContext
    private var mLrclibApiHelper = LrclibApiHelper(RequestHelper.okHttpClient, RequestHelper.gson)
    private lateinit var mNotificationHelper: NotificationHelper
    private lateinit var mTrack: Track
    private var powerampTrackId = PowerampAPI.NO_ID

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = createWorkerNotification()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    override suspend fun doWork(): Result {
        mNotificationHelper = NotificationHelper(mContext)
        powerampTrackId = inputData.getLong(Track.KEY_REAL_ID, PowerampAPI.NO_ID)
        inputData.getString(Track.KEY_TRACK_NAME)?.let { trackName ->
            mTrack = Track(
                trackName = trackName,
                artistName = inputData.getString(Track.KEY_ARTIST_NAME),
                albumName = inputData.getString(Track.KEY_ALBUM_NAME),
                duration = inputData.getInt(Track.KEY_DURATION, 0).takeIf { it != 0 },
                realId = powerampTrackId,
                filePath = inputData.getString(Track.KEY_FILE_PATH) ?: ""
            )
            Log.i(TAG, "doWork: request for $mTrack")
            return handleLyricsRequest()
        }
        return Result.failure()
    }

    private suspend fun handleLyricsRequest(dispatcher: CoroutineDispatcher = Dispatchers.IO): Result {
        Log.i(TAG, "handleLyricsRequest: request for $mTrack")
        val sendToPoweramp = AppPreference.getSendLyricsToPoweramp(mContext)
        val saveToStorage = AppPreference.getSaveAsFile(mContext)
        if (!sendToPoweramp && !saveToStorage) {
            Log.e(TAG, "sendLyrics: both saving options are disabled")
            mNotificationHelper.launchSettings(
                title = getString(R.string.notification_no_saving_method_title),
                text = getString(R.string.notification_no_saving_method_description)
            )
            return Result.failure()
        }
        val preferredLyricsType = AppPreference.getPreferredLyricsType(mContext)
        var result: Result = Result.failure() // Store the result here

        return withTimeoutOrNull(POWERAMP_LYRICS_REQUEST_WAIT_TIMEOUT) {
            getLyrics(
                track = mTrack,
                lyricsType = preferredLyricsType,
                dispatcher = dispatcher,
                onSuccess = { lyrics ->
                    result = Result.success()
                    launch { sendLyrics(lyrics, preferredLyricsType) }
                },
                onError = { error ->
                    when (error) {
                        LrclibApiHelper.Error.TIMEOUT -> {
                            notifyFailure("${getString(R.string.timeout_cancelled)} - ${mTrack.trackName}")
                        }

                        LrclibApiHelper.Error.EMPTY_RESPONSE, LrclibApiHelper.Error.NO_RESULTS -> {
                            notifyFailure("${getString(error.errMsgResId)} - ${mTrack.trackName}")
                        }

                        else -> {
                            notifyFailure("${getString(error.errMsgResId)} - ${mTrack.trackName}")
                        }
                    }

                    Log.e(TAG, "handleLyricsRequest: $error")
                    result = Result.failure()
                },
            )
            result
        } ?: run {
            notifyFailure("${getString(R.string.timeout_cancelled)} - ${mTrack.trackName}")
            Log.e(TAG, "handleLyricsRequest: timeout cancelled")
            Result.retry()
        }
    }

    private suspend fun getLyrics(
        track: Track,
        lyricsType: LyricsType,
        dispatcher: CoroutineDispatcher,
        onSuccess: (Lyrics) -> Unit,
        onError: (LrclibApiHelper.Error) -> Unit
    ) = withContext(dispatcher) {
        val useFallbackMethod = AppPreference.getSearchIfGetFailed(mContext)
        Log.i(TAG, "getLyrics: fallback to search permitted- $useFallbackMethod")
        mLrclibApiHelper.getLyricsForTracks(
            track = track,
            dispatcher = dispatcher,
            onResult = onSuccess,
            onError = { error ->
                Log.e(TAG, "getLyrics: get request failed $error")
                if (useFallbackMethod && error == LrclibApiHelper.Error.NO_RESULTS) {
                    Log.i(TAG, "getLyrics: trying with search method")
                    launch {
                        mLrclibApiHelper.searchLyricsForTrack(
                            query = track,
                            dispatcher = dispatcher,
                            onResult = { results: List<Lyrics> ->
                                val lyrics = if (lyricsType == LyricsType.SYNCED) {
                                    results.firstOrNull { it.syncedLyrics != null }
                                        ?: results.firstOrNull { it.plainLyrics != null }
                                } else {
                                    results.firstOrNull { it.plainLyrics != null }
                                        ?: results.firstOrNull { it.syncedLyrics != null }
                                }
                                lyrics?.let { onSuccess(it) } ?: run {
                                    notifyFailure(
                                        getString(R.string.error_no_results, mTrack.trackName)
                                    )
                                }
                            },
                            onError = onError
                        )
                    }
                } else {
                    Log.e(TAG, "getLyrics: failed or no results, fallback not permitted")
                    onError(error)
                }
            }
        )
    }

    private suspend fun sendLyrics(lyrics: Lyrics, lyricsType: LyricsType) {
        val markInstrumental = AppPreference.getMarkInstrumental(mContext)
        sendLyrics(
            context = mContext,
            filePath = mTrack.filePath,
            powerampId = mTrack.realId,
            lyrics = lyrics,
            lyricsType = lyricsType,
            markInstrumental = markInstrumental
        ).collect { state ->
            val path = mTrack.filePath.substringBeforeLast(File.separatorChar)
            val notificationText = when (state.saveAsFile.result) {
                StorageHelper.Result.NO_PERMISSION -> getString(
                    R.string.notification_storage_missing_access_to_path, path
                )

                else -> null
            }
            notificationText?.let {
                mNotificationHelper.launchSettings(
                    title = mContext.getString(R.string.notification_storage_access_needed_title),
                    text = it,
                    extras = mapOf(SettingsActivity.EXTRA_REQUIRED_PATH to path)
                )
            }
            val completed: Boolean? =
                if (!state.sendToPoweramp.shouldPerform && !state.saveAsFile.shouldPerform) null
                else state.progress == 1f

            if (completed == true) {
                mNotificationHelper.cancelRequestNotification()
            }
        }
    }


    private fun notifyFailure(title: String) {
        val subText = getString(R.string.notification_lyrics_request_failed)
        if (::mTrack.isInitialized) {
            val content = getString(R.string.notification_manual_search_suggestion)
            mNotificationHelper.makeRequestNotification(title, content, subText, mTrack)
        } else {
            mNotificationHelper.makeRequestNotification(title = title, subText = subText)
        }
    }

    private fun createWorkerNotification(): Notification {
        val channelName = getString(R.string.lyrics_request_handling_notifications)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                WORKER_NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(applicationContext, WORKER_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.lyrics_request_handling_notifications))
            .setSmallIcon(R.drawable.app_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun getString(@StringRes resId: Int, vararg formats: String): String {
        return if (formats.isEmpty()) mContext.getString(resId)
        else mContext.getString(resId, *formats)
    }

    companion object {
        private const val TAG = "LyricsRequestWorker"
        const val MANUAL_SEARCH_ACTION =
            "io.github.abhishekabhi789.lyricsforpoweramp.MANUAL_SEARCH_ACTION"
        const val POWERAMP_LYRICS_REQUEST_WAIT_TIMEOUT = 10_000L
        private const val NOTIFICATION_ID = 1
        private const val WORKER_NOTIFICATION_CHANNEL_ID = "lyrics_request_channel"
    }
}
