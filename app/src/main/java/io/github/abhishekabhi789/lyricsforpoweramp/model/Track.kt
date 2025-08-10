package io.github.abhishekabhi789.lyricsforpoweramp.model

/**This data class carries track info from and to PowerAmp.*/
data class Track(
    val trackName: String = "",
    val artistName: String? = null,
    val albumName: String? = null,
    val filePath: String = "",
    val duration: Int? = null,//nullable for manual search
    val realId: Long? = null,//nullable direct search(from launcher)
    val lyrics: Lyrics? = null
) {
    companion object {
        const val KEY_TRACK_NAME = "track_name"
        const val KEY_ARTIST_NAME = "artist_name"
        const val KEY_ALBUM_NAME = "album_name"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_DURATION = "duration"
        const val KEY_REAL_ID = "real_id"
        const val KEY_LYRICS = "lyrics"
    }
}
