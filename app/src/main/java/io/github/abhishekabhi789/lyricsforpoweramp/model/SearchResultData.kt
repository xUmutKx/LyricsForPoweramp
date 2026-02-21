package io.github.abhishekabhi789.lyricsforpoweramp.model

data class SearchResultData(
    val powerampId: Long?,
    val filepath: String?,
    val results: List<Lyrics>,
    val trackDuration: Int?
)
