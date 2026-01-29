package io.github.abhishekabhi789.lyricsforpoweramp.helpers

import android.content.Context
import android.content.Intent
import android.util.Log
import com.maxmpz.poweramp.player.PowerampAPI
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.Track
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference.FilterType
import javax.inject.Inject

/**
 * Contains functions helping to send and receive data with PowerAmp
 */
class PowerampApiHelper @Inject constructor(private val appPreference: AppPreference) {


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
        val filter = appPreference.getFilter(field).lines()
        return filter.fold(value) { cleanedValue, filterItem ->
            try {
                cleanedValue?.replace(Regex(filterItem, RegexOption.IGNORE_CASE), "")
            } catch (e: Exception) {
                Log.w(TAG, "Invalid regex in filter for $field: '$filterItem'", e)
                cleanedValue?.replace(filterItem, "")
            }
        } ?: value
    }

    fun prepareResponseIntent(
        context: Context,
        powerampId: Long,
        lyrics: Lyrics,
        lyricsText: String,
        markInstrumental: Boolean
    ): Intent {
        val infoLine = makeInfoLine(context, lyrics)
        return Intent(PowerampAPI.Lyrics.ACTION_UPDATE_LYRICS).apply {
            putExtra(PowerampAPI.EXTRA_ID, powerampId)
            if (lyrics.instrumental == true) {
                Log.i(TAG, "sendLyrics: track is instrumental")
                if (markInstrumental) {
                    Log.d(TAG, "sendLyrics: marking as instrumental")
                    putExtra(PowerampAPI.Lyrics.EXTRA_LYRICS, INSTRUMENTAL_MARKING)
                }
            } else {
                Log.d(TAG, "sendLyrics: track is vocal")
                putExtra(PowerampAPI.Lyrics.EXTRA_LYRICS, lyricsText)
            }
            putExtra(PowerampAPI.Lyrics.EXTRA_INFO_LINE, infoLine)
        }
    }

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

    companion object {
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
    }
}
