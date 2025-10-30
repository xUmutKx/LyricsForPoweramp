package io.github.abhishekabhi789.lyricsforpoweramp.helpers

import android.content.Context
import android.content.Intent
import android.util.Log
import com.maxmpz.poweramp.player.PowerampAPI
import com.maxmpz.poweramp.player.PowerampAPIHelper
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.model.Track
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference.FilterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Contains functions helping to send and receive data with PowerAmp
 */
object PowerampApiHelper {

    private const val TAG = "PowerampApiHelper"
    const val INSTRUMENTAL_MARKING = "Instrumental Track" +
            """
                  .♫♫♫.
                  ♫♫♫♫'
                ♫
                ♫
                ♫
                ♫
                ♫
    ,♫♫♫♫♫
    ♫♫♫♫♫'
    `♫♫♫'
"""

    /**
     * Makes a [Track] for the intent passed by PowerAmp
     * @param intent received from PowerAmp
     * @return an instance of [Track]
     */
    fun makeTrack(context: Context, intent: Intent): Track? {
        val realId = intent.getLongExtra(PowerampAPI.Track.REAL_ID, PowerampAPI.ID_NO_ID)
        val title = intent.getStringExtra(PowerampAPI.Track.TITLE)
        if (realId == PowerampAPI.ID_NO_ID || title.isNullOrEmpty()) {
            Log.e(
                TAG,
                buildString {
                    append("makeTrack: Failed to parse details.")
                    append(" | realId: $realId")
                    append(" | title: $title")
                },
            )
        }
        val album = intent.getStringExtra(PowerampAPI.Track.ALBUM)
        val artist = intent.getStringExtra(PowerampAPI.Track.ARTIST)
        val durationMs = intent.getIntExtra(PowerampAPI.Track.DURATION, 0)
        val filePath = intent.getStringExtra(PowerampAPI.Track.PATH)?.let { path ->
            if (!path.contains(":")) path.replaceFirst("/", ":") else path
        }
        val duration: Int? = (durationMs / 1000).let { if (it == 0) null else it }
        return processField(context, FilterType.TITLE_FILTER, title)?.let {
            Track(
                trackName = it,
                artistName = processField(context, FilterType.ARTISTS_FILTER, artist),
                albumName = processField(context, FilterType.ALBUM_FILTER, album),
                duration = duration,
                filePath = filePath ?: "",
                realId = realId,
                lyrics = null
            )
        }
    }

    /**
     * Corresponding filter words will be removed from the value.
     */
    fun processField(context: Context, field: FilterType, value: String?): String? {
        val filter = AppPreference.getFilter(context, field)?.lines()
        return filter?.fold(value) { cleanedValue, filterItem ->
            try {
                cleanedValue?.replace(Regex(filterItem, RegexOption.IGNORE_CASE), "")
            } catch (e: Exception) {
                Log.w(TAG, "Invalid regex in filter for $field: '$filterItem'", e)
                cleanedValue?.replace(filterItem, "")
            }
        } ?: value
    }

