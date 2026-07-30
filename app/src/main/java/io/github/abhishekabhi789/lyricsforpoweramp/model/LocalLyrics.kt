package io.github.abhishekabhi789.lyricsforpoweramp.model

import kotlinx.serialization.Serializable

/** A single lyric line of a `.lrc` file kept in the local index. */
@Serializable
data class LocalLyricsLine(
    val number: Int,
    val text: String,
    /** Playback position of this line, -1 when the line carries no timestamp. */
    val positionMs: Long = -1
)

/** One indexed `.lrc` file together with the audio file sitting next to it, if any. */
@Serializable
data class LocalLyricsEntry(
    val lrcUri: String,
    val title: String,
    val artist: String,
    val folder: String,
    val audioUri: String? = null,
    val lastModified: Long = 0L,
    val lines: List<LocalLyricsLine> = emptyList()
) {
    val hasAudio: Boolean get() = audioUri != null
}

/** Whole index of one picked folder, as stored in the cache file. */
@Serializable
data class LocalLyricsIndex(
    val treeUri: String,
    val entries: List<LocalLyricsEntry> = emptyList()
)

/** An audio file found in the picked folder that has no sibling `.lrc` yet. */
data class LocalAudioWithoutLyrics(
    val audioUri: String,
    /** Document id of the folder the audio file sits in - where the new `.lrc` gets written. */
    val folderDocumentId: String,
    val title: String,
    val artist: String?,
    val fileBaseName: String
)

/** A lyric line prepared for display - either a hit itself or a line kept for context. */
data class LocalLyricsMatchLine(
    val line: LocalLyricsLine,
    val isMatch: Boolean,
    val highlightStart: Int = -1,
    val highlightLength: Int = 0
)

/** One song that matched a query, with its hits and the lines around them. */
data class LocalLyricsMatch(
    val entry: LocalLyricsEntry,
    val hits: Int,
    val lines: List<LocalLyricsMatchLine>
) {
    /** Position to start playback from - the first line that actually matched. */
    val firstHitPositionMs: Long?
        get() = lines.firstOrNull { it.isMatch && it.line.positionMs >= 0 }?.line?.positionMs
}
