package io.github.abhishekabhi789.lyricsforpoweramp.helpers

import android.net.Uri
import android.util.Log
import io.github.abhishekabhi789.lyricsforpoweramp.model.LocalAudioWithoutLyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.model.Track
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

/**
 * Fills in the `.lrc` (and, when only plain lyrics exist, `.txt`) files that are missing from
 * the offline library: every audio file with no matching lyrics file gets searched on LRCLIB
 * and, on a hit, written right next to it.
 */
class BulkLyricsDownloader @Inject constructor(
    private val indexer: LocalLyricsIndexer,
    private val lrclibApiHelper: LrclibApiHelper,
    private val appPreference: AppPreference
) {

    sealed interface Progress {
        data class Scanning(val found: Int) : Progress
        data class Downloading(
            val current: Int,
            val total: Int,
            val trackTitle: String,
            val downloaded: Int,
            val skipped: Int,
            val failed: Int
        ) : Progress

        data class Done(
            val downloaded: Int,
            val skipped: Int,
            val failed: Int,
            val total: Int
        ) : Progress
    }

    suspend fun run(treeUri: Uri, onProgress: (Progress) -> Unit): Progress.Done {
        onProgress(Progress.Scanning(0))
        val pending = indexer.findAudioWithoutLyrics(treeUri)
        val preferredType = appPreference.preferredLyricsType.first()

        var downloaded = 0
        var skipped = 0
        var failed = 0

        pending.forEachIndexed { index, audio ->
            coroutineContext.ensureActive()
            onProgress(Progress.Downloading(index, pending.size, audio.title, downloaded, skipped, failed))
            when (fetchAndWrite(treeUri, audio, preferredType)) {
                Outcome.DOWNLOADED -> downloaded++
                Outcome.SKIPPED -> skipped++
                Outcome.FAILED -> failed++
            }
        }

        val done = Progress.Done(downloaded, skipped, failed, pending.size)
        onProgress(done)
        return done
    }

    private enum class Outcome { DOWNLOADED, SKIPPED, FAILED }

    private suspend fun fetchAndWrite(
        treeUri: Uri,
        audio: LocalAudioWithoutLyrics,
        preferredType: LyricsType
    ): Outcome {
        val track = Track(trackName = audio.title, artistName = audio.artist)
        val result = runCatching { lrclibApiHelper.searchLyricsForTrack(track) }.getOrElse {
            Log.e(TAG, "fetchAndWrite: search failed for ${audio.title}", it)
            return Outcome.FAILED
        }
        val lyrics = when (result) {
            is LrclibApiHelper.Result.Success -> result.data.firstOrNull()
            is LrclibApiHelper.Result.Failure -> {
                if (result.error == LrclibApiHelper.Error.NO_RESULTS) return Outcome.SKIPPED
                return Outcome.FAILED
            }
        } ?: return Outcome.SKIPPED

        val useSynced = !lyrics.syncedLyrics.isNullOrBlank() &&
                (preferredType == LyricsType.SYNCED || lyrics.plainLyrics.isNullOrBlank())
        val content = if (useSynced) lyrics.syncedLyrics else lyrics.plainLyrics
        if (content.isNullOrBlank()) return Outcome.SKIPPED

        val extension = if (useSynced) "lrc" else "txt"
        val mimeType = if (useSynced) "text/lrc" else "text/plain"
        val wrote = indexer.writeSiblingFile(
            treeUri = treeUri,
            folderDocumentId = audio.folderDocumentId,
            fileBaseName = audio.fileBaseName,
            extension = extension,
            mimeType = mimeType,
            content = content
        )
        return if (wrote) Outcome.DOWNLOADED else Outcome.FAILED
    }

    companion object {
        private const val TAG = "BulkLyricsDownloader"
    }
}