    /**
     * Sends the prepared lyric data to PowerAmp.
     * @param context required for sending intent and accessing app resources
     * @param filePath track file path from poweramp
     * @param powerampId realId from poweramp.
     * @param lyrics lyrics to be returned,
     * @param lyricsType preferred lyrics type set by the user.
     * @param markInstrumental whether if mark the track as instrumental on poweramp.
     * @return [Pair]<Boolean, StorageHelper.Result> where [Boolean] represents success status for sending to Poweramp.
     * and [StorageHelper.Result] represents success status for saving to storage.
     */
    fun sendLyrics(
        context: Context,
        filePath: String,
        powerampId: Long?,
        lyrics: Lyrics,
        lyricsType: LyricsType,
        markInstrumental: Boolean = false
    ): Flow<SendLyricsState> = flow {
        var progress = 0.1f
        val shouldSendToPA = AppPreference.getSendLyricsToPoweramp(context)
        val shouldSaveAsFile = AppPreference.getSaveAsFile(context)
        val embedIntoFile = AppPreference.getEmbedLyricsAsTag(context)
        val fixMetadata = AppPreference.getFixMetadata(context)
        val saveIdTagsInFile = AppPreference.getSaveIdTagsInFile(context)

        var state = SendLyricsState(
            progress = progress,
            sendToPoweramp = SendLyricsState.Operation(shouldPerform = shouldSendToPA),
            saveAsFile = SendLyricsState.Operation(shouldPerform = shouldSaveAsFile),
            embedIntoFile = SendLyricsState.Operation(shouldPerform = embedIntoFile)
        )
        emit(state)

        val totalSteps = listOf(shouldSendToPA, shouldSaveAsFile, embedIntoFile).count { it }
        val stepSize = 0.5f / totalSteps

        val lyricsText: String? = when (lyricsType) {
            LyricsType.PLAIN -> (lyrics.plainLyrics ?: lyrics.syncedLyrics)
            LyricsType.SYNCED -> (lyrics.syncedLyrics ?: lyrics.plainLyrics)
            LyricsType.INSTRUMENTAL -> return@flow //no-op
        }
        if (shouldSendToPA) {
            val infoLine = makeInfoLine(context, lyrics)
            val intent = Intent(PowerampAPI.Lyrics.ACTION_UPDATE_LYRICS).apply {
                putExtra(PowerampAPI.EXTRA_ID, powerampId)
                if (lyrics.instrumental == true) {
                    Log.i(TAG, "sendLyricResponse: track is instrumental")
                    if (markInstrumental) {
                        Log.d(TAG, "sendLyricResponse: marking as instrumental")
                        putExtra(PowerampAPI.Lyrics.EXTRA_LYRICS, INSTRUMENTAL_MARKING)
                    }
                } else {
                    Log.d(TAG, "sendLyricResponse: track is vocal")
                    putExtra(PowerampAPI.Lyrics.EXTRA_LYRICS, lyricsText)
                }
                putExtra(PowerampAPI.Lyrics.EXTRA_INFO_LINE, infoLine)
            }

            try {
                val sent = PowerampAPIHelper.sendPAIntent(context, intent)
                Log.i(TAG, "sendLyricResponse: Success $sent")
                progress += stepSize
                val sentToPa = state.sendToPoweramp.copy(result = sent)
                state = state.copy(progress = progress, sendToPoweramp = sentToPa)
            } catch (e: Throwable) {
                Log.e(TAG, "sendLyrics: failed to send to PA", e)
                e.printStackTrace()
                val sentToPa = state.sendToPoweramp.copy(result = false)
                state = state.copy(progress = progress, sendToPoweramp = sentToPa)
            } finally {
                emit(state)
            }
        }
        if (shouldSaveAsFile) {
            if (lyricsText != null) {
                val lyricsContent = if (saveIdTagsInFile && lyricsType == LyricsType.SYNCED) {
                    buildString {
                        appendLine("[ti:${lyrics.trackName}]")
                        lyrics.artistName?.let { appendLine("[ar:$it]") }
                        lyrics.albumName?.let { appendLine("[al:$it]") }
                        appendLine("[length:${lyrics.getFormatAsLrcDuration()}]")
                        appendLine("[tool:LyricsForPoweramp]")
                        appendLine("[by:LRCLIB.net]")//Author of the LRC file (not the song)
                        appendLine()
                        appendLine(lyricsText)
                    }
                } else lyricsText
                val saveResult = StorageHelper.writeLyricsFile(
                    context = context,
                    filePath = filePath,
                    lyricsContent = lyricsContent,
                    lyricsType = lyricsType,
                )
                Log.i(TAG, "sendLyricResponse: save to storage $saveResult")
                progress += stepSize
                val savedToFile = state.saveAsFile.copy(result = saveResult)
                state = state.copy(progress = progress, saveAsFile = savedToFile)
            } else {
                Log.w(TAG, "sendLyricResponse: lyrics is null")
                val result = if (lyrics.instrumental == true)
                    StorageHelper.Result.SUCCESS else StorageHelper.Result.INVALID_LYRICS
                val savedToFile =
                    state.saveAsFile.copy(result = result)
                state = state.copy(progress = progress, saveAsFile = savedToFile)
            }
            emit(state)
        }
        if (embedIntoFile) {
            if (lyricsText != null) {
                val tagLibHelper = TaglibHelper(context)
                val filePrepared = tagLibHelper.prepareFile(filePath) { error ->
                    state = state.copy(
                        progress = progress,
                        embedIntoFile = state.embedIntoFile.copy(result = error)
                    )
                }
                progress += stepSize
                if (filePrepared) {
                    tagLibHelper.updateLyricsTag(lyricsText)
                    if (fixMetadata) {
                        if (tagLibHelper.fixMetadata(lyrics))
                            Log.i(TAG, "sendLyrics: metadata updated")
                        else Log.e(TAG, "sendLyrics: failed to update metadata")
                    }
                    tagLibHelper.saveModifiedFile()
                    Log.i(TAG, "sendLyrics: embedded into song tag")
                    state = state.copy(
                        progress = progress,
                        embedIntoFile = state.embedIntoFile.copy(result = StorageHelper.Result.SUCCESS)
                    )
                }
            } else {
                Log.w(TAG, "sendLyricResponse: lyrics is null")
                val result = if (lyrics.instrumental == true)
                    StorageHelper.Result.SUCCESS else StorageHelper.Result.INVALID_LYRICS
                val embedded =
                    state.embedIntoFile.copy(result = result)
                state = state.copy(progress = progress, saveAsFile = embedded)
            }
            emit(state)
        }

        val sent = !state.sendToPoweramp.shouldPerform || state.sendToPoweramp.result == true
        val saved =
            !state.saveAsFile.shouldPerform || state.saveAsFile.result == StorageHelper.Result.SUCCESS
        val embedded =
            !state.embedIntoFile.shouldPerform || state.embedIntoFile.result == StorageHelper.Result.SUCCESS
        if (sent && saved && embedded) {
            state = state.copy(progress = 1f)
            emit(state)
        }
    }.flowOn(Dispatchers.IO)

    private fun makeInfoLine(context: Context, lyrics: Lyrics?): String {
        return buildString {
            if (lyrics != null && lyrics.trackName.isNotEmpty()) {
                appendLine("${context.getString(R.string.input_track_title_label)}: ${lyrics.trackName}")
                lyrics.artistName?.let {
                    appendLine("${context.getString(R.string.input_track_artists_label)}: $it")
                }
                lyrics.albumName?.let {
                    appendLine("${context.getString(R.string.input_track_album_label)}: $it")
                }
                appendLine()
            }
            appendLine(context.getString(R.string.response_footer_text))
        }
    }
}

data class SendLyricsState(
    val progress: Float = 0f,
    val sendToPoweramp: Operation<Boolean?> = Operation(shouldPerform = false),
    val saveAsFile: Operation<StorageHelper.Result?> = Operation(shouldPerform = false),
    val embedIntoFile: Operation<StorageHelper.Result?> = Operation(shouldPerform = false)
) {
    data class Operation<T>(val shouldPerform: Boolean = false, val result: T? = null)
}
