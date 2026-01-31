package io.github.abhishekabhi789.lyricsforpoweramp.helpers

import android.content.Context
import android.util.Log
import com.maxmpz.poweramp.player.PowerampAPIHelper
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class LyricsSavingHelper @Inject constructor(
    private val context: Context,
    private val appPreference: AppPreference,
    private val powerampAPIHelper: PowerampApiHelper,
    private val storageHelper: StorageHelper,
    private val taglibHelper: TaglibHelper,
) {
    fun saveLyrics(
        filePath: String,
        powerampId: Long,
        lyrics: Lyrics,
        lyricsType: LyricsType,
        markInstrumental: Boolean
    ): Flow<LyricsSavingState> = flow {
        //these configs are read everytime user saving lyrics, to get the up-to date values
        val shouldSendToPA = appPreference.sendLyricsToPoweramp.first()
        val shouldSaveAsFile = appPreference.saveLyricsAsFile.first()
        val embedIntoFile = appPreference.embedLyricsIntoFile.first()
        val saveIdTagsInFile = appPreference.saveIdTagsInFile.first()

        var progress = 0.1f
        var state = LyricsSavingState(
            progress = progress,
            sendToPoweramp = LyricsSavingState.Operation(shouldPerform = shouldSendToPA),
            saveAsFile = LyricsSavingState.Operation(shouldPerform = shouldSaveAsFile),
            embedIntoFile = LyricsSavingState.Operation(shouldPerform = embedIntoFile)
        )
        emit(state)

        val totalSteps = listOf(shouldSendToPA, shouldSaveAsFile, embedIntoFile).count { it }
        val stepSize = 0.5f / totalSteps

        val lyricsText: String? = when (lyricsType) {
            LyricsType.PLAIN -> (lyrics.plainLyrics ?: lyrics.syncedLyrics)
            LyricsType.SYNCED -> (lyrics.syncedLyrics ?: lyrics.plainLyrics)
            LyricsType.INSTRUMENTAL -> PowerampApiHelper.INSTRUMENTAL_MARKING
        }
        if (lyricsText.isNullOrBlank()) {
            Log.w(TAG, "saveLyrics: lyrics content is null or blank, aborting")
            return@flow
        }
        if (shouldSendToPA) {
            var sentToPa = false
            try {
                val resultIntent = powerampAPIHelper.prepareResponseIntent(
                    context = context,
                    powerampId = powerampId,
                    lyrics = lyrics,
                    lyricsText = lyricsText,
                    markInstrumental = markInstrumental
                )
                sentToPa = PowerampAPIHelper.sendPAIntent(context, resultIntent)
            } catch (e: Exception) {
                Log.e(TAG, "saveLyrics: failed to send to PA", e)
            } finally {
                if (sentToPa) progress += stepSize
                val newState = state.sendToPoweramp.copy(result = sentToPa)
                state = state.copy(progress = progress, sendToPoweramp = newState)
                emit(state)
            }
        }
        if (lyricsType != LyricsType.INSTRUMENTAL) {
            if (shouldSaveAsFile) {
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
                val saveResult = storageHelper.writeLyricsFile(
                    context = context,
                    filePath = filePath,
                    lyricsContent = lyricsContent,
                    lyricsType = lyricsType,
                )
                Log.i(TAG, "saveLyrics: save to storage $saveResult")
                progress += stepSize
                val savedToFile = state.saveAsFile.copy(result = saveResult)
                state = state.copy(progress = progress, saveAsFile = savedToFile)
                emit(state)
            }
            if (embedIntoFile) {
                val onSessionError = { error: StorageHelper.Result ->
                    state = state.copy(
                        progress = progress,
                        embedIntoFile = state.embedIntoFile.copy(result = error)
                    )
                }
                taglibHelper.getTaglibSession(filePath, onError = onSessionError)?.use { session ->
                    session.updateLyricsTag(lyricsText)
                    session.saveModifiedFile()
                    Log.i(TAG, "saveLyrics: embedded into song tag")
                    progress += stepSize
                    state = state.copy(
                        progress = progress,
                        embedIntoFile = state.embedIntoFile.copy(result = StorageHelper.Result.SUCCESS)
                    )
                }
                emit(state)
            }
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

    companion object {
        private const val TAG = "LyricsSavingHelper"
    }
}

data class LyricsSavingState(
    val progress: Float = 0f,
    val sendToPoweramp: Operation<Boolean?> = Operation(shouldPerform = false),
    val saveAsFile: Operation<StorageHelper.Result?> = Operation(shouldPerform = false),
    val embedIntoFile: Operation<StorageHelper.Result?> = Operation(shouldPerform = false)
) {
    data class Operation<T>(val shouldPerform: Boolean = false, val result: T? = null)
}
