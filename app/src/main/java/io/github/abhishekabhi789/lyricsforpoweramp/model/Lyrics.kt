package io.github.abhishekabhi789.lyricsforpoweramp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

/**This data class represents each item from the API response.
 * @see <a href="https://lrclib.net/docs#:~:text=Soundtrack)%26duration%3D233-,Example%20response,-200%20OK%3A">LRCLIB#Example response</a>*/
@Parcelize
@Serializable
data class Lyrics(
    @SerialName("trackName") val trackName: String,
    @SerialName("artistName") val artistName: String?,
    @SerialName("albumName") val albumName: String?,
    @SerialName("plainLyrics") val plainLyrics: String?,
    @SerialName("syncedLyrics") val syncedLyrics: String?,
    @SerialName("duration") val duration: Double,
    @SerialName("instrumental") val instrumental: Boolean?,
) : Parcelable {
    /** [duration] in readable format. */
    fun getFormattedDuration(): String {
        val hours = (duration / 3600).toInt()
        val minutes = ((duration % 3600) / 60).toInt()
        val seconds = (duration % 60).toInt()
        return buildString {
            if (hours > 0) append("$hours:")
            append(String.format(Locale.US, "%02d:%02d", minutes, seconds))
        }
    }

    fun getFormatAsLrcDuration(): String {
        val minutes = (duration / 60).toInt()
        val seconds = duration.toInt() % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    companion object {
        fun makeDummyLyricsForTrack(track: Track): Lyrics {
            return Lyrics(
                trackName = track.trackName,
                artistName = track.artistName,
                albumName = track.albumName,
                plainLyrics = null,
                syncedLyrics = null,
                duration = (track.duration ?: 0).toDouble(),
                instrumental = false
            )
        }
    }
}
